# Ratel FM 单包部署说明

开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。

## 目录结构

```text
ratel-fm-*-portable
├── app                 # Spring Boot 可执行 Jar，包含前后端与 AOT 预处理结果
├── bin                 # Windows 和 Linux 启动、关闭、状态脚本，包含独立脚本和总控脚本
├── backup              # 数据库备份目录，脚本启动时自动创建
├── certs               # HTTPS 本地 CA 和动态服务证书目录，脚本启动时自动创建
├── config              # 外置应用配置和日志配置
├── data                # H2 文件数据库目录，脚本启动时自动创建
├── database-template   # 预初始化 H2 模板库，首次启动时复制到 data 目录
├── db                  # 数据库初始化 SQL
├── logs                # 运行日志目录，脚本启动时自动创建
├── run                 # PID 文件目录，脚本启动时自动创建
├── runtime/jdk         # 内置 JDK，部署机器不需要单独安装 JDK
└── uploads/avatars     # 头像上传目录，脚本启动时自动创建
```

## 可选 Ollama 独立包

AI 助手默认优先访问本地 Ollama。Ollama 不打入 Ratel FM 主部署包，可使用独立包解压到 Ratel FM 主目录的同级目录：

```text
deploy-root
├── ratel-fm-*-portable
└── ratel-fm-ollama
```

Ratel FM 与 Ollama/Open WebUI 完全隔离启停。Ratel FM 的独立启动、关闭脚本只处理主应用，不会启动或关闭 Ollama。需要本地 AI 助手或模型控制台时，可以单独进入 `ratel-fm-ollama` 执行 Ollama 启停脚本，也可以使用主包里的总控脚本统一调用各独立包脚本。

打包时也保持隔离：常规 `mvn package` 只生成 Ratel FM 主包；只有 Ollama 运行时、启停脚本或模型发生变化时，才执行 `mvn package -Pwith-ollama` 重新生成 Ollama 独立包。

Windows：

```bat
cd ratel-fm-ollama
bin\windows\start.bat
bin\windows\stop.bat
```

Linux：

```bash
cd ratel-fm-ollama
chmod +x bin/linux/*.sh runtime/linux/ollama/ollama
bin/linux/start.sh
bin/linux/stop.sh
```

Ollama 独立包同时内置官方 Windows x64 和 Linux x64 运行时，并从工程根目录 `ollama-models` 自动打入推荐模型。Windows 和 Linux 脚本分别位于 `bin/windows` 和 `bin/linux`，两个平台共用 `models` 目录。

Ollama 独立包脚本默认还会启动 Open WebUI 控制台，访问地址为 `http://10.105.12.136:8080`，现场可改为实际 Ollama 电脑 IP。独立包内置便携 Python 和预装 Open WebUI 依赖；部署机不需要安装 Python，启动时也不执行在线安装。如果包内 Open WebUI 运行时损坏，只会提示警告，不影响 Ollama 服务继续运行。

Ratel FM 支持 Ollama 模型路由：普通业务问答默认使用 `qwen2.5:7b`，语音/操作指令默认使用 `llama3.2:3b`，复杂分析默认使用 `deepseek-r1:8b`。如果优先模型未下载，会自动降级到本机已下载的其他 Ollama 模型。

可用环境变量：

- `OLLAMA_HOST`：Ollama 独立包启动脚本使用，默认 `0.0.0.0:11434`，便于其它电脑访问。
- `OLLAMA_MODELS`：Ollama 独立包启动脚本使用，默认 `<ratel-fm-ollama>/models`。
- `OPEN_WEBUI_ENABLED`：是否随 Ollama 包启动 Open WebUI，默认 `true`。
- `OPEN_WEBUI_PORT`：Open WebUI 端口，默认 `8080`。
- `OPEN_WEBUI_DATA_DIR`：Open WebUI 数据目录，默认 `<ratel-fm-ollama>/data/open-webui`。
- `FM_AI_MODEL_PROVIDER`：Ratel FM 通用大模型提供方，支持 `ollama` 或 `qwen`，默认 `ollama`。
- `FM_AI_OLLAMA_BASE_URL`：Ratel FM 后端访问 Ollama 的地址，默认 `http://10.105.12.136:11434`，远程部署时可改为实际 Ollama 电脑 IP。
- `FM_AI_OLLAMA_CHAT_MODEL`：普通业务问答优先模型，默认 `qwen2.5:7b`。
- `FM_AI_OLLAMA_COMMAND_MODEL`：语音/操作指令优先模型，默认 `llama3.2:3b`。
- `FM_AI_OLLAMA_REASONING_MODEL`：复杂分析优先模型，默认 `deepseek-r1:8b`。
- `FM_AI_VECTOR_DATABASE_PROVIDER`：向量数据库提供方，支持 `qdrant` 或 `h2`，默认 `qdrant`，二者互斥且不自动降级。
- `FM_AI_QDRANT_BASE_URL`：Ratel FM 后端访问 Qdrant 的地址，默认 `http://10.105.12.136:6333`，远程部署时可改为实际 Qdrant 电脑 IP。

## Windows 启动和关闭

只启动、关闭 Ratel FM 主应用：

```bat
bin\windows\start.bat
bin\windows\stop.bat
bin\windows\status.bat
```

统一启动、关闭 Ratel FM、Ollama、Qdrant：

```bat
bin\windows\start-all.bat
bin\windows\stop-all.bat
```

Windows 脚本是完整的原生 `.bat` 实现，不依赖 PowerShell。总控脚本会按主应用同级目录查找独立包，并调用其 `bin/windows` 下的脚本；Linux 总控脚本调用 `bin/linux` 下的脚本。

## HTTPS、麦克风和浏览器定位

便携包默认同时提供 HTTP 和 HTTPS：

```text
http://当前IP:38000/ratel/fm
https://当前IP:38443/ratel/fm
```

普通业务可以继续使用 HTTP。语音控制、麦克风授权和浏览器经纬度定位建议使用 HTTPS，因为现代浏览器通常只在 HTTPS、localhost 或 127.0.0.1 这类安全上下文中开放这些能力。

启动脚本会自动：

1. 在 `certs` 目录生成固定的本地根证书 CA：`ratel-local-ca.cer`。
2. 每次启动读取当前电脑主机名和 IPv4 地址。
3. 重新生成包含当前 IP、`localhost`、`127.0.0.1` 和电脑名的服务证书：`ratel-fm-server.p12`。
4. 将 Spring Boot 主端口切换为 HTTPS 38443，并额外保留 HTTP 38000。

笔记本 IP 改变后，只需要重新执行 `bin\windows\start.bat`，服务证书会自动按新 IP 生成。其他电脑只需要信任一次 `certs\ratel-local-ca.cer`，不需要为每个新 IP 重新安装证书。

Windows 客户端安装 CA 的方式：

```bat
bin\windows\cert\install-local-ca-current-user.bat
```

如果是在其他电脑访问，把服务器上的 `certs\ratel-local-ca.cer` 复制到该电脑后，双击安装到“当前用户”的“受信任的根证书颁发机构”，或把同目录脚本一起复制过去执行。

可用环境变量：

- `RATEL_HTTPS_ENABLED`：是否启用 HTTPS，默认 `true`。
- `RATEL_HTTPS_PORT`：HTTPS 端口，默认 `38443`。
- `SERVER_PORT`：保留的 HTTP 端口，默认 `38000`。

## Linux 启动和关闭

只启动、关闭 Ratel FM 主应用：

```bash
chmod +x bin/linux/*.sh bin/linux/cert/*.sh runtime/jdk/bin/java runtime/jdk/bin/keytool
bin/linux/start.sh
bin/linux/stop.sh
bin/linux/status.sh
```

统一启动、关闭可用组件：

```bash
bin/linux/start-all.sh
bin/linux/stop-all.sh
```

当前 zip 内置的是 Windows JDK。如果要在 Linux 上用同一个目录结构部署，需要把 `runtime/jdk` 替换为 Linux x64 JDK 24 目录，脚本和应用 Jar 不需要变化。

## 配置和日志

- 应用配置：`config/application.yml`
- 默认数据库：H2 文件库，数据文件位于 `data/ratel-fm.mv.db`
- 预置模板库：`database-template/ratel-fm.mv.db`
- 启动优先级：如果 `data/ratel-fm.mv.db` 已存在，启动脚本直接使用该运行库；如果不存在但模板库存在，首次启动时复制模板库；如果两者都不存在，H2 会按配置自动创建空文件库
- PostgreSQL 配置：`config/application-postgres.yml`，需要正式数据库时启用 `postgres` profile
- 日志配置：`config/logback-spring.xml`
- JVM 参数：启动脚本默认 `-Xms1g -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC`，OOM 时会在 `logs` 目录生成 heap dump
- JVM 参数覆盖：可通过环境变量 `RATEL_JVM_XMS`、`RATEL_JVM_XMX`、`RATEL_JVM_MAX_METASPACE`、`RATEL_JAVA_OPTS` 调整
- 数据库初始化：`db/init.sql`
- 系统日志：`logs/system.log`
- 操作审计日志：`logs/operation-audit.log`
- 控制台日志：`logs/console.log` 和 `logs/console-error.log`
