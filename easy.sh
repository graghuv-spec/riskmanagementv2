#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

usage() {
  cat <<'EOF'
Usage:
  ./easy.sh local-start
  ./easy.sh local-stop
  ./easy.sh docker-start
  ./easy.sh docker-stop

Aliases:
  start-local, stop-local, start-docker, stop-docker
EOF
}

if [ $# -lt 1 ]; then
  usage
  exit 1
fi

cmd="$1"

case "$cmd" in
  local-start|start-local)
    exec "$ROOT/scripts/native-start.sh"
    ;;
  local-stop|stop-local)
    exec "$ROOT/scripts/native-stop.sh"
    ;;
  docker-start|start-docker)
    exec "$ROOT/scripts/docker-start.sh"
    ;;
  docker-stop|stop-docker)
    exec "$ROOT/scripts/docker-stop.sh"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    echo "[ERROR] Unknown command: $cmd"
    usage
    exit 1
    ;;
esac
