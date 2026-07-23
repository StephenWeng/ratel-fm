#!/usr/bin/env bash
# Ratel FM Linux 状态查询脚本。
# 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
# 说明：本脚本只查询 Ratel FM 的 PID 状态，Ollama 和 Qdrant 请使用各自独立包脚本。

set -euo pipefail

# 步骤一：根据脚本目录定位 PID 文件。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PID_FILE="$BASE_DIR/run/ratel-fm.pid"
HTTPS_INFO_FILE="$BASE_DIR/certs/https-info.txt"

if [[ ! -f "$PID_FILE" ]]; then
  echo "Ratel FM 未运行。"
  exit 1
fi

PID="$(tr -d '[:space:]' < "$PID_FILE")"
if [[ ! "$PID" =~ ^[0-9]+$ ]]; then
  echo "Ratel FM 状态未知：PID 文件内容无效。"
  exit 1
fi

# 步骤二：按 PID 查询进程状态。
if kill -0 "$PID" >/dev/null 2>&1; then
  echo "Ratel FM 正在运行，PID=$PID"
  if [[ -f "$HTTPS_INFO_FILE" ]]; then
    echo "HTTPS 证书信息："
    cat "$HTTPS_INFO_FILE"
  fi
  exit 0
fi

echo "Ratel FM 未运行：PID=$PID 不存在。"
exit 1
