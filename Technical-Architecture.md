# Ratel FM Technical Architecture

[中文版本](技术架构.md)

Version date: 2026-07-23

This document describes system architecture, module boundaries, deployment packages, AI integration, and quality requirements. Product behavior is documented in `Product-Design.md`; AI implementation details are documented in `LLM-Development-Guide.md`.

## 1. Overview

Ratel FM is a frontend-backend integrated application. Vue is built into static assets, and the Spring Boot backend serves both pages and APIs.

```text
browser
  -> Vue 3 frontend
  -> HTTP/SSE API
  -> Spring Security/JWT cookie
  -> controller
  -> service
  -> JPA repository
  -> H2 or PostgreSQL
```

AI components:

```text
ratel assistant / Business Agent
  -> permission and account-set context
  -> system data / attachment text / local knowledge / optional web search
  -> Ollama or another model provider
  -> conclusion, evidence, suggestion, draft
```

Qdrant is an optional vector store. When Qdrant is disabled or unavailable, the system can fall back to the H2 knowledge index where applicable.

## 2. Backend Layers

- Controller: request validation, permission annotations, DTO conversion, unified responses.
- Service: business rules, state transitions, logs, Agent orchestration.
- Repository: database access with company and account-set filters.
- Entity: persistence model with business meaning.
- DTO: frontend input/output model without exposing unnecessary entity internals.
- Config: security, AI, attachments, database, and initialization configuration.

Backend permission enforcement is mandatory and cannot rely on hidden frontend buttons.

## 3. Frontend Architecture

The frontend is organized by business pages:

- `views`: page-level components.
- `components`: reusable assistant, manual, upload, table, and other components.
- `api`: unified API wrapper.
- `stores`: user, permission, menu, and context state.
- `router`: routing and permission entry.

The theme system is maintained by `stores/theme.ts` and `styles/global.css`. Pages should use variables such as `--surface-color`, `--text-color`, `--primary-color`, `--border-color`, and `--shadow-color` instead of fixed light or dark colors. The shell layout, common Element Plus controls, scrollbars, and focus states are adapted globally for light, dark, emerald, finance-blue, and vue-gold themes. Glassmorphism is controlled by `--glass-surface-color`, `--glass-strong-surface-color`, `--glass-border-color`, `--glass-highlight-color`, and `--glass-blur`. Tab containers, filter forms, table cells, statistic cards, and business sections should receive spacing, radii, shadows, translucent backgrounds, and backdrop blur from global styles. Page-level scoped styles should only handle module layout and must not override controls with fixed white backgrounds, fixed gray borders, or edge-hugging tables.
The icon system continues to reuse Element Plus Icons and the project brand SVG. Menu icons, topbar entries, floating assistant, and voice entry use unified glass icon containers, without adding image assets from unclear sources.

Dialog tables are globally guarded against horizontal overflow. Business pages should avoid fixed right columns and oversized minimum widths inside dialogs; long text should wrap or be ellipsized.

AI entries are unified in the AI Assistant page:

- ratel assistant for natural language conversation.
- Business Agent for structured business analysis.
- Local knowledge for upload, indexing, and retrieval.

The floating ratel assistant uses a stable taller panel on first open. During retrieval and reasoning it hides the question input and send button, and shows only `思考中` with a thinking animation.

The floating send area provides a dropdown `思考` / `AI写作` intent selector, with `思考` as the default. `思考` calls `/api/ai/assistant/stream` with `reasoning` mode, and `AssistantModelRouter` routes it to the reasoning model use case. While waiting for the final answer, the frontend shows staged thinking progress and removes that temporary message when the answer arrives. `AI写作` calls `/api/ai/writing/generate` to generate and download files. Backend `AiWritingService` uses Apache POI for xlsx, docx, and pptx, and PDFBox for pdf. Purchase-list output reuses `OperationService.exportPurchaseOrders`; daily purchase statistics aggregate current-company purchase orders and lines deterministically; business analysis reports read purchasing, AR/AP, and inventory metrics through `BusinessMetricsService` instead of falling back to generic placeholder documents.

Follow-up retrieval augments short requests such as summary or recap with recent user questions and conversation summary. Knowledge retrieval also expands document and resume terms so user documents are not displaced by generic system-module results.

Both direct-answer paths and model prompts constrain answer formatting: multi-item conclusions and evidence use numbered lines instead of hyphen bullets.
Assistant streaming and final response assembly both sanitize model output to remove internal reasoning drafts, review phrases, and think tags before content is shown or saved into conversation summary.
`FinancialIntentTerms` centralizes finance vocabulary for assistant routing and Business Agent selection. When a question matches professional finance terms and is not a document/attachment question, the assistant skips local knowledge retrieval and builds the answer from live system context plus the reasoning model.

Purchase, inventory, AR/AP, and accounting platform pages do not keep independent `Agent Analysis` buttons.

## 4. Data Architecture

The default database is H2 file storage. The portable package copies a template database on first startup. PostgreSQL can be used for formal deployments.

Core requirements:

- All business tables include company and account-set boundaries.
- Amount fields must define direction, currency, and precision.
- Status fields must map to business actions.
- Attachments, workflow, vouchers, and source documents keep traceable relationships.
- Important operations write audit logs and business timelines.

Voucher create/edit uses an online-voucher-style entry panel. The frontend filters blank lines before saving and submits valid lines only. Backend `Voucher` stores customer, department, business user, bookkeeper, and maker snapshots. Summary search matches both voucher-header summary and voucher-line summaries. The online voucher canvas draws a top-right to bottom-left slash across debit and credit amount areas for empty rows to mark them invalid.

## 5. Security Architecture

- Login uses JWT cookies.
- Wrong password, disabled account, and permission denial return explicit business errors.
- Menus and buttons are shown by permission.
- Backend APIs recheck menu, button, or business permissions.
- Attachment access checks account set and business permission.
- AI retrieval and Agent analysis must use current user context.

## 6. AI Architecture

AI consists of:

- Model provider: Ollama, with room for other providers.
- Model router: selects models by QA, command, reasoning, or embedding scenario.
- Assistant model router: `AssistantModelRouter` maps command, normal QA, and complex finance reasoning to model use cases.
- Prompt builder: `AssistantPromptBuilder` owns system prompts, user prompts, and context compaction rules.
- Answer sanitizer: `AssistantAnswerSanitizer` removes think tags, reasoning drafts, and dangerous inherited confirmation wording.
- Component health: exposes configured Ollama models and installed `/api/tags` model list for diagnosis.
- Knowledge index: attachments, policies, contracts, and business summaries.
- Intelligent search: keyword, semantic, and hybrid search.
- SSE output: streaming assistant responses.
- Business Agent: task-oriented data lookup, consistency checking, and suggestion generation.

AI does not write business data directly. Voucher suggestions, approval opinions, and business recommendations are returned as drafts or text.

## 7. Business Agent Architecture

The backend receives Business Agent requests and dispatches analyzers according to `agentTypes`.

Execution steps:

1. Read current user, company, account set, and permissions.
2. Use `BusinessAgentSelector` to identify Agent type, module scope, stage, and query scope.
3. Query controlled business data.
4. For business-analysis questions, use `BusinessMetricsService` to create a structured metrics snapshot before generating findings.
5. Run self-checks for critical Agents.
6. Return conclusions, risks, evidence, and suggestions.

When `FM_AI_AGENT_ENABLED=false`, the frontend hides the entry, ratel assistant does not route to Business Agent, and backend APIs reject requests as a final boundary.

## 8. Deployment Package Architecture

Delivery packages:

| Package | Content | Start/Stop Boundary |
| --- | --- | --- |
| Main portable package | Jar, config, H2 template, JDK, application scripts | Ratel FM only |
| Ollama package | Windows/Linux Ollama, models, optional Open WebUI, scripts | Ollama/Open WebUI only |
| Qdrant package | Windows/Linux Qdrant, dashboard, data directories, scripts | Qdrant only |

Platform-specific layout:

```text
bin/windows
bin/linux
runtime/windows
runtime/linux
```

Linux script requirements:

- Use `bash`.
- Manage processes through PID files.
- Run background processes with `nohup`.
- Create `logs`, `run`, `storage`, and `snapshots` directories as needed.
- Run lightweight health checks after startup.
- Do not modify system services or system directories.

## 9. Packaging Commands

Quote Maven `-D` arguments in PowerShell:

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package
```

Full package:

```powershell
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package "-Pwith-ollama,with-qdrant"
```

## 10. Quality Requirements

- Compile and package with JDK 24.
- Frontend build must pass.
- Backend compile must pass.
- The portable package must include application scripts and bundled JDK.
- Ollama/Qdrant packages must include Windows/Linux runtimes and scripts.
- Logs, models, runtimes, and vector data are not committed by default.
- Architecture-impacting changes must update both English and Chinese architecture documents and must be reflected across all root Markdown documents.

## 11. Known Technical Debt

- Database migrations should move from automatic schema update to Flyway or Liquibase.
- Prompt templates should become versioned resource files.
- Agent audit tables and confirmation-token framework need strengthening.
- Finance, inventory, AR/AP, workflow, and AI retrieval need more regression tests.
