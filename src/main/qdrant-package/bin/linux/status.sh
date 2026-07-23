#!/usr/bin/env bash
# Ratel FM Qdrant Linux status script.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PID_FILE="$BASE_DIR/run/qdrant.pid"
HTTP_PORT="${QDRANT_HTTP_PORT:-6333}"

if [[ -f "$PID_FILE" ]]; then
  pid="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" >/dev/null 2>&1; then
    echo "Qdrant is running, PID=$pid"
    if command -v curl >/dev/null 2>&1 && curl -fsS --max-time 2 "http://127.0.0.1:$HTTP_PORT/" >/dev/null; then
      echo "Health check passed: http://127.0.0.1:$HTTP_PORT/"
    else
      echo "Warning: health check failed or curl is unavailable." >&2
    fi
    exit 0
  fi
fi
echo "Qdrant is not running."
