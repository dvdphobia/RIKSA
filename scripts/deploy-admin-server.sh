#!/usr/bin/env bash
# Publishes the admin panel on the RIKSA server. Run on the server; the
# "Deploy admin panel" workflow uploads the build to $BUNDLE first and pipes
# this file over SSH, so the repo's version always runs.
#
#   1. unpack the build next to the live copy, then swap it in
#   2. keep the three newest previous copies for a quick rollback
#   3. first run only: add the admin site to Caddy (HTTPS is automatic)
#   4. first run only: allow the admin origin in the API's CORS list and
#      restart the API so it picks the change up
set -euo pipefail

BUNDLE=${BUNDLE:-/tmp/riksa-admin.tgz}
WEB_DIR=${WEB_DIR:-/var/www/riksa-admin}
ADMIN_HOST=${ADMIN_HOST:?set ADMIN_HOST, e.g. admin.94-237-78-191.sslip.io}
APP_DIR=${APP_DIR:-/opt/riksa}
CADDYFILE=${CADDYFILE:-/etc/caddy/Caddyfile}
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.prod.yml)

log() { printf '[admin %s] %s\n' "$(date +%H:%M:%S)" "$*"; }

# Shares the backend deploy's lock: a CORS change restarts the API.
exec 9>/tmp/riksa-deploy.lock
flock 9

TS=$(date +%Y%m%d_%H%M%S)
NEXT="$WEB_DIR.next"
rm -rf "$NEXT"
mkdir -p "$NEXT"
tar -xzf "$BUNDLE" -C "$NEXT"
if [ ! -f "$NEXT/index.html" ]; then
  log "the bundle has no index.html; leaving the live panel alone"
  exit 1
fi
if [ -d "$WEB_DIR" ]; then mv "$WEB_DIR" "$WEB_DIR.bak-$TS"; fi
mv "$NEXT" "$WEB_DIR"
rm -f "$BUNDLE"
ls -1dt "$WEB_DIR".bak-* 2>/dev/null | tail -n +4 | xargs -r rm -rf
log "published to $WEB_DIR"

if ! grep -q "^$ADMIN_HOST " "$CADDYFILE"; then
  cp "$CADDYFILE" "$CADDYFILE.bak-$TS"
  cat >> "$CADDYFILE" <<EOF

$ADMIN_HOST {
    encode gzip
    root * $WEB_DIR
    # The panel routes in the browser, so unknown paths get index.html.
    try_files {path} /index.html
    file_server
}
EOF
  if ! caddy validate --config "$CADDYFILE" --adapter caddyfile; then
    cp "$CADDYFILE.bak-$TS" "$CADDYFILE"
    log "Caddy rejected the new site; restored the previous Caddyfile"
    exit 1
  fi
  systemctl reload caddy
  log "added https://$ADMIN_HOST to Caddy"
fi

ENV_FILE="$APP_DIR/.env"
ORIGIN="https://$ADMIN_HOST"
if ! grep -q "^CORS_ALLOWED_ORIGINS=.*$ORIGIN" "$ENV_FILE"; then
  cp "$ENV_FILE" "$ENV_FILE.bak-$TS"
  if grep -q "^CORS_ALLOWED_ORIGINS=" "$ENV_FILE"; then
    sed -i "s#^CORS_ALLOWED_ORIGINS=#CORS_ALLOWED_ORIGINS=$ORIGIN,#" "$ENV_FILE"
  else
    echo "CORS_ALLOWED_ORIGINS=$ORIGIN" >> "$ENV_FILE"
  fi
  log "allowed $ORIGIN in CORS; restarting the API"
  cd "$APP_DIR"
  "${COMPOSE[@]}" up -d app
fi

log "done: https://$ADMIN_HOST"
