# Telegram sign-in and sign-up

Last updated: September 15, 2026 · Backend API base: `/api/v1/auth` · Migration `V32`

Users never enter a Telegram ID. After **Continue with Telegram**, the app
offers two options. Both use the same account rule: the stable Telegram user id
either signs into its linked account or starts sign-up.

| Option | What the user does | Best for |
|---|---|---|
| **Open Telegram** (one-tap) | App opens `t.me/<bot>?start=login_<32 hex>` → bot shows **Confirm sign-in** / **Not me** → user returns, signed in | Telegram on the same phone |
| **Log in with code** | Opens the bot (or sends `/start`, `/code`) → bot replies with an 8-character code → user pastes it | Telegram on another device, links that won't open |

New Telegram users then choose a **username and display name** (prefilled from
Telegram) before the account is created.

```
Flutter                          API                              Telegram bot
───────                          ───                              ────────────
Open Telegram ── POST /telegram/start ──► requestToken + botUrl (login_<nonce>)
   open botAppUrl → botUrl ─────────────────────────────────────► /start login_<nonce>
                                          bind Telegram user ◄──  (webhook / polling)
                                          "Sign in to Riksa?"  ──► [✅ Confirm] [✖️ Not me]
   poll POST /telegram/status ──► PENDING … CONFIRMED ◄────────── callback tga:ok:<nonce>
                                  ├─ account exists → AUTHENTICATED + tokens
                                  └─ new user       → PROFILE_REQUIRED + signupToken

Log in with code                                                  /start or /code
   GET /telegram/bot ──► @handle + https://t.me/<bot>?start=code ─► reply: <pre>VE9BLZ8J</pre>
   POST /telegram/code {code} ──► AUTHENTICATED | PROFILE_REQUIRED

Profile step
   POST /telegram/complete {signupToken, username, displayName} ──► AuthResponse
```

## Security properties

- **The link cannot collect a session.** The deep-link nonce and the
  `requestToken` are separate random values. Only the app that called `/start`
  holds the token that can poll.
- **The first Telegram account to open a link owns it.** A second account
  sees "already opened by a different Telegram account", and Confirm/Not me
  only works for the bound user.
- **Explicit confirmation.** The prompt tells users to confirm only if they
  tapped *Open Telegram* themselves, which counters "click my link" phishing.
- **Codes:** 8 chars from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no 0/O/1/I),
  single use, 5 min TTL (`TELEGRAM_LOGIN_CODE_EXPIRATION_MINUTES`), at most 5 per
  Telegram user per 10 min, `/telegram/code` rate-limited to 10 per IP per 15 min.
  Codes and nonces are stored only as HMACs (`otp.hash-secret`).
- **Single issuance.** Tokens are issued once per challenge: a row lock in
  Postgres, or a `SET NX` claim in Redis.
- **A taken username does not burn sign-up.** It is checked before claiming,
  so the user just picks another name within the 15-minute profile window.

## API contract

All bodies are JSON. Never log `requestToken`, `signupToken`, codes, or JWTs.

### `GET /telegram/bot`

```json
{ "available": true, "botUsername": "riksa_bot",
  "botUrl": "https://t.me/riksa_bot?start=code",
  "botAppUrl": "tg://resolve?domain=riksa_bot&start=code", "codeLength": 8 }
```

`available: false` (other fields null) when Telegram is disabled or not configured.

### `POST /telegram/start`

No body. `409` if Telegram is disabled or misconfigured.

```json
{
  "requestToken": "43-char base64url secret — keep in memory only",
  "botUrl": "https://t.me/riksa_bot?start=login_841f6cb4701d56f61cd1b2fea1f133f8",
  "botAppUrl": "tg://resolve?domain=riksa_bot&start=login_841f6cb4701d56f61cd1b2fea1f133f8",
  "botUsername": "riksa_bot",
  "expiresAt": "2026-09-15T10:10:00+07:00",
  "expiresInSeconds": 600,
  "pollIntervalSeconds": 3
}
```

Open `botAppUrl` first (not on web), then fall back to `botUrl`.

### `POST /telegram/status` `{ "requestToken": "…" }`

### `POST /telegram/code` `{ "code": "VE9B-LZ8J" }`

Codes are case-insensitive, and spaces or dashes are ignored. Both endpoints return a
`TelegramAuthResult`, with null fields omitted:

| `status` | Fields | App behaviour |
|---|---|---|
| `PENDING` | `expiresAt` | keep polling (status only) |
| `AUTHENTICATED` | `auth` = `AuthResponse` | store tokens, load `/me` |
| `PROFILE_REQUIRED` | `signupToken`, `telegram: {displayName, username}`, `expiresAt` | show profile form |
| `DENIED` | none | "declined in Telegram", start again |
| `EXPIRED` | none | expired or already used, start again |

Errors: `400` malformed token, or unknown/expired/used code; `403` suspended
account; `410` a concurrent request already consumed it; `429` rate limit.

### `POST /telegram/complete`

```json
{ "signupToken": "…", "username": "sophea", "displayName": "Sok Sophea" }
```

`200` `AuthResponse` · `409` "Username is already taken" (token still valid) ·
`410` sign-up window expired or already used (restart) · `400` validation.

### `POST /telegram/link` (Bearer)

`{ "requestToken": "…" }` for a **confirmed** one-tap request, or `{ "code": "…" }`.
Returns `200 {message}`. Returns `409` if that Telegram account belongs to another user.

### `POST /telegram/webhook` (Telegram only)

The server checks the `X-Telegram-Bot-Api-Secret-Token` header. It accepts
private-chat `message` and `callback_query` updates.

Legacy `POST /telegram`, `/otp/send`, and `/otp/verify` are unchanged and unused
by the app.

## Bot commands

| Input | Reply |
|---|---|
| `/start login_<nonce>` | Confirmation prompt with **✅ Confirm sign-in** / **✖️ Not me** |
| `/start`, `/start code`, `/code`, `/login` | Welcome + sign-in code (`<pre>` block, tap to copy), expiry, "never share", how to paste it |
| `/help` or any text | Explains both options |

`setMyCommands` registers `/code` and `/help` on startup. Set `TELEGRAM_APP_URL`
(https) to add an **📲 Open Riksa** button under code messages.

## Configuration

```dotenv
TELEGRAM_ENABLED=true
TELEGRAM_BOT_TOKEN=<BotFather token>
TELEGRAM_BOT_USERNAME=riksa_bot
TELEGRAM_AUTH_REQUEST_EXPIRATION_MINUTES=10
TELEGRAM_LOGIN_CODE_EXPIRATION_MINUTES=5
TELEGRAM_APP_URL=

# Production: public HTTPS webhook
TELEGRAM_WEBHOOK_URL=https://api.example.com/api/v1/auth/telegram/webhook
TELEGRAM_WEBHOOK_SECRET=<openssl rand -hex 32>

# Local development: no tunnel needed; leave TELEGRAM_WEBHOOK_URL empty
TELEGRAM_POLLING_ENABLED=true
```

In polling mode the API deletes the bot's webhook and long-polls `getUpdates`.
Don't run polling against a bot that a deployed environment also uses, because
Telegram delivers each update to only one consumer.

## Acceptance checklist

- [ ] Open Telegram (app installed): `tg://` opens the bot; Confirm signs in; app resumes and signs in without waiting.
- [ ] Open Telegram (no app): `https://t.me` fallback works; Copy link works on another device.
- [ ] Not me → app shows "declined"; letting it expire → "expired".
- [ ] Link opened by a second Telegram account is refused.
- [ ] First bot contact (`Start`) returns a code; pasting it signs in or goes to the profile step.
- [ ] Reusing a code fails; the 6th `/code` within 10 min is refused in the bot.
- [ ] New user: taken username shows inline and can be changed; account created once.
- [ ] Later sign-in (either option) returns the same `userId`.
- [ ] No token, code, or secret appears in logs.
