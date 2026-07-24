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
- Workflow: definitions, tasks, history.
- AI Assistant: ratel assistant, Business Agent, local knowledge.
- System management: organization, permissions, logs, configuration.

Business Agent is exposed only in the AI Assistant page. Purchase, inventory, AR/AP, and accounting platform pages do not show separate `Agent Analysis` buttons.

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

File-existence questions must answer whether a file exists first, then list matching file names and key evidence. Follow-up questions must use recent conversation context.

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
- Login, save, delete, and approval failures must show explicit messages.
- Button labels should describe user actions, not technical implementation.
- AI output must not expose prompt, retrieval chunks, raw JSON, or debug fields.
- Loading states should use necessary text and button spinners only.

## 9. Non-Goals

- No direct AI posting.
- No enterprise group consolidation reports.
- No full BPMN designer.
- No automatic bank integration.
- No guarantee that offline packages include every historical model version.

## 10. Maintenance Rules

Update this document when pages, business statuses, Agent entries, Agent switches, safety boundaries, core workflows, or the user manual change.

All documentation changes must be bilingual. When product behavior changes, update both `Product-Design.md` and `产品设计.md`, then review all root Markdown documents so README files, architecture, AI guide, engineering log, and package documentation references remain consistent.
