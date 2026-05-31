# Riksa Auth API Documentation

Base URL: `http://localhost:8080`

All auth endpoints are **public** (no JWT token required). All other endpoints require a valid JWT in the `Authorization` header.

---

## Authentication Flow

```
┌─────────────┐     POST /api/auth/register      ┌──────────────┐
│   Client     │ ──────────────────────────────►  │   Server      │
│              │ ◄──────────────────────────────  │              │
│              │     { accessToken, refreshToken } │              │
│              │                                   │              │
│   (later)    │     POST /api/auth/login          │              │
│              │ ──────────────────────────────►  │              │
│              │ ◄──────────────────────────────  │              │
│              │     { accessToken, refreshToken } │              │
│              │                                   │              │
│   (token     │     POST /api/auth/refresh        │              │
│    expired)   │ ──────────────────────────────►  │              │
│              │ ◄──────────────────────────────  │              │
│              │     { new accessToken, new refre } │              │
│              │                                   │              │
│   (logout)   │     POST /api/auth/logout          │              │
│              │ ──────────────────────────────►  │              │
└─────────────┘                                   └──────────────┘
```

**Token lifetimes:**
- **Access token**: 15 minutes (`900000ms`)
- **Refresh token**: 30 days (`2592000000ms`)

---

## Endpoints

### 1. Register

```
POST /api/auth/register
```

Creates a new user account and returns JWT tokens.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `username` | String | Yes | 3-50 chars, unique |
| `displayName` | String | Yes | max 100 chars |
| `email` | String | Yes | valid email, unique |
| `password` | String | Yes | 6-160 chars |
| `phoneNumber` | String | No | unique if provided |
| `gender` | String | No | — |
| `provinceId` | UUID | No | must exist in provinces table |

**Example Request:**

```json
{
  "username": "john_doe",
  "displayName": "John Doe",
  "email": "john@example.com",
  "password": "securepass123",
  "phoneNumber": "+85512345678",
  "gender": "male"
}
```

**Success Response (200):**

```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "john_doe",
  "role": "USER"
}
```

**Error Responses (400):**

| Scenario | Message |
|---|---|
| Duplicate username | `"Username is already taken"` |
| Duplicate email | `"Email is already taken"` |
| Missing required fields | `"username: Username is required; password: Password is required; ..."` |

---

### 2. Login

```
POST /api/auth/login
```

Authenticates an existing user and returns JWT tokens.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `username` | String | Yes | non-blank |
| `password` | String | Yes | non-blank |

**Example Request:**

```json
{
  "username": "john_doe",
  "password": "securepass123"
}
```

**Success Response (200):**

```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "john_doe",
  "role": "USER"
}
```

**Error Response (400):**

```json
{
  "timestamp": "2026-05-31T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid username or password"
}
```

> Note: The same error message is returned for both wrong username and wrong password to prevent user enumeration.

---

### 3. Refresh Token

```
POST /api/auth/refresh
```

Exchanges a valid refresh token for a new pair of access + refresh tokens. The old refresh token remains valid until it expires, but a new one is also issued.

**Request Body:**

| Field | Type | Required |
|---|---|---|
| `refreshToken` | String | Yes |

**Example Request:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Success Response (200):**

```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...(new)",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...(new)",
  "tokenType": "Bearer",
  "username": "john_doe",
  "role": "USER"
}
```

**Error Response (400):**

```json
{
  "message": "Invalid or expired refresh token"
}
```
or
```json
{
  "message": "Refresh token not found or revoked"
}
```

---

### 4. Logout

```
POST /api/auth/logout
```

Revokes a refresh token so it can no longer be used. The access token remains valid until it expires naturally (15 min).

**Request Body:**

| Field | Type | Required |
|---|---|---|
| `refreshToken` | String | Yes |

**Example Request:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Success Response (200):**

```json
{
  "message": "Logged out successfully"
}
```

---

### 5. OTP Send

```
POST /api/auth/otp/send
```

Sends a one-time verification code via the specified channel. For **SMS**, the code is returned in the response (SMS provider not yet connected). For **TELEGRAM**, the code is sent to the user's Telegram chat and also returned in the response.

**Request Body:**

| Field | Type | Required | Description |
|---|---|---|---|
| `identifier` | String | Yes | Phone number (SMS) or Telegram chat ID (TELEGRAM) |
| `channel` | String | Yes | `"SMS"` or `"TELEGRAM"` |

**Example — Send via Telegram:**

```json
{
  "identifier": "1081239995",
  "channel": "TELEGRAM"
}
```

**Example — Send via SMS:**

```json
{
  "identifier": "+85512345678",
  "channel": "SMS"
}
```

**Success Response (200):**

```json
{
  "message": "OTP sent successfully",
  "channel": "TELEGRAM",
  "code": "931293"
}
```

**Error Response (400):**

```json
{
  "message": "channel: Channel is required (SMS or TELEGRAM); identifier: Identifier is required (phone number or Telegram chat ID)"
}
```

> **Important for Telegram**: The user must have started a conversation with the bot (`@riksaUnique_bot`) before the bot can send them messages. The `identifier` for Telegram is the user's chat ID (a numeric string like `1081239995`). You can get a user's chat ID by having them message `@userinfobot` on Telegram.

**OTP Rules:**
- Code is 6 digits
- Expires in 5 minutes
- Maximum 3 verification attempts per code
- Only the most recent unverified code is valid

---

### 6. OTP Verify

```
POST /api/auth/otp/verify
```

Verifies an OTP code that was previously sent.

**Request Body:**

| Field | Type | Required | Description |
|---|---|---|---|
| `identifier` | String | Yes | Same identifier used when sending |
| `channel` | String | Yes | Same channel used when sending (`"SMS"` or `"TELEGRAM"`) |
| `code` | String | Yes | The 6-digit OTP code |

**Example Request:**

```json
{
  "identifier": "1081239995",
  "channel": "TELEGRAM",
  "code": "931293"
}
```

**Success Response (200):**

```json
{
  "verified": true,
  "message": "Verification successful"
}
```

**Wrong Code Response (200):**

```json
{
  "verified": false,
  "message": "Invalid OTP code"
}
```

**Error Responses (400):**

| Scenario | Message |
|---|---|
| No pending OTP | `"No pending OTP for this identifier"` |
| OTP expired | `"OTP has expired"` |
| Too many attempts | `"Too many attempts. Please request a new OTP."` |

---

## Using JWT Tokens

After login or register, include the access token in the `Authorization` header for all protected endpoints:

```
Authorization: Bearer <accessToken>
```

**Example:**

```
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

If the access token expires (after 15 minutes), use the refresh endpoint to get a new one before making more requests.

---

## Error Format

All errors follow a consistent format:

```json
{
  "timestamp": "2026-05-31T10:00:00+07:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Description of what went wrong"
}
```

| HTTP Status | When |
|---|---|
| `400` | Validation errors, duplicate user, invalid credentials, OTP errors |
| `500` | Unexpected server error |

---

## Postman Collection

### Environment Variables

Set these in your Postman environment:

| Variable | Value |
|---|---|
| `base_url` | `http://localhost:8080` |
| `access_token` | (set automatically after login) |
| `refresh_token` | (set automatically after login) |

### Request Examples

**1. Register**

- Method: `POST`
- URL: `{{base_url}}/api/auth/register`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "username": "testuser1",
  "displayName": "Test User",
  "email": "test1@example.com",
  "password": "password123",
  "phoneNumber": "+85512345678",
  "gender": "male"
}
```

**2. Login**

- Method: `POST`
- URL: `{{base_url}}/api/auth/login`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "username": "testuser1",
  "password": "password123"
}
```

**3. Send OTP (Telegram)**

- Method: `POST`
- URL: `{{base_url}}/api/auth/otp/send`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "identifier": "YOUR_TELEGRAM_CHAT_ID",
  "channel": "TELEGRAM"
}
```

**4. Verify OTP**

- Method: `POST`
- URL: `{{base_url}}/api/auth/otp/verify`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "identifier": "YOUR_TELEGRAM_CHAT_ID",
  "channel": "TELEGRAM",
  "code": "THE_CODE_YOU_RECEIVED"
}
```

**5. Refresh Token**

- Method: `POST`
- URL: `{{base_url}}/api/auth/refresh`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "refreshToken": "{{refresh_token}}"
}
```

**6. Logout**

- Method: `POST`
- URL: `{{base_url}}/api/auth/logout`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "refreshToken": "{{refresh_token}}"
}
```

---

## Telegram Bot Setup

The Telegram OTP channel uses a bot to send verification codes. To set up:

1. Create a bot via `@BotFather` on Telegram and get the bot token
2. Get your Telegram chat ID by messaging `@userinfobot`
3. Start a conversation with your bot (send `/start`) — bots cannot message users who haven't initiated contact
4. Configure in `.env`:
   ```
   TELEGRAM_ENABLED=true
   TELEGRAM_BOT_TOKEN=<your-bot-token>
   TELEGRAM_BOT_USERNAME=<your-bot-username>
   ```

---

## Database Schema (Auth-related)

### `users` table

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, auto-generated |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL |
| `display_name` | VARCHAR(100) | NOT NULL |
| `avatar_url` | TEXT | default random avatar |
| `bio` | VARCHAR(160) | |
| `gender` | VARCHAR(20) | |
| `role` | enum "Role" | NOT NULL, default 'USER' |
| `phone_number` | VARCHAR(20) | UNIQUE |
| `phone_verified` | BOOLEAN | NOT NULL, default false |
| `password` | TEXT | (null for Google-only users) |
| `google_id` | TEXT | UNIQUE |
| `email` | TEXT | UNIQUE |
| `country_id` | VARCHAR(2) | default 'KH' |
| `province_id` | UUID | FK → provinces |
| `created_at` | TIMESTAMPTZ | auto |
| `updated_at` | TIMESTAMPTZ | auto |

### `refresh_tokens` table

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK |
| `token` | TEXT | NOT NULL |
| `user_id` | UUID | FK → users, CASCADE |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `revoked` | BOOLEAN | NOT NULL, default false |
| `created_at` | TIMESTAMPTZ | auto |

### `otp_codes` table

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PK |
| `identifier` | VARCHAR(100) | NOT NULL (phone number or Telegram chat ID) |
| `channel` | enum "OtpChannel" | NOT NULL (`SMS` or `TELEGRAM`) |
| `code` | VARCHAR(6) | NOT NULL |
| `attempts` | INTEGER | NOT NULL, default 0 |
| `verified` | BOOLEAN | NOT NULL, default false |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `created_at` | TIMESTAMPTZ | auto |
