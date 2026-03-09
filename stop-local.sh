#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# stop-local.sh  –  Stop the background backend process started by start-local.sh
# ──────────────────────────────────────────────────────────────────────────────
ROOT="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$ROOT/logs/backend.pid"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

if [ ! -f "$PID_FILE" ]; then
  error "No PID file found at $PID_FILE. Is the backend running?"
  exit 1
fi

PID=$(cat "$PID_FILE")

if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  info "Backend (PID $PID) stopped."
  rm -f "$PID_FILE"
else
  error "Process $PID is not running."
  rm -f "$PID_FILE"
fi
