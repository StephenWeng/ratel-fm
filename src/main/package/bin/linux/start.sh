#!/usr/bin/env bash
# Ratel FM Linux 启动脚本。
# 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
# 说明：本脚本只启动 Ratel FM，不启动同级独立部署的 Ollama 或 Qdrant。

set -euo pipefail

# 步骤一：根据脚本目录推导部署根目录，确保脚本移动到任意目录后仍能运行。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
JAVA_BIN="$BASE_DIR/runtime/jdk/bin/java"
KEYTOOL_BIN="$BASE_DIR/runtime/jdk/bin/keytool"
JAR_FILE="$BASE_DIR/app/ratel-fm.jar"
CONFIG_DIR="$BASE_DIR/config"
CERT_SCRIPT="$SCRIPT_DIR/cert/generate-https-cert.sh"
CERT_DIR="$BASE_DIR/certs"
SERVER_KEYSTORE="$CERT_DIR/ratel-fm-server.p12"
SERVER_PASSWORD_FILE="$CERT_DIR/ratel-fm-server.password"
DATA_DIR="$BASE_DIR/data"
DATABASE_TEMPLATE_DIR="$BASE_DIR/database-template"
TEMPLATE_DB_FILE="$DATABASE_TEMPLATE_DIR/ratel-fm.mv.db"
RUNTIME_DB_FILE="$DATA_DIR/ratel-fm.mv.db"
BACKUP_DIR="$BASE_DIR/backup"
LOG_DIR="$BASE_DIR/logs"
RUN_DIR="$BASE_DIR/run"
UPLOAD_DIR="$BASE_DIR/uploads/avatars"
FILES_DIR="$BASE_DIR/files"
PID_FILE="$RUN_DIR/ratel-fm.pid"
CONSOLE_LOG="$LOG_DIR/console.log"
CONSOLE_ERROR_LOG="$LOG_DIR/console-error.log"
LOGBACK_CONFIG="$CONFIG_DIR/logback-spring.xml"
SERVER_PORT="${SERVER_PORT:-38000}"
SERVER_SERVLET_CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH:-/ratel/fm}"
RATEL_HTTPS_ENABLED="${RATEL_HTTPS_ENABLED:-true}"
RATEL_HTTPS_PORT="${RATEL_HTTPS_PORT:-38443}"

bool_enabled() {
  case "$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')" in
    1|true|yes|y|on|enable|enabled) return 0 ;;
    0|false|no|n|off|disable|disabled) return 1 ;;
    *)
      echo "RATEL_HTTPS_ENABLED 必须是 true 或 false，当前值：$1"
      exit 1
      ;;
  esac
}

# 步骤二：创建运行目录，并检查内置 JDK 和应用 Jar。
mkdir -p "$DATA_DIR" "$BACKUP_DIR" "$LOG_DIR" "$RUN_DIR" "$UPLOAD_DIR" "$FILES_DIR"
if [[ ! -f "$RUNTIME_DB_FILE" && -f "$TEMPLATE_DB_FILE" ]]; then
  # 首次部署时从模板库初始化 H2 文件库；已有运行库时绝不覆盖，避免升级部署丢失业务数据。
  cp "$TEMPLATE_DB_FILE" "$RUNTIME_DB_FILE"
  echo "已初始化 H2 数据库：$RUNTIME_DB_FILE"
fi
if [[ ! -x "$JAVA_BIN" ]]; then
  echo "内置 JDK 不存在或不可执行：$JAVA_BIN"
  exit 1
fi
if bool_enabled "$RATEL_HTTPS_ENABLED" && [[ ! -x "$KEYTOOL_BIN" ]]; then
  echo "内置 keytool 不存在或不可执行：$KEYTOOL_BIN"
  exit 1
fi
if [[ ! -f "$JAR_FILE" ]]; then
  echo "应用 Jar 不存在：$JAR_FILE"
  exit 1
fi
if bool_enabled "$RATEL_HTTPS_ENABLED" && [[ ! -x "$CERT_SCRIPT" ]]; then
  echo "HTTPS 证书脚本不存在或不可执行：$CERT_SCRIPT"
  exit 1
fi

# 步骤三：检查历史 PID，避免重复启动。
if [[ -f "$PID_FILE" ]]; then
  PID="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" >/dev/null 2>&1; then
    echo "Ratel FM 已在运行，PID=$PID"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

# 步骤四：启用 HTTPS 时，按当前主机名和 IP 动态生成服务证书。
HTTPS_ARGS=()
if bool_enabled "$RATEL_HTTPS_ENABLED"; then
  if [[ "$RATEL_HTTPS_PORT" == "$SERVER_PORT" ]]; then
    echo "RATEL_HTTPS_PORT 启用 HTTPS 时不能和 SERVER_PORT 相同。"
    exit 1
  fi
  "$CERT_SCRIPT" \
    --base-dir "$BASE_DIR" \
    --keytool "$KEYTOOL_BIN" \
    --https-port "$RATEL_HTTPS_PORT" \
    --context-path "$SERVER_SERVLET_CONTEXT_PATH"
  if [[ ! -f "$SERVER_KEYSTORE" || ! -f "$SERVER_PASSWORD_FILE" ]]; then
    echo "HTTPS 服务证书生成失败：$SERVER_KEYSTORE"
    exit 1
  fi
  SERVER_KEYSTORE_PASSWORD="$(tr -d '[:space:]' < "$SERVER_PASSWORD_FILE")"
  HTTPS_ARGS=(
    "--server.port=$RATEL_HTTPS_PORT"
    "--server.ssl.enabled=true"
    "--server.ssl.key-store=$SERVER_KEYSTORE"
    "--server.ssl.key-store-password=$SERVER_KEYSTORE_PASSWORD"
    "--server.ssl.key-store-type=PKCS12"
    "--server.ssl.key-alias=ratel-fm-server"
    "--app.https.http-enabled=true"
    "--app.https.http-port=$SERVER_PORT"
  )
fi

# 步骤五：后台启动应用，使用内置 JDK、外置配置和独立日志目录。
# 说明：项目模块和 AI 检索增加后默认堆内存提升到 1g/2g，并允许部署时通过环境变量覆盖。
JVM_XMS="${RATEL_JVM_XMS:-1g}"
JVM_XMX="${RATEL_JVM_XMX:-2g}"
JVM_MAX_METASPACE="${RATEL_JVM_MAX_METASPACE:-512m}"
# RATEL_JAVA_OPTS 用于现场临时扩容、诊断或启用特殊 JVM 参数；按 shell 规则展开。
# shellcheck disable=SC2086
nohup "$JAVA_BIN" \
  -Xms"$JVM_XMS" \
  -Xmx"$JVM_XMX" \
  -XX:MaxMetaspaceSize="$JVM_MAX_METASPACE" \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath="$LOG_DIR" \
  ${RATEL_JAVA_OPTS:-} \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -DLOG_HOME="$LOG_DIR" \
  -DFM_ATTACHMENT_BASE_DIR="$FILES_DIR" \
  -jar "$JAR_FILE" \
  --spring.config.additional-location="file:$CONFIG_DIR/" \
  --logging.config="$LOGBACK_CONFIG" \
  "${HTTPS_ARGS[@]}" \
  > "$CONSOLE_LOG" 2> "$CONSOLE_ERROR_LOG" &

PID="$!"
echo "$PID" > "$PID_FILE"
sleep 2
if ! kill -0 "$PID" >/dev/null 2>&1; then
  rm -f "$PID_FILE"
  echo "Ratel FM 启动失败，请查看日志：$CONSOLE_ERROR_LOG"
  exit 1
fi

echo "Ratel FM 启动成功，PID=$PID"
echo "HTTP 访问地址：http://127.0.0.1:$SERVER_PORT$SERVER_SERVLET_CONTEXT_PATH"
if bool_enabled "$RATEL_HTTPS_ENABLED"; then
  echo "HTTPS 访问地址：https://127.0.0.1:$RATEL_HTTPS_PORT$SERVER_SERVLET_CONTEXT_PATH"
  echo "其他电脑使用 HTTPS 前需要信任 CA 证书：$CERT_DIR/ratel-local-ca.cer"
fi
echo "数据目录：$DATA_DIR"
echo "附件目录：$FILES_DIR"
echo "日志目录：$LOG_DIR"
