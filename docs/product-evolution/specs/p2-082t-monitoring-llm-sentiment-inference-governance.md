# P2-082T - Monitoring LLM Sentiment Inference Governance Spec

Priority: P2
Sequence: 082T
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082t-monitoring-llm-sentiment-inference-governance-plan.md`

## Goal

Add a governed model-inference layer for already-ingested monitoring mentions so marketing operators can run LLM-style sentiment, entity, topic, and risk inference with tenant scoping, prompt/input hashes, fallback evidence, and auditable output without replacing the existing deterministic ingest-time sentiment path.

## Implementation Status

Status: Delivered backend first slice on 2026-06-06.

- Additive `marketing_monitor_inference` ledger for tenant-scoped item inference.
- Service API that validates monitor-item tenant ownership, builds input/prompt hashes, runs an injectable generator, and persists normalized output.
- Default LLM generator backed by `AiLlmGateway` and built-in monitoring inference template ID 9.
- Deterministic local fallback analyzer for credential-free operation and forced fallback runs.
- Controller APIs for single-item inference and bounded ledger query.
- Focused schema, service, generator, and controller tests.

## Current Baseline

P2-082G stores deterministic lexicon sentiment in `marketing_sentiment_analysis` during item ingestion. P2-082I/M/S can bring mentions from signed webhooks, sandbox polling, and first provider poll clients. P2-082R detects anomalies from trend snapshots. What remains missing is a production-style model inference ledger for richer sentiment signals, entity extraction, and governance evidence.

## Research Inputs

- OpenAI Structured Outputs recommends schema-constrained model output for reliable JSON contracts:
  - https://platform.openai.com/docs/guides/structured-outputs
- Amazon Comprehend separates sentiment analysis and entity detection as explicit NLP operations:
  - https://docs.aws.amazon.com/comprehend/latest/dg/how-sentiment.html
  - https://docs.aws.amazon.com/comprehend/latest/dg/how-entities.html
- Google Cloud Natural Language exposes sentiment and entity analysis as separate structured API outputs:
  - https://cloud.google.com/natural-language/docs/analyzing-sentiment
  - https://cloud.google.com/natural-language/docs/analyzing-entities
- Azure AI Language sentiment analysis supports opinion-mining style targets and assessments:
  - https://learn.microsoft.com/azure/ai-services/language-service/sentiment-opinion-mining/overview

## Product Design

The first slice adds a `marketing_monitor_inference` ledger and backend APIs:

- `POST /canvas/marketing-monitoring/items/{itemId}/inferences` runs inference for one existing monitor item.
- `GET /canvas/marketing-monitoring/inferences` lists tenant-scoped inference ledger rows by item, sentiment, model, status, and bounded limit.

Inference is intentionally separate from `marketing_sentiment_analysis`. The deterministic lexicon result remains the fast, stable ingest-time signal used by existing alerts and trend snapshots. The new ledger records richer model output and governance evidence:

- tenant, item, source, provider/template/model identifiers
- input hash and prompt hash
- sentiment label, sentiment score, confidence
- entities, topics, risk flags, and evidence JSON
- provider status, fallback flag, latency, actor, and timestamps

The default generator uses `AiLlmGateway` when a provider/template is configured. If the gateway is unavailable, disabled, times out, returns invalid output, or the caller requests fallback, the service persists a deterministic local fallback analysis so local development and tests remain credential-free.

## Functional Requirements

1. Inference must reject missing item ids and cross-tenant monitor items.
2. Inference must persist a new ledger row without modifying existing `marketing_sentiment_analysis`.
3. Ledger rows must include `input_hash` and `prompt_hash`; neither raw prompt text nor raw model output is required for the first slice.
4. Model output must normalize sentiment labels to `POSITIVE`, `NEGATIVE`, `NEUTRAL`, or `MIXED`.
5. Scores and confidence must be bounded to `[-1, 1]` and `[0, 1]` respectively.
6. Entities, topics, risk flags, and evidence must be persisted as JSON arrays/objects and exposed through typed views.
7. Provider failures and forced fallback must still create auditable rows with `fallback_used=true`.
8. Query APIs must be tenant-scoped, filterable, and bounded to 100 rows.
9. Tests must not require external model providers, network calls, or credentials.

## Out Of Scope

- Replacing lexicon sentiment during monitor item ingestion.
- Auto-triggering inference for every incoming item.
- Batch jobs, queues, and scheduler orchestration.
- Human review workflow and label correction UI.
- Provider credential lifecycle or OAuth.
- Prompt-template authoring UI.

## Acceptance Criteria

- P2-082T docs are indexed after P2-082S.
- Migration creates the governed inference ledger with tenant/item/status/model indexes and hash fields.
- Service tests prove tenant guard, fallback persistence, model-output parsing, hash generation, bounded query, and no mutation of lexicon sentiment.
- Controller tests prove tenant/actor propagation and bounded list parameters.
- Focused backend tests pass with Java 21.
