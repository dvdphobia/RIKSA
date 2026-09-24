#!/usr/bin/env bash
# Deploys the latest `dev` to the RIKSA server. Run on the server; the GitHub
# deploy workflow pipes this file over SSH so the repo's version always runs.
#
#   1. back up the database (and keep the newest 14 backups)
#   2. move any hand edits on the server aside with `git stash`
#   3. fast-forward to origin/dev and rebuild the containers
#   4. wait for the API to report healthy
#   5. on failure, rebuild the previous commit and print the restore command
set -euo pipefail

APP_DIR=${APP_DIR:-/opt/riksa}
BACKUP_DIR=${BACKUP_DIR:-/opt/riksa-backups}
BRANCH=${BRANCH:-dev}
API_CONTAINER=${API_CONTAINER:-riksa-api}
DB_CONTAINER=${DB_CONTAINER:-riksa-postgres}
HEALTH_TIMEOUT=${HEALTH_TIMEOUT:-240}
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.prod.yml)

log() { printf '[deploy %s] %s\n' "$(date +%H:%M:%S)" "$*"; }

# One deploy at a time; a second push waits instead of racing the first.
exec 9>/tmp/riksa-deploy.lock
flock 9

cd "$APP_DIR"
TS=$(date +%Y%m%d_%H%M%S)
PREV=$(git rev-parse HEAD)

db_version() {
  docker exec "$DB_CONTAINER" sh -c \
    'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "select coalesce(max(installed_rank), 0) from flyway_schema_history where success"'
}

log "current commit $(git log --oneline -1)"
PREV_RANK=$(db_version)

BACKUP="$BACKUP_DIR/riksa_db_${TS}_${PREV:0:7}.sql.gz"
mkdir -p "$BACKUP_DIR"
docker exec "$DB_CONTAINER" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | gzip > "$BACKUP"
if [ "$(gunzip -c "$BACKUP" | grep -c '^CREATE TABLE')" -lt 5 ]; then
  log "backup $BACKUP looks empty; aborting before touching anything"
  exit 1
fi
log "database backed up to $BACKUP ($(du -h "$BACKUP" | cut -f1))"
ls -1t "$BACKUP_DIR"/riksa_db_*.sql.gz | tail -n +15 | xargs -r rm --

if [ -n "$(git status --porcelain)" ]; then
  git stash push -u -m "hand edits before deploy $TS" >/dev/null
  log "moved hand edits on the server to: $(git stash list | head -1)"
fi

git fetch --quiet origin "$BRANCH"
git checkout --quiet "$BRANCH"
git merge --ff-only --quiet "origin/$BRANCH"
NEW=$(git rev-parse HEAD)
if [ "$NEW" = "$PREV" ]; then
  log "already at $(git log --oneline -1); rebuilding anyway"
else
  log "updating ${PREV:0:7} -> $(git log --oneline -1)"
fi

wait_healthy() {
  local waited=0 status
  while [ "$waited" -lt "$HEALTH_TIMEOUT" ]; do
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$API_CONTAINER" 2>/dev/null || echo missing)
    case "$status" in
      healthy) return 0 ;;
      unhealthy|exited|dead|missing) [ "$waited" -ge 30 ] && return 1 ;;
    esac
    sleep 5
    waited=$((waited + 5))
  done
  return 1
}

"${COMPOSE[@]}" up -d --build
if wait_healthy; then
  docker image prune -f >/dev/null
  log "healthy on $(git log --oneline -1)"
  log "migrations: rank $PREV_RANK -> $(db_version)"
  docker logs "$API_CONTAINER" 2>&1 | grep -iE 'Successfully applied|Migrating schema|Started ' | tail -8 || true
  exit 0
fi

log "API did not become healthy; last log lines:"
docker logs --tail 60 "$API_CONTAINER" 2>&1 || true

log "rolling code back to ${PREV:0:7}"
git checkout --quiet "$PREV"
"${COMPOSE[@]}" up -d --build
if wait_healthy; then
  log "previous version is back up. Server is on a detached HEAD at ${PREV:0:7};"
  log "the next successful deploy returns it to $BRANCH."
else
  log "previous version did not come back healthy either."
fi
if [ "$(db_version)" != "$PREV_RANK" ]; then
  log "the database schema changed during the failed deploy. If the old version"
  log "misbehaves, restore the backup taken just before it:"
  log "  gunzip -c $BACKUP | docker exec -i $DB_CONTAINER sh -c 'psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\"'"
  log "(restore into an emptied database; see docs/deploy.md)"
fi
exit 1
