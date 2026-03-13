#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[INFO] Docker startup mode (compose-only)"
docker compose up -d

echo "[INFO] Waiting for backend endpoint..."
for i in {1..30}; do
  if curl -sS "http://localhost:8080/api/loans" >/dev/null 2>&1; then
    echo "[INFO] Backend healthy"
    echo "Frontend: http://localhost"
    echo "Backend : http://localhost:8080/api/loans"
    exit 0
  fi
  sleep 2
done

echo "[ERROR] Backend endpoint did not become healthy"
exit 1
