#!/usr/bin/env bash
# Ratel FM Ollama Linux stop script.
# Stops only the Ollama process recorded by this independent package.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUN_DIR="$BASE_DIR/run"
PID_FILE="$RUN_DIR/ollama.pid"
OPEN_WEBUI_PID_FILE="$RUN_DIR/open-webui.pid"

stop_pid_file_process() {
  local name="$1"
  local pid_file="$2"
  if [[ ! -f "$pid_file" ]]; then
    echo "$name is not running: PID file not found."
    return 0
  fi

  local pid
  pid="$(tr -d '[:space:]' < "$pid_file")"
  if [[ ! "$pid" =~ ^[0-9]+$ ]]; then
    rm -f "$pid_file"
    echo "Invalid $name PID file has been removed."
    return 0
  fi

  if ! kill -0 "$pid" >/dev/null 2>&1; then
    rm -f "$pid_file"
    echo "$name is not running: PID=$pid was not found."
    return 0
  fi

  kill "$pid"
  for _ in {1..10}; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      rm -f "$pid_file"
      echo "$name stopped, PID=$pid"
      return 0
    fi
    sleep 1
  done

  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$pid_file"
  echo "$name force stopped, PID=$pid"
}

stop_pid_file_process "Open WebUI" "$OPEN_WEBUI_PID_FILE" || true
stop_pid_file_process "Ollama" "$PID_FILE"
