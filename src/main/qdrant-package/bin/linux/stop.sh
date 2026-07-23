#!/usr/bin/env bash
# Ratel FM Qdrant Linux stop script.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PID_FILE="$BASE_DIR/run/qdrant.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "Qdrant is not running: PID file not found."
  exit 0
fi
pid="$(tr -d '[:space:]' < "$PID_FILE")"
if [[ ! "$pid" =~ ^[0-9]+$ ]] || ! kill -0 "$pid" >/dev/null 2>&1; then
  rm -f "$PID_FILE"
  echo "Qdrant is not running; stale PID file removed."
  exit 0
fi
kill "$pid"
for _ in {1..10}; do
  if ! kill -0 "$pid" >/dev/null 2>&1; then
    rm -f "$PID_FILE"
    echo "Qdrant stopped, PID=$pid"
    exit 0
  fi
  sleep 1
done
kill -9 "$pid" >/dev/null 2>&1 || true
rm -f "$PID_FILE"
echo "Qdrant force stopped, PID=$pid"
