# Ratel FM

[English](README.md)

Ratel FM 是一个面向小团队和本地化部署场景的轻量财务管理 ERP。系统将会计核算、采购、物流、库存、应收应付、出纳、审批流、附件、审计日志、智能检索、本地知识库和业务 Agent 放在同一套应用中，适合单机或局域网部署。

> 默认交付形态：主应用便携包、Ollama 独立包、Qdrant 独立包。主应用不强依赖 Ollama/Qdrant 启动，AI 能力可按配置开关启用或关闭。

## 主要能力

- 会计核算：科目、凭证、过账、反过账、试算平衡、财务报表、会计期间。
- 业务单据：采购单、物流单、库存台账、物料库存、应收应付、付款结算、出纳流水。
- 会计平台：按来源单据生成凭证草稿，保留业务来源链路。
- 审批流：流程定义、流程绑定、审批任务、审批历史。
- 权限体系：公司、账套、用户、角色、菜单、页面按钮和后端权限校验。
- 附件管理：上传、预览、下载、删除、文本抽取和知识入库。
- 审计追踪：登录日志、操作日志、业务时间线、文件日志。
- AI 能力：ratel 助手、本地知识问答、智能检索、OCR、业务 Agent、Ollama、Qdrant。
- AI 组件状态：展示 Ollama 已配置模型和本地已下载模型清单，便于部署排查。
- ratel 助手加载态：思考中隐藏输入框和发送按钮，只保留左侧 `思考中` 与思考动画；初次打开使用更高的稳定面板高度。
- ratel 助手检索：文件存在性回答在结论中列出命中文件，后续总结归纳会继承上一轮文件主题，简历问题会扩展候选人/工程师等同义词。
- ratel 助手回答格式：结论或关键依据存在多条时，统一使用 `1、2、3` 编号，不使用短横线列表。
- ratel 助手推理安全：流式输出和最终答案都会清理内部推理草稿、复盘语句和 think 标签。
- 财务意图路由：会计核算、应收应付、资金现金流、报表、对账、制证、库存、采购、物流、审批和经营分析术语统一维护；财务专业问题优先使用实时系统上下文，不被知识库文件检索带偏。
- AI 架构拆分：助手模型路由、Prompt 构造、答案清洗、Agent 选择和经营指标快照由独立组件承担。
- UI 主题适配：浅色、深色、墨绿、金融蓝和 Vue 金主题共用统一设计变量，壳布局、表格、弹窗、输入控件、滚动条和焦点态保持一致。
- 便携部署：Windows/Linux 启停脚本，主应用、Ollama、Qdrant 分包交付。

## 快速访问

启动后访问：

| 入口 | 地址 |
| --- | --- |
| 系统首页 | `http://localhost:38000/ratel/fm` |
| Knife4j | `http://localhost:38000/ratel/fm/doc.html` |
| OpenAPI | `http://localhost:38000/ratel/fm/v3/api-docs` |

默认账号：

| 字段 | 默认值 |
| --- | --- |
| 公司 | `Ratel默认公司` |
| 用户名 | `admin` |
| 密码 | `admin123` |

首次部署后应立即修改默认密码，也可以在首次启动前通过环境变量覆盖：

```powershell
$env:FM_ADMIN_USERNAME='admin'
$env:FM_ADMIN_IDENTITY_NO='ADMIN_IDENTITY_0001'
$env:FM_ADMIN_PASSWORD='your-strong-password'
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 24、Spring Boot 4、Spring Security、Spring Data JPA |
| 数据库 | H2 文件库，兼容 PostgreSQL 配置 |
| 前端 | Vue 3、TypeScript、Vite |
| AI | Ollama、Qdrant、H2 知识索引、SSE |
| 打包 | Maven、frontend-maven-plugin、maven-assembly-plugin |

## 仓库结构

```text
ratel-fm
├── frontend                         前端工程
├── src/main/java/com/ratel/fm        后端源码
├── src/main/resources                配置、初始化 SQL、构建脚本
├── src/main/package                  主应用便携包模板
├── src/main/ollama-package           Ollama 独立包模板
├── src/main/qdrant-package           Qdrant 独立包模板
├── src/main/assembly                 Maven 装配描述
├── ollama-models                     本地 Ollama 模型仓库，不提交 Git
├── README.md                         英文 GitHub 项目说明
├── README.zh.md                      中文 GitHub 项目说明
├── Product-Design.md                 英文产品设计
├── 产品设计.md                       产品设计
├── Technical-Architecture.md         英文技术架构
├── 技术架构.md                       技术架构
├── LLM-Development-Guide.md          英文大模型开发知识点
├── 大模型开发知识点整理.md           大模型开发知识点
├── Engineering-Log.md                英文编码、打包、注释和变更记录
└── 编码打包注释和变更记录.md         编码、打包、注释和变更记录
```

## 业务 Agent

业务 Agent 统一从 `AI 助手 / 业务 Agent` Tab 进入。采购、库存、应收应付、会计平台等业务页面不再单独展示 `Agent 分析` 按钮，避免入口分散和执行链路不一致。

当前规划和实现的 Agent 类型：

| Agent | 说明 |
| --- | --- |
| 查询型 Agent | 自然语言查询采购单、物流单、库存、应收应付、凭证 |
| 对账检查 Agent | 检查采购、收货、库存、应付、付款、凭证链路一致性 |
| 凭证建议 Agent | 根据业务来源生成凭证草稿建议 |
| 到期提醒 Agent | 识别应收应付到期、逾期、未核销风险 |
| 流程助手 Agent | 解释审批节点、下一处理人、审批意见草稿 |
| 库存风险 Agent | 检查负库存、低库存、调拨异常、收货未入库 |
| 经营分析 Agent | 按项目、供应商、客户、物料汇总风险和建议 |
| 附件/知识问答 Agent | 结合制度、合同、附件文本和系统数据回答问题 |

配置开关：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `FM_AI_AGENT_ENABLED` | `true` | 是否启用业务 Agent |

关闭业务 Agent 时，前端不展示业务 Agent 入口，ratel 助手也不会把相关意图交给业务 Agent，后端接口同时做兜底校验。

## 本地开发

后端需要 JDK 24：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -DskipTests compile
```

前端开发：

```powershell
cd frontend
npm install
npm run dev
```

## 全量打包

主应用包：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package
```

Ollama 独立包：

```powershell
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package -Pwith-ollama
```

Qdrant 独立包：

```powershell
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package -Pwith-qdrant
```

全量交付包应包含：

| 产物 | 说明 |
| --- | --- |
| `target/ratel-fm.jar` | 后端可执行 Jar |
| `target/ratel-fm-0.0.1-SNAPSHOT-portable.zip` | 主应用便携包 |
| `target/ratel-fm-ollama.zip` | Ollama Windows/Linux 独立包 |
| `target/ratel-fm-qdrant.zip` | Qdrant Windows/Linux 独立包 |

## Git 提交原则

不提交以下内容：

- `target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `logs/`
- `ollama-models/`
- Ollama/Qdrant 运行时、模型、向量库和运行数据
- 本地配置、证书、PID 文件、临时下载目录

## 相关文档

文档维护原则：

- 每次产品、架构、AI、打包、编码或交互变更，都必须更新所有受影响的根目录 Markdown 文档。
- 中文版和英文版必须在同一次改动中同步更新。
- 每次改动结束前，需要检查全部根目录 Markdown，保证互链、打包文档清单和实现记录一致。

| 中文 | English |
| --- | --- |
| [产品设计](产品设计.md) | [Product Design](Product-Design.md) |
| [技术架构](技术架构.md) | [Technical Architecture](Technical-Architecture.md) |
| [大模型开发知识点整理](大模型开发知识点整理.md) | [LLM Development Guide](LLM-Development-Guide.md) |
| [编码、打包、注释和变更记录](编码打包注释和变更记录.md) | [Engineering Log](Engineering-Log.md) |

## 维护信息

| 项目 | 内容 |
| --- | --- |
| 组织 | `ratel` |
| 开发人员 | `WenZhang` |
| 联系方式 | `18782945613` |
