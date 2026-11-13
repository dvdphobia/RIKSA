# RIKSA

Riksa backend — the Spring Boot REST and GraphQL API serving the Flutter student app and React admin panel.

## Documentation

Start with [docs/README.md](docs/README.md) for active API contracts, setup, deployment, and design references.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Build | Maven 3.9.16 (wrapper included) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL 17 |
| Ephemeral data | Redis 8.2 |
| Migrations | Flyway |
| Auth | JWT, BCrypt, Google OAuth |
| OTP | Telegram bot integration |
| Caching | Spring Cache backed by Redis |
| Monitoring | Spring Actuator |

## Project Status

The backend includes authentication, learning, gamification, leaderboard, social, blog, AI tutor, buddy, market, quests, notifications, moderation, media, resources, and admin features. The Flutter student app is in the sibling RIKSA-FLUTTER repository; the React admin panel is in this repository under admin-panel/. See [docs/README.md](docs/README.md) for the active API and design references; verify exact behavior against the controllers and migrations.

## Prerequisites

- **Java 21 JDK**
- **Maven 3.9+** (or use `./mvnw` wrapper)
- **Docker + Docker Compose plugin** (for PostgreSQL and Redis)

### Install on Arch Linux

```bash
# Java 21
sudo pacman -S jdk21-openjdk
sudo archlinux-java set java-21-openjdk

# Verify
java -version

# Docker
curl -fsSL https://get.docker.com | sh
# Or install Docker Desktop for Linux from https://www.docker.com/products/docker-desktop/
```

## Setup

1. Clone the repository and enter the project directory.
2. Copy `.env.example` to `.env` and review the values:

```bash
cp .env.example .env
```

3. Make sure PostgreSQL password and JWT secret are set. Example `.env` for local development:

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=riksa_db
DB_USER=riksa_user
DB_PASSWORD=riksaPass123

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=riksaRedis123
REDIS_KEY_PREFIX=riksa:dev

JWT_SECRET=your-secure-random-secret-at-least-256-bits
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=2592000000

GOOGLE_CLIENT_ID=
TELEGRAM_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=

CORS_ALLOWED_ORIGINS=http://localhost:3000
```

## Running the Project

### Recommended: Local App + PostgreSQL and Redis in Docker

Fastest development workflow with hot reload and easy debugging.

**Terminal 1 — start PostgreSQL and Redis:**

```bash
docker compose up -d postgres redis
```

**Terminal 2 — start the Spring Boot app:**

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

> `set -a` exports the variables from `.env` into the current shell so Spring Boot can read them. If `spring-dotenv` is not picking up the file automatically during `spring-boot:run`, this command is required.

Verify:

```bash
curl http://localhost:8080/actuator/health
```

Expected output:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

### Alternative: Full Docker Stack

Builds the app inside Docker and runs the API, PostgreSQL, and Redis together.

```bash
docker compose up --build -d
```

Access the app at `http://localhost:8080`.

## Common Commands

```bash
# Compile
./mvnw clean compile

# Build JAR
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Stop all Docker services

docker compose down

# Stop only the app container (keep Postgres running)
docker compose stop app

# View app logs
docker compose logs -f app
```

## Docker Troubleshooting

### `docker-compose: command not found`

Modern Docker uses the Compose plugin. Use:

```bash
docker compose up --build -d
```

Or create a shell alias:

```bash
alias docker-compose='docker compose'
```

### Docker Desktop service not found on Arch

If `systemctl --user start docker-desktop` fails, Docker Desktop may not be installed. Either install the `docker-desktop` package from the Docker website, or use the standard Docker Engine:

```bash
sudo pacman -S docker docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
# Log out and back in for group changes
```

### `wget: Failed to fetch` during Docker build

If the Maven wrapper cannot download Maven inside the container, the `Dockerfile` already uses the pre-built `maven:3.9.16-eclipse-temurin-21` image. If you still have network issues, build locally first:

```bash
./mvnw clean package -DskipTests
docker compose up -d
```

## Email Verification

Email verification delivery uses Resend and is disabled by default. With RESEND_ENABLED=false, registration still creates a 24-hour verification token, but sends no email and leaves the account unverified.

Set these deployment values to enable delivery:

- RESEND_ENABLED=true
- RESEND_API_KEY: keep this in deployment secrets or a local ignored .env file.
- RESEND_FROM: a sender address on a verified domain, such as Riksa <no-reply@auth.example.com>.
- RESEND_CONFIRMATION_URL: the public HTTPS page URL, such as https://api.example.com/email-verification.html.

Before enabling it, [verify the sending domain in Resend](https://resend.com/docs/dashboard/domains/introduction). The backend sends mail through the [Resend send-email API](https://resend.com/docs/api-reference/emails/send-email). If delivery is enabled but any value is missing or the confirmation URL is not HTTPS, application startup fails with a configuration error.

POST /api/v1/auth/resend-verification-email accepts an email address and always returns the same response, whether or not that address belongs to an unverified account. It is limited to five requests per 15 minutes per client address. The email opens a backend-hosted page where the user presses Confirm email; the page works in a regular browser without native app links and then calls POST /api/v1/auth/verify-email. It also provides a generic resend form for expired or missing links.

Delivery is queued after the database transaction commits and runs asynchronously, so a provider outage does not roll back registration or block the resend response. If the bounded queue is full, the delivery task is dropped and a generic log entry is written; the user can request another email. Delivery failures are caught without logging addresses, tokens, API keys, or provider response bodies. The Resend request has a 3-second connect timeout, a 10-second read timeout, and a stable hashed idempotency key per verification token.

## Development Notes

- The project uses **package-by-feature** under `com.riksa.*`.
- Flutter Telegram signup/login integration: [`docs/FLUTTER_TELEGRAM_AUTH.md`](docs/FLUTTER_TELEGRAM_AUTH.md).
- Database schema is managed by **Flyway**. `spring.jpa.hibernate.ddl-auto` is set to `validate`, so all schema changes must be added as new migrations: `V{N}__description.sql`.
- REST endpoints are under `/api/v1/*`. GraphQL is exposed at `/graphql` for social/community features.
- Authenticated requests require `Authorization: Bearer <accessToken>` except for `/api/v1/auth/**` and `/actuator/health`.
- Redis stores rate limits, OTPs, Telegram challenges, short-lived response/security caches, current leaderboards, and scheduler locks. PostgreSQL remains authoritative for durable data.
- Set `REDIS_RATE_LIMIT_ENABLED`, `REDIS_OTP_ENABLED`, `REDIS_CACHE_ENABLED`, or `REDIS_LEADERBOARD_ENABLED` to `false` to disable an individual Redis feature during rollout. Disabling OTP also keeps Telegram challenges in PostgreSQL.

## First-Time Startup Fixes

The following startup issues were fixed so the project boots cleanly. If you reset the database, these migrations will re-apply automatically:

- `V9__add_blog_likes_created_at.sql` — `blog_likes.created_at` column expected by the entity.
- `V10__add_blog_posts_cover_url.sql` — `blog_posts.cover_url` column expected by the entity.
- `JacksonConfig.java` — explicit `ObjectMapper` bean for cursor pagination.
- `GraphQlConfig.java` — scalar implementations for `UUID` and `DateTime` used by the social schema.

## API Health Check

```bash
curl http://localhost:8080/actuator/health
```

## License

[Add your license here]
