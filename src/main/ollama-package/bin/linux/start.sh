#!/usr/bin/env bash
# Ratel FM Ollama Linux start script.
# Starts only the bundled Ollama runtime. Ratel FM and Qdrant remain independent processes.

set -euo pipefail

get_ollama_port() {
  # OLLAMA_HOST may be configured as host:port or full URL; only the port is needed for conflict checks.
  local host_value="${1:-127.0.0.1:11434}"
  host_value="${host_value#http://}"
  host_value="${host_value#https://}"
  host_value="${host_value%%/*}"
  if [[ "$host_value" == *:* ]]; then
    printf '%s\n' "${host_value##*:}"
    return 0
  fi
  printf '%s\n' "11434"
}

get_ollama_base_url() {
  # Convert OLLAMA_HOST into an HTTP base URL for the lightweight model-list readiness check.
  local host_value="${1:-127.0.0.1:11434}"
  if [[ "$host_value" == http://* || "$host_value" == https://* ]]; then
    printf '%s\n' "${host_value%/}"
    return 0
  fi
  printf 'http://%s\n' "${host_value%/}"
}

get_ollama_probe_base_url() {
  local base_url
  base_url="$(get_ollama_base_url "${1:-0.0.0.0:11434}")"
  if [[ "$base_url" == http://0.0.0.0:* ]]; then
    printf '%s\n' "${base_url/http:\/\/0.0.0.0/http:\/\/127.0.0.1}"
    return 0
  fi
  printf '%s\n' "$base_url"
}

show_model_status() {
  # The package can start without models; show operators the local pull command when no model is present.
  local base_url="$1"
  local ollama_exe="$2"
  shift 2
  local recommended_models=("$@")
  if command -v curl >/dev/null 2>&1; then
    for _ in 1 2 3 4 5; do
      if body="$(curl -fsS --max-time 2 "$base_url/api/tags" 2>/dev/null)"; then
        if printf '%s' "$body" | grep -q '"name"'; then
          echo "Available models: see $base_url/api/tags"
        else
          echo "Available models: none"
          echo "Download recommended models before using AI chat:"
          for model in "${recommended_models[@]}"; do
            echo "  \"$ollama_exe\" pull $model"
          done
        fi
        return 0
      fi
      sleep 1
    done
  fi
  echo "Model list check skipped: curl unavailable or Ollama API did not respond in time."
}

enabled_flag() {
  local value
  value="$(printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]')"
  [[ "$value" == "1" || "$value" == "true" || "$value" == "yes" || "$value" == "on" ]]
}

ensure_open_webui_installed() {
  local base_dir="$1"
  local python_exe="$base_dir/runtime/python-linux/bin/python3"
  local site_packages="$base_dir/runtime/open-webui/site-packages-linux"
  if [[ ! -x "$python_exe" || ! -d "$site_packages/open_webui" ]]; then
    echo "Bundled Linux Python runtime not found; Open WebUI is disabled." >&2
    return 1
  fi
  if ! PYTHONPATH="$site_packages" "$python_exe" - <<'PY' >/dev/null 2>&1
import sys
import open_webui
raise SystemExit(0 if sys.version_info >= (3, 11) else 1)
PY
  then
    echo "Bundled Linux Open WebUI runtime validation failed." >&2
    return 1
  fi
  export PYTHONPATH="$site_packages"
  printf '%s\n' "$python_exe"
}

start_open_webui() {
  if ! enabled_flag "${OPEN_WEBUI_ENABLED:-true}"; then
    echo "Open WebUI disabled by OPEN_WEBUI_ENABLED."
    return 0
  fi
  local base_dir="$1"
  local run_dir="$2"
  local log_dir="$3"
  local ollama_host="$4"
  local open_webui_dir="$base_dir/runtime/open-webui"
  local data_dir="${OPEN_WEBUI_DATA_DIR:-$base_dir/data/open-webui}"
  local pid_file="$run_dir/open-webui.pid"
  local host_value="${OPEN_WEBUI_HOST:-0.0.0.0}"
  local port_value="${OPEN_WEBUI_PORT:-8080}"
  local display_host="$host_value"
  if [[ "$display_host" == "0.0.0.0" || "$display_host" == "::" ]]; then
    display_host="127.0.0.1"
  fi
  mkdir -p "$open_webui_dir" "$data_dir"
  if [[ -f "$pid_file" ]]; then
    local old_pid
    old_pid="$(tr -d '[:space:]' < "$pid_file")"
    if [[ "$old_pid" =~ ^[0-9]+$ ]] && kill -0 "$old_pid" >/dev/null 2>&1; then
      echo "Open WebUI is already running, PID=$old_pid"
      echo "Open WebUI URL: http://$display_host:$port_value"
      return 0
    fi
    rm -f "$pid_file"
  fi
  local python_exe
  python_exe="$(ensure_open_webui_installed "$base_dir")"
  export PYTHONPATH="$base_dir/runtime/open-webui/site-packages-linux"
  export OLLAMA_BASE_URL="${OPEN_WEBUI_OLLAMA_BASE_URL:-$(get_ollama_probe_base_url "$ollama_host")}"
  export DATA_DIR="$data_dir"
  export WEBUI_AUTH="${OPEN_WEBUI_AUTH:-true}"
  nohup "$python_exe" -c 'from open_webui import app; app()' serve --host "$host_value" --port "$port_value" > "$log_dir/open-webui.log" 2> "$log_dir/open-webui-error.log" &
  local pid="$!"
  echo "$pid" > "$pid_file"
  echo "Open WebUI started, PID=$pid"
  echo "Open WebUI URL: http://$display_host:$port_value"
  echo "Open WebUI Ollama base URL: $OLLAMA_BASE_URL"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OLLAMA_EXE="$BASE_DIR/runtime/linux/ollama/ollama"
RUN_DIR="$BASE_DIR/run"
LOG_DIR="$BASE_DIR/logs"
MODELS_DIR="$BASE_DIR/models"
PID_FILE="$RUN_DIR/ollama.pid"

mkdir -p "$RUN_DIR" "$LOG_DIR" "$MODELS_DIR"
if [[ ! -x "$OLLAMA_EXE" ]]; then
  echo "Ollama executable not found or not executable: $OLLAMA_EXE"
  exit 1
fi

export OLLAMA_HOST="${OLLAMA_HOST:-0.0.0.0:11434}"
export OLLAMA_MODELS="${OLLAMA_MODELS:-$MODELS_DIR}"
# Model names match the backend router defaults and can be overridden by deployment environment variables.
CHAT_MODEL="${FM_AI_OLLAMA_CHAT_MODEL:-qwen2.5:7b}"
COMMAND_MODEL="${FM_AI_OLLAMA_COMMAND_MODEL:-llama3.2:3b}"
REASONING_MODEL="${FM_AI_OLLAMA_REASONING_MODEL:-deepseek-r1:8b}"

if [[ -f "$PID_FILE" ]]; then
  PID="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" >/dev/null 2>&1; then
    echo "Ollama is already running, PID=$PID"
    echo "Host: $OLLAMA_HOST"
    echo "Model directory: $OLLAMA_MODELS"
    show_model_status "$(get_ollama_probe_base_url "$OLLAMA_HOST")" "$OLLAMA_EXE" "$CHAT_MODEL" "$COMMAND_MODEL" "$REASONING_MODEL"
    start_open_webui "$BASE_DIR" "$RUN_DIR" "$LOG_DIR" "$OLLAMA_HOST" || echo "Open WebUI failed to start; Ollama is still running."
    exit 0
  fi
  rm -f "$PID_FILE"
fi

OLLAMA_PORT="$(get_ollama_port "$OLLAMA_HOST")"
if command -v lsof >/dev/null 2>&1 && lsof -iTCP:"$OLLAMA_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Ollama port $OLLAMA_PORT is already in use."
  exit 1
fi

nohup "$OLLAMA_EXE" serve > "$LOG_DIR/ollama.log" 2> "$LOG_DIR/ollama-error.log" &
PID="$!"
echo "$PID" > "$PID_FILE"
echo "Ollama started, PID=$PID"
echo "Host: $OLLAMA_HOST"
echo "Model directory: $OLLAMA_MODELS"
echo "Logs: $LOG_DIR"
show_model_status "$(get_ollama_probe_base_url "$OLLAMA_HOST")" "$OLLAMA_EXE" "$CHAT_MODEL" "$COMMAND_MODEL" "$REASONING_MODEL"
start_open_webui "$BASE_DIR" "$RUN_DIR" "$LOG_DIR" "$OLLAMA_HOST" || echo "Open WebUI failed to start; Ollama is still running."
