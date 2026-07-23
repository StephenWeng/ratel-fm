#!/usr/bin/env bash
# Ratel FM Qdrant Linux start script.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
QDRANT_EXE="$BASE_DIR/runtime/linux/qdrant/qdrant"
RUN_DIR="$BASE_DIR/run"
LOG_DIR="$BASE_DIR/logs"
PID_FILE="$RUN_DIR/qdrant.pid"

export QDRANT__SERVICE__HOST="${QDRANT_HOST:-0.0.0.0}"
export QDRANT__SERVICE__HTTP_PORT="${QDRANT_HTTP_PORT:-6333}"
export QDRANT__SERVICE__GRPC_PORT="${QDRANT_GRPC_PORT:-6334}"
export QDRANT__STORAGE__STORAGE_PATH="${QDRANT_STORAGE_DIR:-$BASE_DIR/storage}"
export QDRANT__STORAGE__SNAPSHOTS_PATH="${QDRANT_SNAPSHOTS_DIR:-$BASE_DIR/snapshots}"
export QDRANT__TELEMETRY_DISABLED="true"

mkdir -p "$RUN_DIR" "$LOG_DIR" "$QDRANT__STORAGE__STORAGE_PATH" "$QDRANT__STORAGE__SNAPSHOTS_PATH"
if [[ ! -x "$QDRANT_EXE" ]]; then
  echo "Qdrant executable not found or not executable: $QDRANT_EXE"
  exit 1
fi
if [[ ! -f "$BASE_DIR/static/index.html" ]]; then
  echo "Qdrant Dashboard static files not found: $BASE_DIR/static"
  exit 1
fi

if [[ -f "$PID_FILE" ]]; then
  pid="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" >/dev/null 2>&1; then
    echo "Qdrant is already running, PID=$pid"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

cd "$BASE_DIR"
nohup "$QDRANT_EXE" > "$LOG_DIR/qdrant.log" 2> "$LOG_DIR/qdrant-error.log" &
pid="$!"
echo "$pid" > "$PID_FILE"
echo "Qdrant started, PID=$pid"
echo "HTTP bind: $QDRANT__SERVICE__HOST:$QDRANT__SERVICE__HTTP_PORT"
echo "gRPC bind: $QDRANT__SERVICE__HOST:$QDRANT__SERVICE__GRPC_PORT"
echo "Logs: $LOG_DIR"

probe_host="$QDRANT__SERVICE__HOST"
if [[ "$probe_host" == "0.0.0.0" || "$probe_host" == "::" ]]; then
  probe_host="127.0.0.1"
fi
if command -v curl >/dev/null 2>&1; then
  for _ in {1..20}; do
    if curl -fsS --max-time 2 "http://$probe_host:$QDRANT__SERVICE__HTTP_PORT/" >/dev/null 2>&1; then
      echo "Health check passed: http://$probe_host:$QDRANT__SERVICE__HTTP_PORT/"
      exit 0
    fi
    sleep 1
  done
  echo "Warning: health check did not complete in time; check logs." >&2
fi
