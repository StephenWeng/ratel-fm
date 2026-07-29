# Ratel FM Engineering Rules, Packaging Rules, Comment Rules, And Change Log

[中文版本](编码打包注释和变更记录.md)

Version date: 2026-07-23

This document records engineering principles, packaging rules, and recent changes. Product design is documented in `Product-Design.md`; technical architecture is documented in `Technical-Architecture.md`; AI knowledge is documented in `LLM-Development-Guide.md`.

## 1. Coding Principles

- Read existing code and root documents before changing behavior.
- Place code by business module; do not add flat shared files without a clear reason.
- All reads and writes must carry company, account-set, and permission context.
- Frontend hiding is an experience control; backend checks are mandatory.
- State changes must write operation logs or business timelines.
- New business fields must update entity, DTO, frontend type, form validation, database initialization, and log display.
- Prefer existing utilities and structured APIs.
- Do not perform unrelated refactoring.

## 2. Comment Rules

Java:

- Class comments describe module responsibility.
- Field comments describe business meaning.
- Method comments describe purpose and key steps.
- Configuration comments describe defaults, boundaries, and risks.

Vue/TypeScript:

- Complex state and flows need comments.
- Obvious assignments and templates should not have empty comments.
- API types should express field meaning.

## 3. Frontend Principles

- Page entries and buttons must match permissions and business status.
- Errors must be explicit, especially login, save, delete, and approval failures.
- Tables and dialogs must have stable scroll areas.
- Loading states should not create unnecessary blocking boxes.
- AI output must not show internal retrieval cards, long raw chunks, or debug information.
- Business Agent is shown only in the AI Assistant page, not repeated on business pages.

## 4. Backend Principles

- Controllers do not contain complex business logic.
- Services own business rules, state transitions, and logs.
- Repository queries must filter by company and account set.
- Write APIs must check permission, status, and idempotency risks.
- AI and Agent APIs must check switches, permissions, account set, and data scope.

## 5. Packaging Principles

- Use JDK 24.
- Quote Maven `-D` arguments in PowerShell.
- Package and start/stop main app, Ollama, and Qdrant independently.
- Separate scripts by platform: `bin/windows`, `bin/linux`.
- Separate runtimes by platform: `runtime/windows`, `runtime/linux`.
- Logs, PID files, runtime data, and model repositories are not committed by default.

Recommended full packaging command:

```powershell
$env:JAVA_HOME='D:\jdk\jdk-24.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dmaven.test.skip=true" "-Dspring-boot.aot.skip=true" clean package "-Pwith-ollama,with-qdrant"
```

## 6. Git Rules

Do not commit:

- `target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `logs/`
- `ollama-models/`
- Ollama/Qdrant runtimes, models, vector data, and runtime data
- local configuration, certificates, and temporary downloads

Use Git LFS or an external artifact repository before uploading large files.

## 7. Documentation Rules

- Every product, architecture, AI, packaging, coding, or interaction change must update all affected root Markdown documents.
- English and Chinese versions are maintained as pairs and must be updated in the same change.
- Before finishing any change, scan all root Markdown documents and keep README files, product design, technical architecture, AI guide, engineering log, and package documentation references consistent.

## 8. Recent Changes

### 2026-07-25: Online-Voucher-Style Voucher Entry

- Changed voucher create/edit to an online-voucher-style WYSIWYG entry surface with five default lines, while keeping `Add Line` and voucher import.
- Moved summary to line level. Blank lines are ignored before save; lines with content must complete summary, subject, debit/credit amount, currency, and exchange-rate validation.
- Added customer, department, business user, bookkeeper, and maker snapshots to the voucher header. Summary search now matches both voucher-header summary and voucher-line summaries.
- The online voucher image draws slashes across debit and credit amount areas for empty rows to mark them invalid.

### 2026-07-25: ratel Assistant AI Writing File Generation

- Added the manual `思考` / `AI写作` intent selector in the ratel assistant send area.
- Added `/api/ai/writing/generate` for downloadable xlsx, docx, pdf, and pptx generation.
- Added `AiWritingService`, using Apache POI for Office files and PDFBox for PDF files.
- Purchase-list xlsx output reuses the existing purchase export service. Daily purchase statistics aggregate current-company purchase orders, dates, and line quantities deterministically.

### 2026-07-25: Vue Gold Theme

- Added the `vue-gold` theme configuration, displayed as `Vue 金` in the theme selector.
- Added Vue Gold variables for backgrounds, surfaces, borders, text, primary color, status colors, shadows, focus rings, and scrollbars.
- Adjusted the ratel assistant logo alignment so the brand SVG appears visually centered in the avatar container.
- Improved unified spacing for tabs, filters, tables, and statistic cards so content no longer sits against borders.
- Replaced local hard-coded white panels and gray borders in inventory, accounts receivable/payable, and workflow center screens with theme variables.
- Added global glassmorphism variables and container styling so the shell, panels, filters, tabs, tables, dialogs, and cards use translucent backgrounds, backdrop blur, soft borders, and shadows.
- Added unified glass icon containers, highlights, translucent backgrounds, and soft shadows for menu icons, the brand icon, topbar icon buttons, ratel assistant, and the voice entry.

### 2026-07-25: Voucher Dialog Horizontal Scrollbar Fix

- Reduced voucher-line table column widths and hid horizontal overflow inside voucher dialogs.
- Removed the fixed action column from the attachment table to avoid bottom horizontal scrollbars in empty attachment lists.
- Attachment file names now wrap instead of stretching dialogs.
- Added a global dialog-table overflow guard, and adjusted workflow definition node tables and workflow preview to adapt without horizontal scrolling.

### 2026-07-24: UI Theme And Visual Hierarchy

- Added global theme tokens for shadows, focus rings, scrollbars, radii, and control height.
- Unified Element Plus tables, dialogs, poppers, inputs, buttons, menus, and tabs across themes.
- Improved visual hierarchy for the shell sidebar, topbar, tab area, and content area.
- Improved shell topbar and theme, user, and weather controls for narrow screens.
- Replaced hard-coded light colors in the session-expired dialog with theme variables.

### 2026-07-23: Bilingual Root Documentation

- `README.md` is the English GitHub overview.
- `README.zh.md` is the Chinese GitHub overview.
- English and Chinese versions exist for product design, architecture, AI/LLM guide, and engineering log.

### 2026-07-24: Documentation Synchronization Rule

- Added the rule that Chinese and English documents must be updated together.
- Added the rule that each change must review and update all root Markdown documents affected by the change.
- Synchronized the rule across README, product design, technical architecture, AI guide, and engineering log.

### 2026-07-24: Ollama Model Status Display

- AI component status now shows configured Ollama chat, command, reasoning, vision/OCR, and embedding models.
- The Ollama component also shows the installed model list returned by Ollama `/api/tags`.
- This avoids showing only `qwen2.5:7b` when more local models are installed or configured for other scenarios.

### 2026-07-24: ratel Assistant Searching State

- The floating assistant now opens with a taller stable panel.
- The assistant hides the question input and send button while thinking.
- During retrieval and reasoning it shows only `思考中` with a thinking animation.
- After retrieval finishes, the footer returns to the `输入问题` input state and send button.

### 2026-07-24: ratel Assistant Retrieval Reasoning Review

- File-existence direct answers now list matched files in the conclusion.
- Follow-up detection now treats summary, recap, document-content, and previous-topic phrases as context-dependent requests.
- Knowledge retrieval expands document and resume terms such as candidate, engineer, work experience, project experience, years, and Harbin.
- User-uploaded documents receive file-intent boosting for document, resume, and candidate queries.
- Multi-item conclusions and key evidence now use numbered lines instead of hyphen bullets.
- Assistant output now strips internal reasoning drafts, review phrases, and think tags from both streaming display and final responses.
- Added a unified finance vocabulary for assistant routing and Business Agent selection. Professional finance questions now prioritize live system context instead of local knowledge-file retrieval.
- Architecture split: added `AssistantModelRouter`, `AssistantPromptBuilder`, `AssistantAnswerSanitizer`, `BusinessAgentSelector`, and `BusinessMetricsService` to reduce assistant and Agent orchestration responsibilities.

### 2026-07-23: Unified Business Agent Entry

- The AI Assistant page keeps the `Business Agent` tab.
- Purchase, inventory, AR/AP, and accounting platform pages removed independent `Agent Analysis` buttons.
- ratel assistant routes reconciliation, due reminder, voucher suggestion, and inventory risk intents through the unified entry.
- When `FM_AI_AGENT_ENABLED=false`, the frontend hides the entry, ratel assistant does not call Agent, and backend APIs enforce the switch.

### 2026-07-23: ratel Assistant Experience

- Conversation results show conclusions and key evidence only.
- Search loading no longer shows an extra placeholder card.
- File-existence questions answer whether a file exists first.
- Follow-up questions use recent context.

### 2026-07-23: Page Fixes

- Wrong login password now shows an error.
- Intelligent search result area has internal scrolling.
- The operation manual was refreshed.

### 2026-07-23: Linux Package Completion

- Ollama package includes `runtime/linux/ollama/ollama`.
- Qdrant package includes `runtime/linux/qdrant/qdrant`.
- Ollama Linux has `start.sh`, `stop.sh`, and `status.sh`.
- Qdrant Linux has `start.sh`, `stop.sh`, and `status.sh`.
- If Linux Open WebUI runtime is not bundled, the script skips it without affecting Ollama.

## 9. Verification History

- `frontend` `npm run build`: passed.
- `mvn -DskipTests compile` with JDK 24: passed.
- Full package executed on 2026-07-23 and produced:
  - `target/ratel-fm.jar`: 96.76 MB.
  - `target/ratel-fm-portable.zip`: 317.59 MB.
  - `target/ratel-fm-ollama.zip`: 22343.69 MB.
  - `target/ratel-fm-qdrant.zip`: 64.72 MB.
- ZIP inspection confirmed:
  - Ollama package contains `bin/linux/start.sh`, `bin/linux/stop.sh`, `bin/linux/status.sh`, `runtime/linux/ollama/ollama`.
  - Qdrant package contains `bin/linux/start.sh`, `bin/linux/stop.sh`, `bin/linux/status.sh`, `runtime/linux/qdrant/qdrant`.

## 2026-07-25 Voucher UX, OCR diagnostics, and 20-user regression

- Changed voucher subject entry to hierarchical cascader selection, tightened voucher columns, added per-line subject/debit-credit validation, and rendered one slash per blank debit/credit area in online vouchers.
- Distinguished unavailable OCR capability from an available model failing because of timeout or constrained CPU, memory, or disk resources.
- Removed connection-pool starvation by moving database audit persistence to a bounded background executor while preserving synchronous file audit.
- Prevented Qdrant/Ollama incremental-index failures from rolling back core business writes; full manual index rebuild still reports failures.
- Ran three rounds with 20 concurrent users and retained 180-day data. Final core regression completed 780 requests with no failures. Deep AI testing found 30/30 search and 15/15 Agent passes, but only 5/30 assistant answers passed content-quality checks.

## 2026-07-25 ratel Assistant Deep Thinking and AI Writing Business Report Regression

- Changed ratel assistant intent selection from flat buttons to a dropdown, with `思考` as the default and room for more future intents.
- Routed the `思考` intent through `reasoning` mode so `AssistantModelRouter` selects the reasoning model use case instead of normal QA for complex business and finance questions.
- Added staged thinking progress while the assistant is working; the temporary progress message is removed when the final answer arrives, and final answers still show only conclusions and key evidence.
- Changed AI writing business analysis reports to read structured metrics from `BusinessMetricsService`, covering purchase amount, remaining AR/AP, stocked material count, negative-stock count, risks, and suggestions instead of generating a generic placeholder document.
- Added `AiWritingServiceTest` and `BusinessAgentServiceTest` for report file content, permission boundaries, Business Analysis Agent metrics, risks, and self-checks.
- This round ran targeted tests and the frontend build only, not full packaging, because the change is limited to assistant, AI writing, and Agent regression logic and does not affect deployment scripts, runtimes, or assembly descriptors.
- Verification: `npm run build` passed; `mvn "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=AiWritingServiceTest,BusinessAgentServiceTest" test` passed with 3 successful tests.

## 2026-07-27 Three-Round Load and AI Regression Completion

- Added `TEST-PROGRESS.md` as the restart checkpoint, including fixed commands, retained database/report locations, per-stage status, known failures, and the next resumable action.
- Fixed the local build mismatch where the shell `java` was JDK 24 but `JAVA_HOME` still pointed to JDK 8. Added workspace JDK 24 settings and a Surefire Mockito agent because JDK 24 no longer reliably permits inline mock-maker self-attachment.
- Enhanced `tools/perf-ai-regression.mjs` with per-case `progress.json` checkpoints, bounded empty-index retries, actual round/user/day report labels, dynamic leaf accounting-subject selection, and balanced draft-voucher seeding.
- Completed the final immutable-JAR core regression with 20 users, 3 rounds, 180 days, 540 purchase/inventory/AR-AP chains, and 180 balanced draft vouchers: 1,121 requests, zero HTTP failures, zero issues.
- AI round 1 ran 20 search cases, 20 assistant cases, 10 professional Agent cases, and 4 writing files. It found six quality/data-coverage issues. Search was 19/20 and assistant/Agent was 25/30 before fixes.
- Added deterministic accounting safeguards for voucher entries, project gross margin, month-end trial balance/closing, invoice/tax checks, and professional cash-flow gaps. The assistant now states missing evidence instead of substituting AR/AP facts or inventing account codes and conclusions.
- Added the missing draft-voucher data flow so trial-balance, unposted, cross-period, accrual, and closing terminology is searchable through Qdrant. The focused voucher flow and closing search probes passed with zero failures.
- AI rounds 2 and 3 completed without HTTP or high-severity failures. They exposed one and two stochastic content issues respectively; after moving deterministic safeguards ahead of exact knowledge hits, final cash-flow and invoice probes both passed 17/17 requests with no issues.
- Local notebook baseline: Qdrant search P95 was below 200 ms in the full AI round; `qwen2.5:7b` assistant average was about 29 seconds and the observed maximum was about 91 seconds. `deepseek-r1:8b` remains unsuitable for this machine's default reasoning route because it exceeded 180 seconds.
- Final verification before packaging: backend tests 11/11 passed and the frontend production build passed. Optional Ollama/Qdrant assemblies are intentionally not rebuilt because runtime/model contents did not change.
- A discarded environment run produced 60 failures because `spring-boot:run` was serving mutable `target/classes` while packaging and the VS Code compiler replaced that directory. The same workload passed from the packaged immutable JAR; future load tests must run the JAR and must not package concurrently against a class-directory service.
