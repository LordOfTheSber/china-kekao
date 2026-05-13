#!/usr/bin/env bash
# One-shot Ubuntu (22.04 / 24.04) bootstrap for china-kekao.
#
# What it does, idempotently:
#   1. installs Docker Engine + compose plugin, ufw, openssl, curl, awscli
#   2. opens 22/tcp, 80/tcp, 443/tcp on ufw (skipped if ufw inactive by choice)
#   3. clones (or pulls) the repo into /opt/china-kekao
#   4. seeds .env from .env.example, generates JWT RS256 keys, sets a
#      random POSTGRES_PASSWORD on first run
#   5. brings the prod stack up (postgres + backend + frontend + Caddy)
#   6. installs a daily cron entry for deploy/backup.sh
#   7. waits for /actuator/health to report UP
#
# Re-running is safe: secrets are only generated once, ufw rules are
# additive, and `docker compose up -d` reuses healthy containers.
#
# Usage (as root or with sudo):
#   curl -fsSL https://raw.githubusercontent.com/<owner>/china-kekao/main/deploy/bootstrap.sh \
#     | KEKAO_PUBLIC_HOST=kekao.example.com ACME_EMAIL=admin@example.com sudo -E bash
# or, if the repo is already on disk:
#   sudo KEKAO_PUBLIC_HOST=kekao.example.com ACME_EMAIL=admin@example.com \
#        /opt/china-kekao/deploy/bootstrap.sh

set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/LordOfTheSber/china-kekao.git}"
REPO_BRANCH="${REPO_BRANCH:-main}"
PROJECT_DIR="${PROJECT_DIR:-/opt/china-kekao}"
KEKAO_PUBLIC_HOST="${KEKAO_PUBLIC_HOST:-}"
ACME_EMAIL="${ACME_EMAIL:-}"
SKIP_FIREWALL="${SKIP_FIREWALL:-false}"
SKIP_BACKUP_CRON="${SKIP_BACKUP_CRON:-false}"

log()  { printf '\033[1;36m[bootstrap]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[fatal]\033[0m %s\n' "$*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "must run as root (use sudo)"
[ -n "$KEKAO_PUBLIC_HOST" ] || die "KEKAO_PUBLIC_HOST must be set (e.g. kekao.example.com)"
[ -n "$ACME_EMAIL" ] || warn "ACME_EMAIL not set — Caddy will use the default '<host>'"

. /etc/os-release 2>/dev/null || die "unsupported OS (no /etc/os-release)"
[ "${ID:-}" = "ubuntu" ] || warn "tested only on Ubuntu; got ID=$ID, continuing anyway"

# ---------------------------------------------------------------------------
# 1. apt packages
# ---------------------------------------------------------------------------
log "updating apt index"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y --no-install-recommends \
    ca-certificates curl gnupg lsb-release git ufw openssl cron awscli

# Docker official repo (apt's docker.io ships stale compose).
if ! command -v docker >/dev/null 2>&1; then
    log "installing Docker Engine"
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        | gpg --dearmor --yes -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable --now docker
else
    log "Docker already installed: $(docker --version)"
fi

docker compose version >/dev/null 2>&1 || die "docker compose plugin missing"

# ---------------------------------------------------------------------------
# 2. firewall
# ---------------------------------------------------------------------------
if [ "$SKIP_FIREWALL" != "true" ]; then
    log "configuring ufw (22, 80, 443)"
    ufw allow 22/tcp  >/dev/null
    ufw allow 80/tcp  >/dev/null
    ufw allow 443/tcp >/dev/null
    if ! ufw status | grep -q "Status: active"; then
        log "enabling ufw (default: deny incoming)"
        ufw --force default deny incoming
        ufw --force default allow outgoing
        ufw --force enable
    fi
else
    log "SKIP_FIREWALL=true → leaving ufw alone"
fi

# ---------------------------------------------------------------------------
# 3. repo
# ---------------------------------------------------------------------------
if [ ! -d "$PROJECT_DIR/.git" ]; then
    log "cloning $REPO_URL → $PROJECT_DIR"
    mkdir -p "$(dirname "$PROJECT_DIR")"
    git clone --branch "$REPO_BRANCH" "$REPO_URL" "$PROJECT_DIR"
else
    log "updating repo at $PROJECT_DIR (branch $REPO_BRANCH)"
    git -C "$PROJECT_DIR" fetch --prune origin "$REPO_BRANCH"
    git -C "$PROJECT_DIR" checkout "$REPO_BRANCH"
    git -C "$PROJECT_DIR" reset --hard "origin/$REPO_BRANCH"
fi

cd "$PROJECT_DIR"

# ---------------------------------------------------------------------------
# 4. secrets / .env
# ---------------------------------------------------------------------------
ENV_FILE="$PROJECT_DIR/.env"
KEY_DIR="$PROJECT_DIR/secrets"
install -d -m 0700 "$KEY_DIR"

if [ ! -f "$ENV_FILE" ]; then
    log "seeding $ENV_FILE from .env.example"
    cp .env.example "$ENV_FILE"
    sed -i \
        -e "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(openssl rand -hex 24)|" \
        -e "s|^KEKAO_PUBLIC_HOST=.*|KEKAO_PUBLIC_HOST=${KEKAO_PUBLIC_HOST}|" \
        -e "s|^SPRING_PROFILES_ACTIVE=.*|SPRING_PROFILES_ACTIVE=prod,json|" \
        "$ENV_FILE"
fi

# RS256 keys for JWT. Only generated on first run.
PRIV="$KEY_DIR/jwt-private.pem"
PUB="$KEY_DIR/jwt-public.pem"
if [ ! -s "$PRIV" ] || [ ! -s "$PUB" ]; then
    log "generating RS256 JWT keypair into $KEY_DIR"
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIV" >/dev/null 2>&1
    openssl pkey -in "$PRIV" -pubout -out "$PUB"
    chmod 600 "$PRIV" "$PUB"
fi

upsert_env() {
    local key="$1" value="$2"
    if grep -q "^${key}=" "$ENV_FILE"; then
        # Use a tmpfile because the value contains newlines/PEM headers.
        python3 - "$ENV_FILE" "$key" "$value" <<'PY'
import sys, pathlib
path, key, value = pathlib.Path(sys.argv[1]), sys.argv[2], sys.argv[3]
lines = path.read_text().splitlines()
out, replaced = [], False
for line in lines:
    if line.startswith(f"{key}="):
        out.append(f"{key}={value}")
        replaced = True
    else:
        out.append(line)
if not replaced:
    out.append(f"{key}={value}")
path.write_text("\n".join(out) + "\n")
PY
    else
        printf '%s=%s\n' "$key" "$value" >> "$ENV_FILE"
    fi
}

JWT_PRIVATE_PEM="$(awk 'BEGIN{ORS="\\n"} {print}' "$PRIV")"
JWT_PUBLIC_PEM="$(awk 'BEGIN{ORS="\\n"} {print}' "$PUB")"
upsert_env JWT_PRIVATE_KEY "$JWT_PRIVATE_PEM"
upsert_env JWT_PUBLIC_KEY  "$JWT_PUBLIC_PEM"
upsert_env KEKAO_PUBLIC_HOST "$KEKAO_PUBLIC_HOST"
[ -n "$ACME_EMAIL" ] && upsert_env ACME_EMAIL "$ACME_EMAIL"
chmod 600 "$ENV_FILE"

# ---------------------------------------------------------------------------
# 5. bring the stack up
# ---------------------------------------------------------------------------
COMPOSE=(docker compose
    -f docker-compose.yml
    -f docker-compose.prod.yml
    -f deploy/docker-compose.caddy.yml
    --env-file "$ENV_FILE")

log "building images (this is slow on first run)"
"${COMPOSE[@]}" build --pull

log "starting stack"
"${COMPOSE[@]}" up -d --remove-orphans

# ---------------------------------------------------------------------------
# 6. backup cron
# ---------------------------------------------------------------------------
if [ "$SKIP_BACKUP_CRON" != "true" ]; then
    CRON_FILE="/etc/cron.d/china-kekao-backup"
    if [ ! -f "$CRON_FILE" ] || ! grep -qF "$PROJECT_DIR/deploy/backup.sh" "$CRON_FILE"; then
        log "installing daily backup cron → $CRON_FILE"
        cat > "$CRON_FILE" <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
15 3 * * * root PROJECT_DIR=$PROJECT_DIR $PROJECT_DIR/deploy/backup.sh >> /var/log/kekao-backup.log 2>&1
EOF
        chmod 0644 "$CRON_FILE"
        touch /var/log/kekao-backup.log
    fi
    systemctl enable --now cron >/dev/null 2>&1 || true
fi

# ---------------------------------------------------------------------------
# 7. health wait
# ---------------------------------------------------------------------------
log "waiting for backend health (up to 5 min)"
deadline=$(( $(date +%s) + 300 ))
while :; do
    if "${COMPOSE[@]}" exec -T backend \
            curl -fsS http://localhost:8080/actuator/health 2>/dev/null \
            | grep -q '"status":"UP"'; then
        log "backend is UP"
        break
    fi
    if [ "$(date +%s)" -ge "$deadline" ]; then
        warn "backend did not report UP within 5 min; check logs:"
        warn "  ${COMPOSE[*]} logs --tail=200 backend"
        exit 1
    fi
    sleep 5
done

log "done. Visit https://${KEKAO_PUBLIC_HOST}/ once DNS points here and"
log "Let's Encrypt issues a cert (Caddy logs will show progress)."
