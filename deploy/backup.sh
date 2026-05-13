#!/usr/bin/env bash
# Daily PostgreSQL backup. Designed for cron, e.g.
#   15 3 * * *  /opt/china-kekao/deploy/backup.sh >> /var/log/kekao-backup.log 2>&1
#
# Reads connection + S3 settings from the project .env file. Keeps the last
# 7 dumps locally and uploads each one to BACKUP_S3_BUCKET when configured.
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env}"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_DIR/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  set -a; . "$ENV_FILE"; set +a
fi

: "${POSTGRES_DB:?missing}"
: "${POSTGRES_USER:?missing}"
: "${POSTGRES_PASSWORD:?missing}"

mkdir -p "$BACKUP_DIR"
TS=$(date -u +%Y%m%dT%H%M%SZ)
DUMP_FILE="$BACKUP_DIR/${POSTGRES_DB}-${TS}.sql.gz"

echo "[$(date -Iseconds)] dumping ${POSTGRES_DB} -> ${DUMP_FILE}"
docker compose -f "$PROJECT_DIR/docker-compose.yml" -f "$PROJECT_DIR/docker-compose.prod.yml" \
    exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" postgres \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges \
  | gzip -9 > "$DUMP_FILE"

# Optional: copy to S3-compatible storage (Backblaze B2, MinIO, Cloudflare R2, AWS S3).
if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
  if ! command -v aws >/dev/null 2>&1; then
    echo "aws CLI missing; skipping upload" >&2
  else
    EXTRA=()
    if [ -n "${BACKUP_S3_ENDPOINT:-}" ]; then
      EXTRA+=(--endpoint-url "$BACKUP_S3_ENDPOINT")
    fi
    aws "${EXTRA[@]}" s3 cp "$DUMP_FILE" "s3://${BACKUP_S3_BUCKET}/$(basename "$DUMP_FILE")"
  fi
fi

# Prune local copies older than RETENTION_DAYS.
find "$BACKUP_DIR" -type f -name "${POSTGRES_DB}-*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete

echo "[$(date -Iseconds)] backup complete"
