# Ratel FM Qdrant 独立包

开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。

## 目录结构

```text
ratel-fm-qdrant
├── bin/windows           # Windows 原生 .bat 启动、关闭、状态脚本
├── bin/linux             # Linux .sh 启动、关闭、状态脚本
├── logs                  # Qdrant 控制台日志目录，启动脚本自动创建或使用
├── run                   # Qdrant PID 文件目录，启动、关闭脚本自动维护
├── snapshots             # Qdrant 快照目录
├── static                # Qdrant Web UI v0.2.15 静态资源，由 /dashboard 提供
├── storage               # Qdrant 本地持久化数据目录
├── runtime/windows/qdrant # Qdrant Windows x64 运行时
└── runtime/linux/qdrant   # Qdrant Linux x64 运行时
```

## 放置位置

将本目录解压到 Ratel FM 主部署目录的同级目录：

```text
deploy-root
├── ratel-fm-0.0.1-SNAPSHOT-portable
├── ratel-fm-ollama
└── ratel-fm-qdrant
```

Ratel FM、Ollama、Qdrant 完全隔离启停。Ratel FM 的启动、关闭脚本不会启动或关闭本目录下的 Qdrant；需要本地向量数据库时，单独执行本目录的 Qdrant 启停脚本。

Windows：

```bat
bin\windows\start.bat
bin\windows\status.bat
bin\windows\stop.bat
```

Linux：

```bash
chmod +x bin/linux/*.sh runtime/linux/qdrant/qdrant
bin/linux/start.sh
bin/linux/status.sh
bin/linux/stop.sh
```

## 内置运行时

当前独立包同时封装官方 Windows x64 MSVC 和 Linux x64 GNU 运行时：

```text
runtime/windows/qdrant/qdrant.exe
runtime/linux/qdrant/qdrant
```

Windows 和 Linux 启动脚本只调用各自平台目录中的程序，数据和快照目录由两个平台共用。

默认监听地址：

```text
http://0.0.0.0:6333
```

其它电脑访问时使用 Qdrant 所在电脑的真实内网 IP，例如 `http://10.105.12.136:6333`。Windows BAT 会尝试创建 6333 和 6334 防火墙规则；普通权限创建失败时只告警并继续启动，本机仍可访问，需要局域网访问时再以管理员身份补充规则。

如果 `http://127.0.0.1:6333/` 能返回版本信息，但 `http://10.105.12.136:6333/` 连接被拒绝，说明当前 Qdrant 进程只监听在 loopback。处理步骤：

1. 执行 `bin\windows\stop.bat` 停掉旧进程。
2. 检查并清除用户或系统环境变量中的 `QDRANT_HOST=127.0.0.1`、`QDRANT__SERVICE__HOST=127.0.0.1`。
3. 重新执行 `bin\windows\start.bat`，确认 HTTP 端口为 `6333`。
4. 在 Windows 防火墙放行 TCP 6333 和 6334。

根路径 `/` 返回 JSON 版本信息即表示 HTTP API 正常。独立包已内置 Qdrant Web UI v0.2.15 静态资源，Windows 启动脚本固定从包根目录启动 Qdrant，访问 `http://127.0.0.1:6333/dashboard/` 应返回管理页面。若该地址返回 404，检查包内 `static/index.html` 是否存在，以及是否使用本包的启动脚本启动。

Dashboard 静态资源来自 Qdrant 官方 `qdrant-web-ui` v0.2.15，按 Apache License 2.0 分发，许可证见 `LICENSE-QDRANT-WEB-UI.txt`。

默认 gRPC 端口：

```text
0.0.0.0:6334
```

## 常用环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| QDRANT_HOST | `0.0.0.0` | Qdrant 独立包启动脚本使用的监听地址 |
| QDRANT_HTTP_PORT | `6333` | Qdrant HTTP API 端口 |
| QDRANT_GRPC_PORT | `6334` | Qdrant gRPC 端口 |
| QDRANT_STORAGE_DIR | `<ratel-fm-qdrant>/storage` | Qdrant 本地数据目录 |
| QDRANT_SNAPSHOTS_DIR | `<ratel-fm-qdrant>/snapshots` | Qdrant 快照目录 |

## 启停边界

- Ratel FM 启动、关闭脚本只处理 Ratel FM。
- Ollama 启动、关闭脚本只处理 Ollama。
- Qdrant 启动、关闭脚本只处理本包内由 PID 文件记录的 Qdrant 进程。
- 任一组件启动或关闭失败，都不影响另外两个组件。
