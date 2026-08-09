# Riksa Admin

Web admin panel for the Riksa Learning Platform, built with [Refine](https://refine.dev) (React) + shadcn/ui + Tailwind.

Implements every endpoint documented in [`riksa_app/ADMIN_API.md`](../riksa_app/ADMIN_API.md).

## Stack

- Refine v5 (headless) + React Router v7
- shadcn/ui + Tailwind CSS
- TanStack Table / React Query
- Recharts (dashboard charts)

## Setup

```bash
npm install
npm run dev        # http://localhost:5173
```

## Configuration

`.env`:

```
VITE_API_URL=http://localhost:8080/api/v1
```

Login requires an account with role `ADMIN` or `MODERATOR`. `USER` accounts are rejected.

## Modules

| Route | Module | Access |
|---|---|---|
| `/` | Dashboard (stats, charts, hardest exercises) | MODERATOR+ |
| `/users` | Search, promote, suspend/unsuspend | ADMIN only |
| `/lessons` | CRUD, bulk JSON import, markdown upload | MODERATOR+ |
| `/categories` | CRUD | MODERATOR+ |
| `/reports` | Report queue (cursor pagination), resolve/dismiss | MODERATOR+ |
| `/blog` | Approve / reject queue | MODERATOR+ |
| `/moderation` | Hide / restore posts & comments | MODERATOR+ |
| `/audit-logs` | Filterable audit trail | ADMIN only |

Users and Audit logs are hidden from the sidebar for `MODERATOR` accounts (enforced by the access-control provider and the backend).

## Notes

- Auth: JWT bearer with automatic single-flight refresh on `401` (`src/lib/api.ts`).
- Pagination: Refine's 1-based pages are mapped to Spring's 0-based `page` param in `src/providers/data-provider.ts`; reports use cursor pagination via an infinite query.
- Lesson answer keys are never returned by the API — the edit form keeps existing exercises unless "Replace exercises" is used.
