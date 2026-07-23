#!/usr/bin/env bash
# Ratel FM integrated Linux start script.
# Starts Ratel FM and available independent packages by delegating to their own scripts.
# A component failure is reported but does not stop the remaining components.

set +e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RATEL_BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOY_ROOT="${DEPLOY_ROOT:-$(cd "$RATEL_BASE_DIR/.." && pwd)}"

find_component_dir() {
  local preferred_name="$1"
  local wildcard="$2"
  if [[ -d "$DEPLOY_ROOT/$preferred_name" ]]; then
    printf '%s\n' "$DEPLOY_ROOT/$preferred_name"
    return 0
  fi
  find "$DEPLOY_ROOT" -maxdepth 1 -type d -name "$wildcard" | head -n 1
}

run_component_start() {
  local name="$1"
  local script_path="$2"
  local work_dir="$3"
  if [[ ! -f "$script_path" ]]; then
    echo "[$name] start script not found: $script_path"
    return 0
  fi
  echo "[$name] starting..."
  (cd "$work_dir" && bash "$script_path")
  local code=$?
  if [[ $code -eq 0 ]]; then
    echo "[$name] start command completed."
  else
    echo "[$name] start command exited with code $code."
  fi
  return 0
}

echo "Deploy root: $DEPLOY_ROOT"

OLLAMA_DIR="$(find_component_dir "ratel-fm-ollama" "ratel-fm-ollama*")"
QDRANT_DIR="$(find_component_dir "ratel-fm-qdrant" "ratel-fm-qdrant*")"

if [[ -n "$OLLAMA_DIR" ]]; then
  run_component_start "Ollama" "$OLLAMA_DIR/bin/linux/start.sh" "$OLLAMA_DIR"
else
  echo "[Ollama] package directory not found under $DEPLOY_ROOT, skipped."
fi

if [[ -n "$QDRANT_DIR" ]]; then
  run_component_start "Qdrant" "$QDRANT_DIR/bin/linux/start.sh" "$QDRANT_DIR"
else
  echo "[Qdrant] package directory not found under $DEPLOY_ROOT, skipped."
fi

run_component_start "Ratel FM" "$SCRIPT_DIR/start.sh" "$RATEL_BASE_DIR"

echo "Integrated start finished. Check each package logs if a component reported warnings."
exit 0
