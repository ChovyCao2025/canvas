# P2-080 - Conversational Session Foundation Spec

Priority: P2
Sequence: 080
Source: `docs/optimization/todo/plans/2026-05-31-direction-23-conversational-marketing-social-channels-P0P1P2.md`
Implementation plan: `../plans/p2-080-conversational-session-foundation-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add the reusable conversation session foundation that lets inbound customer replies resume canvas execution through existing WAIT semantics before any single external conversational channel, such as WhatsApp or social DM, is fully integrated.

## Current Baseline

- Direction 23 defines an 11.2 person-month conversational marketing program across WhatsApp, social DM, web chat, conversational AI, and RCS.
- Existing canvas delivery is mostly one-way: a send handler emits a message and the graph continues or ends without waiting for a user reply.
- `WaitHandler`, `WaitSubscriptionService`, and `WaitResumeService` already support `WAIT UNTIL_EVENT`, event filters, completion, timeout, and idempotent resume.
- `UserInputService` already persists pending input and resumes execution from explicit user submissions.
- `ChannelConnector`, `MessageTemplate`, and Webhook subscription foundations already exist for provider abstraction, templates, and external callbacks.
- There is no durable `conversation_session` or `conversation_message` model, no common inbound reply API, and no common mapping from channel replies to WAIT resume events.

## Acceleration Strategy

Implement the conversation core before provider-specific channels. This pulls the highest-risk product behavior, "a user reply drives the canvas forward," into one small slice that can be tested locally with sandbox channel payloads. WhatsApp, WeCom callbacks, social DM, web chat, and AI reply generation then become adapters around the same ingress and session model instead of separate implementations.

Add a small adapter harness as the acceleration mechanism: each channel-specific callback maps its provider payload into `ConversationIngressReq`, the adapter catalog registers adapters by stable channel key and payload type, then the harness can convert raw internal payloads, validates the required routing fields, and delegates to the single ingress service. This keeps future provider work parallelizable and testable with tiny adapter tests while preserving one durable session, idempotency, and WAIT resume path.

Add a reusable provider-adapter contract matrix as the quality gate for that acceleration. Each future provider slice should contribute one raw payload fixture under `backend/canvas-engine/src/test/resources/conversation/adapter-contracts/`; the matrix discovers JSON fixtures from that directory directly, so no separate fixture index needs to be maintained. Each fixture proves raw conversion, normalized channel/provider/message type, text content, external and event ids, complete normalized attributes, and exact missing-payload rejection through the same support. The dynamic matrix cases are loaded from those discovered fixtures rather than Java hard-coded payload tables, and raw fixture conversion is direct raw-map-to-payload adapter execution so the fastest adapter-contract-only gate stays independent of ingress-service, WAIT, and workspace compilation. The matrix also discovers concrete provider adapter classes in the conversation package and fails when an adapter exists without a contract case, the fixture-specific gate fails when an adapter exists without a discovered JSON fixture, the fixture resource gate validates deterministic unique discovery, the retired-index gate rejects any reintroduced `index.json`, the metadata gate requires every fixture to declare the expected routing fields and `expectedAttributes.adapter`, the raw-payload gate requires each fixture to carry `userId`, `provider`, `externalMessageId`, and `eventId`, the runtime contract binds adapter output `userId` back to `rawPayload.userId`, the common-field gate binds optional `canvasId`, `versionId`, `executionId`, `intent`, and `occurredAt` raw fixture values back to the actual ingress request, every provider must have at least one discovered fixture covering all of those common session fields, Java time conversion is enabled for ISO `occurredAt` fixture values, the missing-payload gate requires the fixture's `missingPayloadMessage` to exactly match the adapter exception message instead of a broad substring, the expected-attributes gate requires every non-`adapter` expected attribute to bind back to the representative raw payload, the provider-specific raw-field gate requires top-level channel-only raw fields to become expected normalized attributes, the shared-base gate requires provider adapters to extend `AbstractProviderConversationReplyAdapter`, the normalized-key gate requires provider adapters to return uppercase trimmed key constants, the duplicate-key gate requires discovered provider adapter key declarations to be unique before registry indexing, the fixture-alias gate requires fixture adapter keys to use canonical lowercase snake aliases derived from expected channels, the name traceability gate requires adapter class and payload record names to match the adapter key, the stateless gate requires provider adapters to remain field-free public no-arg definitions, the Spring-component gate requires provider adapters to be component-scanned into the runtime catalog, the provider-specific component gate requires discovered raw fixtures to cover every non-common payload record component, and the constructor-only gates keep provider adapters from hand-writing `ConversationIngressReq` construction or any other custom methods. `ProviderConversationReplyPayload`, `AbstractProviderConversationReplyAdapter`, and `ConversationReplyAdapterSupport` centralize common provider payload fields, adapter metadata, payload typing, missing-payload rejection, provider normalization, text fallback, optional attribute assembly, interactive marker detection, and ingress request construction so future adapters declare channel-specific optional attributes, fallback text, and interactive markers through the shared base constructor only. This keeps the adapter surface fast to extend while preventing WhatsApp, social DM, web chat, and RCS from drifting into subtly different ingress contracts.

Use `scripts/verify-conversation-focus.sh` as the focused regression gate for this PRD slice. It syncs main resources directly through a reviewed top-level allowlist, syncs reviewed test-resource roots without deleting warmed compiled classes, reuses a fresh generated backend test classpath when the classpath file is newer than the backend POM inputs and every dependency entry still exists, refreshes that classpath through Maven on any stale or missing evidence, preflights listed backend sources, clears stale project main classes, writes a focused javac source argfile, and compiles conversation-focused backend sources with explicit Lombok processor support when the Lombok target-class evidence is stale. The full backend gate stores that evidence under `${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/`, invalidates it on backend classpath/POM drift, relevant conversation watched-source drift, direct DAL import drift from those watched sources, or relevant source timestamp changes, and on cache hit compiles only the provider/test source set before deriving and running the backend Surefire selector with no-test failure protection. Stale Lombok refreshes still discover and compile every Lombok main source so implicit javac dependencies keep generated getters, builders, and log fields, but unrelated repository-wide Lombok source-list changes no longer invalidate the conversation cache by themselves, and cache hits defer that full Lombok source discovery entirely. It keeps fixed non-conversation dependencies for security routes, demo sandbox, WAIT, channel connector, outbox, and delivery dispatch, then automatically discovers `domain/conversation/*Test.java` and `web/*Conversation*Test.java` so future conversation domain, provider, workspace, private-domain, routing, AI-reply, and controller tests are picked up without selector edits while avoiding full Maven main compilation and Maven resource phases. For backend schema-only iteration, `--backend-schema-only` discovers and compiles only `domain/conversation/*SchemaTest.java` with `-proc:none`, syncs backend main resources so migration SQL reads are current, and runs only the conversation schema/migration assertions while leaving API, domain service, adapter, runtime, frontend, and full integration coverage to `--backend-only` or the full focused gate. For backend domain-only iteration, `--backend-domain-only` compiles the Lombok-free `domain/conversation` main and test source set with `-proc:none`, refreshes test resources for adapter fixtures, and runs the domain/conversation suite while deliberately leaving web, security, WAIT, outbox, delivery, channel, and frontend coverage to `--backend-only` or the full focused gate. For backend API/controller iteration, `--backend-api-only` compiles the Lombok-free conversation domain, conversation/demo controllers, demo service, and security route sources with `-proc:none`, discovers conversation web tests, and runs only security route, conversation controller, provider/public webhook, private-domain, workspace, and demo sandbox controller tests while leaving WAIT, outbox, delivery, channel, adapter, frontend, and full integration coverage to `--backend-only` or the full focused gate. For backend runtime iteration, `--backend-runtime-only` compiles the WAIT, delivery outbox, reach dispatch, channel connector, and runtime helper source set with Lombok processing enabled, then runs only the WAIT/outbox/delivery/channel runtime tests while leaving API, domain, adapter, frontend, and full integration coverage to `--backend-only` or the full focused gate. The frontend gate keeps fixed cross-cutting authoring tests for node palette and insert-node coverage, then automatically discovers `services/conversation*.test.ts(x)`, `services/demoSandbox*.test.ts(x)`, `pages/conversations/*.test.ts(x)`, and `pages/demo-sandbox/*.test.ts(x)` so future conversation UI and demo sandbox UI tests are picked up without script edits; `--frontend-logic-only` exposes the conversation/demo API and presentation-helper subset for fastest frontend logic iteration, and `--frontend-conversation-only` adds TSX conversation/demo page smoke tests while still skipping authoring coverage until `--frontend-only` or the full focused gate. For provider fixture-only work, `scripts/verify-conversation-focus.sh --adapter-fixture-lint-only` parses discovered fixture JSON with Node standard library only and checks syntax, required metadata, canonical aliases, normalized expected fields, routing keys, expected text, expected routing, expected attributes, and provider-specific raw-field coverage before Java starts; backend verifier paths also run that fixture lint before Maven/resource/classpath/compile/Surefire work so obvious JSON and metadata drift fails at the front of `--adapter-contract-only` and the full focused gate; `--adapter-source-compile-only` runs the scaffold and fixture guards, cached classpath resolution, stale provider class cleanup, and focused provider/contract `javac` without resource sync or Surefire for provider Java compile-error iteration; the adapter-only focused `javac` paths use `-proc:none` because the provider/contract source set is Lombok-free, while the full focused gate keeps explicit Lombok processing for broader cache refreshes; `--adapter-fixture-contract-only` can then run the adapter matrix without focused `javac` only when focused provider/test classes are already fresh; `--adapter-contract-only` remains the source-refreshing behavior gate and compiles only the dynamically discovered provider adapter/payload main source set plus the adapter contract matrix, with the matrix de-duplicating provider adapter classes discovered from multiple classpath roots. For scaffold script-only edits, `scripts/verify-conversation-focus.sh --scaffold-preflight-only` runs just the scaffold syntax, dry-run output, repo-relative path, next-step command, and invalid-input checks before Java, Maven, Node, backend resource, or frontend setup; it is not a substitute for the provider adapter contract or full focused gates. For scaffold template changes, `scripts/verify-conversation-focus.sh --scaffold-contract-probe` generates a disposable provider, proves it through adapter-contract-only, and removes the probe source, fixtures, and target output. For actual provider bootstrap, `scripts/scaffold-conversation-provider.sh --verify` can generate the provider files and immediately run the adapter-contract-only gate, while `--dry-run --verify` remains no-write and prints the command it would execute. Before focused `javac`, the verifier refreshes adapter fixture resources so deleted or renamed fixtures cannot survive as stale discovered fixtures, clears stale provider adapter/payload classes from both main and test compiled outputs before focused compilation, and preflights the provider scaffold script with `bash -n` plus no-write dry-runs for both normal next-step output and `--verify` output. The matrix rejects any `index.json` on the test classpath so old fixture-index maintenance cannot return. This gives future provider slices a single fast command while still catching broken runtime prerequisites, missing tests, scaffold regressions, and conversation source regressions without being blocked by unrelated dirty worktree modules, a stale script source list, stale target resources, stale provider classes, duplicated selector maintenance, or fixture index maintenance.

For backend source compile preflight, `--backend-source-compile-only` runs scaffold preflight, fixture lint, classpath resolution, focused source discovery, and `javac` while skipping Surefire, backend resource sync, and broad main-class cleanup. It can run as the full conversation-focused backend compile preflight or combine with backend drill-down flags such as `--backend-domain-only`, `--backend-runtime-only`, `--backend-conversation-webhooks-only`, and `--backend-whatsapp-only`. The full source preflight compiles the conversation-relevant watched main sources and focused tests with the Lombok processor and `-implicit:none`, while Lombok-free slices keep `-proc:none -implicit:none`; this catches source wiring errors quickly without refreshing the full Lombok cache or implicitly compiling unrelated dirty sourcepath classes. It is not a behavior gate, so service, adapter, API, runtime, backend-only, or full focused tests still remain required before integration.

For backend conversation service-only iteration, `--backend-conversation-services-only` dynamically discovers `domain/conversation/*ServiceTest.java`, compiles the Lombok-free conversation domain main source set plus those service tests with `-proc:none`, and skips backend main-resource sync, test-resource sync, broad class cleanup, provider cleanup, schema tests, adapter/support tests, API/controller tests, runtime tests, frontend tests, and full integration coverage. `--backend-domain-only`, `--backend-only`, and the full focused gate remain the broader quality gates for service changes before integration.

For backend conversation adapter-only iteration, `--backend-conversation-adapters-only` compiles the Lombok-free conversation domain source set with `-proc:none`, syncs adapter fixture test resources, and runs adapter harness/catalog/support/base tests, discovered provider adapter unit tests, and the adapter contract matrix. It skips backend main-resource sync, broad class cleanup, service tests, API/controller/webhook tests, runtime tests, frontend tests, and full integration coverage; `--adapter-contract-only`, `--backend-domain-only`, `--backend-only`, and the full focused gate remain the narrower fixture matrix and broader domain/integration gates.

For adapter matrix iteration, `--adapter-contract-only` preserves warmed backend main target classes by skipping backend main-resource sync and broad main-class cleanup while still syncing adapter fixture test resources, clearing stale provider adapter/payload classes, compiling the focused provider/contract source set with `-proc:none`, and running the matrix. This prevents the matrix gate from forcing later no-processor drill-down gates to recompile Lombok-backed DAL or runtime dependencies without generated members.

For backend conversation controller-only iteration, `--backend-conversation-controllers-only` dynamically discovers `web/*Conversation*Test.java`, includes `DemoSandboxControllerTest`, compiles the Lombok-free conversation domain, conversation/demo controller, and demo sandbox service source set with `-proc:none`, and skips backend main-resource sync, test-resource sync, broad class cleanup, `SecurityConfigRouteTest`, runtime tests, frontend tests, and full integration coverage. `--backend-api-only`, `--backend-only`, and the full focused gate remain the broader quality gates for security route and integration coverage before release.

For backend conversation webhook-only iteration, `--backend-conversation-webhooks-only` compiles the Lombok-free conversation domain plus `ConversationProviderWebhookController` and `PublicConversationWebhookController` with `-proc:none`, then runs only the WhatsApp payload mapper, webhook security service, internal provider webhook controller, and public webhook controller tests. It skips backend main-resource sync, test-resource sync, broad class cleanup, security route tests, non-webhook controller tests, runtime tests, frontend tests, and full integration coverage; `--backend-api-only`, `--backend-domain-only`, `--backend-only`, and the full focused gate remain the broader quality gates before integration.

For backend WhatsApp slice iteration, `--backend-whatsapp-only` compiles the Lombok-free conversation domain, WhatsApp provider/public webhook controllers, and WhatsApp Cloud API connector source set with `-proc:none`, then runs only WhatsApp adapter, payload mapper, webhook security, provider/public webhook controller, and Cloud API connector tests. It skips non-WhatsApp provider adapters, non-WhatsApp domain services, security route tests, non-webhook controllers, WAIT/outbox/reach runtime tests, frontend tests, and full integration coverage; adapter-only, webhook-only, runtime-only, backend-only, and the full focused gate remain the broader quality gates before integration.

For frontend authoring-only iteration, `--frontend-authoring-only` runs only the node palette and insert-node authoring tests that cover the conversation WAIT authoring preset. It skips backend verification and conversation/demo API, presentation, and page tests; `--frontend-only` and the full focused gate remain the broader frontend quality gates before integration.

## In Scope

- Add tenant-scoped conversation session and message persistence.
- Add a small domain service for inbound replies that:
  - finds or creates the active conversation session,
  - writes inbound messages idempotently,
  - updates turn count, status, context, and last message time,
  - creates a normalized reply event payload for WAIT resume.
- Add an authenticated operator/internal ingress API for sandbox and future channel callbacks.
- Route inbound replies through `WaitResumeService.resumeEventWaits` with event code `CONVERSATION_REPLY`.
- Keep event attributes structured so existing WAIT event filters can branch by `channel`, `messageType`, `text`, `intent`, `externalMessageId`, and `sessionId`.
- Add read-only recent session and message APIs for operator verification.
- Add frontend service and presentation helpers for conversation session inspection.
- Add a frontend canvas authoring preset that exposes "wait for conversation reply" as a product-facing palette item while still creating the governed `WAIT` node type.
- Add focused schema, domain service, controller, WAIT integration, and frontend tests.

## Out Of Scope

- Full WhatsApp Business API lifecycle management, template approval synchronization, tenant-specific provider credential storage, connector-bound callback registration, replay-window controls, and Meta app lifecycle management. A local WhatsApp reply adapter skeleton, authenticated internal Cloud API payload bridge, minimal public verification/signature shell, and minimal `WHATSAPP:CLOUD_API` outbound connector bridge are allowed as acceleration templates.
- Real social DM, web chat widget, or RCS provider integration. Local social DM, web chat, and RCS reply adapter skeletons are allowed as ingress mapping templates only.
- AI-generated replies, LLM prompt orchestration, sentiment analysis, or knowledge base retrieval.
- Public anonymous chat hosting.
- Full inbox UI or customer support workspace.
- Human handoff workflow beyond recording `TRANSFERRED` as a future-compatible session status.

## Runtime Semantics

1. A canvas sends a message through existing send/template/channel connector paths.
2. The graph can then enter `WAIT` with `waitType=UNTIL_EVENT`, `eventCode=CONVERSATION_REPLY`, and optional event filters.
3. A channel adapter, sandbox tool, or operator posts an inbound reply to the conversation ingress API.
4. The ingress service computes an idempotency key from tenant, channel, provider, external message id or event id, user id, and payload direction.
5. If the inbound message already exists, the service returns the existing normalized result and does not resume the graph again.
6. If the message is new, the service persists it, updates the active session, and calls `WaitResumeService.resumeEventWaits("CONVERSATION_REPLY", userId, attributes, eventId)`.
7. `WaitResumeService` applies existing WAIT filters and CAS completion; only matching active waits resume.
8. Timeout behavior stays in the existing WAIT timeout scan. The conversation service does not own wait expiry.

## Data Model

`conversation_session` stores one active conversation thread per tenant, user, channel, provider, and optional canvas execution context.

Required fields:

- `id`
- `tenant_id`
- `canvas_id`
- `version_id`
- `execution_id`
- `user_id`
- `channel`
- `provider`
- `status`: `ACTIVE`, `COMPLETED`, `EXPIRED`, `TRANSFERRED`
- `turn_count`
- `context_json`
- `last_message_at`
- `expires_at`
- `created_at`
- `updated_at`

`conversation_message` stores individual inbound or outbound messages.

Required fields:

- `id`
- `tenant_id`
- `session_id`
- `direction`: `INBOUND`, `OUTBOUND`
- `message_type`: `TEXT`, `IMAGE`, `INTERACTIVE`, `UNKNOWN`
- `external_message_id`
- `idempotency_key`
- `content_json`
- `text_content`
- `intent`
- `processed`
- `created_at`

Indexes must support tenant-scoped active session lookup, recent session listing, session message lookup, and idempotent message insertion.

## API Contract

### Inbound Reply

`POST /canvas/conversations/ingress`

Request body:

```json
{
  "canvasId": 1001,
  "versionId": 2001,
  "executionId": "exec-1",
  "userId": "user-1",
  "channel": "SANDBOX",
  "provider": "DEFAULT",
  "externalMessageId": "reply-1",
  "eventId": "event-1",
  "messageType": "TEXT",
  "text": "I want product A",
  "intent": "PRODUCT_A",
  "attributes": {
    "buttonId": "product-a"
  },
  "occurredAt": "2026-06-06T00:00:00"
}
```

Response body:

```json
{
  "sessionId": 10,
  "messageId": 20,
  "status": "RECORDED",
  "duplicate": false,
  "resumedWaitCount": 1
}
```

Duplicate requests return the existing `sessionId` and `messageId`, `duplicate=true`, and `resumedWaitCount=0`.

### Adapter Inbound Reply

`POST /canvas/conversations/adapters/{adapterKey}/ingress`

This authenticated internal/operator endpoint accepts the adapter-specific payload body, converts it using the adapter's declared `payloadType()`, and routes it through `ConversationAdapterHarness`. It is not a public provider webhook endpoint and does not replace provider-specific signature verification.

Example sandbox body:

```json
{
  "canvasId": 1001,
  "versionId": 2001,
  "executionId": "exec-1",
  "userId": "user-1",
  "externalMessageId": "reply-1",
  "eventId": "event-1",
  "text": "I want product A",
  "intent": "PRODUCT_A",
  "attributes": {
    "buttonId": "product-a"
  }
}
```

### Internal WhatsApp Provider Webhook Bridge

`POST /canvas/conversations/provider-webhooks/whatsapp`

This authenticated internal/operator endpoint accepts a WhatsApp Cloud API webhook-like payload, extracts inbound `messages`, maps text and interactive replies into `WhatsAppConversationReplyPayload`, and delegates each mapped message to `ConversationAdapterHarness` with adapter key `WHATSAPP`.

It is a bridge for local/operator acceleration only. It is not a public Meta webhook receiver, does not implement verification-token challenge handling, and does not perform provider signature validation. A production provider endpoint must add those controls before being exposed externally.

Status-only webhook payloads with no inbound messages return an empty response list and do not invoke the harness.

### Public WhatsApp Webhook Security Shell

`GET /public/conversation-webhooks/{tenantId}/whatsapp`

Accepts Meta-style `hub.mode`, `hub.verify_token`, and `hub.challenge` query parameters. If `hub.mode=subscribe` and the supplied verify token matches `canvas.conversation.whatsapp.webhook.verify-token`, the response body is the raw challenge string.

`POST /public/conversation-webhooks/{tenantId}/whatsapp`

Accepts the raw webhook JSON body and requires `X-Hub-Signature-256`. The signature is verified as `sha256=` plus HMAC-SHA256 of the exact raw body using `canvas.conversation.whatsapp.webhook.app-secret`. Only after signature verification does the controller parse JSON, map inbound messages through `WhatsAppWebhookPayloadMapper`, and delegate to `ConversationAdapterHarness` with adapter key `WHATSAPP` and actor `whatsapp-webhook`.

For backward compatibility with the initial bridge naming, `/public/conversations/webhooks/{tenantId}/whatsapp` is also routed to the same controller. Both public paths are anonymous at the security-filter layer and rely on controller-level challenge/signature validation.

When the signed Cloud API payload contains `statuses[]`, `WhatsAppWebhookPayloadMapper` converts each status into a `DeliveryReceiptRequest` with provider `CLOUD_API`, the WhatsApp message id as `providerMessageId`, uppercase receipt type, UTC event time from the provider timestamp, and an idempotency key of `whatsapp:{entryId}:{messageId}:{receiptType}:{timestamp}`. If `DeliveryOutboxService` is available, the public webhook controller records those receipts before message ingress. Status-only webhook payloads return an empty conversation response list and do not invoke `ConversationAdapterHarness`.

### WhatsApp Cloud API Outbound Connector Bridge

`ChannelConnectorRegistry` can resolve the connector key `WHATSAPP:CLOUD_API` through `WhatsAppCloudApiConnector`.

The connector is a minimal send-side bridge for future REAL connector routing:

- It requires `canvas.conversation.whatsapp.cloud.phone-number-id`, `canvas.conversation.whatsapp.cloud.access-token`, and `canvas.conversation.whatsapp.cloud.graph-api-version`.
- It defaults `canvas.conversation.whatsapp.cloud.graph-base-url` to `https://graph.facebook.com`, `canvas.conversation.whatsapp.cloud.default-language` to `en_US`, and `canvas.conversation.whatsapp.cloud.timeout-ms` to `10000`.
- If `templateId` is present, it sends a WhatsApp template payload with `messaging_product=whatsapp`, normalized recipient, template name, language code, and body parameters built from the payload `variables` map in caller-provided iteration order.
- If `templateId` is absent, it sends a session text message using `content.body` or `content.content`.
- It fails closed with connector status `DISABLED` when the connector has no client, phone number id, or access token.
- It extracts the first `messages[0].id` from the Graph response as `externalMessageId`.

`AbstractSendMessageHandler` still preserves the existing delivery/outbox boundary: REAL mode builds the same `ReachDeliveryService.DeliveryRequest` and enqueues or records through `ReachDeliveryService`. Provider dispatch is now connector-aware at `ReachDeliveryService.dispatchToProvider(DeliveryOutboxDO)`: when `ChannelConnectorRegistry` resolves an enabled connector for the outbox tenant/channel/provider, the dispatcher calls `connector.send(...)` with the original outbox payload and returns the connector message id as `messageId` for the existing outbox consumer `markSent` path. If no enabled connector can be resolved, the legacy reach-platform HTTP dispatch remains as a compatibility fallback.

### Recent Sessions

`GET /canvas/conversations?userId=user-1&channel=SANDBOX&limit=20`

Returns recent tenant-scoped session summaries.

### Session Messages

`GET /canvas/conversations/{sessionId}/messages?limit=50`

Returns tenant-scoped messages for the session.

## WAIT Event Attributes

Inbound replies publish these attributes to `WaitResumeService`:

```json
{
  "conversationSessionId": 10,
  "conversationMessageId": 20,
  "channel": "SANDBOX",
  "provider": "DEFAULT",
  "messageType": "TEXT",
  "text": "I want product A",
  "intent": "PRODUCT_A",
  "externalMessageId": "reply-1",
  "attributes": {
    "buttonId": "product-a"
  }
}
```

Operators can configure `WAIT UNTIL_EVENT` filters such as:

```json
{
  "channel": "SANDBOX",
  "intent": "PRODUCT_A"
}
```

## Canvas Authoring Preset

The editor node palette includes a conversation reply wait preset named `等待会话回复`. The preset is frontend-only and deliberately reuses the governed backend node type `WAIT` instead of adding a new node handler. Dropping the preset creates a normal `WAIT` node with default `bizConfig`:

```json
{
  "waitType": "UNTIL_EVENT",
  "eventCode": "CONVERSATION_REPLY"
}
```

Operators can then add channel, intent, or message-type filters in the existing WAIT configuration surface. This keeps runtime behavior on the tested `WaitHandler`/`WaitResumeService` path while making conversational journeys easier to author.

## Functional Requirements

1. Inbound replies must be persisted idempotently per tenant and idempotency key.
2. Active sessions must be tenant scoped and isolated by user, channel, provider, and execution context when provided.
3. Inbound replies must update session `turn_count`, `last_message_at`, and `context_json`.
4. Inbound replies must resume only matching active WAIT subscriptions via the existing `CONVERSATION_REPLY` event path.
5. Duplicate inbound replies must not trigger a second WAIT resume.
6. Recent session and message queries must be tenant scoped.
7. The feature must work without real WhatsApp or social DM credentials.
8. Existing WAIT, UserInput, ChannelConnector, MessageTemplate, and Webhook tests must continue to pass.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V270__conversation_session_foundation.sql`.
- Add DOs and mappers for `conversation_session` and `conversation_message`.
- Add `ConversationIngressService`, request/response records, and read models.
- Add `ConversationReplyAdapter`, `ConversationAdapterCatalog`, `ConversationAdapterHarness`, the internal adapter ingress endpoint, the sandbox adapter, plus constructor-only WhatsApp, social DM, web chat, and RCS adapter skeletons so future provider callbacks reuse the same ingress path with stable adapter keys, payload type conversion, plus a common non-null `userId` and `channel` routing contract.
- Add `WhatsAppWebhookPayloadMapper` and an authenticated internal WhatsApp provider webhook bridge that parses Cloud API text and interactive inbound messages into the existing WhatsApp adapter payload.
- Add `WhatsAppWebhookSecurityService` and `PublicConversationWebhookController` for minimal public WhatsApp challenge/signature verification before routing signed payloads into the existing WhatsApp mapper and adapter harness.
- Add `WhatsAppCloudApiClient`, `HttpWhatsAppCloudApiClient`, and `WhatsAppCloudApiConnector` as a minimal outbound connector bridge registered under `WHATSAPP:CLOUD_API`.
- Make `ReachDeliveryService.dispatchToProvider(DeliveryOutboxDO)` connector-aware so REAL connector dispatch happens from the delivery outbox worker boundary, preserving message-send records, outbox retry/dead handling, and receipt reconciliation semantics.
- Add `ConversationController`.
- Add frontend `conversationApi` and presentation helpers for recent sessions/messages.
- Add a frontend `ingestConversationAdapterReply(adapterKey, payload)` service helper for internal/operator adapter payload tests without duplicating URL construction in future channel slices.
- Add backend schema, service, controller, and WAIT integration tests.
- Add frontend service and presentation tests.
- Add `scripts/scaffold-conversation-provider.sh` as the provider-slice starter so future channel adapters can generate constructor-only adapter classes, payload records, and discovered TEXT/INTERACTIVE contract fixtures without recreating the matrix-required naming and metadata shape by hand. The scaffold must reject provider-specific fields and adapter keys that would generate invalid Java, non-lowerCamel provider attributes, duplicate common payload components, collapsed class names, or unstable fixture aliases, while still supporting providers that need no extra text attributes beyond the default interactive marker fields, printing repo-relative generated file paths, printing the fixture-lint and adapter-contract verifier commands as the next steps, and optionally running the adapter-contract verifier after generation with `--verify`.
- Extend `scripts/verify-conversation-focus.sh` to clear stale provider adapter/payload class files from both `target/classes` and `target/test-classes` before focused backend compilation, avoid full Maven main compilation and Maven resource phases in the full focused gate, sync backend resources directly with reviewed main/test resource roots, compile focused backend sources through a javac argfile with explicit Lombok processor support, reuse valid generated backend test classpath and Lombok target-class evidence while falling back to refresh work when POM inputs, dependency entries, Lombok sources, or focused watched main sources make them stale, and preflight the provider scaffold script before backend verification, preventing deleted or renamed provider scaffold experiments from lingering in matrix discovery while keeping the scaffold itself covered by the focused gate. The scaffold preflight must assert representative output, `--verify` dry-run output, and invalid-input invariants, not just shell syntax; `--scaffold-preflight-only` must expose that scaffold check as a standalone script-edit gate before Java, Maven, Node, backend resource, or frontend setup; `--scaffold-contract-probe` must prove the scaffold template by generating a disposable provider, running adapter-contract-only, and cleaning probe artifacts; and `--adapter-fixture-contract-only` must skip focused javac only after proving focused class freshness and otherwise fail fast to `--adapter-contract-only`.
- Update product-evolution indexes.

## Acceptance Criteria

- P2-080 spec and plan are indexed.
- Schema test proves the two conversation tables, unique idempotency constraint, and tenant lookup indexes exist.
- Service tests prove first inbound reply creation, duplicate suppression, active session update, tenant isolation, and recent/message query behavior.
- WAIT integration test proves a `CONVERSATION_REPLY` inbound reply resumes exactly one matching wait and does not resume non-matching waits.
- Controller tests prove ingress, recent session, and message endpoints are tenant scoped.
- Frontend tests prove request payload normalization, duplicate status formatting, and message/session presentation helpers.
- Adapter catalog and harness tests prove adapter key normalization, duplicate key rejection, sandbox payload normalization, raw payload conversion, key-based harness delegation to `ConversationIngressService`, and rejection of adapter outputs that omit the minimum routing contract (`userId`, `channel`, `provider`, `externalMessageId`, and `eventId`).
- A reusable adapter contract test helper proves new adapter slices can verify raw payload conversion and missing-payload rejection without repeating mock harness setup.
- The adapter contract matrix includes discovered JSON fixture-backed cases for every provider adapter so future provider slices can add representative raw payloads without expanding Java-only matrix setup or editing a fixture index.
- The adapter contract matrix covers both `TEXT` and `INTERACTIVE` fixture cases for every provider adapter.
- Adapter contract fixture `missingPayloadMessage` values exactly match the adapter exception message, preventing broad substring expectations from hiding drift in provider diagnostics.
- Adapter contract fixtures' raw payload shape matches the expected message type, so `TEXT` fixtures carry nonblank text input and `INTERACTIVE` fixtures carry a concrete reply/action marker.
- Adapter contract fixtures use only PRD-supported message types (`TEXT`, `IMAGE`, `INTERACTIVE`, `UNKNOWN`) so provider slices cannot introduce channel-local message-type vocabulary.
- Adapter contract fixture expected routing fields bind to the raw payload and adapter key, so channel/provider/external-message/event expectations cannot drift from representative input.
- Adapter contract fixtures declare expected normalized text content, and the matrix proves that content binds to the raw text or interactive display field.
- Adapter contract fixtures declare the complete expected normalized attributes map; dynamic ingress cases reject undeclared extra attributes as well as missing attributes.
- Adapter contract fixtures bind every non-`adapter` expected attribute to a same-named top-level raw payload field or `rawPayload.attributes` entry, preventing fixture expectations from masking hardcoded adapter attributes.
- Adapter contract fixtures require every provider-specific top-level raw payload field to appear in `expectedAttributes`, preventing channel-only fields from being parsed but dropped from normalized ingress attributes.
- Adapter contract fixture filenames start with the fixture adapter key in kebab-case, keeping fixture files traceable to provider adapters without opening the JSON.
- Provider adapters share `ProviderConversationReplyPayload` and `ConversationReplyAdapterSupport` for common payload fields, provider normalization, text fallback, optional text attributes, adapter attribute assembly, interactive marker detection, and common ingress request construction, with focused tests covering those helpers.
- Provider adapters extend `AbstractProviderConversationReplyAdapter`, centralizing stable adapter keys, declared payload types, missing-payload rejection, adapter attribute assembly, message-type selection, fallback text handling, and ingress construction so future provider slices only declare channel-specific optional attributes, fallback text, and interactive marker fields.
- Provider adapter keys are uppercase trimmed constants, so route aliases and fixtures can normalize inputs without masking unstable adapter identities.
- Adapter contract fixture `adapterKey` values use canonical lowercase snake aliases derived from `expectedChannel`, keeping JSON samples readable while preserving stable production adapter identities.
- Provider adapter classes do not declare custom `ConversationIngressReq` construction methods; the matrix enforces the shared template to prevent future slices from reintroducing hand-written ingress assembly.
- Provider adapter classes declare provider mapping through the shared base constructor only and do not declare custom non-synthetic methods; the base class keeps provider mapping methods private so override hooks cannot reappear accidentally.
- Provider adapter classes are stateless public no-arg definitions with no declared fields, keeping provider dependencies in mappers, controllers, connectors, or services rather than the adapter contract surface.
- Provider adapter class names and payload record names match the adapter key after normalizing case and underscores, keeping fixtures, routes, adapters, and payload records traceable without opening each file.
- Adapter contract raw fixture conversion is covered without depending on `ConversationIngressService`; key-based raw harness conversion remains covered separately by backend harness tests.
- The adapter-contract-only verifier dynamically discovers provider adapter and payload source files, so adding a provider slice does not require editing the script source list before the matrix can see it.
- The provider scaffold script generates a constructor-only adapter, matching payload record, and TEXT/INTERACTIVE fixture pair that can pass the adapter contract matrix from `--adapter-contract-only`, so a new provider slice starts from the enforced shared contract instead of bespoke Java and JSON setup.
- The provider scaffold script supports a no-extra-attribute provider and rejects common payload field reuse, Java keywords or literals, repeated `--attribute` values, non-lowerCamel provider-specific field names, and identical interactive id/text field names before writing files.
- The provider scaffold script rejects adapter keys with trailing underscores or consecutive underscores, while preserving valid uppercase snake keys with digits such as `B2B_CHAT`, so generated class names and fixture aliases remain traceable.
- The provider scaffold script prints generated file paths relative to the repository root, keeping scaffold output stable across developer machines and suitable for PRD notes or review comments.
- The provider scaffold script prints `scripts/verify-conversation-focus.sh --adapter-fixture-lint-only` and `scripts/verify-conversation-focus.sh --adapter-contract-only` after successful dry-run or generation, so the fastest fixture metadata gate and the provider behavior gate are both visible next steps.
- The provider scaffold script supports `--verify` to run `scripts/verify-conversation-focus.sh --adapter-contract-only` after generation, while `--dry-run --verify` only prints the would-run verifier command.
- The focused verifier exposes `--adapter-fixture-lint-only` for fixture JSON iteration, checking syntax, metadata, aliases, routing values, expected text, expected attributes, and provider-specific raw-field coverage before Java compilation starts.
- The focused verifier exposes `--adapter-fixture-contract-only` for fixture-only behavior iteration, syncing fixture resources and running the adapter matrix without focused javac only when provider adapter/payload and adapter contract test classes are fresher than their sources and backend POM inputs; stale cache evidence fails fast to `--adapter-contract-only`.
- The focused verifier runs fixture lint before backend resource sync, classpath refresh, focused javac, and Surefire in both `--adapter-contract-only` and the full backend gate, while still keeping the adapter matrix and full focused gate as the behavior gates.
- The focused verifier's Lombok target-class cache evidence watches the conversation-relevant source roots and their direct DAL imports, not the entire backend DAL or Lombok source list, while stale refreshes still compile the full discovered Lombok source set to preserve generated members for implicit dependencies.
- The focused verifier defers repository-wide Lombok source discovery until the stale-refresh path; cache-hit dry-runs and full focused gates use the narrowed evidence check and keep the warm compile set to provider/test sources.
- The focused verifier exposes `--adapter-source-compile-only` as a provider Java preflight that compiles the 19 provider/contract source set without backend resource sync or Surefire, and dry-run output must mark Surefire as skipped so it is not confused with the adapter behavior gate.
- The adapter-only focused javac paths use `-proc:none` for the Lombok-free provider/contract source set, while the full focused backend gate still uses the explicit Lombok processor for broader cache refreshes.
- The focused verifier exposes `--backend-source-compile-only` as a broader backend Java preflight that can run alone or with backend drill-down flags, skips Surefire and backend resource sync, uses `-implicit:none` even when Lombok processing is enabled, and must not refresh the full Lombok cache or implicitly compile unrelated dirty sourcepath classes.
- The focused verifier's `--adapter-contract-only` gate does not sync backend main resources or run broad main-class cleanup, preserving warmed Lombok-generated main target classes for subsequent no-processor backend drill-down gates while still syncing adapter fixture test resources and cleaning stale provider adapter/payload classes.
- The focused verifier exposes `--backend-domain-only` for conversation-domain backend iteration, compiling `domain/conversation` main and test sources with `-proc:none`, syncing adapter fixture test resources, and running the domain suite without replacing the web, security, WAIT, delivery, channel, frontend, or full focused gates.
- The focused verifier exposes `--backend-conversation-adapters-only` for adapter support/base/catalog/harness and provider adapter iteration, compiling the conversation domain source set with `-proc:none`, syncing adapter fixture test resources, and running adapter unit tests plus the contract matrix without replacing adapter-contract-only, service, schema, API/controller, runtime, frontend, backend-only, or full focused gates.
- The focused verifier exposes `--backend-conversation-services-only` for conversation service iteration, compiling `domain/conversation` main sources plus discovered `domain/conversation/*ServiceTest.java` tests with `-proc:none`, and running only the service suite without replacing schema, adapter/support, API/controller, runtime, frontend, domain, backend-only, or full focused gates.
- The focused verifier exposes `--backend-conversation-controllers-only` for conversation controller iteration, compiling conversation domain, conversation/demo controller, and demo sandbox service sources plus discovered conversation controller tests with `-proc:none`, and running only the controller suite without replacing security route, API, runtime, frontend, backend-only, or full focused gates.
- The focused verifier exposes `--backend-conversation-webhooks-only` for WhatsApp/internal/public webhook iteration, compiling conversation domain plus webhook controller sources with `-proc:none`, and running only payload mapper, webhook security, and webhook controller tests without replacing backend API, domain, runtime, frontend, backend-only, or full focused gates.
- The focused verifier exposes `--backend-whatsapp-only` for WhatsApp channel-slice iteration, compiling conversation domain, WhatsApp webhook controller, and WhatsApp Cloud API connector sources with `-proc:none`, and running only WhatsApp adapter, webhook, security, and connector tests without replacing adapter-only, webhook-only, runtime, API, domain, frontend, backend-only, or full focused gates.
- The focused verifier exposes `--backend-api-only` for conversation API/controller iteration, compiling the conversation domain plus conversation/demo controllers, demo service, and security route sources with `-proc:none`, discovering conversation web tests, and running only the API/security/controller suite without replacing backend domain, WAIT, delivery, channel, frontend, or full focused gates.
- The focused verifier exposes `--backend-runtime-only` for WAIT, delivery outbox, reach dispatch, WhatsApp Cloud connector, and channel registry iteration, compiling the focused runtime source set with the Lombok processor and running only the runtime test suite without replacing backend API, domain, adapter, frontend, or full focused gates.
- The focused verifier exposes `--backend-schema-only` for conversation migration/schema assertion iteration, compiling only discovered `domain/conversation/*SchemaTest.java` sources with `-proc:none`, syncing main resources for current migration reads, and running only the schema suite without replacing backend API, domain, runtime, adapter, frontend, or full focused gates.
- The focused verifier exposes `--frontend-authoring-only` for conversation WAIT authoring preset iteration, running only node palette and insert-node tests without replacing conversation/demo frontend, backend, frontend-only, or full focused gates.
- The focused verifier preflights the provider scaffold script with shell syntax validation and a no-write no-extra-attribute dry-run whenever backend verification runs, so scaffold regressions are caught by the same adapter-contract-only gate future providers use.
- The focused verifier scaffold preflight asserts dry-run output contains repo-relative adapter/fixture paths and the next adapter-contract-only command, does not contain the local workspace absolute prefix, and rejects representative invalid scaffold inputs for ambiguous adapter keys, common payload field reuse, Java keyword attributes, and non-lowerCamel provider fields.
- The focused verifier scaffold preflight asserts `--verify --dry-run` output contains the would-run adapter-contract-only command and does not contain the local workspace absolute prefix.
- The focused verifier exposes `--scaffold-preflight-only` for scaffold-script iteration, printing the same `provider-scaffold-*` dry-run commands and running the same scaffold invariant checks without treating it as a substitute for `--adapter-contract-only` or the full focused gate.
- The focused verifier exposes `--scaffold-contract-probe` for scaffold template changes, generating a disposable provider, passing the temporary adapter contract matrix, and removing generated probe source, fixture, and target output files before returning.
- The focused verifier derives backend Surefire selectors from the focused backend test source list and preflights listed backend source paths before dry-run or execution, keeping selector maintenance and source maintenance in one place.
- The full focused verifier discovers `domain/conversation/*Test.java` and `web/*Conversation*Test.java`, so future conversation-domain and conversation-controller tests join the regression gate without hand-editing the script.
- The full focused verifier discovers conversation and demo-sandbox frontend service/page tests and preflights resolved frontend paths, so future conversation UI tests join the regression gate without hand-editing the script.
- The focused verifier exposes `--frontend-logic-only` for conversation/demo API and presentation-helper tests, and `--frontend-conversation-only` for conversation/demo service plus page tests; both remain drill-down gates, while `--frontend-only` and the full focused gate keep the node palette and insert-node authoring tests.
- The focused verifier clears adapter-contract target resources before copying test fixtures, so deleted or renamed fixture JSON files cannot remain in `target/test-classes` and be auto-discovered accidentally.
- The focused verifier clears stale provider adapter/payload class files from both `target/classes` and `target/test-classes` before focused `javac`, so removed scaffold experiments cannot continue to satisfy or pollute adapter discovery after source and fixture deletion.
- The full focused verifier prepares resources and classpath without running full Maven main compilation, clears stale project main classes, writes the focused source list to a javac argfile, and explicitly includes Lombok-annotated main sources with the Lombok processor only when the Lombok target-class evidence is stale, so unrelated dirty main-source modules do not block conversation-focused verification.
- The focused verifier reuses the verifier temp classpath file at `${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/test-classpath.txt` only when it is newer than the backend POM inputs and all listed dependency entries exist, and otherwise refreshes the classpath through Maven, so warm provider gates avoid repeated dependency classpath generation without hiding dependency drift.
- The focused verifier syncs main resources and test resources directly through reviewed top-level allowlists, refreshing adapter fixture resource roots without deleting warmed compiled classes, so warm provider gates avoid Maven resource phases while deleted adapter fixtures cannot linger.
- The adapter contract matrix rejects a reintroduced `conversation/adapter-contracts/index.json`, keeping future provider fixture additions to one discovered JSON file instead of reviving duplicate index maintenance.
- Provider adapters declare concrete payload records for raw JSON conversion instead of relying on the default `Object.class` payload type.
- Provider adapter payload records implement `ProviderConversationReplyPayload`, keeping future provider adapters compatible with the shared ingress construction helper.
- Provider adapter payload records declare routing components for `userId`, `provider`, `externalMessageId`, and `eventId` so required raw payload keys can bind into typed payloads.
- Adapter contract fixtures cover every provider-specific payload record component at least once, so adding a channel-only field requires a representative raw sample in the same provider slice.
- Adapter contract fixture raw payload keys bind to the declared adapter payload record components, catching misspelled fixture fields before adapter execution.
- Adapter contract dynamic test display names include the discovered fixture resource path so a failed provider fixture is directly traceable to its JSON file.
- Adapter contract fixtures declare nonblank text values for raw routing inputs (`userId`, `provider`, `externalMessageId`, and `eventId`), not just the keys.
- Adapter contract runtime cases require normalized ingress `userId` to equal `rawPayload.userId`, preventing provider adapters from hardcoding or dropping the customer identity while still satisfying nonblank routing checks.
- Adapter contract fixtures declare normalized uppercase expected channel, provider, and message-type fields so fixture expectations cannot mask adapter normalization regressions.
- Controller tests prove the internal adapter ingress endpoint passes the current tenant and operator to the harness.
- Frontend service tests prove the internal adapter ingress helper posts typed provider payloads to `/canvas/conversations/adapters/{adapterKey}/ingress`.
- WhatsApp adapter tests prove text and interactive reply mapping without adding real webhook security or provider API dependencies.
- WhatsApp webhook bridge tests prove Cloud API text, interactive reply, timestamp, contact metadata, and status-only payload mapping, plus controller delegation to the `WHATSAPP` adapter harness behind the current tenant/operator context.
- Public WhatsApp webhook tests prove verify-token challenge handling, exact raw-body `X-Hub-Signature-256` validation, rejection before payload parsing on invalid signatures, anonymous security-filter access to the public route, and signed delegation to the `WHATSAPP` adapter harness.
- WhatsApp Cloud API connector tests prove template payload construction, caller-provided variable order preservation, session text fallback, message-id extraction, and fail-closed behavior when required credentials are missing.
- Delivery connector dispatch tests prove outbox provider dispatch resolves the REAL connector, passes tenant/channel/provider/user/payload from the outbox, returns the connector external message id as `messageId`, and avoids the legacy reach HTTP path when an enabled connector is available.
- Social DM adapter tests prove platform, page, thread, text, quick reply, and raw payload conversion without adding real social platform webhook security or provider API dependencies.
- Web chat adapter tests prove visitor text replies, interactive actions, session attributes, and raw payload conversion without adding a real widget or public chat hosting.
- RCS adapter tests prove text replies, suggestion replies, agent/conversation attributes, and raw payload conversion without adding real RCS provider webhook security or provider API dependencies.
- Focused backend and frontend tests pass with JDK 21 for backend.

## Rollout

1. Deploy schema and backend service with no real provider callbacks wired.
2. Use sandbox/operator ingress to test a canvas flow: send message, wait for `CONVERSATION_REPLY`, post inbound reply, verify downstream branch.
3. Add channel-specific adapters in later slices by mapping provider callbacks into the same ingress command through `ConversationAdapterHarness`.
4. Add richer UI after the backend contract is stable.

## Rollback

- Stop using the conversation ingress API.
- Existing send nodes and WAIT behavior remain available.
- Conversation tables can remain unused; no existing canvas execution path depends on them unless a canvas is explicitly configured to wait for `CONVERSATION_REPLY`.
