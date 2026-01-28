#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "[dev] Starting PostgreSQL (docker compose)..."
cd "$ROOT_DIR"
docker compose up -d db

echo "[dev] Waiting for PostgreSQL to become healthy..."
for i in {1..30}; do
  STATUS="$(docker inspect --format='{{.State.Health.Status}}' ferrycatch-db 2>/dev/null || true)"
  if [[ "$STATUS" == "healthy" ]]; then
    echo "[dev] DB is healthy."
    break
  fi
  sleep 2
done

if [[ "${STATUS:-}" != "healthy" ]]; then
  echo "[dev] DB did not become healthy in time. Check logs:"
  docker compose logs db
  exit 1
fi

cat <<'EOF'

Next steps (run in separate terminals):

1) API:
   cd apps/api
   ./gradlew bootRun

   Health:
   curl http://localhost:8080/actuator/health

2) Web (after you create Vue app under apps/web):
   cd apps/web
   npm install
   npm run dev

EOF
