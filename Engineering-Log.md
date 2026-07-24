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

### 2026-07-23: Bilingual Root Documentation

- `README.md` is the English GitHub overview.
- `README.zh.md` is the Chinese GitHub overview.
- English and Chinese versions exist for product design, architecture, AI/LLM guide, and engineering log.

### 2026-07-24: Documentation Synchronization Rule

- Added the rule that Chinese and English documents must be updated together.
- Added the rule that each change must review and update all root Markdown documents affected by the change.
- Synchronized the rule across README, product design, technical architecture, AI guide, and engineering log.

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
