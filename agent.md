# Riksa Learning Platform API — Agent Guide

## Project Overview

Riksa is a Cambodian learning platform API built with Spring Boot. The backend serves a future Flutter mobile app (REST for most domains, GraphQL for social/community features).

**Current status:** Auth module fully implemented. Database schema exists for all domains (`V1__initial_schema.sql`). Learning, Gamification, Leaderboard, Social, Blog, and AI Chat domains need Java backend implementation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Build | Maven 3.9.16 (w/ wrapper) |
| ORM | Spring Data JPA / Hibernate |
| DB | PostgreSQL 18 |
| Migrations | Flyway |
| Auth | JWT (jjwt 0.12.6), BCrypt, Google OAuth |
| OTP | Telegram bot integration |
| Validation | `spring-boot-starter-validation` (`@Valid`, `@NotBlank`, etc.) |
| Caching | `spring-boot-starter-cache` |
| Monitoring | Spring Actuator |
| Testing | JUnit 5 + Mockito (no `@SpringBootTest`, pure unit tests) |
| Config | `.env` via `spring-dotenv` |

---

## Architecture & Conventions

### Package-by-Feature

```
com.riksa
├── auth/          # Authentication module (implemented)
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── google/
│   ├── otp/
│   ├── repository/
│   ├── security/
│   ├── service/
│   ├── telegram/
│   └── token/
├── learning/          # Future: lessons, exercises, completions
├── gamification/      # Future: XP, streaks, achievements
├── leaderboard/       # Future: rankings, snapshots
├── social/            # Future: posts, comments, reactions (GraphQL)
├── blog/              # Future: blog posts, likes, views
├── chat/              # Future: AI chat sessions, messages
└── common/            # Cross-cutting (implemented)
    ├── config/
    ├── exception/
    ├── filter/
    └── scheduler/
```

### API Style

- **REST** for all domains **except Social** (posts, comments, reactions, shares → GraphQL)
- Base path: `/api/{domain}` (e.g. `/api/auth`, `/api/lessons`, `/api/gamification`)
- Stateless, JWT-based auth for all endpoints except `/api/auth/**` and `/actuator/health`
- JSON request/response bodies
- Error responses follow the pattern from `GlobalExceptionHandler`:
  ```json
  { "timestamp": "...", "status": 400, "error": "Bad Request", "message": "..." }
  ```

### Coding Patterns

- **Lombok**: `@RequiredArgsConstructor`, `@Slf4j`, `@Getter`/`@Setter` on DTOs. Avoid `@Data` on JPA entities.
- **DTO Mapping**: Manual mapping in service layer (no MapStruct). See `AuthService.mapToProfile()`.
- **Validation**: `@Valid` on controller request bodies. Use `jakarta.validation` constraints (`@NotBlank`, `@Email`, `@Size`, `@Pattern`).
- **Exceptions**: Business logic errors → `IllegalArgumentException` (handled as 400 by `GlobalExceptionHandler`). Avoid custom exception classes unless needed.
- **Transactions**: `@Transactional` on service methods. `@Transactional(readOnly = true)` for queries.
- **Entity IDs**: UUID with `uuid_generate_v4()` at DB level.
- **Timestamps**: `OffsetDateTime` in Java, `TIMESTAMPTZ` in PostgreSQL. `updated_at` auto-updated via DB trigger.
- **Environment config**: Values via `@Value("${...}")` in services. All secrets/config in `.env`, not in code.

### Security Conventions

- `SecurityConfig` defines `SecurityFilterChain` with JWT + RateLimit filters
- New domain endpoints must be added to the `.anyRequest().authenticated()` block (no explicit permitAll unless public)
- Rate limiting already applied to `/api/auth/**` (5 req/60s per IP)
- User identity from JWT: `jwtService.getUserId(token)` in controllers, or Spring Security context in services

### Database Conventions

- **All schema changes must be Flyway migrations** (JPA `ddl-auto: validate` — never auto-create)
- Follow existing naming: `snake_case` table/column names, quoted identifiers (`"users"`)
- New migrations: `V{next}__{description}.sql`
- Add both table creation and performance indexes (see existing migrations for pattern)
- All tables should have `created_at` and `updated_at` with the trigger

### Testing Conventions

- **Pure unit tests** with `@ExtendWith(MockitoExtension.class)` — no `@SpringBootTest`
- `@Mock` for dependencies, `@InjectMocks` for service under test
- `assertThat` from AssertJ (fluent assertions)
- `assertThatThrownBy(...)` for exception testing
- Test naming: `method_scenario_expectedBehavior` (snake_case)

---

## Domain Roadmap

### ✅ Implemented

| Domain | Status |
|---|---|
| Auth (register, login, JWT refresh, logout, Google OAuth, OTP, email verification, profile CRUD, password reset, account lockout, rate limiting) | Fully implemented |

### 📋 Database Schema Defined — Needs Backend

| Domain | Key Tables |
|---|---|
| **Learning** | `lessons`, `exercises`, `lesson_completions` |
| **Gamification** | `xp_transactions`, `user_gamification`, `streak_milestones`, `rank_tiers`, `rank_history`, `achievements`, `user_achievements` |
| **Leaderboard** | `leaderboard_snapshots` |
| **Social** | `posts`, `comments`, `reactions`, `shares`, `user_category_memberships`, `categories` |
| **Blog** | `blog_posts`, `blog_views`, `blog_likes` |
| **AI Chat** | `chat_sessions`, `chat_messages` |

---

## Flutter Integration Notes

- **REST API** for auth, learning, gamification, leaderboard, blog, and chat
- **GraphQL** for social/community features (posts, comments, reactions, shares — needs richer querying)
- Standard JSON serialization, no nested resource objects (flat DTOs)
- All authenticated requests include `Authorization: Bearer <accessToken>`
- Token refresh via `POST /api/auth/refresh` with `{ "refreshToken": "..." }`

---

## Running the Project

```bash
# Run tests
./mvnw test

# Build
./mvnw clean package

# Run locally (requires PostgreSQL + .env)
./mvnw spring-boot:run

# Docker (PostgreSQL + app)
docker-compose up --build
```


  git config --global user.email "l1gend@outlook.com"
  git config --global user.name "DVDphobia"