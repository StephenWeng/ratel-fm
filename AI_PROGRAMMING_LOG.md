# Ratel FM 编码、打包、注释和变更记录

版本日期：2026-07-23

本文档记录项目工程原则、打包规则和近期变更。产品设计见 `PA.md`，技术架构见 `TECH_ARCHITECTURE.md`，大模型知识见 `AI_LLM_DEVELOPMENT_GUIDE.md`。

## 1. 编码原则

- 修改前先阅读现有代码和根目录文档。
- 按业务模块放置代码，不随意新增扁平公共文件。
- 所有查询和写入必须带公司、账套、权限上下文。
- 前端隐藏只是体验控制，后端必须做最终校验。
- 状态变更必须记录操作日志或业务时间线。
- 新业务字段必须同步实体、DTO、前端类型、表单校验、数据库初始化和日志展示。
- 优先使用项目已有工具类和结构化 API。
- 不做和当前需求无关的重构。

## 2. 注释原则

Java：

- 类注释说明模块职责。
- 字段注释说明业务含义。
- 方法注释说明目的和关键步骤。
- 配置项注释说明默认值、边界和风险。

Vue/TypeScript：

- 复杂状态和流程需要注释。
- 简单赋值、显而易见的模板不写空注释。
- API 类型要表达字段含义。

## 3. 前端原则

- 页面入口和按钮必须符合权限与业务状态。
- 错误要明确展示，尤其是登录、保存、删除、审批。
- 表格和弹窗必须有稳定滚动区域。
- 加载态只保留必要提示，不制造额外遮挡。
- AI 输出不展示内部检索卡片、长原文和调试信息。
- 业务 Agent 统一在 AI 助手页展示，不在业务页面重复放按钮。

## 4. 后端原则

- Controller 不承载复杂业务逻辑。
- Service 负责业务规则、状态流转和日志。
- Repository 查询必须按公司和账套过滤。
- 写接口必须校验权限、状态和幂等风险。
- AI 和 Agent 接口必须校验开关、权限、账套和数据范围。

## 5. 打包原则

- 使用 JDK 24。
- PowerShell 下 Maven `-D` 参数加引号。
- 主应用、Ollama、Qdrant 独立打包和独立启停。
- Windows 和 Linux 脚本按目录区分：`bin/windows`、`bin/linux`。
- Windows 和 Linux 运行时按目录区分：`runtime/windows`、`runtime/linux`。
- 日志、PID、运行库数据、模型仓库默认不提交 Git。

推荐全量打包命令：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package -Pwith-ollama,with-qdrant
```

## 6. Git 原则

不提交：

- `target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `logs/`
- `ollama-models/`
- Ollama/Qdrant runtime、模型、向量库和运行数据
- 本地配置、证书、临时下载目录

需要上传大文件时，应先明确是否使用 Git LFS 或外部制品库。

## 7. 近期变更记录

### 2026-07-23：根目录文档中文化重写

- `README.md` 改为 GitHub 风格中文说明。
- `PA.md` 重写为产品设计文档。
- `TECH_ARCHITECTURE.md` 重写为技术架构文档。
- `AI_LLM_DEVELOPMENT_GUIDE.md` 重写为大模型开发知识点。
- `AI_PROGRAMMING_LOG.md` 重写为工程规则和变更记录。

### 2026-07-23：业务 Agent 统一入口

- AI 助手页保留 `业务 Agent` Tab。
- 采购、库存、应收应付、会计平台页面移除独立 `Agent 分析` 按钮。
- ratel 助手识别对账、到期、制证建议、库存风险等意图时，通过统一入口承接。
- `FM_AI_AGENT_ENABLED=false` 时，前端隐藏入口，ratel 助手不调用 Agent，后端兜底拦截。

### 2026-07-23：ratel 助手体验修正

- 对话结果只展示结论和关键依据。
- 检索中不再显示额外占位卡片。
- 文件存在性问题优先回答是否有文件。
- 连续追问会结合上一轮主题。

### 2026-07-23：页面问题修正

- 登录密码错误时展示错误提示。
- 智能检索结果区域增加内部滚动，避免底部内容不可见。
- 操作手册内容更新到当前版本。

### 2026-07-23：Linux 部署包补齐

- Ollama 独立包包含 `runtime/linux/ollama/ollama`。
- Qdrant 独立包包含 `runtime/linux/qdrant/qdrant`。
- Ollama Linux 增加 `start.sh`、`stop.sh`、`status.sh`。
- Qdrant Linux 保留 `start.sh`、`stop.sh`、`status.sh`。
- Linux Open WebUI 未内置时只跳过控制台，不影响 Ollama 服务。

## 8. 验证记录

- `frontend` `npm run build`：已通过。
- JDK 24 下 `mvn -DskipTests compile`：已通过。
- 后续全量打包完成后，需要在本节补充最终产物名称和大小。
