# Riksa Backend — Local Setup Guide

This guide explains how to run the Riksa Spring Boot backend from the `dev` branch.

## 1. Prerequisites

Install the following tools:

- Git
- Java 21 JDK
- Docker Desktop, or Docker Engine with the Docker Compose plugin

Maven does not need to be installed because the repository includes the Maven wrapper.

Verify the tools:

```bash
git --version
java -version
docker --version
docker compose version
```

Make sure `java -version` reports Java 21 and Docker is running.

## 2. Clone the `dev` Branch

If the project has not been cloned yet:

```bash
git clone --branch dev --single-branch https://github.com/dvdphobia/RIKSA.git
cd RIKSA
```

If it is already cloned:

```bash
cd RIKSA
git switch dev
git pull origin dev
```

## 3. Create the Local Environment File

Copy the example file:

```bash
cp .env.example .env
```

Open `.env` and use these values for local development. Keep the rest of the settings from `.env.example` unchanged.

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

JWT_SECRET=local-development-secret-key-must-be-at-least-256-bits-long
OTP_HASH_SECRET=another-local-development-secret-used-to-hash-otp-codes

GOOGLE_CLIENT_ID=

TELEGRAM_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=
TELEGRAM_WEBHOOK_URL=
TELEGRAM_WEBHOOK_SECRET=

CORS_ALLOWED_ORIGINS=http://localhost:3000
PORT=8080
```

Do not commit `.env` or share real production secrets.

## 4. Start PostgreSQL and Redis

Run the infrastructure services in Docker:

```bash
docker compose up -d postgres redis
```

Confirm that both containers are healthy:

```bash
docker compose ps
```

The expected containers are:

- `riksa-postgres`
- `riksa-redis`

## 5. Start the Backend

On macOS or Linux:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

On Windows PowerShell, import the `.env` values or configure them in the IDE, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

On first startup, Maven downloads the dependencies and Flyway creates and seeds the PostgreSQL schema. Wait until the log contains a message similar to:

```text
Started RiksaApiApplication
```

## 6. Verify the Backend

In another terminal:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

The backend is now available at `http://localhost:8080`.

## Alternative: Run Everything in Docker

Instead of running Spring Boot locally, start the complete stack with:

```bash
docker compose up --build -d
docker compose logs -f app
```

Press `Ctrl+C` to stop following the logs. The containers will continue running in the background.

## Useful Commands

```bash
# Run tests
./mvnw test

# Compile the project
./mvnw clean compile

# Stop all containers
docker compose down

# View backend logs when the app runs in Docker
docker compose logs -f app

# Update the local dev branch
git switch dev
git pull origin dev
```

## Troubleshooting

### Port already in use

The project uses ports `8080`, `5432`, and `6379`. Stop the application or container already using the conflicting port, then start the project again.

### Cannot connect to PostgreSQL or Redis

Check Docker and the service health:

```bash
docker compose ps
docker compose logs postgres
docker compose logs redis
```

Also confirm that the passwords in `.env` match the values used when the containers were first created.

### PostgreSQL password was changed after the first startup

PostgreSQL keeps its original password in the Docker volume. For a new local setup with no data worth keeping, reset the local database volume and start again:

```bash
docker compose down -v
docker compose up -d postgres redis
```

Warning: `docker compose down -v` permanently deletes the local PostgreSQL data for this project.

### Maven wrapper is not executable

On macOS or Linux:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### Application fails after pulling new changes

Make sure the infrastructure is running, then rebuild:

```bash
docker compose up -d postgres redis
./mvnw clean spring-boot:run
```

## Development Notes

- REST APIs use the `/api/v1/*` prefix.
- The social GraphQL endpoint is `/graphql`.
- Database changes are applied automatically through Flyway migrations.
- Most API routes require a JWT bearer token; the auth routes and health check are public.
- Keep PostgreSQL and Redis running while developing the backend.
