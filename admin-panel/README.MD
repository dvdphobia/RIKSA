# Riksa Admin Panel

The admin panel is the React web interface for managing Riksa content, moderation, users, gamification, market items, and platform settings. It lives in this monorepo and calls the Spring API under `/api/v1`.

## Run locally

From `admin-panel/`:

```bash
npm ci
npm run dev
npm run build
```

Vite serves the development panel at `http://localhost:5173`. Set `VITE_API_URL` to the API base URL; the default is `http://localhost:8080/api/v1`. See [`admin-panel/.env.example`](.env.example) for the environment variable example.

Sign-in uses the Telegram authentication flow (`/api/v1/auth/telegram/*` — one-tap bot confirmation or 8-character bot code) and requires a linked backend account with role `ADMIN`. Non-admin accounts (`USER`, `MODERATOR`, or unlinked Telegram accounts) are rejected at login.

## Pages and access

All pages require an authenticated `ADMIN` session.

| Route | Page | Access |
|---|---|---|
| `/` | Dashboard and operational stats | ADMIN |
| `/users` | User search and account controls | ADMIN |
| `/lessons` | Lessons, exercises, bulk import, and markdown upload | ADMIN |
| `/categories` | Lesson categories | ADMIN |
| `/roadmap` | Roadmap units and lesson ordering | ADMIN |
| `/resources` | Lesson resource review and moderation | ADMIN |
| `/reports` | Report queue and resolution | ADMIN |
| `/blog` | Blog moderation queue | ADMIN |
| `/moderation` | Post and comment moderation | ADMIN |
| `/gamification` | Achievements, ranks, streaks, and XP boosts | ADMIN |
| `/leaderboard` | Leaderboard snapshots | ADMIN |
| `/market` | Market item catalog | ADMIN |
| `/platform` | XP rules, app flags, announcements, AI chat, and banned words | ADMIN |
| `/audit-logs` | Filterable admin audit trail | ADMIN |

## Implementation notes

- Authentication reuses the backend Telegram challenge flow (`GET /auth/telegram/bot`, `POST /auth/telegram/start`, `POST /auth/telegram/status`, and `POST /auth/telegram/code`) and validates that the resolved user has `role === "ADMIN"`.
- API requests use bearer JWTs and automatic refresh on `401` responses.
- Server tables use Spring pagination; reports use cursor pagination.
- Lesson bodies are edited as markdown for the mobile app renderer. Existing markdown is preserved until the body is edited.
