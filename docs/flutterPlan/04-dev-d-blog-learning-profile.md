# Dev D — Blog, Learning & Profile

> **Backend status: mixed.**
> Blog (REST) ✅ fully implemented — build now.
> Learning ❌ backend stub (only a `Lesson` entity, no endpoints) — build UI on mocks.
> Profile uses the existing auth `/me` endpoints ✅.

---

## Mission

Own the content + identity surfaces: the **Blog** (articles, likes, moderation), the **Learning** experience (lessons/courses — UI-first since backend is pending), and the **Profile & Settings** screens.

## Folders you own
- `features/blog/`
- `features/learning/`
- `features/profile/`

> Profile depends on Dev A's `AuthUser` model + `authControllerProvider`. Coordinate the handoff in Week 4.

---

## Part 1 — Blog (✅ backend ready)

### Backend — `/api/v1/blog` (REST, Bearer)
| Screen / action | Endpoint |
|-----------------|----------|
| List posts | `GET /api/v1/blog?status=APPROVED&...` → `BlogListResponse` |
| Post detail | `GET /api/v1/blog/{id}` → `BlogPostResponse` |
| Create post | `POST /api/v1/blog` |
| Like / unlike | `POST /api/v1/blog/{id}/like` |
| Track view | `POST /api/v1/blog/{id}/view` |
| Moderate (MODERATOR role) | `PATCH /api/v1/blog/admin/{id}/status` |

### Work breakdown
- [ ] **Week 1:** `BlogRepository`; blog list screen (status filter, pagination); blog card
- [ ] **Week 1–2:** blog detail screen + view tracking on open
- [ ] **Week 2:** create post screen (title, content, cover URL); like/unlike with optimistic UI
- [ ] **Week 2:** moderation screen gated by `MODERATOR` role (read role from `authControllerProvider`) → `PATCH /blog/admin/{id}/status`

---

## Part 2 — Learning (❌ backend stub — UI-first)

The backend has only a `Lesson` entity (baseXp, difficulty) and a `LessonCompletedEvent`. **No controller, service, or endpoints exist yet.** When a lesson is completed server-side, the gamification pipeline already listens for `LessonCompletedEvent` and awards XP — but there's no API to trigger it from the client yet.

### Strategy
1. Design the learning UX and build it entirely against `FakeLearningRepository`.
2. Define the repository interface you *expect* the backend to fulfill (course list, lesson detail, mark-complete). Share it with the backend team as a contract proposal.
3. Ship behind a feature flag (`AppConfig.learningEnabled = false`) until endpoints land.

### Work breakdown
- [ ] **Week 3:** `LearningRepository` interface + fake; course/lesson list UI; lesson detail UI
- [ ] **Week 4–5:** lesson completion flow (UI) → on complete, call Dev C's `refreshGamification()` so XP updates
- [ ] **When backend lands:** swap fake → real; remove the flag
- [ ] Coordinate with backend on the contract: `GET /courses`, `GET /lessons/{id}`, `POST /lessons/{id}/complete` (proposed)

---

## Part 3 — Profile & Settings (✅ via auth)

### Backend — `/api/v1/auth` (REST, Bearer)
| Screen | Endpoint |
|--------|----------|
| View profile | `GET /api/v1/auth/me` → `UserProfileResponse` |
| Edit profile | `PUT /api/v1/auth/me` (displayName, bio, avatarUrl, gender, province, etc.); avatarUrl must be `/avatars/avatar-1.png` through `/avatars/avatar-30.png` |

### Work breakdown
- [ ] **Week 4:** profile screen (avatar, displayName, username, bio, province, role badge)
- [ ] **Week 4:** show the user's gamification snapshot — reuse Dev C's dashboard widget (don't reimplement; embed their widget)
- [ ] **Week 5:** edit profile screen → `PUT /auth/me`; avatar handling (`UserAvatar` shared widget)
- [ ] **Week 5:** settings (language toggle KM/EN, logout via Dev A's controller, about/version)
- [ ] Optional: show the user's own blog posts + social posts on their profile (reuse Dev D/B list widgets)

---

## Cross-feature dependencies (coordinate early)
- **Dev A:** `AuthUser` model + `authControllerProvider` (role, profile fields). Agree the model shape in Week 2 so profile isn't blocked in Week 4.
- **Dev C:** embed their gamification dashboard widget on the profile; call `refreshGamification()` after lesson completion.
- **Dev B:** optionally reuse their post-list widget for "my posts" on the profile.

---

## Definition of Done (your lane)
Standard DoD (`README.md` §8) plus:
- [ ] Learning ships behind a flag with a documented backend contract proposal
- [ ] Moderation UI is invisible to non-MODERATOR users
- [ ] Profile reuses shared/Dev C widgets rather than duplicating gamification UI
