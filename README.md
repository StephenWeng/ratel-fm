# Ratel FM

Ratel FM 是一套基于 Spring Boot 4、Java 24、Vue 3 的财务管理 ERP 前后端一体工程，覆盖人员授权、基础信息字典、会计科目、凭证记账、采购、物流、统计分析和智能检索。

开发组织：`ratel`  
开发人员：`WenZhang`  
联系方式：`18782945613`

## 技术栈

- Spring Boot 4.1.0
- JDK 24
- Maven 3.6.3
- PostgreSQL
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Alibaba FastJson2
- Knife4j / OpenAPI
- Hutool / Apache Commons Lang / Commons Collections / Commons IO
- Vue 3 / Vite / TypeScript / Element Plus

## 项目结构

```text
frontend                     # 前端源码，构建产物进入 Spring Boot static
└── src
    ├── views                # 页面视图，按业务模块分目录
    │   ├── auth             # 登录
    │   ├── assistant        # AI 财务助手
    │   ├── audit            # 操作日志查询
    │   ├── basic            # 基础信息字典
    │   ├── dashboard        # 首页概览
    │   ├── finance          # 科目、凭证、报表
    │   ├── inventory        # 库存台账
    │   ├── operation        # 采购、物流
    │   ├── receivable       # 应收应付
    │   ├── search           # 智能检索
    │   ├── shell            # 主框架
    │   └── system           # 人员、角色
    ├── api                  # 前端接口聚合入口
    ├── router               # 路由
    ├── stores               # Pinia 状态
    └── types                # 前端类型定义

src/main/java/com/ratel/fm
├── common                   # 统一响应、异常处理、基础实体
├── config                   # 配置类，按配置域分目录
│   ├── bootstrap            # 初始化基础数据
│   ├── database             # 数据库兼容与注释初始化
│   ├── json                 # FastJson2 消息转换器和 JWT JSON 编解码
│   ├── openapi              # OpenAPI / Knife4j 配置
│   ├── security             # Spring Security 配置
│   └── web                  # Web MVC 配置
├── domain                   # JPA 实体和业务枚举，按业务模块分目录
│   ├── audit                # 用户关键操作日志
│   ├── auth                 # 人员、角色、权限、登录会话
│   ├── basic                # 基础信息字典
│   ├── cashier              # 出纳资金流水
│   ├── finance              # 科目、凭证、凭证分录
│   ├── inventory            # 库存台账
│   ├── logistics            # 物流单
│   ├── period               # 会计期间
│   ├── purchase             # 采购单
│   └── receivable           # 应收应付
├── repository               # Spring Data JPA Repository，按业务模块分目录
│   ├── audit
│   ├── auth
│   ├── basic
│   ├── finance
│   ├── inventory
│   ├── logistics
│   ├── purchase
│   └── receivable
├── security                 # Cookie JWT、当前用户上下文、登录会话校验
├── service                  # 核心业务服务，按业务模块分目录
│   ├── assistant
│   ├── audit
│   ├── auth
│   ├── basic
│   ├── finance
│   ├── insight
│   ├── inventory
│   ├── operation
│   ├── receivable
│   └── report
└── web                      # REST Controller 和 DTO，按业务模块分目录
    ├── audit
    ├── auth
    ├── basic
    ├── finance
    ├── insight
    ├── operation
    ├── phasetwo
    └── dto
        ├── auth
        ├── basic
        ├── finance
        ├── insight
        ├── operation
        └── phasetwo
```

## 模块功能

| 模块 | 功能 |
| --- | --- |
| 人员与授权 | 登录、人员增删改查、头像维护、个人中心、密码修改、角色维护、模块/页面/按钮授权 |
| 基础信息管理 | 人员、角色、菜单和字典集中维护；字典支持采购方、物流方、物料、仓库、客户/供应商等启用/禁用基础资料 |
| 财务记账 | 初始化财政部企业会计准则附录标准科目，支持科目维护、凭证草稿、凭证修改、项目维度、所属年月、过账、作废、试算平衡；凭证明细支持行级币种、汇率和人民币金额 |
| 会计期间 | 创建期间、月结检查、关闭期间和反结账，月结前检查本期草稿凭证和未结清往来风险 |
| 会计平台 | 参考用友业财一体化思路，将采购单、应收应付单、库存流水和出纳流水统一作为制证来源，选择借贷科目后自动生成凭证草稿并保留来源链路 |
| 出纳管理 | 维护收款、付款、转账和调账流水，支持确认、取消、导出，并作为会计平台资金类制证来源 |
| 采购管理 | 采购单创建、修改、查询、项目维度、状态流转、金额汇总，供应商从采购方字典选择，采购明细支持行级物料、币种和汇率 |
| 物流管理 | 物流单创建、修改、查询、项目维度、状态流转、送达日期记录，承运商从物流方字典选择，发货地/目的地保存行政区划和详址 |
| 统计分析 | 人员、科目、凭证、采购、物流、三大报表和金额指标概览 |
| 智能检索 | 混合检索科目、凭证、采购单、物流单、库存、应收应付和业务附件 |
| 库存台账 | 入库、出库、调拨、盘点流水，项目、物料和仓库从基础字典选择 |
| 应收应付 | 客户、供应商、项目、账龄、付款计划、收付核销和收付统计，往来单位从客户/供应商字典选择 |
| AI 财务助手 | 接入千问模型，基于业务知识索引和引用来源进行自然语言问答 |
| 日志管理 | 查询业务系统操作记录，支持按时间、账号、身份证、联系方式、部门和终端过滤 |

## 认证与登录

- 身份令牌采用 JWT。
- 登录成功后后端写入浏览器 `FM_TOKEN` HttpOnly Cookie。
- JWT 中包含当前登录人的姓名、身份证、部门、岗位、组织、联系方式、权限、终端类型、终端标识和登录会话 ID。
- 令牌有效期 1 小时。
- 每次 API 请求后端会校验令牌签名、过期时间、人员状态、人员信息一致性、会话状态和终端信息一致性。
- 距离过期不足 30 分钟时，后端自动刷新 JWT Cookie。
- 同一所属公司、同一身份证号、同一终端类型下只允许一个有效登录。终端类型包括 `PC` 和 `APP`，`PC` 默认使用请求 IP 作为终端标识，`APP` 使用手机号作为终端标识。
- 身份证号是唯一登录判断依据，人员新增和修改时必须维护且在同一所属公司内唯一。
- 登录账号和身份证号均按所属公司隔离唯一；登录时可填写账号或身份证号。
- 后登录者发现同终端类型已有登录时，接口返回 `REPEAT_ERROR(400002)`；前端弹窗询问是否挤掉之前登录者。确认后旧会话变为 `FORCE_LOGOUT`，旧登录者后续请求返回 `FORCE_LOGOUT(200003)`。
- 登录成功、登录失败、重复登录提醒和强制登录都会写入数据库操作日志，便于审计登录行为。
- 前端收到 401 后弹出 10 秒倒计时模态框，并跳转登录页。

## 菜单授权

- 授权资源统一维护在 `fm_menus`，菜单类型分为 `MODULE`、`PAGE`、`BUTTON`。
- 菜单管理页面支持维护模块、页面、按钮三级层级资源，新增功能后可先维护菜单编码，再给角色授权。
- 人员关联角色，角色关联菜单；登录成功后前端立即请求 `/api/auth/menu-codes` 获取当前登录人的授权菜单编码，并请求 `/api/auth/menus` 获取授权菜单资源。左侧导航按菜单管理中的模块、页面层级渲染，业务按钮仍按菜单编码控制显隐。
- 角色授权时选择页面或按钮会自动补齐上级模块或页面；后端保存角色时也会兜底补齐上级菜单。
- 每次浏览器刷新页面后，前端不会复用本地缓存的旧菜单编码和旧菜单层级，而是重新请求 `/api/auth/menu-codes`、`/api/auth/menus` 后再匹配渲染。
- 后端接口仍使用 Spring Security `@PreAuthorize` 进行兜底校验，角色保存时由菜单绑定的 `permission_code` 自动推导接口权限码。
- 个人中心也纳入菜单授权：个人信息、个人密码、个人头像、退出登录均有独立按钮码；接口文档入口不在业务页面暴露。
- 默认账号通过后端和前端双层保护，不允许在个人中心自行修改个人资料、密码或头像。
- 头像只能通过上传接口维护，后端校验 jpg、jpeg、png、webp 图片和 2MB 大小限制，并以 Base64 数据保存到 `fm_users.avatar_base64`。

## JSON 处理

- 项目业务 JSON 统一使用阿里 FastJson2。
- REST 请求体和响应体通过 `config/json/FastJsonHttpMessageConverter` 处理。
- JWT Header 和 Claims 通过 `config/json/FastJsonJwtCodec` 处理，JJWT 使用项目内 FastJson2 适配器完成 JSON 编解码。

## 统一响应

所有接口统一返回：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-06-27T12:00:00+08:00"
}
```

响应码由 `ResponseCode` 枚举统一管理，包含 `SUCCESS(200)`、`ILLEGAL_PARAM(000001)`、`NO_TOKEN_ERROR(200001)`、`JWT_OVERTIME(200002)`、`FORCE_LOGOUT(200003)`、`NO_AUTH(300001)`、`PASSWORD_ERROR(400001)`、`REPEAT_ERROR(400002)` 等。

## 基础信息管理

- 左侧“基础信息管理”下包含人员管理、角色管理、菜单管理、字典管理四个二级页面。
- 字典管理原“基础信息管理”页面，用于维护采购方、物流方、物料、仓库、客户/供应商等业务基础字典。
- 左侧菜单按菜单管理中的模块、页面层级展示：首页概览独立为第一个模块，统计报表靠后展示，日志管理为最后一个模块，AI 助手挂在智能检索模块下。
- 主工作区支持多页签切换，并在页签栏右侧提供“关闭全部”按钮；关闭后自动打开当前人员有权限的第一个页面。

- 基础字典表为 `fm_basic_dictionaries`，支持任意层级的父子结构。
- 字典编码非必填，用户未填写时后端自动生成唯一随机编码；同一父级下字典名称唯一，不同层级允许重名。
- 字典包含启用状态，采购、物流、库存、应收应付等业务模块只加载启用字典项。
- 当前预置根字典包含 `PROJECT`（项目）、`SUPPLIER`（采购方）、`CARRIER`（物流方）、`MATERIAL`（物料）、`WAREHOUSE`（仓库）、`PARTNER`（客户/供应商）、`ORGANIZATION`（所属公司）、`CURRENCY`（币种）、`BANK_ACCOUNT`（银行账户）、`PAYMENT_TERMS`（收付款条件）、`SETTLEMENT_METHOD`（结算方式）、`DELIVERY_TERMS`（交货条件）、`UNIT`（计量单位）和 `ADMINISTRATIVE_DIVISION`（全国行政区划），并预置默认业务子项。
- 全国行政区划明细字典 code 直接使用区划代码，不再增加 `AREA_` 前缀；省、市、区县使用 6 位行政区划代码，例如 `110000`，乡镇街道使用国家统计局统计用区划代码，例如 `510105002`。
- 当前内置行政区划数据覆盖省、市、区县、乡镇街道四级；CSV 中 `administrative_code` 字段保存递归继承到上级后的 6 位行政区划代码，`source` 字段记录数据来源。国家统计局公开具体代码最新可用口径为 2023 年统计用区划代码和城乡划分代码，港澳台根节点沿用原始预置数据。
- 字典树、菜单树和角色授权树默认只展开第一层。

## 金额与汇率

- 凭证记账、采购管理、应收应付中的金额输入、后端计算、人民币折算金额和数据库金额字段统一保留 8 位小数。
- 数据库金额字段统一使用 `numeric(26, 8)`，汇率字段使用 `numeric(18, 8)`。
- 凭证分录和采购明细按行选择币种、保存当时汇率并计算金额（人民币）；同一张凭证或采购单允许不同明细使用不同币种或不同汇率。
- 涉币种字段切换非人民币币种时，前端调用 `/api/basic/dictionaries/exchange-rate` 自动获取最新公开参考汇率并填充“汇率”字段。
- 汇率来源为 Frankfurter 公开汇率接口，后端使用 JDK HttpClient 请求并使用 FastJson2 解析；Frankfurter 官方说明其跟踪的是 daily exchange rates，因此系统按“最新参考汇率”展示，并返回汇率日期和来源，不标记为秒级实时行情。
- 如果外部网络或汇率服务不可用，前端提示后仍允许用户手工填写汇率；业务单据最终保存业务发生时的汇率快照，后续统计不再依赖外部汇率接口。

## ratel助手会话上下文

- ratel助手支持短会话摘要和最近若干轮原文，用于理解“刚才那个单据”“继续查它的物流”等追问；智能检索仍保持单次关键词检索，不维护对话状态。
- 会话上下文只辅助理解指代，不作为金额、状态、库存、审批结果等实时业务事实依据；每次回答仍重新读取系统上下文和知识索引。
- 最近原文轮次由 `FM_AI_ASSISTANT_RECENT_RAW_ROUNDS` 配置，默认保留 4 轮；可通过 `FM_AI_ASSISTANT_CONVERSATION_ENABLED=false` 关闭会话上下文。
- ratel助手支持 SSE 流式输出，前端优先使用 `/api/ai/assistant/stream`，异常时回退原 `/api/ai/assistant` 非流式接口。流式输出只在权限过滤、菜单过滤、系统上下文和知识上下文准备完成后开始；智能检索结果仍一次性返回，不边搜边推送未过滤结果。
- 流式输出带连接保护：全局并发、单用户并发、最长连接时间、心跳、客户端断开取消和 Ollama 上游请求取消。服务端只保留一段回答样本用于最终元数据和会话摘要，不为了拼完整回答无限缓存内容。
- 新增、修改、删除、审批、确认、取消等动作不能从历史摘要继承确认意图，必须以用户当前这一次明确表达为准。

## 业务 Agent

业务 Agent 统一接口为 `POST /api/agent/business`，权限码为 `AI_ASSISTANT_USE`。接口复用现有采购、物流、库存、应收应付、财务、出纳、审批和知识检索服务，所有读取均按当前 Cookie/JWT 中的所属公司和权限过滤。

请求字段：

- `question`：自然语言问题或分析目标。
- `stage`：执行阶段，支持 `readOnly`、`draft`、`controlled`、`multiStep`，默认 `readOnly`。
- `modules`：可选模块编码，支持 `purchase`、`shipment`、`inventory`、`arAp`、`finance`、`workflow`。
- `agentTypes`：可选 Agent 能力类型，支持 `query`、`reconciliation`、`voucherSuggestion`、`dueReminder`、`workflowAssistant`、`inventoryRisk`、`businessAnalysis`、`knowledgeQa`。
- `limit`：每类结果最多返回的证据条数，服务端限制在 1 到 10。

响应字段：

- `modules`：按业务模块返回摘要、发现、风险、建议和业务证据。
- `capabilities`：按 Agent 能力返回查询、对账、制证建议、到期提醒、流程、库存、经营和知识问答结果。
- `actions`：草稿动作或受控执行计划。
- `selfChecks`：权限边界、证据约束、写操作阻断和阶段合法性自检。
- `guardrails`：当前执行边界。

当前安全边界：

- 前端必须先读取 `/api/ai/status`，当 `agentEnabled=false` 时隐藏所有业务 Agent 入口，并避免调用 `/api/agent/business`。
- AI 助手页提供“业务 Agent”Tab；采购、库存、应收应付和会计平台页面提供“Agent 分析”按钮，按钮同样受 `agentEnabled` 控制。
- ratel助手识别到对账、到期、制证建议、库存风险等意图时，会切换到“业务 Agent”Tab 并调用前端 `api.runBusinessAgent()`。
- `readOnly` 只返回分析结果。
- `draft` 只生成草稿说明。
- `controlled` 和 `multiStep` 只生成 `executable=false` 的受控执行计划。
- 如果绕过前端直接调用后端 Agent 接口，后端只返回禁用说明和空结果，不选择模块、不读取业务证据、不生成 Agent 计划。
- 在确认令牌、Agent 审计表、工具白名单和服务端二次校验完成前，Agent 不允许自动保存、删除、审批、取消或过账。

## 会计科目初始化

- 系统启动时读取 `src/main/resources/data/accounting-subjects.csv` 初始化科目树。
- 初始化口径参考财政部《企业会计准则应用指南》附录“会计科目和主要账务处理”的会计科目表，包含 6 个分类根节点和 156 个标准科目。
- 科目类别包含资产、负债、共同、权益、收入、成本、费用；共同类用于金融工具、套期等余额方向需结合业务判断的科目。
- 分类根节点只用于组织科目树，不允许作为凭证分录或会计平台制证科目；业务入账接口只接受启用状态的叶子科目。
- 财政部附录没有统一规定所有企业的二级、三级明细科目，企业可在标准科目下继续新增明细科目。

## 项目维度与收付统计

- 项目维度来自基础字典 `PROJECT`，凭证记账、采购管理、物流管理、库存台账、应收应付新增和查询均支持选择项目。
- 业务表保存 `project_code` 和 `project_name` 快照，项目名称用于列表、导出、查看流水、AI 知识索引和统计展示。
- 查看流水会展示项目字段，物流状态确认流水也会保留确认时点的项目快照。
- 应收应付模块新增“收付统计”页面，路由为 `/ar-ap-stats`，筛选条件为项目和客户/供应商。
- 收付统计按每个应收应付单号展示应付金额、应收金额、待收金额、待付金额，并返回页面顶部和表格底部总计。
- 应收应付列表提供收款/付款核销入口，按当前单据剩余金额限制核销金额，保存核销流水并实时回写已收/已付和待收/待付金额。

## 用友财务业务逻辑学习

本项目后续财务能力设计参考用友 BIP、U8 cloud、YonSuite/NC Cloud 等公开产品资料中的通用财务软件逻辑，但只吸收适合 Ratel FM 当前阶段的业务规则，不照搬大型集团产品的复杂度。

参考资料：

- 用友财务会计方案：`https://www.yonyouvietnam.com/solutions/financial-management-system/financial-accounting/`
- 用友 BIP 官网财务云介绍：`https://www.yonyou.com/`
- 用友 BIP 智能财务：`https://www.yonyou.com/subject/gd-cw-szcw`
- 用友智能财务场景化应用：`https://www.yonyou.com/subject/ys-zncw`
- 用友 U8 cloud 官方资料：`https://www.yonyou.com/subject/zd-U8C`

学习结论：

- 用友财务产品通常以总账为核算核心，围绕应收、应付、固定资产、存货核算、现金/资金、报表、合同、会计平台等模块形成财务闭环。
- 业务系统不应只把结果写入凭证，而应保留业务单据、核算单据、凭证、报表之间的链路，支持从汇总金额穿透到原始业务单。
- 会计平台是业财一体化的关键：采购、销售、库存、报销、银行、发票等业务数据根据凭证模板自动生成凭证，财务人员审核和修正，而不是重复手工录入。
- 应收应付以往来单据为主线，完整流程是应收/应付单据生成、审核、收款/付款、核销、生成凭证、账龄分析、客户/供应商对账、总账对账。
- 月末处理强调先子系统结账和对账，再总账结账；应收、应付、存货、现金、固定资产等子模块需要与总账保持一致。
- 基础数据要统一，包括组织、部门、人员、客户、供应商、项目、物料、仓库、币种、税率、结算方式、付款条件、会计科目、凭证模板和审批规则。
- 角色化工作台是成熟财务软件的常见方式，应支持总账会计、应收会计、应付会计、出纳、采购、库存、财务主管等不同岗位关注不同任务。
- 智能财务不是简单问答，核心价值是自动制单、智能审核、自动对账、异常预警、月结检查、报表分析和可追溯审计。

当前已融合落地：

- 新增“会计平台”页面，路由为 `/accounting-platform`，菜单编码为 `PAGE_ACCOUNTING_PLATFORM`。
- 会计平台当前支持从采购单、应收应付单、库存流水和出纳流水读取制证来源，展示来源单号、往来单位、项目、日期、金额、币种和是否已制证。
- 财务人员选择借方科目和贷方科目后，系统自动生成凭证草稿；摘要、项目、来源单号、币种、汇率和金额沿用业务来源。
- 自动制证默认拦截同一来源单号的未作废凭证，避免重复入账；确需重复制证时前端提供显式开关。
- 后端新增 `/api/finance/accounting-platform/sources` 和 `/api/finance/accounting-platform/auto-vouchers`，均通过 `FINANCE_VOUCHER_MANAGE` 权限兜底。
- 新增“会计期间”页面，路由为 `/accounting-periods`，支持期间创建、月结检查、结账和反结账。
- 新增“出纳管理”页面，路由为 `/cashier`，支持收款、付款、转账、调账流水的新增、确认、取消、批量删除和导出。
- 应收应付已补齐收款/付款核销记录，核销后回写单据已收/已付金额、剩余金额和已结清状态。

Ratel FM 后续融合方向：

- 短期优先补齐自动凭证模板、客户/供应商对账、应收应付与总账对账、银行对账和月结差异处理入口。
- 采购、物流、库存、应收应付要逐步沉淀“来源单据 -> 业务单据 -> 财务单据 -> 凭证”的来源链路。
- 增加自动凭证模板，把采购入库、应付确认、付款、库存出入库、物流费用、应收确认、收款等场景转为凭证草稿。
- 增加月结工作台，按模块检查未审核单据、未核销往来、库存异常、凭证借贷不平、子账与总账不一致等问题。
- AI 助手后续应围绕“查业务、查凭证、查差异、查风险、解释规则、辅助生成凭证”增强，而不是只返回通用说明。

## 操作日志

系统运行日志通过 Logback 写入 `logs/system.log`。关键用户操作会先写入 `logs/operation-audit.log` 文件，再使用独立事务写入数据库表 `fm_user_operation_logs`。数据库日志写入异常只会记录 warning，不影响业务主流程提交。

数据库操作日志包含账号、身份证、联系方式、部门、操作时间、终端类型、终端标识、操作模块、操作功能、参数、操作是否成功和操作响应值。日志管理页面通过 `AUDIT_LOG_VIEW` 权限控制，可按操作时间范围、账号、身份证、联系方式、部门、终端类型和终端标识查询。

日志查询默认时间范围为最近一个月；查询日志本身不再写入操作日志，避免审计表被查询行为反复放大。

系统日志策略：

- 按天滚动。
- 单文件达到 100MB 后滚动。
- 滚动文件使用 gzip 压缩。
- 最多保留 10 天，到期自动删除。

## 数据库

默认部署使用 H2 文件数据库，不需要单独安装数据库。打包时会预先生成模板库 `database-template/ratel-fm.mv.db`，启动脚本优先使用部署机器上的运行库 `data/ratel-fm.mv.db`；如果运行库不存在，则首次启动时从模板库复制；如果模板库也不存在，H2 会按配置自动创建文件库。

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/ratel-fm;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    username: sa
    password:
```

如后续数据量变大或需要正式服务器，可启用 `postgres` profile 使用 PostgreSQL。PostgreSQL 配置保留在 `application-postgres.yml`，默认地址仍为 `jdbc:postgresql://192.168.241.89:5432/fm`。

开发阶段配置为 `spring.jpa.hibernate.ddl-auto=update`，启动后会在当前数据库中自动创建或更新表结构。默认 H2 文件库随应用进程启动和关闭，PostgreSQL profile 只在明确启用时连接外部数据库。升级部署时不要用模板库覆盖 `data/ratel-fm.mv.db`，该文件才是笔记本上的真实业务数据。

`src/main/resources/init.sql` 是当前数据库初始化基线，包含表结构、约束、注释、索引、默认菜单、默认角色、默认管理员、默认会计科目和基础字典初始化数据。后续只要涉及数据库表、字段、索引、约束或初始化数据变化，必须同步更新 `init.sql`。

数据库命名采用 snake_case，Java 代码采用 camelCase。例如数据库字段为 `created_time`、`modify_time`，实体字段为 `createdTime`、`modifyTime`。

数据库表和字段说明采用双层策略：

- JPA 实体使用 Hibernate `@Comment` 标明表注释和字段注释。
- PostgreSQL profile 下，`DatabaseCommentInitializer` 在应用启动后执行 PostgreSQL `comment on table/column`，对已经存在的表和字段补齐描述。
- 人员角色、角色权限、角色菜单这类 JPA 关联表没有独立实体类，由 `DatabaseCommentInitializer` 在 PostgreSQL profile 下统一维护表注释和字段注释。
- `DatabaseIndexInitializer` 在应用启动后执行 `create index if not exists`，补齐外键、日期排序、状态过滤、授权和审计查询索引。

## 本地运行

PowerShell 下建议显式设置 JDK 24，避免本机 Maven 默认使用 Java 8：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn spring-boot:run
```

也可以先编译：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn clean package
```

服务默认端口：`38000`，默认上下文路径：`/ratel/fm`。

## 单包部署

项目支持打包为一个可拷贝部署的 zip，包内包含前端、后端、AOT 预处理结果、应用 Jar、内置 JDK、配置文件、数据库初始化脚本、Windows 启停脚本和 Linux 启停脚本。部署时只需要拷贝这一个 zip。

生成部署包：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn clean package
```

产物位置：

```text
target/ratel-fm-portable.zip
```

解压后使用：

```bat
bin\windows\start.bat
bin\windows\stop.bat
bin\windows\status.bat
```

Linux 使用：

```bash
chmod +x bin/linux/*.sh bin/linux/cert/*.sh runtime/jdk/bin/java runtime/jdk/bin/keytool
bin/linux/start.sh
bin/linux/stop.sh
bin/linux/status.sh
```

Windows 使用 `bin\windows` 下的原生 `.bat` 脚本，不依赖 PowerShell；Linux 使用 `bin/linux` 下的 `.sh` 脚本。外置配置位于 `config\application.yml`，日志位于 `logs\`，业务附件位于 `files\`，内置 JDK 位于 `runtime\jdk`，启动脚本默认 JVM 参数为 `-Xms1g -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC`，OOM 时会在 `logs` 目录生成 heap dump。可通过 `RATEL_JVM_XMS`、`RATEL_JVM_XMX`、`RATEL_JVM_MAX_METASPACE`、`RATEL_JAVA_OPTS` 覆盖默认值。

便携包默认同时提供 HTTP 和 HTTPS。普通业务可以继续通过 `http://当前IP:38000/ratel/fm` 使用；语音控制、麦克风授权和浏览器经纬度定位建议通过 `https://当前IP:38443/ratel/fm` 使用。启动脚本会在 `certs` 目录维护固定本地 CA，并在每次启动时根据当前电脑主机名和 IPv4 地址重新生成服务证书。笔记本 IP 变化后只需要重新启动，其他电脑只要信任一次 `certs\ratel-local-ca.cer`。

Windows 客户端信任本地 CA：

```bat
bin\windows\cert\install-local-ca-current-user.bat
```

如果是其他电脑访问，把服务器 `certs\ratel-local-ca.cer` 复制到该电脑后，安装到“当前用户”的“受信任的根证书颁发机构”。相关环境变量：`RATEL_HTTPS_ENABLED` 默认 `true`，`RATEL_HTTPS_PORT` 默认 `38443`，`SERVER_PORT` 默认 `38000`。

当前默认内置的是 `D:\jdk\jdk-24.0.1` 下的 Windows JDK。若要把同一个 zip 目录部署到 Linux，需要把 `runtime/jdk` 替换为 Linux x64 JDK 24 目录；应用 Jar、配置和脚本结构不变。

### 可选 Ollama 独立包

AI 助手默认优先访问本地 Ollama。Ollama 不打入 Ratel FM 主部署包，而是封装为独立产物：

```text
target/ratel-fm-ollama.zip
```

Ollama 独立包包含运行时和本地模型，压缩耗时较长；默认 `mvn package` 不会重新生成它。只有 Ollama 运行时、启停脚本或 `ollama-models` 里的模型发生变化时，再显式执行：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn -DskipTests package -Pwith-ollama
```

部署时将 Ollama 独立包解压到 Ratel FM 主部署目录的同级目录：

```text
deploy-root
├── ratel-fm-0.0.1-SNAPSHOT-portable
└── ratel-fm-ollama
```

Ratel FM 与 Ollama 完全隔离启停。Ratel FM 的 `bin/windows/*.bat`、`bin/linux/*.sh` 只处理主应用，不会启动或关闭 Ollama。需要本地 AI 助手时，单独进入 `ratel-fm-ollama` 执行 Ollama 启停脚本。

Ollama 独立包内的默认约定：

```text
ratel-fm-ollama
├── bin/windows           # Windows 启动、关闭脚本
├── bin/linux             # Linux 启动、关闭脚本
├── runtime/windows/ollama # Windows x64 运行时
├── runtime/linux/ollama   # Linux x64 运行时
├── models                # Ollama 模型目录
├── logs                  # Ollama 日志目录
└── run                   # Ollama PID 文件目录
```

Windows 使用：

```bat
cd ratel-fm-ollama
bin\windows\start.bat
bin\windows\stop.bat
```

Linux 使用：

```bash
cd ratel-fm-ollama
chmod +x bin/linux/*.sh runtime/linux/ollama/ollama
bin/linux/start.sh
bin/linux/stop.sh
```

独立包同时封装官方 Windows x64 和 Linux x64 Ollama 运行时，并从工程根目录 `ollama-models` 自动打入推荐模型到 `models` 目录，包含 `qwen2.5:7b`、`llama3.2:3b`、`deepseek-r1:8b`、`bge-m3:latest`。两个平台共用模型目录。启动脚本默认监听 `0.0.0.0:11434`；Windows BAT 会确保防火墙规则存在，首次运行需要管理员权限。

Ollama 独立包脚本默认同时启动 Open WebUI 控制台，默认访问地址为 `http://10.105.12.136:8080`。独立包内置便携 Python 3.11 和预装 Open WebUI 依赖，部署机不需要安装 Python，启动脚本也不会在线安装依赖或修改系统 Python 环境。Open WebUI 启动失败只输出警告，不影响 Ollama 继续提供模型 API。

Ratel FM 支持 Ollama 模型路由：普通业务问答默认使用 `qwen2.5:7b`，语音/操作指令默认使用 `llama3.2:3b`，复杂分析默认使用 `deepseek-r1:8b`，知识索引和智能检索 embedding 默认使用 `bge-m3:latest`。对话模型如果优先模型未下载，会自动降级到本机已下载的其他 Ollama 模型；embedding 模型在 Qdrant 模式下必须可用，不会降级到千问 embedding 或 H2 知识表。

主应用便携包同时提供总控脚本，用于统一调用 Ratel FM、Ollama、Qdrant 各自独立包的启停脚本。总控脚本不会把三个组件耦合成同一个进程；任一组件启动或关闭失败，只记录提示，不影响另外两个组件继续处理。

Windows：

```bat
bin\windows\start-all.bat
bin\windows\stop-all.bat
```

Linux：

```bash
bin/linux/start-all.sh
bin/linux/stop-all.sh
```

### 可选 Qdrant 独立包

本地向量数据库不打入 Ratel FM 主部署包，而是封装为独立产物：

```text
target/ratel-fm-qdrant.zip
```

默认 `mvn package` 不会重新生成它。Qdrant 独立包和 Ollama 独立包遵循同一原则：日常发布只打 Ratel FM 主包；只有 Qdrant 运行时、启停脚本或本目录结构发生变化时，才显式执行：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn -DskipTests package -Pwith-qdrant
```

部署时将 Qdrant 独立包解压到 Ratel FM 主部署目录的同级目录：

```text
deploy-root
├── ratel-fm-0.0.1-SNAPSHOT-portable
├── ratel-fm-ollama
└── ratel-fm-qdrant
```

Ratel FM、Ollama、Qdrant 完全隔离启停。Ratel FM 的 `bin/windows/*.bat`、`bin/linux/*.sh` 只处理主应用，不会启动或关闭 Qdrant。需要本地向量数据库时，单独进入 `ratel-fm-qdrant` 执行 Qdrant 启停脚本。

Qdrant 独立包内的默认约定：

```text
ratel-fm-qdrant
├── bin/windows           # Windows 启动、关闭、状态脚本
├── bin/linux             # Linux 启动、关闭、状态脚本
├── runtime/windows/qdrant # Windows x64 运行时
├── runtime/linux/qdrant   # Linux x64 运行时
├── static                # Qdrant Web UI v0.2.15，访问 /dashboard/
├── storage               # Qdrant 本地持久化数据目录
├── snapshots             # Qdrant 快照目录
├── logs                  # Qdrant 日志目录
└── run                   # Qdrant PID 文件目录
```

Windows 使用；启动后 Dashboard 地址为 `http://127.0.0.1:6333/dashboard/`：

```bat
cd ratel-fm-qdrant
bin\windows\start.bat
bin\windows\status.bat
bin\windows\stop.bat
```

Linux 使用：

```bash
chmod +x bin/linux/*.sh runtime/linux/qdrant/qdrant
bin/linux/start.sh
bin/linux/status.sh
bin/linux/stop.sh
```

独立包同时封装官方 Windows x64 MSVC 和 Linux x64 GNU 运行时，默认监听 `http://0.0.0.0:6333`，gRPC 端口为 `0.0.0.0:6334`；Windows BAT 会确保防火墙规则存在，首次运行需要管理员权限。

如果 `http://127.0.0.1:6333/` 能返回 Qdrant 版本信息，但 `http://<本机IP>:6333/` 显示连接被拒绝，说明当前 Qdrant 进程只监听在 loopback。先停止旧进程，确认没有环境变量 `QDRANT_HOST=127.0.0.1` 或 `QDRANT__SERVICE__HOST=127.0.0.1`，再使用本包新脚本启动；仍无法访问时检查 Windows 防火墙是否放行 TCP 6333/6334。

启用 Qdrant 作为智能检索和 ratel助手的向量库时，需要先单独启动 Qdrant，再启动 Ratel FM，并在 Ratel FM 环境变量中打开开关：

```powershell
$env:FM_AI_VECTOR_DATABASE_PROVIDER='qdrant'
$env:FM_AI_QDRANT_BASE_URL='http://10.105.12.136:6333'
$env:FM_AI_OLLAMA_EMBEDDING_MODEL='bge-m3:latest'
```

默认 `FM_AI_VECTOR_DATABASE_PROVIDER=qdrant`，知识分片 payload 和 embedding 向量只写入 Qdrant，智能检索和 ratel助手只从 Qdrant 召回，不再保留或回退到 H2 知识表，避免同一份向量数据存在多份。切换到 Qdrant 后应执行一次知识索引重建，重建成功后系统会清理 H2 知识分片；增量更新和删除也会同步清理 H2 中的旧分片。如果现场不部署 Qdrant，可显式配置 `FM_AI_VECTOR_DATABASE_PROVIDER=h2`，系统使用 H2 中的 `fm_knowledge_documents` 保存知识分片；如开启 `FM_AI_EMBEDDING_ENABLED=true`，会在 H2 中保存 embedding JSON 并在 Java 侧做关键词/语义混合评分。

关系型数据库仍按部署选择使用 PostgreSQL 或 H2；向量数据库通过 `FM_AI_VECTOR_DATABASE_PROVIDER` 在 `h2` 和 `qdrant` 之间选择，二者互斥。Qdrant 只保存和检索向量，不负责生成 embedding；Ratel FM 通过本地 Ollama 的 `FM_AI_OLLAMA_EMBEDDING_MODEL` 生成向量。Qdrant 模式下如果 Qdrant 未启动、collection 不可用或本地 embedding 模型不可用，索引重建和检索会显式失败，不会自动改用 H2 或千问 embedding。

系统提供“AI 组件状态”页面，用于查看当前大模型 provider、向量库 provider、知识索引分片数、各组件状态和按来源类型统计的索引数量。该页面只读，不触发重建或业务写操作，可用于排查 Qdrant、Ollama、embedding 和流式输出问题。流式输出现场验证可使用 `tools/ai-stream-smoke.ps1`，登录后复制浏览器 Cookie 执行一次 SSE 请求，观察 `meta`、`delta`、`done` 或 `error` 事件是否按预期返回。

## 前端开发

前端源码位于 `frontend/`，开发时可单独启动 Vite，接口代理到后端：

```powershell
cd frontend
npm install
npm run dev
```

开发地址：`http://localhost:5173/ratel/fm/`。

生产部署不需要单独部署前端。执行 Maven 打包时会自动构建前端，并把产物写入 `target/classes/static` 打进后端 jar；最终交付 `target/ratel-fm-portable.zip`，解压后启动后端即可同时访问前端页面：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn clean package
```

访问首页：`http://localhost:38000/ratel/fm/`。

## 附件存储

凭证记账、采购管理、物流管理、库存台账、应收应付支持多个业务附件。附件元数据保存到 `fm_attachments`，业务记录和附件 ID 的关系保存到 `fm_business_attachments`；物理文件默认存储到程序运行目录下的 `files/`，可通过 `FM_ATTACHMENT_BASE_DIR` 覆盖。

## 默认账号

系统首次启动会初始化：

- 登录账号：`admin`
- 身份证标识：`ADMIN_IDENTITY_0001`
- 密码：`admin123`
- 角色：`ADMIN`

首次部署后应立即修改默认密码，或通过环境变量覆盖：

```powershell
$env:FM_ADMIN_USERNAME='admin'
$env:FM_ADMIN_IDENTITY_NO='ADMIN_IDENTITY_0001'
$env:FM_ADMIN_PASSWORD='your-strong-password'
```

## API 文档

启动后访问：

- Knife4j：`http://localhost:38000/ratel/fm/doc.html`
- OpenAPI JSON：`http://localhost:38000/ratel/fm/v3/api-docs`
- Swagger UI：`http://localhost:38000/ratel/fm/swagger-ui/index.html`

在线接口文档使用 Knife4j 增强注解：

- Controller 类使用 `@ApiSupport(order = ..., author = "ratel / WenZhang / 18782945613")` 标识模块排序和开发信息。
- Controller 方法使用 `@ApiOperationSupport(order = ..., author = "ratel / WenZhang / 18782945613")` 标识接口排序和开发信息。
- DTO 字段使用 `@Schema(description = "...")` 描述字段含义。

登录接口：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123",
  "terminalType": "PC",
  "force": false
}
```

`username` 字段可填写账号或身份证号。账号和身份证号均全系统唯一。

后续请求添加：

```http
Cookie: FM_TOKEN=<jwt>
```

登录成功后后端会写入 HttpOnly JWT Cookie，浏览器后续自动携带；令牌有效期 1 小时，距离过期不足 30 分钟时后端自动续期。重复登录时前端会二次确认，确认后请求体使用 `"force": true` 挤掉旧登录。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/me` | 当前登录人 |
| GET | `/api/auth/menu-codes` | 当前登录人授权菜单编码 |
| GET | `/api/auth/menus` | 当前登录人授权菜单资源 |
| PUT | `/api/auth/profile` | 修改个人资料 |
| PUT | `/api/auth/password` | 修改个人密码 |
| POST | `/api/auth/avatar` | 上传个人头像 |
| GET | `/api/users` | 人员列表 |
| POST | `/api/users` | 新增人员 |
| PUT | `/api/users/{id}` | 修改人员 |
| PUT | `/api/users/{id}/password` | 管理员重置人员密码 |
| POST | `/api/users/{id}/avatar` | 管理员维护人员头像 |
| DELETE | `/api/users/{id}` | 删除人员 |
| GET | `/api/roles` | 角色列表 |
| GET | `/api/menus` | 菜单资源列表 |
| GET | `/api/menus/all` | 菜单管理列表 |
| POST | `/api/menus` | 新增或更新菜单资源 |
| DELETE | `/api/menus/{id}` | 删除菜单资源 |
| POST | `/api/roles` | 新增或更新角色 |
| GET | `/api/finance/subjects` | 科目列表 |
| POST | `/api/finance/subjects` | 新增科目 |
| POST | `/api/finance/vouchers` | 新增凭证 |
| POST | `/api/finance/vouchers/{id}/post` | 凭证过账 |
| GET | `/api/finance/reports/trial-balance` | 试算平衡表 |
| POST | `/api/purchase-orders` | 新增采购单 |
| POST | `/api/purchase-orders/{id}/status/{status}` | 变更采购状态 |
| POST | `/api/shipments` | 新增物流单 |
| POST | `/api/shipments/{id}/status-confirm` | 确认物流状态和最新物流信息 |
| GET | `/api/ar-ap-bills/payment-stats` | 按项目和客户/供应商查询收付统计 |
| GET | `/api/insights/overview` | 经营与财务概览 |
| GET | `/api/search?keyword=xxx` | 智能检索 |

## 配置项

右上角天气优先使用浏览器 Geolocation API 获取当前电脑位置经纬度，并按该坐标查询 Open-Meteo 天气；浏览器不支持定位、非安全上下文、用户拒绝授权或定位失败时，回退到 `FM_WEATHER_LOCATION_NAME`、`FM_WEATHER_LATITUDE`、`FM_WEATHER_LONGITUDE` 配置的默认地区。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| SERVER_PORT | `38000` | 服务端口 |
| SERVER_SERVLET_CONTEXT_PATH | `/ratel/fm` | 服务上下文路径 |
| RATEL_HTTPS_ENABLED | `true` | 便携包启动脚本是否启用 HTTPS 入口 |
| RATEL_HTTPS_PORT | `38443` | HTTPS 入口端口；HTTP 继续使用 `SERVER_PORT` |
| FM_DB_URL | `jdbc:h2:file:./data/ratel-fm;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH` | 默认 H2 文件数据库地址 |
| FM_DB_USERNAME | `sa` | 数据库用户名 |
| FM_DB_PASSWORD | 空 | 数据库密码 |
| FM_DB_DRIVER | `org.h2.Driver` | 数据库驱动 |
| FM_JPA_DIALECT | `org.hibernate.dialect.H2Dialect` | JPA 数据库方言 |
| FM_TOKEN_SECRET | `ratel-fm-local-development-secret-change-me` | Token 签名密钥 |
| FM_TOKEN_TTL_MINUTES | `60` | Token 有效期分钟数 |
| FM_TOKEN_REFRESH_THRESHOLD_MINUTES | `30` | Token 距离过期多少分钟内自动续期 |
| FM_ADMIN_USERNAME | `admin` | 初始化管理员账号 |
| FM_ADMIN_IDENTITY_NO | `ADMIN_IDENTITY_0001` | 初始化管理员唯一身份证标识，默认账号保护也使用该值 |
| FM_ADMIN_PASSWORD | `admin123` | 初始化管理员密码 |
| FM_WEATHER_ENABLED | `true` | 是否启用右上角天气查询 |
| FM_WEATHER_LOCATION_NAME | `成都` | 浏览器定位不可用时使用的默认天气地区名称 |
| FM_WEATHER_LATITUDE | `30.5728` | 浏览器定位不可用时使用的默认纬度 |
| FM_WEATHER_LONGITUDE | `104.0668` | 浏览器定位不可用时使用的默认经度 |
| FM_WEATHER_FORECAST_HOURS | `12` | 右上角天气面板展示的未来小时数量 |
| FM_WEATHER_TIMEOUT_SECONDS | `5` | 天气和位置反查接口请求超时时间 |
| FM_WEATHER_CACHE_SECONDS | `600` | 天气结果内存缓存秒数；浏览器定位和默认地区分别按坐标缓存 |
| QWEN_API_KEY | 空 | 千问 DashScope API Key；为空时 AI 助手降级为只返回检索结果 |
| QWEN_BASE_URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 千问 OpenAI 兼容接口地址 |
| QWEN_CHAT_MODEL | `qwen-plus` | AI 助手使用的对话模型 |
| QWEN_TIMEOUT_SECONDS | `30` | 千问接口请求超时时间 |
| QWEN_MAX_CONCURRENT_REQUESTS | `2` | 千问接口最大并发请求数，防止多次超时造成内存堆积 |
| QWEN_EXECUTOR_THREADS | `4` | 千问 HTTP 客户端固定线程数 |
| QWEN_FAILURE_THRESHOLD | `3` | 千问连续失败后触发熔断的次数 |
| QWEN_CIRCUIT_BREAKER_SECONDS | `60` | 千问熔断持续秒数 |
| QWEN_MAX_PROMPT_CHARS | `16000` | 单次发送给千问的提示词最大字符数 |
| QWEN_MAX_RESPONSE_CHARS | `120000` | 千问响应体最大字符数 |
| QWEN_MAX_OUTPUT_TOKENS | `800` | 千问单次回答最大输出 token 数 |
| QWEN_EMBEDDING_MODEL | `text-embedding-v4` | 千问兼容 embedding 模型配置；当前知识索引默认使用本地 Ollama embedding，不依赖该项 |
| FM_AI_OLLAMA_ENABLED | `true` | 是否启用本地 Ollama 作为 AI 助手优先模型 |
| FM_AI_OLLAMA_BASE_URL | `http://10.105.12.136:11434` | Ratel FM 后端访问 Ollama 的地址，现场可改为实际 Ollama 电脑 IP |
| FM_AI_OLLAMA_CHAT_MODEL | `qwen2.5:7b` | 普通业务问答优先使用的 Ollama 本地模型 |
| FM_AI_OLLAMA_COMMAND_MODEL | `llama3.2:3b` | 语音/操作指令优先使用的 Ollama 本地模型 |
| FM_AI_OLLAMA_REASONING_MODEL | `deepseek-r1:8b` | 复杂分析优先使用的 Ollama 本地模型 |
| FM_AI_OLLAMA_EMBEDDING_MODEL | `bge-m3:latest` | 知识索引、智能检索和 Qdrant 查询向量使用的本地 Ollama embedding 模型 |
| FM_AI_OLLAMA_TIMEOUT_SECONDS | `180` | Ollama 请求超时时间 |
| FM_AI_OLLAMA_MAX_CONCURRENT_REQUESTS | `1` | Ollama 最大并发请求数，保护本机 CPU 和内存 |
| FM_AI_OLLAMA_EXECUTOR_THREADS | `2` | Ollama HTTP 客户端固定线程数 |
| FM_AI_OLLAMA_FAILURE_THRESHOLD | `2` | Ollama 连续失败后触发熔断的次数 |
| FM_AI_OLLAMA_CIRCUIT_BREAKER_SECONDS | `30` | Ollama 熔断持续秒数 |
| FM_AI_OLLAMA_MAX_PROMPT_CHARS | `12000` | 单次发送给 Ollama 的提示词最大字符数 |
| FM_AI_OLLAMA_MAX_RESPONSE_CHARS | `80000` | Ollama 响应体最大字符数 |
| FM_AI_OLLAMA_MAX_OUTPUT_TOKENS | `800` | Ollama 单次回答最大输出 token 数 |
| FM_AI_ASSISTANT_STREAM_ENABLED | `true` | 是否启用 ratel助手 SSE 流式输出 |
| FM_AI_ASSISTANT_STREAM_TIMEOUT_SECONDS | `90` | 单次流式连接最长存活时间，超时会取消上游模型请求 |
| FM_AI_ASSISTANT_STREAM_HEARTBEAT_SECONDS | `10` | SSE 心跳间隔，避免代理或浏览器误判空闲 |
| FM_AI_ASSISTANT_MAX_CONCURRENT_STREAMS | `4` | ratel助手全局最大流式连接数 |
| FM_AI_ASSISTANT_MAX_STREAMS_PER_USER | `1` | 同一登录用户最大流式连接数 |
| FM_AI_ASSISTANT_STREAM_EXECUTOR_THREADS | `4` | ratel助手流式输出专用线程数 |
| FM_AI_ASSISTANT_STREAM_CAPTURE_CHARS | `12000` | 服务端为会话摘要和最终元数据保留的回答样本长度，不限制实际流式输出 |
| OLLAMA_HOST | `0.0.0.0:11434` | Ollama 独立包启动脚本使用的监听地址 |
| OLLAMA_MODELS | `<ratel-fm-ollama>/models` | Ollama 独立包启动脚本使用的模型目录 |
| QDRANT_HOST | `0.0.0.0` | Qdrant 独立包启动脚本使用的监听地址 |
| QDRANT_HTTP_PORT | `6333` | Qdrant HTTP API 端口 |
| QDRANT_GRPC_PORT | `6334` | Qdrant gRPC 端口 |
| QDRANT_STORAGE_DIR | `<ratel-fm-qdrant>/storage` | Qdrant 本地数据目录 |
| QDRANT_SNAPSHOTS_DIR | `<ratel-fm-qdrant>/snapshots` | Qdrant 快照目录 |
| FM_AI_REBUILD_ON_STARTUP | `false` | 启动时是否强制重建 AI 知识索引 |
| FM_AI_REBUILD_WHEN_EMPTY | `true` | 当前向量库为空时是否在后台初始化索引 |
| FM_AI_REBUILD_INITIAL_DELAY_SECONDS | `15` | 空索引首次初始化前等待 Ollama/Qdrant 就绪的秒数 |
| FM_AI_REBUILD_MAX_ATTEMPTS | `12` | 空索引初始化失败后的最大尝试次数 |
| FM_AI_REBUILD_RETRY_DELAY_SECONDS | `15` | 空索引初始化失败后的重试间隔秒数 |
| FM_AI_INCLUDE_ADMINISTRATIVE_DIVISIONS | `false` | 是否为四万余条全国行政区划逐条生成向量；笔记本默认关闭 |
| FM_AI_MAX_DOCUMENTS | `100000` | 单次知识索引最多生成的分片数量 |
| FM_AI_EMBEDDING_ENABLED | `false` | H2 向量库模式下是否为知识索引生成向量；Qdrant 模式会强制使用本地 embedding |
| FM_AI_MAX_CONTEXT_DOCUMENTS | `5` | AI 助手单次最多使用的本地知识上下文条数 |
| FM_AI_CHUNK_SIZE | `800` | 知识索引分片大小，越大单条上下文 token 越多 |
| FM_AI_QUERY_REWRITE_MODEL_ENABLED | `false` | 智能检索 query 改写是否调用大模型；默认关闭以节省 token |
| FM_AI_VECTOR_DATABASE_PROVIDER | `qdrant` | 向量数据库提供方，支持 `h2` 或 `qdrant`；二者互斥，不做自动降级 |
| FM_AI_QDRANT_BASE_URL | `http://10.105.12.136:6333` | Ratel FM 后端访问 Qdrant HTTP API 的地址，现场可改为实际 Qdrant 电脑 IP |
| FM_AI_QDRANT_COLLECTION_NAME | `ratel_fm_knowledge` | Qdrant 知识索引集合名称 |
| FM_AI_QDRANT_TIMEOUT_SECONDS | `10` | Qdrant 请求超时时间 |
| FM_AI_QDRANT_MAX_CONCURRENT_REQUESTS | `2` | Qdrant 最大并发请求数 |
| FM_AI_QDRANT_EXECUTOR_THREADS | `2` | Qdrant HTTP 客户端固定线程数 |
| FM_AI_QDRANT_BATCH_SIZE | `64` | 重建索引时单批写入 Qdrant 的向量点数量 |
| FM_AI_QDRANT_FAILURE_THRESHOLD | `2` | Qdrant 连续失败后触发熔断的次数 |
| FM_AI_QDRANT_CIRCUIT_BREAKER_SECONDS | `30` | Qdrant 熔断持续秒数 |
| FM_AI_QDRANT_MAX_RESPONSE_CHARS | `200000` | Qdrant 单次响应体最大字符数 |
| FM_AI_WEB_SEARCH_ENABLED | `true` | 是否启用 ratel助手互联网检索 |
| FM_AI_WEB_SEARCH_PROVIDER | `tavily` | 互联网检索提供方，可选 `tavily`、`bing`、`duckduckgo` |
| FM_AI_TAVILY_API_KEY | 空 | Tavily API Key；provider 为 `tavily` 时使用 |
| FM_AI_TAVILY_ENDPOINT | `https://api.tavily.com/search` | Tavily Search API 地址 |
| FM_AI_WEB_SEARCH_MAX_RESULTS | `3` | 单次互联网检索最多返回来源数量 |
| FM_AI_WEB_SEARCH_TIMEOUT_SECONDS | `8` | 互联网检索请求超时时间 |
| FM_AI_WEB_SEARCH_MAX_CONCURRENT_REQUESTS | `2` | 互联网检索最大并发请求数 |
| FM_AI_WEB_SEARCH_EXECUTOR_THREADS | `4` | 互联网检索 HTTP 客户端固定线程数 |
| FM_AI_WEB_SEARCH_MAX_RESPONSE_BYTES | `524288` | 搜索接口响应体最大读取字节数 |
| FM_AI_WEB_SEARCH_MAX_PAGE_BYTES | `262144` | 单个网页正文最大读取字节数 |
| FM_AI_WEB_SEARCH_MAX_FETCH_PAGES | `1` | 单次检索最多补抓正文的网页数量 |
| FM_AI_TAVILY_INCLUDE_RAW_CONTENT | `false` | Tavily 是否返回原始网页正文；默认关闭以降低内存占用 |

## 代码注释规范

项目要求源码具备可交接性。后续任何新增或修改代码，都必须同步补充注释。

- Java 类、接口、枚举、record 必须说明模块职责。
- Java 实体字段、DTO 字段、常量、成员变量必须说明业务含义、来源、用途和限制。
- Java 方法必须说明方法目的，并按步骤说明核心实现流程。
- Vue/TypeScript 的 `interface`、`type`、`const`、`let`、响应式变量、表单字段、计算属性和函数必须说明用途。
- 查询、保存、状态流转、凭证过账、库存扣减、AI 检索、查看流水、导出和附件类方法必须写清主要步骤。
- 新增数据库字段时，必须同步维护实体注释、DTO 注释、前端类型注释、数据库注释初始化和查看流水展示。
- 配置文件必须同步维护注释：`application*.yml`、`logback-spring.xml`、Maven assembly 描述符、启停脚本等要说明配置用途、默认值来源、运行边界和安全注意事项；JSON、lock 文件等语法不支持注释的配置文件，在相邻 README 或主文档中说明。

JSON 配置文件维护口径：

- `frontend/package.json`：声明前端包名、版本、私有包标记、ESM 模块类型、`dev/build/preview` 脚本、运行依赖和开发依赖；不能写入注释，修改依赖后必须同步提交 `frontend/package-lock.json`。
- `frontend/package-lock.json`：锁定 npm 依赖解析结果，保证打包机和开发机安装一致；由 `npm install` 生成，不手工编辑。
- `frontend/tsconfig.json`：声明 TypeScript 编译目标、Vite/Vue 模块解析方式、严格模式、DOM 类型、`@/*` 路径别名和源码包含范围；因为 JSON 不支持注释，字段说明维护在本段。

当前可用下面的声明级扫描思路自查是否遗漏注释：扫描 Java 的类/字段/方法声明，以及前端脚本区的类型、变量、常量、方法和对象字段声明，确认声明前存在 `/** ... */` 或 `// ...` 注释。

## 表单校验规范

前端负责即时提示，后端负责最终兜底。新增表单字段时，必须同时维护前端输入限制、保存前校验、后端 DTO 校验和数据库字段长度。

| 字段类型 | 规则 |
| --- | --- |
| 一般人名 | 限制 20 个中文字符 |
| 联系方式 | 支持手机号或座机号 |
| 身份证号 | 校验 18 位中国居民身份证格式和校验位 |
| 车牌号 | 校验普通车牌、新能源车牌，并兼容电动车牌号 |
| 地址输入框 | 限制 300 个中文字符 |
| 备注/说明 | 使用文本域，限制 2000 个中文字符；全局限制文本域最大拖拽高度，避免影响弹窗和页面布局 |
| 摘要 | 限制 200 个字符 |
| 来源单号 | 限制 300 个中文字符 |

相关前端公共规则位于 `frontend/src/utils/validators.ts`。后端公共校验注解位于 `src/main/java/com/ratel/fm/common/validation`。

## 查看流水维护规范

新增表单字段后，必须同步维护查看流水。

- 业务保存或状态确认时，要把新增字段写入业务操作日志快照。
- 前端 `OperationLogDrawer` 要展示新增字段。
- 物流状态确认流水必须保存确认时点的物流主表字段，避免后续主表变更影响历史追溯。
- 快照字段优先保存用户可理解的名称，不要只保存内部编码。

## AI 检索维护规范

ratel 助手默认使用本地知识库模式，知识范围应覆盖系统业务数据、附件文本和模块说明，不应只覆盖会计科目。

- 新增业务模块、字段或关键状态时，需要同步更新知识索引内容。
- 智能检索结果默认屏蔽基础信息字典等低价值基础资料，减少干扰。
- AI 回答应基于检索引用来源；缺少来源时需要明确说明无法确认，减少幻觉。
- 千问、Tavily、Bing 等 API Key 只能通过环境变量或安全外置配置注入，禁止写入源码、README、PA 或打包配置。

## 打包验证

Windows 构建机可直接运行 `src/main/resources` 下的三个脚本：`build-ratel-fm.bat` 构建主包，`build-qdrant.bat` 构建 Qdrant 独立包，`build-ollama.bat` 准备包内 Python/Open WebUI 并构建 Ollama 独立包。脚本默认固定使用仓库约定的 JDK 24 和 Maven；需要切换工具目录时通过 `RATEL_BUILD_JAVA_HOME`、`RATEL_BUILD_MAVEN_HOME` 显式覆盖，避免误用机器遗留的 Java 8 环境变量。

每次改动完成后必须执行 Ratel FM 主包打包验证。当前本机已验证通过的 PowerShell 命令为：

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;D:\java_develop_V1.0\apache-maven-3.6.3\bin;$env:Path"
mvn -DskipTests package
```

成功后生成：

```text
target/ratel-fm.jar
target/ratel-fm-portable.zip
```

只有 Ollama 运行时、启停脚本或 `ollama-models` 里的模型发生变化时，才执行完整 Ollama 独立包打包：

```powershell
mvn -DskipTests package -Pwith-ollama
```

成功后额外生成：

```text
target/ratel-fm-ollama.zip
```

只有 Qdrant 运行时、启停脚本或本目录结构发生变化时，才执行完整 Qdrant 独立包打包；否则只执行上面的 Ratel FM 主包打包命令即可：

```powershell
mvn -DskipTests package -Pwith-qdrant
```

成功后额外生成：

```text
target/ratel-fm-qdrant.zip
```

构建中可能出现 Vite chunk 体积、第三方库 PURE 注释、H2Dialect 显式配置和 npm audit 警告；只要 Maven 最终输出 `BUILD SUCCESS`，本次打包产物即已生成。

## 开发说明

- 产品架构见 [PA.md](PA.md)。
- 系统支持多所属公司/多账套服务。登录页必须选择所属公司，后端将所属公司编码写入 JWT Cookie；首页概览、会计科目、凭证记账、采购管理、物流管理、库存台账、应收应付、统计报表、操作日志和 AI 知识检索均按当前 Cookie 中的所属公司自动隔离。
- 所属公司来自 `ORGANIZATION` 字典。只有默认 `admin` 账号可以在字典管理中查看和维护所属公司字典，也只有 `admin` 可在人员管理中修改人员所属公司；其他用户新增人员时默认当前登录所属公司且前端禁用该字段，后端同时兜底校验。
- 凭证记账列表提供“生成凭证”按钮，前端基于凭证明细 Canvas 绘制在线记账凭证，支持弹窗预览和 PNG 下载。
- 目前默认使用 H2 文件库 + JPA 自动建表，并维护 [init.sql](src/main/resources/init.sql) 作为 PostgreSQL 初始化基线；生产环境建议引入 Flyway 或 Liquibase。
- 当前智能检索已覆盖为 AI 知识检索：向量库选择 `h2` 时，业务数据和附件文本写入 `fm_knowledge_documents`，可在 H2 中保存向量 JSON 并由 Java 侧混合评分；向量库选择 `qdrant` 时，知识 payload 和向量只写入 Qdrant，不再同步保存到 H2 知识表，也不做自动降级。
- 千问模型通过 DashScope OpenAI 兼容接口接入，使用 `QWEN_API_KEY` 环境变量配置密钥；互联网检索可使用 `FM_AI_TAVILY_API_KEY` 接入 Tavily。不要把密钥写入源码、README 或外置配置文件。
- 当前 Token 是 HMAC 签名 JWT，登录会话状态落库以支持强制下线和同终端类型唯一登录。
- 后续新增前后端文件必须先判断所属业务模块，`web`、`service`、`repository`、`domain`、`config`、前端 `views` 等目录都按模块分目录存储，避免重新出现平铺结构。
