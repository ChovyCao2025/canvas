# P2-082 - Marketing Platform Gap Closure Spec

Priority: P2
Sequence: 082
Source: user capability audit, local code audit, and external product research
Implementation plan: `../plans/p2-082-marketing-platform-gap-closure-plan.md`

## Goal

Close the remaining gaps between the current Marketing Canvas product and a production-grade daily marketing stack after the core CDP, segmentation, omnichannel reach, BI, and journey canvas foundations.

## Current Baseline

The repository already has meaningful coverage for the five highest-frequency marketing capabilities:

- CDP/user profile, computed attributes, computed tags, realtime audiences, and audience materialization foundations.
- Tag and audience operations for segmentation.
- Omnichannel send execution across email, SMS, push, in-app, WeChat-style channels, plus consent, quiet-hour, frequency, fallback, and dedupe controls.
- BI workbench and analytics foundations.
- Journey canvas/marketing automation execution, wait nodes, and message templates.

The capabilities that remain partial or absent are not equal in urgency. The production sequencing should start with shared measurement and governance foundations, then move to lifecycle and activation surfaces.

## Research Inputs

- Braze Canvas controls show that journey products need operational send governance such as rate limits, quiet hours, frequency controls, and step-level observability: https://www.braze.com/docs/user_guide/engagement_tools/canvas/
- Optimizely Stats Engine documents the need for experiment statistics that handle ongoing monitoring and false discovery risk rather than static one-time reads: https://docs.developers.optimizely.com/experimentation/docs/stats-engine
- Salesforce Loyalty Management centers loyalty around accounts, ledgers, transaction journals, tier groups, and benefit rules rather than a bare points table: https://help.salesforce.com/s/articleView?id=sf.loyalty_mgmt.htm
- Google Ads Customer Match and Meta Custom Audiences both require provider-specific policy, hashing, consent, and eligibility controls for audience activation:
  - https://developers.google.com/google-ads/api/docs/remarketing/audience-segments/customer-match
  - https://developers.facebook.com/docs/marketing-api/audiences/guides/custom-audiences

## Implementation Status

Implemented and verified backend slices:

- P2-082A Attribution v2 and ROI foundation.
- P2-082B Experiment metrics and governance first slice.
- P2-082C Loyalty accounts, rules, and redemption first slice.
- P2-082D SCRM operator workspace backend and frontend first slice.
- P2-082D2 private-domain contact/group sync backend first slice.
- P2-082E paid-media audience sync backend first slice.
- P2-082F AI decision models backend first slice.
- P2-082G sentiment and competitor monitoring backend first slice.
- P2-082H monitoring workbench frontend first slice.
- P2-082I monitoring webhook ingestion backend first slice.
- P2-082J monitoring alert fanout backend first slice.
- P2-082K SCRM routing and SLA backend first slice.
- P2-082L SCRM AI reply assistance backend first slice.
- P2-082M monitoring polling and trends backend first slice.
- P2-082N monitoring scheduler and trend workbench slice.
- P2-082O creator/KOL/KOC collaboration foundation backend first slice.
- P2-082P SEO/SEM search marketing foundation backend first slice.
- P2-082Q DSP/programmatic advertising foundation backend first slice.
- P2-082R monitoring anomaly detection foundation backend first slice.
- P2-082S monitoring provider connectors backend first slice.
- P2-082T monitoring LLM sentiment inference governance backend first slice.
- P2-082U monitoring provider credential lifecycle backend first slice.
- P2-082V monitoring provider OAuth authorization backend first slice.
- P2-082W monitoring provider OAuth refresh and revocation backend first slice.
- P2-082X monitoring provider OAuth wizard UI frontend first slice.
- P2-082Y search marketing provider write gateway backend first slice.
- P2-082Z creator provider write gateway backend first slice.
- P2-082AA programmatic DSP provider write gateway backend first slice.
- P2-082AB provider write operations UI frontend first slice.
- P2-082AC provider write adapter contracts backend first slice.
- P2-082AD search marketing production closed-loop backend foundation in progress.
- Supporting SCRM adapter foundations for WhatsApp and Web Chat ingress.
- Supporting BI datasource schema-to-dataset creation for BI onboarding.
- Supporting enterprise OLAP evidence automation and query SLO proof gates.

Remaining product gaps:

- Provider-specific monitoring poll clients now start with X, YouTube, Google Business reviews, and TikTok Research, with tenant-scoped encrypted credential references, OAuth refresh lifecycle evidence, OAuth authorization-code onboarding, scheduled credential refresh, provider revocation, and a monitoring workbench OAuth wizard UI. Paid-search, creator campaign, and programmatic DSP provider writes now have dry-run-first mutation gateways, a unified provider write operations UI, credential-safe adapter contracts, dry-run delegation, and sandbox write clients. Search marketing closed-loop work has started with additive sync/reconciliation/impact schema, provider-read contracts, sandbox ingestion, and encrypted credential resolution. Broader credential-backed real provider adapters remain a future layer.

## Gap Tracks

### P2-082A Attribution v2 and ROI Foundation

The existing attribution service records one last-touch row per conversion. Production reporting needs configurable first-touch, last-touch, linear, and time-decay models with per-touch weights and touch timestamps. The first code slice must preserve current last-touch behavior while adding multi-touch rows only when the canvas model requires it.

Required behavior:

- Add `canvas.attribution_model` with default `LAST_TOUCH`.
- Add `canvas_conversion_attribution.attribution_weight` and `touch_created_at`.
- Replace the current unique key so retries are idempotent per canvas, conversion event, attribution model, and touch record.
- Support `FIRST_TOUCH`, `LAST_TOUCH`, `LINEAR`, and `TIME_DECAY`.
- Keep conversion amount extraction backward compatible with `conversionAmount` and `conversion_amount`.
- Keep `/canvas/{id}/attribution-summary` compatible while reporting weighted totals when weights exist.

### P2-082B Experiment Metrics and Governance

The existing A/B foundation needs experiment layers, mutually exclusive allocation, primary and guardrail metrics, sequential-safe reporting, sample-size guidance, and automatic audience/tag writeback for winners. The first production slice should avoid auto-stopping until metric lineage and guardrails are auditable.

Delivered first slice:

- Experiment layers, metrics, metric snapshots, allocation records, and governance decision persistence.
- Backend governance service and controller API for recording snapshots and evaluating governance decisions.
- Focused schema, service, group-service, and controller tests.

### P2-082C Loyalty Accounts, Rules, and Redemption

The current loyalty shape is too close to a points action ledger. Production loyalty needs member accounts, tier state, earn/burn/expire rules, transaction journals, idempotent redemptions, and benefit eligibility APIs.

Delivered first slice:

- Member accounts, loyalty rules, transaction journals, and redemption persistence.
- Backend service APIs for earning, redemption, idempotency, and account lookup.
- Backend controller API and focused schema/service/controller tests.

### P2-082D Conversation, SCRM, and Private-Domain Workspace

P2-080 creates durable conversation sessions and WAIT resume semantics. The next slices need a real operator inbox, customer timeline, WeCom/private-domain contact and group models, SOP tasks, follow-up reminders, assignment, and audit trails.

Delivered backend foundation:

- Adapter catalog and harness for provider-key based ingestion.
- WhatsApp and Web Chat reply adapters that normalize raw channel payloads into conversation ingress requests.
- Tenant-scoped SCRM contact profiles, inbox work items, assignment, status/reminder changes, SOP tasks, customer timeline, and immutable audit events.
- Optional inbound-message hook so new conversation replies update the workspace while duplicate replies do not.
- Frontend operator inbox and timeline UI under `/conversations`.
- Tenant-scoped private-domain contacts, owner relationships, customer groups, group members, sync-run ledger, and contact-profile projection for normalized WeCom/private-domain snapshots.
- Capacity-aware and skill-based work-item routing, SLA due-time assignment, SLA breach evaluation, and routing/SLA audit events.

Next slices:

- AI reply assistance is delivered as P2-082L.

### P2-082E Paid Media Audience Sync

DSP, Google/Meta-style custom audiences, KOL/KOC, SEO, and SEM should start as activation and governance modules around existing audiences. The first slice is a sandbox provider registry plus hashed audience export/sync audit. Real provider calls stay behind explicit connector configuration, consent checks, and provider eligibility gates.

Delivered backend first slice:

- Tenant-scoped paid-media destinations with provider, account, external audience id, identifier types, consent channel, and policy metadata.
- Sandbox-safe audience sync runs that validate destination, audience, tenant, consent, and CDP profile identifiers.
- SHA-256 hashed email/phone member audit rows without storing raw identifiers.
- Sync-run and member query APIs under `/canvas/paid-media/audience-sync`.

### P2-082F AI Decision Models

Delivered backend first slice:

- Tenant-scoped decision run ledger for governed AI recommendations.
- Deterministic baseline recommendations for LTV, next-best-action, next-best-offer, and channel affinity.
- Budget-aware offer eligibility, confidence, fallback reason, feature snapshot, and explanation JSON persistence.
- Recommendation feedback API for operator and downstream outcome capture.

### P2-082G Sentiment and Competitor Monitoring

Delivered backend first slice:

- Tenant-scoped monitoring source registry with idempotent source upsert.
- Connector-neutral monitored item ingestion with source provenance and raw payload audit JSON.
- Deterministic lexicon sentiment analysis with label, score, confidence, keyword evidence, model key, and model version.
- Caller-provided competitor term extraction with persisted matched terms and sentiment context.
- Negative sentiment and competitor-negative alert workflow rows with tenant-scoped reads and resolution API.

### P2-082H Monitoring Workbench Frontend

Delivered frontend first slice:

- Authenticated `/marketing-monitoring` operator workbench.
- Source upsert and manual mention ingestion surfaces.
- Mention review table with sentiment, competitor, and limit filters.
- Alert triage table with open-alert resolution action.
- Route and navigation integration under “运营值班”.
- Mobile and desktop app-shell width constraints for the workbench route.

### P2-082I Monitoring Webhook Ingestion

Delivered backend first slice:

- Source-scoped signed webhook secret rotation.
- Public generic monitoring webhook endpoint.
- Raw-body HMAC-SHA256 signature verification with timestamp replay guard.
- Generic social-listening payload normalization into existing monitored item ingestion.
- Anonymous security-filter access only for the signed public webhook route.

### P2-082J Monitoring Alert Fanout

Delivered backend slice:

- Tenant-scoped monitoring alert channels with encrypted destination secrets.
- Generic webhook, Slack, Feishu, and Teams payload formatting.
- Delivery logs, retry classification, and manual alert resend.
- Automatic fanout after alert creation without failing mention ingestion.

### P2-082M Monitoring Polling And Trends

Delivered backend first slice:

- Tenant-scoped monitoring source polling configuration with interval, cursor, next run, last run, and status state.
- Provider poll client contract plus sandbox metadata-backed client for credential-free environments.
- Poll run ledger with cursor before/after, requested window, item, duplicate, inserted, alert, status, error, and metadata evidence.
- Manual polling API that ingests new provider items through existing monitoring ingestion and skips duplicates.
- Trend snapshot build/query APIs for bucketed mention, sentiment, competitor, alert, and average sentiment metrics.

### P2-082N Monitoring Scheduler And Trend Workbench

Delivered backend and frontend slice:

- Due-source polling scheduler service and disabled-by-default scheduled wrapper.
- Optional distributed lease and single-JVM overlap guard for production scheduler safety.
- Frontend API coverage for polling/trend endpoints.
- Operator workbench trend snapshot panel with filters, bucketed metric table, and snapshot build action.

### P2-082O Creator Collaboration Foundation

Delivered backend first slice:

- Tenant-scoped KOL/KOC creator registry with provider identity, audience metrics, tags, risk status, and metadata.
- Campaign brief and collaboration ledger with offer terms, tracking links, discount codes, and permissions metadata.
- Deliverable evidence ledger with content lifecycle and performance metrics.
- Summary API for impressions, engagement, clicks, conversions, revenue, cost, commission, ROI, and overdue deliverables.

### P2-082P Search Marketing Foundation

Delivered backend first slice:

- Tenant-scoped SEO/SEM source registry for search-console, ads, and sandbox evidence.
- Keyword portfolio with normalized keyword identity, match type, landing page, intent, labels, and status.
- Daily performance snapshots for impressions, clicks, cost, conversions, revenue, CTR/CPC/conversion-rate/ROAS inputs, and average position.
- Deterministic opportunity ledger for low CTR, SEO page-two opportunity, and wasted paid-search spend.
- Summary API for channel/source/keyword/date performance aggregation.

### P2-082Q Programmatic DSP Foundation

Delivered backend first slice:

- Tenant-scoped DSP seat registry with provider/account, currency, timezone, and supply-chain enforcement metadata.
- Campaign and line-item ledgers with budgets, bid controls, pacing mode, targeting JSON, frequency caps, and status.
- Supply-path governance ledger for exchanges, deals, sellers, ads.txt/sellers.json/SupplyChain evidence, and activation status.
- Daily performance snapshots for bids, wins, impressions, clicks, conversions, spend, revenue, and viewability.
- Pacing summary API for win rate, CTR, CVR, CPA, ROAS, viewability rate, budget spend ratio, and pacing status.

### P2-082R Monitoring Anomaly Detection Foundation

Delivered backend first slice:

- Tenant-scoped anomaly rule registry for metric, source, brand, competitor, direction, baseline window, threshold, and metadata.
- Rolling-baseline detector that uses existing trend snapshots and robust statistical evidence.
- Anomaly event ledger for actual value, baseline median, MAD, robust z-score, delta, severity, status, and evidence JSON.
- Optional monitoring alert rows for detected anomalies so existing alert views can surface the signal.
- Tenant-scoped APIs for rule upsert, detection, event query, and resolution.

### P2-082S Monitoring Provider Connectors

Delivered backend first slice:

- Credential-ref based provider poll client support for X recent search, YouTube search, Google Business reviews, and TikTok Research video query.
- Provider-specific request builders for authorization, query windows, page cursors, and max-item bounds.
- Normalized poll item mapping into the existing monitoring ingestion contract.
- Sanitized provider response metadata that does not expose raw tokens or API keys.
- Deterministic tests with fake HTTP transport and fake credential resolver.

### P2-082T Monitoring LLM Sentiment Inference Governance

Delivered backend first slice:

- Tenant-scoped `marketing_monitor_inference` ledger for governed sentiment/entity/topic/risk inference over existing monitor items.
- Independent inference service that preserves deterministic ingest-time lexicon sentiment and writes a separate audit row.
- Input and prompt hashes, provider/template/model identifiers, provider status, fallback flag, latency, requested actor, and JSON output evidence.
- Default LLM generator through `AiLlmGateway` with built-in monitoring inference template ID 9.
- Deterministic local fallback analyzer for forced fallback, disabled/missing generators, and credential-free tests.
- Tenant-scoped APIs for single-item inference and bounded inference ledger query.

### P2-082U Monitoring Provider Credential Lifecycle

Delivered backend first slice:

- Tenant-scoped encrypted provider credential registry for monitoring connectors.
- API-key and OAuth bearer credential references usable by provider poll sources.
- OAuth refresh-token exchange with sanitized success/failure evidence.
- Credential lifecycle event ledger for create, rotate, refresh, refresh failure, and disable actions.
- Tenant-scoped operator APIs for upsert, list, refresh, disable, and event query.

### P2-082V Monitoring Provider OAuth Authorization

Delivered backend first slice:

- Tenant-scoped OAuth authorization state and event ledgers.
- PKCE S256 authorization URL generation with bounded state expiry.
- Authorization-code callback exchange through provider token endpoints.
- Successful token exchange persisted through encrypted provider credentials.
- Sanitized failure evidence for provider errors, expired state, duplicate callback, and token exchange failure.

### P2-082W Monitoring Provider OAuth Refresh And Revocation

Delivered backend first slice:

- Bounded due-refresh API and disabled-by-default scheduler for expiring OAuth provider credentials.
- Provider token revocation request path with sanitized lifecycle evidence.
- Local credential disablement after provider revoke success.
- Credential model fields for revoke endpoint, revoked timestamp, and last revoke status/error.

### P2-082X Monitoring Provider OAuth Wizard UI

Delivered frontend first slice:

- Monitoring workbench credential panel for sanitized provider credentials.
- OAuth authorization start/callback forms and authorization URL handling.
- Refresh, refresh-due, revoke, disable, and lifecycle evidence actions.

### P2-082Y Search Marketing Provider Write Gateway

Delivered backend first slice:

- Approved, idempotent SEM provider mutation ledger.
- Dry-run-first execution gate with provider request/response evidence.
- Fail-closed provider gateway for unsupported live writes.

### P2-082Z Creator Provider Write Gateway

Delivered backend first slice:

- Approved, idempotent creator provider mutation ledger.
- Dry-run-first execution gate with provider request/response evidence.
- Fail-closed provider gateway for unsupported live writes.

### P2-082AA Programmatic DSP Provider Write Gateway

Delivered backend first slice:

- Approved, idempotent DSP provider mutation ledger.
- Dry-run-first execution gate with provider request/response evidence.
- Fail-closed provider gateway for unsupported live writes.

### P2-082AB Provider Write Operations UI

Delivered frontend first slice:

- Unified SEM, creator, and DSP provider mutation queue.
- Operator actions for approval, dry-run, and apply.
- Provider-write status KPIs in the marketing platform page.

### P2-082AC Provider Write Adapter Contracts

Delivered backend first slice:

- Credential-safe provider write evidence sanitizer.
- Dry-run delegation for registered provider clients with local fallback.
- Sandbox SEM, creator, and DSP provider write clients for deterministic local live-apply verification.

### P2-082AD Search Marketing Production Closed Loop

In progress backend foundation:

- Additive sync-run, URL-inspection, provider-change, and impact-window schema.
- Provider-read gateway with fail-closed unsupported-provider behavior.
- Sandbox search provider read client for deterministic local ingestion evidence.
- Encrypted search-provider credential resolver with JSON/toString token redaction.

## Out Of Scope

- Real paid-media spend management or bid optimization.
- Real Google, Meta, DSP, WeCom, WhatsApp, or social credentials in local tests.
- Black-box autonomous campaign optimization without approval, audit, and rollback controls.
- Replacing the existing canvas execution model.

## Acceptance Criteria

- This spec and its plan are indexed after P2-081.
- P2-082A is implemented with failing tests first and preserves existing last-touch tests.
- P2-082B and P2-082C first backend slices are implemented with focused tests.
- Attribution data can represent one-row single-touch models and multi-row weighted models idempotently.
- Remaining gap tracks have explicit implementation order and production boundaries so later slices can be built without re-auditing scope.
- Focused backend tests for implemented slices pass with Java 21.
