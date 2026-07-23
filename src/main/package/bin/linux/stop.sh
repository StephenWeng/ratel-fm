#!/usr/bin/env bash
# Ratel FM Linux 关闭脚本。
# 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
# 说明：本脚本只关闭 Ratel FM，不关闭同级独立部署的 Ollama 或 Qdrant。

set -euo pipefail

# 步骤一：根据脚本目录定位 PID 文件。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PID_FILE="$BASE_DIR/run/ratel-fm.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "Ratel FM 未运行：未找到 PID 文件。"
  exit 0
fi

PID="$(tr -d '[:space:]' < "$PID_FILE")"
if [[ ! "$PID" =~ ^[0-9]+$ ]]; then
  rm -f "$PID_FILE"
  echo "PID 文件内容无效，已清理。"
  exit 0
fi

# 步骤二：先尝试优雅终止，超时后强制终止。
if ! kill -0 "$PID" >/dev/null 2>&1; then
  rm -f "$PID_FILE"
  echo "Ratel FM 未运行：PID=$PID 不存在，已清理 PID 文件。"
  exit 0
fi

kill "$PID"
for _ in {1..15}; do
  if ! kill -0 "$PID" >/dev/null 2>&1; then
    rm -f "$PID_FILE"
    echo "Ratel FM 已关闭，PID=$PID"
    exit 0
  fi
  sleep 1
done

kill -9 "$PID" >/dev/null 2>&1 || true
rm -f "$PID_FILE"
echo "Ratel FM 已强制关闭，PID=$PID"
