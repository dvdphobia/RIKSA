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

Sign-in requires a backend account with role `ADMIN` or `MODERATOR`. Regular `USER` accounts cannot access the panel.

## Pages and access

All pages below are available to both administrators and moderators unless marked **ADMIN only**. The backend also enforces these roles.

| Route | Page | Access |
|---|---|---|
| `/` | Dashboard and operational stats | ADMIN, MODERATOR |
| `/users` | User search and account controls | ADMIN only |
| `/lessons` | Lessons, exercises, bulk import, and markdown upload | ADMIN, MODERATOR |
| `/categories` | Lesson categories | ADMIN, MODERATOR |
| `/roadmap` | Roadmap units and lesson ordering | ADMIN, MODERATOR |
| `/resources` | Lesson resource review and moderation | ADMIN, MODERATOR |
| `/reports` | Report queue and resolution | ADMIN, MODERATOR |
| `/blog` | Blog moderation queue | ADMIN, MODERATOR |
| `/moderation` | Post and comment moderation | ADMIN, MODERATOR |
| `/gamification` | Achievements, ranks, streaks, and XP boosts | ADMIN only |
| `/leaderboard` | Leaderboard snapshots | ADMIN only |
| `/market` | Market item catalog | ADMIN only |
| `/platform` | XP rules, app flags, announcements, AI chat, and banned words | ADMIN only |
| `/audit-logs` | Filterable admin audit trail | ADMIN only |

## Implementation notes

- API requests use bearer JWTs and automatic refresh on `401` responses.
- Server tables use Spring pagination; reports use cursor pagination.
- Lesson bodies are edited as markdown for the mobile app renderer. Existing markdown is preserved until the body is edited.
