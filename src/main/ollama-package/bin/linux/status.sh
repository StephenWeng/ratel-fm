#!/usr/bin/env bash
# Ratel FM Ollama Linux status script.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUN_DIR="$BASE_DIR/run"
OLLAMA_PID_FILE="$RUN_DIR/ollama.pid"
OPEN_WEBUI_PID_FILE="$RUN_DIR/open-webui.pid"
OLLAMA_HOST_VALUE="${OLLAMA_HOST:-127.0.0.1:11434}"

base_url() {
  local host_value="$1"
  if [[ "$host_value" == http://* || "$host_value" == https://* ]]; then
    printf '%s\n' "${host_value%/}"
    return 0
  fi
  printf 'http://%s\n' "${host_value%/}"
}

probe_url="$(base_url "$OLLAMA_HOST_VALUE")"
probe_url="${probe_url/http:\/\/0.0.0.0/http:\/\/127.0.0.1}"

if [[ -f "$OLLAMA_PID_FILE" ]]; then
  pid="$(tr -d '[:space:]' < "$OLLAMA_PID_FILE")"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" >/dev/null 2>&1; then
    echo "Ollama is running, PID=$pid"
    if command -v curl >/dev/null 2>&1 && curl -fsS --max-time 2 "$probe_url/api/tags" >/dev/null; then
      echo "Health check passed: $probe_url/api/tags"
    else
      echo "Warning: health check failed or curl is unavailable." >&2
    fi
  else
    echo "Ollama is not running; stale PID file exists."
  fi
else
  echo "Ollama is not running."
fi

if [[ -f "$OPEN_WEBUI_PID_FILE" ]]; then
  webui_pid="$(tr -d '[:space:]' < "$OPEN_WEBUI_PID_FILE")"
  if [[ "$webui_pid" =~ ^[0-9]+$ ]] && kill -0 "$webui_pid" >/dev/null 2>&1; then
    echo "Open WebUI is running, PID=$webui_pid"
  else
    echo "Open WebUI is not running; stale PID file exists."
  fi
else
  echo "Open WebUI is not running."
fi
