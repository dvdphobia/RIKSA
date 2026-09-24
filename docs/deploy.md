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

Optional variables `DEPLOY_HOST` / `DEPLOY_USER` override `94.237.78.191` / `root`.

Also create an environment named `production` (**Settings → Environments**).
Add yourself as a required reviewer there if you want to approve each deploy
before it runs.

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
