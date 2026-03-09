#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# start-local.sh  –  Start backend + frontend for local development
#
# Usage:  ./start-local.sh
#
# - Detects and stops any process already holding port 8080 or 4200
# - Backend logs  →  logs/backend.log
# - Frontend runs in the foreground (Ctrl+C to stop everything)
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── Helper: find PID listening on a port ─────────────────────────────────────
# Tries lsof first (Linux/macOS), then netstat (Windows / Git Bash).
pid_on_port() {
  local port=$1
  local pid=""

  if command -v lsof >/dev/null 2>&1; then
    pid=$(lsof -ti tcp:"$port" 2>/dev/null | head -1)
  fi

  # Fallback: Windows netstat -ano
  if [ -z "$pid" ]; then
    pid=$(netstat -ano 2>/dev/null \
          | grep -E "[:.]${port}[[:space:]].*LISTENING" \
          | awk '{print $NF}' \
          | head -1)
  fi

  echo "$pid"
}

# ── Helper: kill whatever is holding a port ───────────────────────────────────
free_port() {
  local port=$1
  local label=$2
  local pid
  pid=$(pid_on_port "$port")

  if [ -z "$pid" ]; then
    info "Port $port ($label) is free."
    return 0
  fi

  warn "Port $port is in use by PID $pid ($label) – stopping it..."

  # taskkill is more reliable on Windows; fall back to kill
  if command -v taskkill >/dev/null 2>&1; then
    taskkill //PID "$pid" //F >/dev/null 2>&1 || true
  else
    kill -9 "$pid" 2>/dev/null || true
  fi

  sleep 1

  pid=$(pid_on_port "$port")
  if [ -n "$pid" ]; then
    error "Could not free port $port (PID $pid still running). Kill it manually and retry."
  fi

  info "Port $port is now free."
}

# ── Banner ────────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo "  RiskManagement Pro  –  Local startup"
echo "============================================================"
echo ""

# ── Prerequisite checks ───────────────────────────────────────────────────────
info "Checking prerequisites..."

command -v java >/dev/null 2>&1 || error "Java not found. Install JDK 21 and add it to PATH."
command -v node >/dev/null 2>&1 || error "Node.js not found. Install Node 20 and add it to PATH."
command -v npm  >/dev/null 2>&1 || error "npm not found."
command -v psql >/dev/null 2>&1 || warn  "psql not on PATH – ensure PostgreSQL is running on localhost:5432."

JAVA_VER=$(java -version 2>&1 | head -1 | grep -oP '(?<=version ")[\d]+')
NODE_VER=$(node --version | grep -oP '^\d+')

[[ "$JAVA_VER" -ge 21 ]] || error "Java 21+ required (found Java $JAVA_VER)."
[[ "$NODE_VER" -ge 20 ]] || error "Node 20+ required (found Node $NODE_VER)."

info "Java $JAVA_VER  |  Node $NODE_VER  –  OK"

# ── Stop any existing processes on our ports ──────────────────────────────────
echo ""
info "Checking for processes already on ports 8080 / 4200..."
free_port 8080 "backend"
free_port 4200 "frontend"

# Clean up a stale PID file from a previous run
if [ -f "$LOG_DIR/backend.pid" ]; then
  OLD_PID=$(cat "$LOG_DIR/backend.pid")
  kill "$OLD_PID" 2>/dev/null || true
  rm -f "$LOG_DIR/backend.pid"
fi

# ── Prepare directories ───────────────────────────────────────────────────────
mkdir -p "$LOG_DIR"

# ── Install frontend dependencies if needed ───────────────────────────────────
if [ ! -d "$ROOT/frontend/node_modules" ]; then
  info "node_modules not found – running npm install..."
  npm --prefix "$ROOT/frontend" install
fi

# ── Start backend ─────────────────────────────────────────────────────────────
echo ""
info "Starting Spring Boot backend (logs → logs/backend.log)..."

cd "$ROOT/backend"
./gradlew bootRun --no-daemon > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "$BACKEND_PID" > "$LOG_DIR/backend.pid"
info "Backend PID $BACKEND_PID"

# ── Wait for backend to be ready ──────────────────────────────────────────────
info "Waiting for backend on http://localhost:8080 ..."
RETRIES=30
until curl -s http://localhost:8080/api/loans > /dev/null 2>&1 || [ "$RETRIES" -eq 0 ]; do
  sleep 2
  RETRIES=$((RETRIES - 1))
  echo -n "."
done
echo ""

if [ "$RETRIES" -eq 0 ]; then
  warn "Backend did not respond after 60 s – check logs/backend.log for errors."
else
  info "Backend is up!"
fi

# ── Start frontend ────────────────────────────────────────────────────────────
echo ""
info "Starting Angular dev server on http://localhost:4200 ..."
echo ""
echo "  App  →  http://localhost:4200"
echo "  API  →  http://localhost:8080 (proxied via proxy.conf.json)"
echo ""
echo "  Press Ctrl+C to stop both services"
echo ""

# Ensure backend is killed on exit (Ctrl+C, unhandled error, normal exit)
trap 'echo ""; info "Shutting down..."; kill "$BACKEND_PID" 2>/dev/null; rm -f "$LOG_DIR/backend.pid"; exit 0' INT TERM EXIT

cd "$ROOT/frontend"
npm start
