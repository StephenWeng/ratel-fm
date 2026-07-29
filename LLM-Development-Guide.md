# Ratel FM AI And LLM Development Guide

[中文版本](大模型开发知识点整理.md)

Version date: 2026-07-23

This document explains the development principles for LLMs, RAG, local knowledge, intelligent search, OCR, and Business Agent in Ratel FM.

## 1. Core Boundary

Large models must not directly operate business data.

```text
user question + permission context + controlled data
  -> retrieve and assemble evidence
  -> model generates conclusion or draft
  -> user confirmation or business service validation
```

Any write, posting, payment, approval, deletion, or attachment change must go through existing business APIs.

## 2. Concepts

| Term | Meaning |
| --- | --- |
| LLM | model used for QA, reasoning, OCR explanation, and draft generation |
| Provider | model provider, such as Ollama |
| Router | selects a model by scenario |
| Prompt | system and user instructions sent to the model |
| RAG | retrieve evidence first, then answer |
| Embedding | text vector used for semantic search |
| Vector Store | Qdrant or H2 knowledge index |
| Chunk | sliced content from attachments, documents, or business summaries |
| Agent | controlled analyzer for a clear business task |

## 3. Model Routing

Default model scenarios:

| Scenario | Default Model | Purpose |
| --- | --- | --- |
| General QA | `qwen2.5:7b` | Chinese business QA and system explanation |
| Command recognition | `llama3.2:3b` | menu navigation and lightweight intent |
| Complex reasoning | `deepseek-r1:8b` | risk analysis and business suggestions |
| Vector retrieval | `bge-m3:latest` | embedding for knowledge and attachments |

Chat models may degrade to another available local model. Embedding models must not be changed casually because vector dimensions and semantic space must remain stable.

The AI component status page shows both configured Ollama models and the installed model list returned by Ollama `/api/tags`, so deployment issues can distinguish "configured but not downloaded" from "downloaded but not selected".

## 4. ratel Assistant

ratel assistant is responsible for:

- answering system capability questions;
- retrieving local knowledge and attachments;
- querying readable business information;
- identifying whether Business Agent should be used;
- answering file-existence questions at file level.
- opening the floating panel at a stable taller height; hiding the input and send button during retrieval and reasoning while showing only `思考中` with a thinking animation.

Before sending, users choose `思考` or `AI写作` from a dropdown, with `思考` as the default. `思考` uses the deep reasoning model use case for QA, retrieval, explanation, and business analysis. `AI写作` generates xlsx, docx, pdf, and pptx files from user intent. The model may help understand file type, business object, date range, dimensions, and metrics, but business data must come from deterministic backend queries with permission checks and account-set isolation. Business analysis reports must read a structured business metrics snapshot before composing report content.

Internal assistant components:

- `AssistantModelRouter`: selects chat, command, or reasoning model use cases.
- `AssistantPromptBuilder`: owns prompts and context compaction.
- `AssistantAnswerSanitizer`: removes internal reasoning, think tags, and dangerous inherited confirmation wording.

Output requirements:

- Show conclusions and key evidence only.
- Do not show retrieval cards, long raw chunks, internal JSON, prompts, or debug fields.
- State limitations when evidence is insufficient.
- Use recent context for follow-up questions.
- Hide the question input and send button during retrieval and reasoning; show only the `思考中` indicator with a thinking animation until the answer is complete.
- AI assistant and Business Agent UI must use global theme variables and support light, dark, emerald, finance-blue, and vue-gold themes.
- For file-existence questions, list matched file names in the conclusion. For follow-up summarization, preserve the previous document topic. Resume queries should expand to candidate, engineer, work experience, project experience, and location terms.
- For multi-item conclusions or key evidence, use numbered lines instead of hyphen bullets.
- The UI may show staged thinking progress, but must not expose chain-of-thought drafts, review notes, or think tags. Staged progress is removed after the final answer returns, and streaming and final answers must both be sanitized.
- Use the unified finance vocabulary for intent routing. Professional finance/statistics questions must prioritize live system context and reasoning; document retrieval is only primary for attachment, file, contract, policy, resume, and knowledge-base questions.

## 5. File-Existence QA

When users ask whether a file exists or whether a topic has been uploaded, answer with:

```text
Conclusion: yes/no/insufficient evidence.
Key evidence: matched file name, topic, and short snippet summary.
Next step: view, open, or search the file.
```

Do not answer only with content summaries, and do not ignore the topic from the previous turn.

## 6. Local Knowledge

Sources:

- policies, contracts, and documents uploaded by users;
- extracted attachment text;
- built-in system documents;
- business data summaries.

Pipeline:

1. Extract text.
2. Clean invalid content.
3. Split into chunks.
4. Generate embeddings.
5. Write to Qdrant or H2 knowledge index.
6. Filter by permission and account set at retrieval time.

## 7. Intelligent Search

Search modes:

- Keyword search: document numbers, codes, exact names.
- Semantic search: natural language topics.
- Hybrid search: keyword and semantic results together.

Search result pages must have scrollable areas so bottom content is not hidden.

## 8. OCR And Attachments

OCR extracts text and creates structured suggestions only. It does not create business documents directly. Attachment text must keep attachment source, business object, uploader, company, and account set before entering the knowledge base.

## 9. Business Agent

Business Agent is a controlled business analyzer, not free-form chat.

`BusinessAgentService` orchestrates execution. `BusinessAgentSelector` owns stage, module, and Agent-type selection. `BusinessMetricsService` builds structured operating metrics before business-analysis findings are generated.

Current Agent types:

- Query Agent.
- Reconciliation Agent.
- Voucher Suggestion Agent.
- Due Reminder Agent.
- Workflow Assistant Agent.
- Inventory Risk Agent.
- Business Analysis Agent.
- Attachment/Knowledge QA Agent.

Unified entry:

- `AI Assistant / Business Agent` tab.
- ratel assistant may guide or switch to Business Agent after recognizing reconciliation, due reminder, voucher suggestion, or inventory risk intent.
- Business module pages no longer show independent `Agent Analysis` buttons.

## 10. Agent Self-Check

Critical Agents must check:

- whether permission allows target data;
- whether company and account set match;
- whether date range is clear;
- whether amount direction and currency are consistent;
- whether source, inventory, AR/AP, payment, and voucher chains are complete;
- whether evidence is insufficient;
- whether suggestions may be misunderstood as automatic execution.

If self-check fails, return risk notes instead of deterministic conclusions.

## 11. Configuration Switches

| Configuration | Default | Description |
| --- | --- | --- |
| `FM_AI_ENABLED` | `true` | AI master switch |
| `FM_AI_AGENT_ENABLED` | `true` | Business Agent switch |
| `FM_AI_KNOWLEDGE_ENABLED` | `true` | local knowledge switch |
| `FM_AI_WEB_SEARCH_ENABLED` | `false` | web search switch |

When Agent is disabled, the product hides entries and stops routing flows to Agent. Backend APIs still enforce the boundary.

## 12. Common Mistakes

- Showing the full retrieved text to users.
- Summarizing file content while not answering whether the file exists.
- Ignoring the previous turn.
- Treating frontend hiding as backend security.
- Letting the model generate directly executable finance actions.
- Changing embedding models without rebuilding indexes.
- Continuing to call Agent after Agent is disabled.

## 13. Verification Checklist

- AI answers contain conclusions and key evidence only.
- File-existence questions return file names.
- Follow-up questions use previous topics.
- Agent entry is hidden when Agent is disabled.
- ratel assistant does not route to Agent when Agent is disabled.
- Backend still checks Agent switch and permissions.
- Qdrant failures have clear fallback or error behavior.
- AI, RAG, Agent, model, prompt, retrieval, or OCR changes update both English and Chinese AI documents and are reflected across all root Markdown documents.
- AI writing business analysis reports must have automated tests covering metric rules, permission boundaries, and generated-file key fields. Business Agent regression must cover business metrics, risks, self-checks, and low-data scenarios.
