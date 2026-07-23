# Ratel FM Ollama 独立包

开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。

## 目录结构

```text
ratel-fm-ollama
├── bin/windows           # Windows 原生 .bat 启动、关闭脚本
├── bin/linux             # Linux .sh 启动、关闭脚本
├── data/open-webui       # Open WebUI 数据目录，保存账号、会话和页面配置
├── logs                  # Ollama/Open WebUI 控制台日志目录，启动脚本自动创建或使用
├── models                # Ollama 模型目录，启动脚本默认设置 OLLAMA_MODELS 指向这里
├── run                   # Ollama/Open WebUI PID 文件目录，启动、关闭脚本自动维护
├── runtime/windows/ollama # Ollama Windows x64 运行时
├── runtime/linux/ollama   # Ollama Linux x64 运行时
└── runtime/open-webui    # Open WebUI 虚拟环境和离线 wheels 目录
```

## 放置位置

将本目录解压到 Ratel FM 主部署目录的同级目录：

```text
deploy-root
├── ratel-fm-0.0.1-SNAPSHOT-portable
└── ratel-fm-ollama
```

Ratel FM 与 Ollama/Open WebUI 完全隔离启停。Ratel FM 的启动、关闭脚本不会启动或关闭本目录下的 Ollama；需要本地 AI 助手或模型控制台时，单独执行本目录的 Ollama 启停脚本。

Windows：

```bat
bin\windows\start.bat
bin\windows\stop.bat
```

Linux：

```bash
chmod +x bin/linux/*.sh runtime/linux/ollama/ollama
bin/linux/start.sh
bin/linux/stop.sh
```

## 内置运行时

当前独立包同时封装官方 Windows x64 和 Linux x64 Ollama 运行时：

```text
runtime/windows/ollama/ollama.exe
runtime/linux/ollama/ollama
```

Windows 和 Linux 启动脚本只调用各自平台目录中的程序，模型目录由两个平台共用。

当前独立包默认内置 Ratel FM 推荐模型，模型文件由工程根目录 `ollama-models` 自动打入本包的 `models` 目录：

| 模型 | 用途 | 约占空间 |
| --- | --- | --- |
| `qwen2.5:7b` | 普通业务问答 | 4.7 GB |
| `llama3.2:3b` | 语音/操作指令 | 2.0 GB |
| `deepseek-r1:8b` | 复杂分析 | 5.2 GB |
| `bge-m3:latest` | 知识索引和智能检索 embedding | 1.2 GB |

注意：`bin\windows\start.bat` 使用本包内置模型目录。启动脚本会检查 `/api/tags`；如果手工精简过包内容，需要重新放入完整 `models` 目录，或在有网络的机器上重新下载模型：

```bat
runtime\windows\ollama\ollama.exe pull qwen2.5:7b
runtime\windows\ollama\ollama.exe pull llama3.2:3b
runtime\windows\ollama\ollama.exe pull deepseek-r1:8b
runtime\windows\ollama\ollama.exe pull bge-m3:latest
```

Ratel FM 会按业务场景选择模型：

| 场景 | 默认模型 | 说明 |
| --- | --- | --- |
| 业务问答 | `qwen2.5:7b` | 中文财务 ERP 问答、系统数据解释 |
| 语音/操作指令 | `llama3.2:3b` | 短指令、菜单跳转、填表意图识别 |
| 复杂分析 | `deepseek-r1:8b` | 报表分析、原因解释、趋势和风险判断 |
| 知识向量 | `bge-m3:latest` | 知识索引、智能检索和 Qdrant 查询向量 |

如果某个对话优先模型未下载，系统会自动降级到本机已下载的其他 Ollama 模型；如果本机没有任何对话模型，则提示下载模型，不会直接展示内部系统上下文。`bge-m3:latest` 是 embedding 模型，在 Qdrant 模式下必须可用，不会降级到千问 embedding 或 H2 知识表。

## Open WebUI 控制台

Ollama 独立包脚本会默认同时管理 Open WebUI：

```text
http://10.105.12.136:8080
```

Open WebUI 用于在浏览器里查看和调用 Ollama 模型，不承载 Ratel FM 的业务权限和业务数据。启动脚本会先启动 Ollama，再启动 Open WebUI；关闭脚本会先停止 Open WebUI，再停止 Ollama。Open WebUI 启动失败只输出警告，不会影响 Ollama 服务继续运行。

Open WebUI 0.10.2 使用独立包内的 Python 3.11.9 `runtime/python/python.exe` 和 `runtime/open-webui/site-packages`。部署机不需要安装 Python，启动脚本也不会修改系统 PATH、写入注册表或执行在线 `pip install`。这两部分运行时由 `src/main/resources/build-ollama.bat` 在构建机准备并打入 ZIP；如果包内运行时不完整，Open WebUI 会明确跳过。

Open WebUI 默认监听 `0.0.0.0:8080`，便于其它电脑访问。Windows BAT 会尝试创建 11434 和 8080 防火墙规则；普通权限创建失败时只告警并继续启动，本机访问不受影响。Open WebUI 连接 Ollama 的地址默认使用本机探测地址 `http://127.0.0.1:11434`，因为两者在同一台电脑上运行。

## 模型目录

默认模型目录：

```text
models
```

Ollama 独立包启动脚本会在启动 Ollama 前设置：

```text
OLLAMA_MODELS=<ratel-fm-ollama>/models
```

如果要使用其他模型目录，可以在启动 Ollama 前设置 `OLLAMA_MODELS`。独立包默认监听 `0.0.0.0:11434`，便于其它电脑访问；Ratel FM 后端默认通过 `FM_AI_OLLAMA_BASE_URL=http://10.105.12.136:11434` 指向该服务，现场可改为实际 Ollama 电脑 IP，并在 Ollama 所在电脑放行 TCP 11434。

## 常用环境变量

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| OLLAMA_HOST | `0.0.0.0:11434` | Ollama 独立包启动脚本使用的监听地址 |
| OLLAMA_MODELS | `<ratel-fm-ollama>/models` | Ollama 独立包启动脚本使用的模型目录 |
| OLLAMA_MAX_LOADED_MODELS | `2` | 同时保留 embedding 与聊天模型，避免机械盘频繁换模 |
| OLLAMA_NUM_PARALLEL | `1` | 笔记本默认串行推理，限制 CPU 和内存峰值 |
| OPEN_WEBUI_ENABLED | `true` | 是否随 Ollama 脚本一起启动 Open WebUI |
| OPEN_WEBUI_AUTO_INSTALL | 已废弃 | 部署机不再安装 Open WebUI；运行时必须在构建阶段打入独立包 |
| OPEN_WEBUI_HOST | `0.0.0.0` | Open WebUI 监听地址 |
| OPEN_WEBUI_PORT | `8080` | Open WebUI 监听端口 |
| OPEN_WEBUI_DATA_DIR | `<ratel-fm-ollama>/data/open-webui` | Open WebUI 数据目录 |
| OPEN_WEBUI_OLLAMA_BASE_URL | `http://127.0.0.1:11434` | Open WebUI 调用 Ollama 的地址 |
| OPEN_WEBUI_PACKAGE | `open-webui` | pip 安装的 Open WebUI 包名或版本约束 |
| OPEN_WEBUI_AUTH | `true` | Open WebUI 是否启用自身登录认证 |
| FM_AI_MODEL_PROVIDER | `ollama` | Ratel FM 通用大模型提供方；使用本包时保持为 `ollama` |
| FM_AI_OLLAMA_BASE_URL | `http://10.105.12.136:11434` | Ratel FM 后端访问 Ollama 的地址，现场可改为实际 Ollama 电脑 IP |
| FM_AI_OLLAMA_CHAT_MODEL | `qwen2.5:7b` | 普通业务问答优先模型 |
| FM_AI_OLLAMA_COMMAND_MODEL | `llama3.2:3b` | 语音/操作指令优先模型 |
| FM_AI_OLLAMA_REASONING_MODEL | `deepseek-r1:8b` | 复杂分析优先模型 |
| FM_AI_OLLAMA_EMBEDDING_MODEL | `bge-m3:latest` | 知识索引、智能检索和 Qdrant 查询向量模型 |

## 启停边界

- Ratel FM 启动、关闭脚本只处理 Ratel FM。
- Ollama 启动、关闭脚本只处理本包内由 PID 文件记录的 Ollama 和 Open WebUI 进程。
- Ratel FM 启动失败不会影响 Ollama；Ollama 启动失败也不会影响 Ratel FM。
- 两个部署包放在同级目录只是为了运维清晰，不代表脚本互相调用。
