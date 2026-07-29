# Ratel FM Product Design

[中文版本](产品设计.md)

Version date: 2026-07-23

This document describes product positioning, business scope, page behavior, and interaction principles. Technical architecture is documented in `Technical-Architecture.md`; AI implementation principles are documented in `LLM-Development-Guide.md`; engineering and packaging rules are documented in `Engineering-Log.md`.

## 1. Positioning

Ratel FM is designed for small companies, project teams, and finance-operation collaboration. Its goal is to provide a low-cost ERP and finance loop for local or LAN deployment.

Product focus:

- Business documents and accounting vouchers are recorded in one system.
- Purchase, logistics, inventory, AR/AP, cashier, and vouchers remain traceable.
- Workflow supports daily small-team approval without becoming a heavy BPM platform.
- AI provides query, explanation, checking, suggestions, and drafts; it does not directly modify business data.
- The default deployment is single-machine friendly, with optional PostgreSQL, Ollama, and Qdrant.

## 2. Product Principles

- Account-set isolation first: all pages, APIs, retrieval, and Agent results must respect company and account set.
- Traceable chains: source documents, accounting sources, vouchers, attachments, workflow, and logs must be linkable.
- Human confirmation first: AI and the accounting platform may create suggestions or drafts; users confirm final actions.
- Frontend and backend permissions must align: frontend controls visibility, backend enforces final checks.
- Clear errors: login, save, approval, and AI failures must produce understandable messages.
- Low operational burden: the main app, Ollama, and Qdrant are packaged and started independently.

## 3. User Roles

| Role | Main Work |
| --- | --- |
| System administrator | company, user, role, menu, and base configuration |
| Finance user | subjects, vouchers, AR/AP, payment, reports, closing |
| Business user | purchase, logistics, receipt, inventory, attachments |
| Approver | review documents, approve, reject, add opinions |
| Manager | dashboard, business analysis, risk summary, Agent suggestions |

## 4. Navigation And Entries

Navigation is organized by business domain:

- Home: dashboard, tasks, and risk summary.
- Master data: suppliers, customers, projects, materials, departments, staff, warehouses.
- Purchase: requests, purchase orders, and receipt-related information.
- Logistics: logistics orders and transport information.
- Inventory: inventory ledger, stock balance, transfers, and risks.
- AR/AP: receivables, payables, reconciliation, due risks.
- Cashier: payments, receipts, transactions.
- Accounting platform: source documents, voucher drafts, voucher generation.
- Accounting: vouchers, accounts, reports, periods.
- Voucher entry: uses an online-voucher WYSIWYG interaction. Summary is line-level instead of a whole-voucher input. Five lines are shown by default and users can add more lines. Blank lines may remain but are not saved; once any column in a line has content, that line must complete summary, subject, debit/credit amount direction, currency, and exchange-rate validation. At least one valid line is required.
- Workflow: definitions, tasks, history.
- AI Assistant: ratel assistant, Business Agent, local knowledge.
- System management: organization, permissions, logs, configuration.

Business Agent is exposed only in the AI Assistant page. Purchase, inventory, AR/AP, and accounting platform pages do not show separate `Agent Analysis` buttons.

The floating ratel assistant opens with a taller stable panel. While thinking, it hides both the question input and send button, shows only `思考中` with a thinking animation, and restores the input and send button after the answer is complete.

## 5. Core Business Loops

Purchase chain:

```text
purchase request -> purchase order -> receipt/stock-in -> payable -> payment -> voucher
```

Sales and collection chain:

```text
business source -> receivable -> collection -> reconciliation -> voucher
```

Inventory chain:

```text
opening/stock-in/stock-out/transfer -> inventory ledger -> stock balance -> risk check
```

Workflow chain:

```text
submit document -> match workflow -> approve node -> approve/reject -> update business status
```

## 6. AI Product Behavior

ratel assistant supports natural language QA, knowledge retrieval, business explanation, and entry guidance. Answers should contain conclusions and key evidence only. Long raw knowledge chunks, retrieval cards, and internal context are not shown.

Before sending, users must choose the intent for the current turn from a dropdown, with `思考` as the default. `思考` is for QA, retrieval, explanation, reasoning, and business analysis, and uses the deep reasoning model use case. `AI写作` is for report or file generation. AI writing should understand file type, business object, date range, dimensions, and metrics, and can generate xlsx, docx, pdf, and pptx files. When business data is involved, file content must come from deterministic backend queries under current account-set and permission scope, not model fabrication.

File-existence questions must answer whether a file exists first, then list matching file names and key evidence. Follow-up questions must use recent conversation context.
When conclusions or key evidence contain multiple items, the assistant must use numbered lines instead of hyphen bullets.
While thinking, the UI may show staged progress similar to mainstream LLM products, such as understanding the question, reading business context, checking metric rules, and preparing the conclusion. This is not internal chain-of-thought, and it must be removed after the final answer is returned. User-facing final answers must not expose chain-of-thought drafts, review notes, or think tags.
Professional finance questions such as business statistics, reporting, reconciliation, aging, cash flow, voucher suggestions, purchasing, inventory, AR/AP, and workflow should be routed through live system data and Business Agent capabilities before knowledge-file QA.
Business-analysis answers should rely on structured operating metrics before model summarization, so conclusions are based on consistent purchase, AR/AP, and inventory snapshots.

Business Agent supports structured business analysis:

- Query Agent: looks up documents, inventory, AR/AP, and vouchers.
- Reconciliation Agent: checks business-to-finance consistency.
- Voucher Suggestion Agent: creates voucher draft suggestions.
- Due Reminder Agent: detects due, overdue, and unreconciled risks.
- Workflow Assistant Agent: explains approval nodes and next handlers.
- Inventory Risk Agent: checks inventory anomalies.
- Business Analysis Agent: summarizes risks and suggestions.
- Attachment/Knowledge QA Agent: answers from attachments, contracts, policies, and system data.

Critical Agents must run self-checks covering permissions, account set, data source, amount direction, date range, evidence completeness, and risk wording.

## 7. Configuration Switches

| Configuration | Default | Behavior |
| --- | --- | --- |
| `FM_AI_ENABLED` | `true` | AI master switch |
| `FM_AI_AGENT_ENABLED` | `true` | Business Agent switch |
| `FM_AI_KNOWLEDGE_ENABLED` | `true` | local knowledge switch |
| `FM_AI_WEB_SEARCH_ENABLED` | `false` | web search switch |

When Business Agent is disabled:

- The frontend hides the Business Agent tab.
- ratel assistant does not call Business Agent.
- Backend APIs still enforce the switch.
- The product should behave as if the feature does not exist, instead of primarily showing "service unavailable".

## 8. Interaction Principles

- Pages should show business conclusions and executable actions first.
- Tables must support scrolling and must not hide bottom content.
- Tables inside dialogs should adapt to available width and must not show meaningless bottom horizontal scrollbars; field-heavy business details should prefer grouping, wrapping, or row editing to reduce horizontal width.
- Login, save, delete, and approval failures must show explicit messages.
- Button labels should describe user actions, not technical implementation.
- Colors, borders, shadows, radii, scrollbars, and focus states must use unified theme tokens for light, dark, emerald, finance-blue, and vue-gold themes.
- The current UI direction uses glassmorphism: container layers use translucent backgrounds, backdrop blur, soft borders, and shadows; inputs, table bodies, and financial figures must prioritize readability and avoid excessive transparency.
- Icons should prefer the existing open-source icon system and use unified glass icon containers for highlights, translucent backgrounds, and soft shadows; do not mix in external image icons with unclear licensing.
- Module tabs, filters, tables, and statistic cards must keep stable inner spacing; content must not sit directly against borders.
- UI refinement should use global theme tokens, unified tab containers, table padding, light shadows, and status colors instead of page-level hard-coded white panels and gray borders.
- AI output must not expose prompt, retrieval chunks, raw JSON, or debug fields.
- Loading states should use necessary text and button spinners only.
- AI component status should show deployment-facing model information clearly, including configured and installed Ollama models.
- ratel assistant opens with a taller stable panel; while thinking it hides the question input and send button, shows only `思考中` with a thinking animation, and returns to the `输入问题` state after the answer finishes.
- File-existence answers must name matched files in the conclusion, and follow-up summary requests must keep the previous document topic.

## 9. Non-Goals

- No direct AI posting.
- No enterprise group consolidation reports.
- No full BPMN designer.
- No automatic bank integration.
- No guarantee that offline packages include every historical model version.

## 10. Maintenance Rules

Update this document when pages, business statuses, Agent entries, Agent switches, safety boundaries, core workflows, or the user manual change.

All documentation changes must be bilingual. When product behavior changes, update both `Product-Design.md` and `产品设计.md`, then review all root Markdown documents so README files, architecture, AI guide, engineering log, and package documentation references remain consistent.
