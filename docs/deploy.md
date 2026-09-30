# Deploying the API

Every push to `dev` deploys automatically (`.github/workflows/deploy.yml`):

1. **Test** — `./mvnw verify` against PostgreSQL 17. A failing test stops the deploy.
2. **Deploy** — SSH to the server and run `scripts/deploy-server.sh`, which:
   - backs up the database to `/opt/riksa-backups/` (keeps the newest 14)
   - moves any hand edits in `/opt/riksa` aside with `git stash`
   - fast-forwards to `origin/dev` and rebuilds with
     `docker-compose.yml` + `docker-compose.prod.yml`
   - waits up to 4 minutes for `riksa-api` to report healthy
   - if it never does, rebuilds the previous commit and prints the backup to
     restore if the database schema changed

Deploy by hand (same script) from the Actions tab → **Deploy** → **Run workflow**.

## One-time setup

In GitHub → the repo → **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `DEPLOY_SSH_KEY` | the private key that can SSH to the server (the whole file, including the BEGIN/END lines) |
| `DEPLOY_KNOWN_HOSTS` | `94.237.78.191 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKsnOZNAdtju/mzb8TrNx9HoAMjOl53nZYDZelmIe42Y` |

Then add the repository **variable** `AUTO_DEPLOY` = `true` (same page,
**Variables** tab). Until it is set, pushes only run the tests.

Optional variables `DEPLOY_HOST` / `DEPLOY_USER` override `94.237.78.191` / `root`.

Without GitHub, deploy from a machine that has the key (Git Bash):

```bash
ssh -i ~/.ssh/riksa_deploy root@94.237.78.191 'bash -s' < scripts/deploy-server.sh
```

Also create an environment named `production` (**Settings → Environments**).
Add yourself as a required reviewer there if you want to approve each deploy
before it runs.

## Admin panel

Every push to `dev` that changes `admin-panel/` publishes the panel
(`.github/workflows/deploy-admin.yml`). It uses the same secrets, the same
`AUTO_DEPLOY` switch and the same `production` environment as the API deploy.

1. **Build**: `npm ci && npm run build` in `admin-panel/`, with `VITE_API_URL`
   pointing at the live API.
2. **Publish**: upload the build and run `scripts/deploy-admin-server.sh` on
   the server, which:
   - unpacks it next to `/var/www/riksa-admin` and swaps it in, keeping the
     three previous copies as `/var/www/riksa-admin.bak-*`
   - first run only: adds the admin site to `/etc/caddy/Caddyfile` (HTTPS is
     automatic) and reloads Caddy; an invalid Caddyfile is put back untouched
   - first run only: adds the admin origin to `CORS_ALLOWED_ORIGINS` in
     `/opt/riksa/.env` and restarts the API (about a minute of downtime)

The panel is served at `https://admin.94-237-78-191.sslip.io`. Optional
repository variables `ADMIN_HOST` / `ADMIN_API_URL` override that host and
`https://94-237-78-191.sslip.io/api/v1`. When the real domains are live, set
`ADMIN_HOST=admin.riksa.app` and `ADMIN_API_URL=https://api.riksa.app/api/v1`.

Deploy by hand from the Actions tab → **Deploy admin panel** → **Run workflow**,
or without GitHub from a machine that has the key (Git Bash, repo root):

```bash
(cd admin-panel && VITE_API_URL=https://94-237-78-191.sslip.io/api/v1 npm run build && tar -czf ../riksa-admin.tgz -C dist .)
scp -i ~/.ssh/riksa_deploy riksa-admin.tgz root@94.237.78.191:/tmp/riksa-admin.tgz
ssh -i ~/.ssh/riksa_deploy root@94.237.78.191 "ADMIN_HOST=admin.94-237-78-191.sslip.io bash -s" < scripts/deploy-admin-server.sh
```

Roll back to the previous panel on the server:

```bash
cd /var/www && rm -rf riksa-admin && mv "$(ls -1dt riksa-admin.bak-* | head -1)" riksa-admin
```

## Restoring a backup

Only needed when a deploy changed the schema and the old version misbehaves.
Stop the API, recreate the database, load the backup, start again:

```bash
cd /opt/riksa
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop app
docker exec riksa-postgres sh -c 'dropdb -U "$POSTGRES_USER" "$POSTGRES_DB" && createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
gunzip -c /opt/riksa-backups/<backup>.sql.gz | docker exec -i riksa-postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d app
```

Hand edits the deploy moved aside: `git stash list` in `/opt/riksa`.
