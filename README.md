# Ratel FM

[中文文档](README.zh.md)

Ratel FM is a lightweight financial management ERP for small teams and local deployments. It combines accounting, purchasing, logistics, inventory, AR/AP, cashier management, workflow approval, attachments, audit logs, intelligent search, local knowledge, and Business Agent analysis in one application.

> Default delivery model: one main application package, one independent Ollama package, and one independent Qdrant package. The main application does not require Ollama or Qdrant to start; AI capabilities are controlled by configuration switches.

## Highlights

- Accounting: chart of accounts, vouchers, posting, unposting, trial balance, statements, and accounting periods.
- Business documents: purchase orders, logistics orders, inventory ledger, material stock, AR/AP bills, settlement, and cashier transactions.
- Accounting platform: creates voucher drafts from source documents while preserving source links.
- Workflow: workflow definitions, bindings, tasks, approval history, approval and rejection.
- Authorization: company, account set, user, role, menu, page button, and backend permission checks.
- Attachments: upload, preview, download, delete, text extraction, and knowledge indexing.
- Auditability: login records, operation logs, business timelines, and file logs.
- AI: ratel assistant, local knowledge QA, intelligent search, OCR, Business Agent, Ollama, and Qdrant.
- Portable deployment: Windows/Linux scripts with separate application, Ollama, and Qdrant packages.

## Quick Access

After startup:

| Entry | URL |
| --- | --- |
| Application | `http://localhost:38000/ratel/fm` |
| Knife4j | `http://localhost:38000/ratel/fm/doc.html` |
| OpenAPI | `http://localhost:38000/ratel/fm/v3/api-docs` |

Default account:

| Field | Default |
| --- | --- |
| Company | `Ratel默认公司` |
| Username | `admin` |
| Password | `admin123` |

Change the default password after first deployment, or override it before first startup:

```powershell
$env:FM_ADMIN_USERNAME='admin'
$env:FM_ADMIN_IDENTITY_NO='ADMIN_IDENTITY_0001'
$env:FM_ADMIN_PASSWORD='your-strong-password'
```

## Tech Stack

| Layer | Stack |
| --- | --- |
| Backend | Java 24, Spring Boot 4, Spring Security, Spring Data JPA |
| Database | H2 file database, PostgreSQL-compatible configuration |
| Frontend | Vue 3, TypeScript, Vite |
| AI | Ollama, Qdrant, H2 knowledge index, SSE |
| Packaging | Maven, frontend-maven-plugin, maven-assembly-plugin |

## Repository Layout

```text
ratel-fm
├── frontend                         frontend project
├── src/main/java/com/ratel/fm        backend source code
├── src/main/resources                configuration, SQL, build scripts
├── src/main/package                  main portable package template
├── src/main/ollama-package           Ollama package template
├── src/main/qdrant-package           Qdrant package template
├── src/main/assembly                 Maven assembly descriptors
├── ollama-models                     local Ollama model repository, not committed
├── README.md                         English GitHub overview
├── README.zh.md                      Chinese GitHub overview
├── Product-Design.md                 English product design
├── 产品设计.md                       Chinese product design
├── Technical-Architecture.md         English technical architecture
├── 技术架构.md                       Chinese technical architecture
├── LLM-Development-Guide.md          English AI/LLM guide
├── 大模型开发知识点整理.md           Chinese AI/LLM guide
├── Engineering-Log.md                English engineering rules and log
└── 编码打包注释和变更记录.md         Chinese engineering rules and log
```

## Business Agent

Business Agent is exposed only through the unified `AI Assistant / Business Agent` tab. Purchase, inventory, AR/AP, and accounting platform pages do not show separate `Agent Analysis` buttons, so the analysis entry and execution path remain consistent.

Current Agent types:

| Agent | Purpose |
| --- | --- |
| Query Agent | Natural language lookup for purchase, logistics, inventory, AR/AP, and vouchers |
| Reconciliation Agent | Checks consistency across purchase, receipt, inventory, payable, payment, and voucher chains |
| Voucher Suggestion Agent | Creates voucher draft suggestions from business sources |
| Due Reminder Agent | Detects due, overdue, and unreconciled AR/AP risks |
| Workflow Assistant Agent | Explains current approval node, next handler, and draft opinions |
| Inventory Risk Agent | Checks negative stock, low stock, transfer anomalies, and receipt-not-in-stock |
| Business Analysis Agent | Summarizes risks and suggestions by project, supplier, customer, and material |
| Attachment/Knowledge QA Agent | Answers from policies, contracts, attachments, and system data |

Configuration:

| Variable | Default | Description |
| --- | --- | --- |
| `FM_AI_AGENT_ENABLED` | `true` | Enables Business Agent |

When Business Agent is disabled, the frontend hides its entry, ratel assistant does not route related intents to the Agent, and backend APIs still enforce the switch as a safety boundary.

## Local Development

Backend requires JDK 24:

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -DskipTests compile
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

## Full Packaging

PowerShell users should quote Maven `-D` arguments:

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package "-Pwith-ollama,with-qdrant"
```

Expected artifacts:

| Artifact | Description |
| --- | --- |
| `target/ratel-fm.jar` | executable backend Jar |
| `target/ratel-fm-portable.zip` | main application portable package |
| `target/ratel-fm-ollama.zip` | independent Ollama Windows/Linux package |
| `target/ratel-fm-qdrant.zip` | independent Qdrant Windows/Linux package |

## Git Rules

Do not commit generated or local runtime content:

- `target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `logs/`
- `ollama-models/`
- Ollama/Qdrant runtime, models, vector data, and runtime data
- local configuration, certificates, PID files, temporary downloads

## Documentation

Documentation maintenance rule:

- Every product, architecture, AI, packaging, coding, or interaction change must update all affected root Markdown documents.
- Chinese and English versions must be updated in the same change.
- Before finishing a change, scan all root Markdown documents and keep cross-links, package documentation lists, and implementation notes consistent.

| English | Chinese |
| --- | --- |
| [Product Design](Product-Design.md) | [产品设计](产品设计.md) |
| [Technical Architecture](Technical-Architecture.md) | [技术架构](技术架构.md) |
| [LLM Development Guide](LLM-Development-Guide.md) | [大模型开发知识点整理](大模型开发知识点整理.md) |
| [Engineering Log](Engineering-Log.md) | [编码、打包、注释和变更记录](编码打包注释和变更记录.md) |

## Maintainers

| Item | Value |
| --- | --- |
| Organization | `ratel` |
| Developer | `WenZhang` |
| Contact | `18782945613` |
