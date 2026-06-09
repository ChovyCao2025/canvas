# Conversational Session Foundation Implementation Plan

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable conversation session foundation so inbound customer replies can persist conversation state and resume canvas WAIT nodes through `CONVERSATION_REPLY`.

**Architecture:** Add a tenant-scoped conversation persistence layer and an ingress service that normalizes inbound replies into durable messages plus existing WAIT resume events. Keep real WhatsApp, social DM, web chat, and AI reply generation out of this slice so future provider adapters can map into one tested ingress contract.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, TypeScript, Vitest.

---

## Scope

- Add P2-080 spec, plan, and index rows.
- Add a migration, DOs, mappers, and schema test.
- Add the conversation ingress domain model and service.
- Add controller endpoints for ingress, recent sessions, and session messages.
- Add WAIT integration coverage for `CONVERSATION_REPLY`.
- Add frontend API and presentation helpers.
- Verify focused backend/frontend tests.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V270__conversation_session_foundation.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationSessionDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationMessageDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationSessionMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationMessageMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressReq.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressResp.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSessionView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationMessageView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationSessionSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationIngressServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapperTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationControllerTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationProviderWebhookControllerTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java`.
- Create `frontend/src/services/conversationApi.ts`.
- Create `frontend/src/services/conversationApi.test.ts`.
- Create `frontend/src/pages/conversations/conversationPresentation.ts`.
- Create `frontend/src/pages/conversations/conversationPresentation.test.ts`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create the P2-080 spec and plan files. Insert P2-080 rows immediately after P2-079 in:

- `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`

Verification:

```bash
rg -n "P2-080|p2-080-conversational-session-foundation" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-080-conversational-session-foundation.md docs/product-evolution/plans/p2-080-conversational-session-foundation-plan.md
```

Expected: all five files contain the P2-080 slug or order id.

- [x] **Step 2: Write failing schema test**

Create `ConversationSessionSchemaTest` that reads `V270__conversation_session_foundation.sql` and asserts:

- `CREATE TABLE conversation_session`
- `CREATE TABLE conversation_message`
- `uk_conversation_message_idempotency`
- `idx_conversation_session_active`
- `idx_conversation_message_session`

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=ConversationSessionSchemaTest
```

Expected before implementation: FAIL because the migration does not exist.

- [x] **Step 3: Add migration**

Create `V270__conversation_session_foundation.sql` with:

```sql
CREATE TABLE conversation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    canvas_id BIGINT NULL,
    version_id BIGINT NULL,
    execution_id VARCHAR(128) NULL,
    user_id VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    provider VARCHAR(64) NOT NULL DEFAULT 'DEFAULT',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    turn_count INT NOT NULL DEFAULT 0,
    context_json JSON NULL,
    last_message_at DATETIME NOT NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_session_active (tenant_id, user_id, channel, provider, status, last_message_at),
    INDEX idx_conversation_session_canvas (tenant_id, canvas_id, version_id),
    INDEX idx_conversation_session_execution (tenant_id, execution_id),
    INDEX idx_conversation_session_recent (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Conversation sessions';

CREATE TABLE conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    session_id BIGINT NOT NULL,
    direction VARCHAR(12) NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    external_message_id VARCHAR(128) NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    content_json JSON NOT NULL,
    text_content TEXT NULL,
    intent VARCHAR(64) NULL,
    processed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_message_idempotency (tenant_id, idempotency_key),
    INDEX idx_conversation_message_session (tenant_id, session_id, created_at),
    INDEX idx_conversation_message_external (tenant_id, external_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Conversation messages';
```

- [x] **Step 4: Add DOs and mappers**

Create `ConversationSessionDO` and `ConversationMessageDO` with MyBatis-Plus `@TableName`, `@TableId(type = IdType.AUTO)`, camelCase fields matching the migration, and `LocalDateTime` timestamp fields.

Create mapper interfaces:

```java
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSessionDO> {
}

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageDO> {
}
```

Run `ConversationSessionSchemaTest` and expect PASS.

- [x] **Step 5: Write failing service tests**

Create `ConversationIngressServiceTest` with mocks for the two mappers and `WaitResumeService`.

Cover:

- first inbound reply creates one session and one message, calls `resumeEventWaits("CONVERSATION_REPLY", userId, attributes, eventId)`, and returns `duplicate=false`;
- duplicate inbound reply returns the existing message/session and does not call `resumeEventWaits`;
- active session update increments turn count and merges context with latest `intent`;
- recent sessions filter by tenant;
- message listing rejects a session from another tenant.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=ConversationIngressServiceTest
```

Expected before implementation: FAIL because service and records do not exist.

- [x] **Step 6: Implement conversation domain records and service**

Implement records:

```java
public record ConversationIngressReq(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String channel,
        String provider,
        String externalMessageId,
        String eventId,
        String messageType,
        String text,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) {
}

public record ConversationIngressResp(
        Long sessionId,
        Long messageId,
        String status,
        boolean duplicate,
        int resumedWaitCount) {
}
```

`ConversationIngressService.ingest(Long tenantId, ConversationIngressReq req)` must:

- require nonblank `userId` and `channel`;
- normalize channel/provider/messageType to uppercase with provider default `DEFAULT`;
- build idempotency key from `channel`, `provider`, `externalMessageId` if present, otherwise `eventId`, otherwise `executionId:userId:text`;
- return duplicate response without resume when a message with the idempotency key exists;
- find latest active session for tenant/user/channel/provider/executionId or create one;
- insert inbound message with `direction=INBOUND`;
- update session turn count, context JSON, last message time, and status;
- call `waitResumeService.resumeEventWaits("CONVERSATION_REPLY", req.userId(), eventAttributes, eventId)`;
- return `RECORDED`.

Run `ConversationIngressServiceTest` and expect PASS.

- [x] **Step 7: Add controller tests**

Create `ConversationControllerTest` with mocked `ConversationIngressService` and `TenantContextResolver`.

Cover:

- `POST /canvas/conversations/ingress` passes current tenant to service and returns the response;
- `GET /canvas/conversations` passes current tenant, filters, and bounded limit;
- `GET /canvas/conversations/{sessionId}/messages` passes current tenant and bounded limit.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=ConversationControllerTest
```

Expected before implementation: FAIL because controller does not exist.

- [x] **Step 8: Implement controller**

Create `ConversationController` with routes:

- `POST /canvas/conversations/ingress`
- `GET /canvas/conversations`
- `GET /canvas/conversations/{sessionId}/messages`

Use existing `R<T>` response wrapper and `TenantContextResolver` pattern from nearby controllers. Bound list limits to `1..100`.

Run controller and service tests and expect PASS.

- [x] **Step 9: Extend WAIT integration test**

Modify `WaitEventFilterTest` or add a focused method proving `WaitResumeService.resumeEventWaits("CONVERSATION_REPLY", "user-1", attributes, "event-1")` resumes a wait whose filters match `intent=PRODUCT_A` and skips a wait whose filters specify another intent.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=WaitEventFilterTest,ConversationIngressServiceTest
```

Expected: PASS.

- [x] **Step 10: Add frontend API and presentation tests**

Create `conversationApi.ts` with:

- `ingestConversationReply(payload)`
- `ingestConversationAdapterReply(adapterKey, payload)`
- `listConversationSessions(params)`
- `listConversationMessages(sessionId, params)`

Create presentation helpers:

- `formatConversationStatus(status)`
- `formatConversationDuplicate(resp)`
- `conversationMessageLine(message)`
- adapter-backed inbox channel options for `WEB_CHAT`, `WHATSAPP`, `SOCIAL_DM`, `RCS`, and `SANDBOX`

Run:

```bash
cd frontend
npm run test -- --run src/services/conversationApi.test.ts src/pages/conversations/conversationPresentation.test.ts
```

Latest focused verification:

```bash
npm run test -- --run src/services/conversationApi.test.ts src/pages/conversations/conversationPresentation.test.ts src/services/demoSandboxApi.test.ts src/pages/demo-sandbox/demoSandbox.test.ts
```

Result: 4 test files passed, 14 tests passed.

Expected before implementation: FAIL because files do not exist.

- [x] **Step 11: Implement frontend API and presentation helpers**

Follow existing service style in `frontend/src/services/messageTemplateApi.ts` and test style in `frontend/src/pages/message-templates/messageTemplateCenter.test.ts`.

Run the frontend tests and expect PASS.

- [x] **Step 12: Focused verification**

Run backend focused verification:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,WaitEventFilterTest,WaitSubscriptionServiceTest,WaitResumeServiceTest,UserInputServiceTest,ChannelConnectorRegistryTest,MessageTemplateServiceTest,WebhookDispatcherServiceTest
```

Expected: all selected tests pass.

Latest backend verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,WaitEventFilterTest,WaitSubscriptionServiceTest,WaitResumeServiceTest,UserInputServiceTest,ChannelConnectorRegistryTest,MessageTemplateServiceTest,WebhookDispatcherServiceTest
```

Result: 41 tests run, 0 failures, 0 errors, 0 skipped.

Run frontend focused verification:

```bash
cd frontend
npm run test -- --run src/services/conversationApi.test.ts src/pages/conversations/conversationPresentation.test.ts src/services/messageTemplateApi.test.ts src/services/channelConnectorApi.test.ts
```

Expected: all selected tests pass.

Latest frontend verification:

```bash
npm run test -- --run src/services/conversationApi.test.ts src/pages/conversations/conversationPresentation.test.ts src/services/messageTemplateApi.test.ts src/services/channelConnectorApi.test.ts
```

Result: 4 test files passed, 13 tests passed.

- [x] **Step 13: Add internal WhatsApp webhook bridge**

Add an authenticated internal/operator route for WhatsApp Cloud API webhook-like payloads:

- `WhatsAppWebhookPayloadMapper` parses `entry[].changes[].value.messages[]` into `WhatsAppConversationReplyPayload`.
- Text messages map `text.body` into reply text.
- Interactive `button_reply` and `list_reply` messages map reply id/title into interactive reply fields.
- Contact/profile, phone number metadata, message type, interactive type, and provider timestamp are preserved in attributes.
- Status-only payloads return an empty list and do not call the adapter harness.
- `ConversationProviderWebhookController` exposes `POST /canvas/conversations/provider-webhooks/whatsapp`, resolves the current tenant/operator, and delegates mapped payloads through `ConversationAdapterHarness.ingest(tenantId, "WHATSAPP", payload, operator)`.

This endpoint is not a public Meta webhook receiver. It does not implement verification-token challenge handling, signature validation, or send-side WhatsApp Business API integration.

Focused verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=WhatsAppWebhookPayloadMapperTest,ConversationProviderWebhookControllerTest
```

Result: 6 tests run, 0 failures, 0 errors, 0 skipped.

Broader conversation adapter verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationProviderWebhookControllerTest,WaitEventFilterTest,DemoSandboxControllerTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WhatsAppWebhookPayloadMapperTest,WebChatConversationReplyAdapterTest,SocialDmConversationReplyAdapterTest,RcsConversationReplyAdapterTest
```

Result: 52 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 14: Add public WhatsApp webhook security shell**

Add a minimal public provider ingress shell that can receive real WhatsApp webhook callbacks without requiring JWT:

- `WhatsAppWebhookSecurityService` verifies subscribe challenges against `canvas.conversation.whatsapp.webhook.verify-token`, with fallback support for the earlier `canvas.conversation.whatsapp.verify-token` property.
- `WhatsAppWebhookSecurityService` verifies `X-Hub-Signature-256` as `sha256=` plus HMAC-SHA256 over the exact raw request body using `canvas.conversation.whatsapp.webhook.app-secret`, with fallback support for `canvas.conversation.whatsapp.app-secret`.
- `PublicConversationWebhookController` exposes `GET/POST /public/conversation-webhooks/{tenantId}/whatsapp` and the compatibility alias `/public/conversations/webhooks/{tenantId}/whatsapp`.
- The POST route validates signature before JSON parsing, then maps payloads through `WhatsAppWebhookPayloadMapper` and delegates to `ConversationAdapterHarness.ingest(tenantId, "WHATSAPP", payload, "whatsapp-webhook")`.
- `SecurityConfig` permits anonymous filter-layer access to both public webhook path variants; controller-level challenge/signature checks remain the security boundary.

This is still not the full WhatsApp Business API connector: tenant-specific secret storage, connector-bound callback URLs, send-side WhatsApp messages, template approval sync, replay-window controls, and Meta app lifecycle management remain later slices.

Focused verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=WhatsAppWebhookSecurityServiceTest,PublicConversationWebhookControllerTest,SecurityConfigRouteTest
```

Result: 14 tests run, 0 failures, 0 errors, 0 skipped.

Broader conversation verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationProviderWebhookControllerTest,PublicConversationWebhookControllerTest,WhatsAppWebhookSecurityServiceTest,SecurityConfigRouteTest,WaitEventFilterTest,DemoSandboxControllerTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WhatsAppWebhookPayloadMapperTest,WebChatConversationReplyAdapterTest,SocialDmConversationReplyAdapterTest,RcsConversationReplyAdapterTest,ConversationPrivateDomainControllerTest,ConversationPrivateDomainSyncServiceTest,ConversationPrivateDomainSchemaTest
```

Result: 75 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 15: Add WhatsApp Cloud API outbound connector bridge**

Add a minimal send-side bridge so future REAL connector routing can target WhatsApp Cloud API without adding the full provider lifecycle in this slice:

- `WhatsAppCloudApiClient` defines the send boundary.
- `HttpWhatsAppCloudApiClient` posts to `/{graphApiVersion}/{phoneNumberId}/messages` with Bearer auth.
- `WhatsAppCloudApiConnector` registers as `@Component("WHATSAPP:CLOUD_API")`.
- Connector properties:
  - `canvas.conversation.whatsapp.cloud.phone-number-id`
  - `canvas.conversation.whatsapp.cloud.access-token`
  - `canvas.conversation.whatsapp.cloud.default-language`, default `en_US`
  - `canvas.conversation.whatsapp.cloud.graph-base-url`, default `https://graph.facebook.com`
  - `canvas.conversation.whatsapp.cloud.graph-api-version`
  - `canvas.conversation.whatsapp.cloud.timeout-ms`, default `10000`
- Template sends build a Cloud API template payload and preserve caller-provided `variables` map iteration order for body parameters.
- Session text sends use `content.body` or `content.content` when `templateId` is absent.
- Missing connector client, phone number id, or access token returns `DISABLED` without calling the client.
- The first Graph response `messages[0].id` is returned as `externalMessageId`.

This step deliberately keeps `AbstractSendMessageHandler` behind the existing `ReachDeliveryService` boundary, so delivery records and outbox enqueue behavior stay stable while the connector is introduced.

Focused RED/GREEN verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=WhatsAppCloudApiConnectorTest
```

RED result after tightening the test: `sendsTemplateMessageThroughCloudApiClient` failed because template parameters were sorted by key instead of preserving caller-provided map order.

GREEN result after connector fix: 3 tests run, 0 failures, 0 errors, 0 skipped.

Broader connector/conversation verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=WhatsAppCloudApiConnectorTest,ChannelConnectorRegistryTest,ChannelConnectorHandlerTest,SendMessageHandlerTest,ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationProviderWebhookControllerTest,PublicConversationWebhookControllerTest,WhatsAppWebhookSecurityServiceTest,WhatsAppConversationReplyAdapterTest,WhatsAppWebhookPayloadMapperTest,BiDatasourceControllerTest,BiDatasourceOnboardingServiceTest,PaidMediaAudienceSyncSchemaTest,PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest
```

Latest result after the template-order TDD tightening: 73 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 16: Route outbox provider dispatch through REAL connectors**

Add connector-aware provider dispatch at the delivery outbox boundary instead of calling `connector.send()` directly from the DAG handler:

- `ReachDeliveryService` accepts an optional `ChannelConnectorRegistry`.
- `dispatchToProvider(DeliveryOutboxDO)` parses the original outbox payload.
- If the registry resolves an enabled connector for the outbox tenant/channel/provider, it calls `connector.send(...)` with the outbox tenant id, channel, provider, user id, and payload.
- Accepted connector sends return a response map containing `messageId`, `externalMessageId`, and `connectorStatus`, so the existing `DeliveryOutboxConsumer` continues to call `markSent(outboxId, providerMessageId, response)` without a new reconciliation path.
- Rejected connector sends raise an error, so the existing outbox retry/dead logic remains responsible for failure handling.
- If no enabled connector can be resolved, the legacy reach-platform HTTP dispatch remains as a compatibility fallback for older outbox rows and direct delivery callers.

This is the safe REAL routing point for WhatsApp outbound: send handlers still create the same delivery request and outbox record, while provider-specific dispatch now happens through the connector contract when connector configuration exists.

Focused RED/GREEN verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ReachDeliveryServiceConnectorDispatchTest
```

RED result: test compilation failed because `ReachDeliveryService` had no connector-registry constructor/integration point and still exposed only the legacy delivery dependencies.

GREEN result: 1 test run, 0 failures, 0 errors, 0 skipped.

Focused outbound regression verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ReachDeliveryServiceConnectorDispatchTest,DeliveryOutboxConsumerTest,ChannelConnectorHandlerTest,SendMessageHandlerTest,WhatsAppCloudApiConnectorTest
```

Result: 14 tests run, 0 failures, 0 errors, 0 skipped.

Broader connector/conversation verification after REAL connector outbox dispatch:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ReachDeliveryServiceConnectorDispatchTest,DeliveryOutboxConsumerTest,WhatsAppCloudApiConnectorTest,ChannelConnectorRegistryTest,ChannelConnectorHandlerTest,SendMessageHandlerTest,ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationProviderWebhookControllerTest,PublicConversationWebhookControllerTest,WhatsAppWebhookSecurityServiceTest,WhatsAppConversationReplyAdapterTest,WhatsAppWebhookPayloadMapperTest,BiDatasourceControllerTest,BiDatasourceOnboardingServiceTest,PaidMediaAudienceSyncSchemaTest,PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest
```

Result: 79 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 17: Bridge WhatsApp Cloud API status receipts into delivery outbox receipts**

Add signed public-webhook handling for WhatsApp Cloud API `statuses[]` payloads:

- `WhatsAppWebhookPayloadMapper.toDeliveryReceipts(...)` maps status events into `DeliveryReceiptRequest`.
- Receipt provider is `CLOUD_API`, provider message id is the WhatsApp status `id`, and receipt type is the uppercase provider status.
- Receipt idempotency key uses `whatsapp:{entryId}:{messageId}:{receiptType}:{timestamp}`.
- Raw receipt payload includes phone number id, display phone number, recipient id, status, timestamp, and entry id when present.
- `PublicConversationWebhookController` accepts an optional `DeliveryOutboxService` and records mapped receipts after signature verification and before message ingress.
- Status-only webhook payloads return an empty conversation response list and do not invoke `ConversationAdapterHarness`.

Focused WhatsApp receipt verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=WhatsAppWebhookPayloadMapperTest,PublicConversationWebhookControllerTest
```

Result: 9 tests run, 0 failures, 0 errors, 0 skipped.

Broader conversation/outbox regression verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=WhatsAppWebhookPayloadMapperTest,PublicConversationWebhookControllerTest,WhatsAppWebhookSecurityServiceTest,SecurityConfigRouteTest,ReachDeliveryServiceConnectorDispatchTest,DeliveryOutboxConsumerTest,WhatsAppCloudApiConnectorTest,ChannelConnectorRegistryTest,ChannelConnectorHandlerTest,SendMessageHandlerTest,ConversationSessionSchemaTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationProviderWebhookControllerTest
```

Result: 50 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 18: Add a conversation reply wait authoring preset**

Expose a product-facing canvas palette preset for conversational journeys without adding a new backend node type:

- `withConversationNodePresets(...)` augments backend node metadata with a `等待会话回复` palette item when the governed `WAIT` node is available.
- The preset drops the same backend node type `WAIT`, but writes default `bizConfig` with `waitType=UNTIL_EVENT` and `eventCode=CONVERSATION_REPLY`.
- `NodePanel` carries preset display name and default config through drag metadata.
- `buildNodeExpansion(...)` preserves the preset display name when creating the canvas node.
- The editor safely ignores malformed preset config drag metadata.

Focused frontend RED/GREEN verification:

```bash
npm run test -- --run src/components/node-panel/nodeLibrary.test.ts src/pages/canvas-editor/insertNode.test.ts
```

RED result: 2 tests failed because `withConversationNodePresets` did not exist and `buildNodeExpansion` always used the generic `WAIT` display name.

GREEN result: 2 test files passed, 13 tests passed.

Frontend production build verification:

```bash
npm run build
```

Result: TypeScript compile and Vite production build completed successfully.

Browser verification note: the in-app Browser returned unavailable for this session, and local Playwright is not installed in `frontend/node_modules`; visual browser verification could not be performed in this environment.

- [x] **Step 19: Add a provider adapter contract matrix**

Add a reusable matrix gate so future conversational provider slices can extend the adapter surface with one raw payload fixture instead of rebuilding mock harness plumbing:

- `ConversationAdapterContractSupport.RawContractCase` captures the adapter, raw payload, normalized channel/provider/message type, text content, external message id, event id, expected attributes, and missing-payload error message.
- `ConversationAdapterContractSupport.assertRawIngressContract(...)` routes the raw payload through `ConversationAdapterHarness`, captures the normalized `ConversationIngressReq`, asserts expected routing fields and attributes, and verifies null payload rejection.
- `ConversationAdapterContractMatrixTest` covers representative raw payloads for WhatsApp text, WhatsApp interactive, social DM text, web chat action, and RCS suggestion replies.
- Future provider slices should add one matrix case for the canonical raw provider payload before adding webhook/controller behavior.

Focused RED/GREEN verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationAdapterContractMatrixTest
```

RED result: test compilation failed because `ConversationAdapterContractSupport.RawContractCase` did not exist.

GREEN result: 5 tests run, 0 failures, 0 errors, 0 skipped.

Broader adapter/conversation regression verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationAdapterContractMatrixTest,ConversationAdapterHarnessTest,ConversationAdapterCatalogTest,WhatsAppConversationReplyAdapterTest,SocialDmConversationReplyAdapterTest,WebChatConversationReplyAdapterTest,RcsConversationReplyAdapterTest,ConversationIngressServiceTest
```

Result: 35 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 20: Enforce provider adapter matrix completeness**

Turn the Step 19 guidance into an automated gate so future provider adapters cannot be added without a raw-payload contract case:

- `ConversationAdapterContractSupport.providerAdapterKeys()` discovers concrete `ConversationReplyAdapter` classes from the compiled conversation package, excluding the sandbox adapter because it is the internal demo harness rather than a provider adapter.
- `ConversationAdapterContractSupport.contractKeys(...)` extracts normalized keys from the matrix fixtures.
- `ConversationAdapterContractMatrixTest.contractMatrixCoversEveryProviderAdapter()` compares both sets and fails when a provider adapter is missing from the matrix.
- The discovery path intentionally requires provider adapters to remain stateless and test-instantiable, matching the current adapter design and keeping future provider slices cheap to verify.

Focused RED/GREEN verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationAdapterContractMatrixTest -DfailIfNoTests=true -Dsurefire.failIfNoSpecifiedTests=true
```

RED result: test compilation failed because `ConversationAdapterContractSupport.contractKeys(...)` and `providerAdapterKeys()` did not exist.

GREEN result: 6 tests run, 0 failures, 0 errors, 0 skipped.

Compile-gate note: the dirty worktree had BI acceleration testCompile drift while verifying this step. `BiDatasetAccelerationService` already had extract retention scaffolding, but a public cleanup method collided with a private helper. Renaming the private helper restored compilation without changing the public API.

Focused BI verification for that compile-gate fix:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=BiDatasetAccelerationServiceTest,BiDatasetControllerTest -DfailIfNoTests=true -Dsurefire.failIfNoSpecifiedTests=true
```

Result: 17 tests run, 0 failures, 0 errors, 0 skipped.

Broader adapter/conversation regression verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationAdapterContractMatrixTest,ConversationAdapterHarnessTest,ConversationAdapterCatalogTest,WhatsAppConversationReplyAdapterTest,SocialDmConversationReplyAdapterTest,WebChatConversationReplyAdapterTest,RcsConversationReplyAdapterTest,ConversationIngressServiceTest -DfailIfNoTests=true -Dsurefire.failIfNoSpecifiedTests=true
```

Result: 36 tests run, 0 failures, 0 errors, 0 skipped.

- [x] **Step 21: Add a reusable conversation focus verifier**

Add one script so future conversation provider slices can run the full focused quality gate without reassembling backend and frontend commands by hand:

- `scripts/verify-conversation-focus.sh` discovers Java 21 for backend verification.
- It compiles main backend code with Maven, builds the test classpath, copies test resources for fixture-backed adapter contracts, compiles only the P2-080 focused backend test sources with `javac`, and then runs the Surefire selector with `-DfailIfNoTests=true` and `-Dsurefire.failIfNoSpecifiedTests=true`.
- This avoids unrelated dirty-worktree testCompile drift while still requiring main backend compilation and real focused test execution.
- It discovers a frontend Node runtime compatible with the current Vite/Vitest lockfile (`^20.19.0`, `>=22.12.0`, or `>=24.0.0`) and uses the matching npm binary.
- It runs the focused frontend tests for conversation API, presentation helpers, demo sandbox, node palette, and insert-node behavior.
- Options: `--backend-only`, `--frontend-only`, `--skip-frontend`, `--with-frontend-build`, and `--dry-run`.

RED verification:

```bash
scripts/verify-conversation-focus.sh --dry-run
```

RED result: command failed because `scripts/verify-conversation-focus.sh` did not exist.

Dry-run verification after implementation:

```bash
scripts/verify-conversation-focus.sh --dry-run
```

Result: dry-run passed, resolved Java 21 at `/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home`, selected Node 25.8.1 from Homebrew for frontend verification, and printed backend/frontend commands.

Focused end-to-end verification:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 97 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

Optional frontend build verification:

```bash
scripts/verify-conversation-focus.sh --frontend-only --with-frontend-build
```

Result: frontend focused gate ran 6 test files and 27 tests with 0 failures; `tsc && vite build` completed successfully.

- [x] **Step 22: Add fixture-backed provider adapter contract cases**

Move the adapter contract matrix toward provider-slice fixtures without rewriting the existing Java matrix:

- `ConversationAdapterContractSupport.fixtureCases(...)` reads `conversation/adapter-contracts/index.json` and converts each indexed JSON fixture into the existing `RawContractCase` contract model.
- `ConversationAdapterContractSupport.providerAdapterRegistry()` reuses the provider adapter class discovery path to build the fixture adapter registry, so future stateless provider adapters can pair with JSON fixtures without adding another hand-written test registry.
- Fixture adapter keys are normalized before lookup, and unknown adapter references fail fast with the fixture resource path.
- `backend/canvas-engine/src/test/resources/conversation/adapter-contracts/whatsapp-text.json` is the first representative raw payload fixture, proving the same WhatsApp text ingress contract through JSON-backed raw payload conversion.
- `ConversationAdapterContractMatrixTest.contractMatrixIncludesFixtureBackedCases()` guards that fixture-backed cases are part of the matrix instead of living as unused resources.
- `scripts/verify-conversation-focus.sh` now runs `mvn -pl canvas-engine resources:testResources` before the focused `javac`/Surefire path so fixture resources are available even when unrelated dirty-worktree test sources are bypassed.

Focused RED/GREEN verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationAdapterContractMatrixTest -DfailIfNoTests=true -Dsurefire.failIfNoSpecifiedTests=true
```

RED result: 7 tests ran with 1 failure because the hard-coded matrix did not include `whatsapp text fixture`.

GREEN result: 8 tests run, 0 failures, 0 errors, 0 skipped.

Focused backend verifier regression:

```bash
scripts/verify-conversation-focus.sh --dry-run
scripts/verify-conversation-focus.sh --backend-only
```

Result: dry-run printed the backend test-resource command; backend focused gate ran 97 tests with 0 failures, 0 errors, 0 skipped.

- [x] **Step 23: Enforce fixture coverage for every provider adapter**

Turn the fixture-backed matrix from an example into a future-provider gate:

- `ConversationAdapterContractMatrixTest.contractFixturesCoverEveryProviderAdapter()` compares keys from indexed JSON fixtures with discovered provider adapters.
- The gate fails when a stateless provider adapter exists without an indexed fixture, so future provider slices can no longer satisfy coverage only through a Java hard-coded case.
- Added fixture-backed raw payloads for `SOCIAL_DM`, `WEB_CHAT`, and `RCS`, alongside the existing WhatsApp fixture.
- The indexed fixture set now covers WhatsApp text, Social DM text, Web Chat action, and RCS suggestion provider skeletons through the same `ConversationAdapterHarness` path.

Focused RED/GREEN verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationAdapterContractMatrixTest -DfailIfNoTests=true -Dsurefire.failIfNoSpecifiedTests=true
```

RED result: 9 tests ran with 1 failure because fixture keys only contained `WHATSAPP` and were missing `RCS`, `SOCIAL_DM`, and `WEB_CHAT`.

GREEN result: 12 tests run, 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 97 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 24: Make the provider contract matrix fixture-first**

Remove the remaining Java hard-coded raw payload table from the adapter contract matrix so future provider slices can extend the contract surface primarily through fixture files:

- `ConversationAdapterContractMatrixTest.cases()` now returns only `ConversationAdapterContractSupport.fixtureCases(...)`.
- `ConversationAdapterContractMatrixTest.contractMatrixUsesIndexedFixturesAsRawPayloadCases()` guards that the dynamic raw payload contract cases exactly match the indexed JSON fixtures.
- Added `whatsapp-interactive.json` so the fixture-only matrix preserves WhatsApp interactive reply coverage after removing the Java hard-coded case.
- The indexed fixture set now covers WhatsApp text, WhatsApp interactive, Social DM text, Web Chat action, and RCS suggestion cases.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: backend focused gate ran the matrix and failed `contractMatrixUsesIndexedFixturesAsRawPayloadCases` because `cases()` still included five Java hard-coded raw payload cases in addition to indexed fixtures. A direct Maven targeted run was blocked earlier by unrelated dirty-worktree BI compile drift, so the focused verifier was used as the authoritative RED check for this slice.

GREEN result: backend focused gate ran 94 tests with 0 failures, 0 errors, 0 skipped. `ConversationAdapterContractMatrixTest` ran 9 tests with 0 failures.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 94 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 25: Add adapter fixture index integrity gate**

Protect the fixture-first matrix from silent coverage gaps when future provider slices add files under `conversation/adapter-contracts/`:

- `ConversationAdapterContractSupport.indexedFixtureResources()` reads `index.json` and normalizes every entry to its classpath fixture resource path.
- `ConversationAdapterContractSupport.discoveredFixtureResources()` scans the fixture resource directory and discovers every JSON fixture file except `index.json`.
- `ConversationAdapterContractMatrixTest.contractFixtureIndexReferencesEveryFixtureResource()` fails when a fixture file exists but is not listed in the index, or when the index references a different fixture set.
- `ConversationAdapterContractMatrixTest.contractFixtureIndexEntriesAreUnique()` fails duplicate index entries before they create duplicate dynamic contract cases.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused backend compilation failed because `ConversationAdapterContractSupport.indexedFixtureResources()` and `discoveredFixtureResources()` did not exist.

GREEN result: backend focused gate ran 97 tests with 0 failures, 0 errors, 0 skipped. `ConversationAdapterContractMatrixTest` ran 11 tests with 0 failures.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 97 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 26: Add adapter fixture metadata completeness gate**

Make the fixture-first matrix safer for future provider slices by validating that each JSON fixture declares the full contract metadata it is supposed to prove:

- `ConversationAdapterContractSupport.FixtureDefinition` exposes fixture resource name, raw payload, expected normalized fields, expected attributes, and missing-payload message for metadata assertions.
- `ConversationAdapterContractSupport.fixtureDefinitions()` loads indexed fixtures once and is reused by `fixtureCases(...)`, so dynamic contract execution and metadata checks read the same source of truth.
- `ConversationAdapterContractMatrixTest.contractFixturesDeclareRequiredMetadata()` fails if a fixture omits resource/name/adapter key/raw payload, expected channel/provider/message type, external/event ids, `expectedAttributes.adapter`, or missing-payload message.
- `ConversationAdapterContractMatrixTest.contractFixtureNamesAreUnique()` keeps dynamic test names unambiguous.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused backend compilation failed because `ConversationAdapterContractSupport.fixtureDefinitions()` and `FixtureDefinition` did not exist.

GREEN result: backend focused gate ran 99 tests with 0 failures, 0 errors, 0 skipped. `ConversationAdapterContractMatrixTest` ran 13 tests with 0 failures.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 99 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 27: Add an adapter-contract-only verifier mode**

Give future provider adapter and fixture slices a faster local gate without weakening the contract checks:

- `scripts/verify-conversation-focus.sh --adapter-contract-only` runs backend verification only.
- It still resolves Java 21, compiles main backend code, builds the test classpath, and copies test resources so adapter class discovery and JSON fixture loading happen against current code and current resources.
- It compiles only `ConversationAdapterContractSupport` and `ConversationAdapterContractMatrixTest` test sources with `javac`.
- It runs only `ConversationAdapterContractMatrixTest` through Surefire with the existing no-test failure protections.
- The default `scripts/verify-conversation-focus.sh` behavior remains the broader backend conversation gate plus focused frontend tests.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run
```

RED result: command failed with `ERROR: unknown argument: --adapter-contract-only`.

GREEN dry-run result: command resolved Java 21, printed the backend compile/classpath/test-resource steps, limited test source compilation to `ConversationAdapterContractSupport.java` and `ConversationAdapterContractMatrixTest.java`, and limited Surefire to `ConversationAdapterContractMatrixTest`.

Contract-only verifier regression:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

Result: `ConversationAdapterContractMatrixTest` ran 13 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 99 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 28: Add raw payload routing key gate for adapter fixtures**

Keep future provider fixtures from accidentally proving only expected output metadata while omitting the raw input fields the adapter must normalize:

- `ConversationAdapterContractSupport.requiredRawPayloadRoutingKeys()` centralizes the fixture raw payload keys required for provider adapter contract fixtures: `userId`, `provider`, `externalMessageId`, and `eventId`.
- `ConversationAdapterContractMatrixTest.contractFixtureRawPayloadsDeclareRoutingKeys()` fails when an indexed fixture omits any required raw routing key.
- This complements the Step 26 metadata gate: Step 26 verifies expected normalized fields, while this step verifies the representative raw provider payload carries the corresponding routing inputs.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: contract-only verifier compilation failed because `ConversationAdapterContractSupport.requiredRawPayloadRoutingKeys()` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 14 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 100 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 29: Include fixture resource names in adapter contract dynamic tests**

Make future provider fixture failures immediately actionable by including the JSON fixture resource path in each dynamic raw payload contract test name:

- `ConversationAdapterContractSupport.RawContractCase` now carries the indexed fixture resource name alongside the fixture display name.
- `RawContractCase.displayName()` formats dynamic tests as `fixture name [conversation/adapter-contracts/file.json]`.
- `ConversationAdapterContractMatrixTest.rawPayloadIngressDynamicTestsIncludeFixtureResourceNames()` guards that the `@TestFactory` display names include fixture resource paths.
- The existing fixture name uniqueness gate still keeps the human-readable part unambiguous, while the resource path makes failed matrix entries directly traceable to the fixture file.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `ConversationAdapterContractMatrixTest.rawPayloadIngressDynamicTestsIncludeFixtureResourceNames` failed because dynamic display names were only `whatsapp text fixture`, `whatsapp interactive fixture`, `social dm text fixture`, `web chat action fixture`, and `rcs suggestion fixture`.

GREEN result: `ConversationAdapterContractMatrixTest` ran 15 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 101 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 30: Require usable raw routing values in adapter fixtures**

Make provider fixtures prove representative routing input, not just the presence of routing keys:

- `ConversationAdapterContractMatrixTest.contractFixtureRawPayloadRoutingValuesAreUsableText()` checks every indexed fixture through a reusable support assertion.
- `ConversationAdapterContractSupport.assertRawPayloadRoutingValuesAreUsable(...)` requires `userId`, `provider`, `externalMessageId`, and `eventId` to be present as nonblank strings.
- Assertion descriptions include the fixture resource name and raw payload key, so a bad fixture points directly to `conversation/adapter-contracts/*.json` and the broken field.
- This strengthens Step 28 by preventing future fixtures from satisfying `containsKeys(...)` with blank or non-text routing values.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertRawPayloadRoutingValuesAreUsable(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 16 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 102 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 31: Require normalized expected fields in adapter fixtures**

Prevent future provider fixtures from masking adapter normalization regressions by encoding already-denormalized expected values:

- `ConversationAdapterContractMatrixTest.contractFixturesDeclareNormalizedExpectedFields()` checks every indexed fixture through a reusable support assertion.
- `ConversationAdapterContractSupport.assertExpectedFieldsAreNormalized(...)` requires `expectedChannel`, `expectedProvider`, and `expectedMessageType` to be nonblank, trimmed, and uppercase.
- Assertion descriptions include the fixture resource name and field name, so a failed normalization gate points directly to the fixture and expected field.
- This keeps the contract matrix focused on normalized ingress output rather than letting fixtures redefine "expected" values to match a broken adapter.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertExpectedFieldsAreNormalized(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 17 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 103 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 32: Move adapter idempotency routing validation into the harness**

Make the adapter harness fail fast when future provider adapters omit the routing fields required for stable idempotency and WAIT resume correlation:

- `ConversationAdapterHarnessTest.harnessRejectsAdaptersThatOmitProviderAndIdempotencyFields()` proves adapter output with a blank provider plus missing/blank `externalMessageId` and `eventId` is rejected before `ConversationIngressService` is called.
- `ConversationAdapterHarness.validate(...)` now keeps the existing `userId`/`channel` guard and adds a separate `provider`, `externalMessageId`, and `eventId` guard.
- The fixture matrix still validates representative raw payloads and expected normalized fields; this step brings the same minimum contract into the runtime harness boundary used by internal/operator adapter ingress and public provider webhook delegation.
- Direct `ConversationIngressService.ingest(...)` keeps its existing fallback idempotency behavior; the stricter rule applies to adapter-produced ingress requests where provider callbacks should supply stable upstream ids.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: `ConversationAdapterHarnessTest.harnessRejectsAdaptersThatOmitProviderAndIdempotencyFields` failed because the harness allowed the invalid adapter output and delegated it to the mocked ingress service.

GREEN result: backend focused gate ran 104 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 104 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 33: Require provider adapters to declare concrete payload records**

Keep raw provider payload conversion reliable for future channel slices by making concrete payload typing part of the provider contract:

- `ConversationAdapterContractMatrixTest.providerAdaptersDeclareConcretePayloadTypes()` checks every discovered provider adapter in the provider registry.
- `ConversationAdapterContractSupport.assertProviderAdapterDeclaresConcretePayloadType(...)` requires provider adapters to return a non-null `payloadType()` that is not `Object.class` and is a Java record.
- This locks in the current adapter pattern where raw JSON maps convert into small immutable payload records before adapter normalization.
- Future provider slices now fail the adapter-contract-only gate immediately if they rely on the `ConversationReplyAdapter` default `Object.class` payload type.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterDeclaresConcretePayloadType(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 18 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 104 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 34: Require payload records to declare routing components**

Make raw JSON conversion failures more direct by validating the payload record shape before executing adapter fixture cases:

- `ConversationAdapterContractMatrixTest.providerAdapterPayloadTypesDeclareRequiredRoutingComponents()` checks every discovered provider adapter.
- `ConversationAdapterContractSupport.assertProviderAdapterPayloadTypeDeclaresRoutingComponents(...)` reuses the concrete payload record gate and then verifies the record components include `userId`, `provider`, `externalMessageId`, and `eventId`.
- This complements Step 33: Step 33 proves an adapter has a record payload type, while this step proves that payload type exposes the routing fields required by the harness and fixture matrix.
- Future provider slices now fail the adapter-contract-only gate with the adapter key and payload record class if the raw payload keys cannot bind into the adapter's typed payload.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterPayloadTypeDeclaresRoutingComponents(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 19 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 106 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 35: Require fixture raw payload keys to bind to payload records**

Catch fixture field typos and payload-record drift before adapter execution by validating raw fixture keys against the adapter's declared payload record:

- `ConversationAdapterContractMatrixTest.contractFixtureRawPayloadKeysBindToPayloadRecordComponents()` checks every indexed fixture with the same provider adapter registry used by dynamic contract cases.
- `ConversationAdapterContractSupport.assertFixtureRawPayloadKeysBindToPayloadRecord(...)` resolves the fixture adapter key, reuses the concrete payload record gate, then verifies every top-level `rawPayload` key exists as a component on the adapter payload record.
- Failure messages include the fixture resource, adapter key, and payload record class, so a misspelled fixture field points directly to the broken JSON file and target payload type.
- This complements Step 34: Step 34 checks the payload record has required routing components, while this step checks each representative fixture only uses fields that can bind into that payload record.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertFixtureRawPayloadKeysBindToPayloadRecord(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 20 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 107 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 36: Cover TEXT and INTERACTIVE fixtures for every provider adapter**

Make the fixture matrix a reusable pattern for both free-text and button/action-style replies across provider slices:

- `ConversationAdapterContractMatrixTest.contractFixturesCoverTextAndInteractiveMessageTypesForEveryProviderAdapter()` groups indexed fixtures by adapter key and requires each provider adapter to cover both `TEXT` and `INTERACTIVE` expected message types.
- Added `social-dm-quick-reply.json` to cover the Social DM interactive quick reply path.
- Added `web-chat-text.json` to cover the Web Chat text path.
- Added `rcs-text.json` to cover the RCS text path.
- `index.json` now lists eight provider fixtures: WhatsApp text/interactive, Social DM text/interactive, Web Chat text/interactive, and RCS text/interactive.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `contractFixturesCoverTextAndInteractiveMessageTypesForEveryProviderAdapter` failed because RCS lacked `TEXT`, Social DM lacked `INTERACTIVE`, and Web Chat lacked `TEXT` fixture coverage.

GREEN result: `ConversationAdapterContractMatrixTest` ran 24 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 111 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 37: Require fixture raw payload shape to match expected message type**

Prevent future provider fixtures from satisfying TEXT/INTERACTIVE coverage with the wrong raw input shape:

- `ConversationAdapterContractMatrixTest.contractFixturesMatchRawPayloadShapeToExpectedMessageType()` checks every indexed fixture through the fixture-definition support path.
- `ConversationAdapterContractSupport.assertRawPayloadShapeMatchesExpectedMessageType(...)` requires `TEXT` fixtures to declare nonblank `rawPayload.text`.
- The same support assertion requires `INTERACTIVE` fixtures to carry at least one concrete interaction marker: `interactiveReplyId`, `interactiveReplyTitle`, `quickReplyPayload`, `quickReplyTitle`, `actionId`, `actionLabel`, `suggestionReplyId`, or `suggestionText`.
- Assertion descriptions include the fixture resource name so a mismatched fixture shape points directly to the JSON file.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertRawPayloadShapeMatchesExpectedMessageType(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 25 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 112 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 38: Bind fixture expected routing fields to raw payload input**

Prevent provider fixtures from using self-consistent expected values that no longer represent the raw callback payload:

- `ConversationAdapterContractMatrixTest.contractFixtureExpectedRoutingFieldsBindToRawPayload()` checks every indexed fixture through reusable support.
- `ConversationAdapterContractSupport.assertExpectedRoutingFieldsBindToRawPayload(...)` requires `expectedChannel` to match the normalized `adapterKey`.
- The same gate requires `expectedProvider` to match normalized `rawPayload.provider`, and requires `expectedExternalMessageId`/`expectedEventId` to match the raw payload routing values exactly.
- Assertion descriptions include the fixture resource name, so a copied or stale expected field points directly to the JSON file.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertExpectedRoutingFieldsBindToRawPayload(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 26 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 113 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 39: Assert provider fixture expected text content**

Extend the fixture-first matrix from routing metadata into normalized message content so future provider slices prove what text enters the conversation session:

- `ConversationAdapterContractMatrixTest.contractFixturesDeclareExpectedTextContent()` requires every indexed fixture to declare nonblank `expectedText`.
- `ConversationAdapterContractMatrixTest.contractFixtureExpectedTextBindsToRawPayloadContent()` requires the expected text to bind back to `rawPayload.text` for `TEXT` fixtures, or to the first concrete interactive display field for `INTERACTIVE` fixtures.
- `ConversationAdapterContractSupport.RawContractCase` and `FixtureDefinition` now carry `expectedText`, and `assertRawIngressContract(...)` compares it with `ConversationIngressReq.text()`.
- All eight indexed fixtures now declare `expectedText`, proving text and button/action-title normalization for WhatsApp, Social DM, Web Chat, and RCS.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation first failed because `FixtureDefinition.expectedText()` and `ConversationAdapterContractSupport.assertExpectedTextBindsToRawPayloadContent(...)` did not exist. After adding support, the same gate ran 28 tests and failed because every indexed fixture had `expectedText=null`, while dynamic ingress cases produced `"hello"` or `"Book a demo"`.

GREEN result: `ConversationAdapterContractMatrixTest` ran 28 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 115 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 40: Make fixture expected attributes exact**

Prevent provider fixture contracts from passing while adapter output carries undeclared normalized attributes:

- `ConversationAdapterContractSupport.assertRawIngressContract(...)` now compares `ConversationIngressReq.attributes()` with `expectedAttributes` using an exact map assertion.
- The indexed fixtures now declare the complete adapter output attribute set, including interaction titles/labels, Social DM `pageId`/`locale`, and RCS `agentId`/`suggestionText`.
- This turns fixture attributes from a loose subset check into a full output contract while preserving the existing dynamic fixture display names for failure traceability.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `providerAdaptersSatisfyRawPayloadIngressContract` failed for five fixture runs because expected attributes omitted `interactiveReplyTitle`, Social DM `pageId`/`locale`/`quickReplyTitle`, Web Chat `actionLabel`, and RCS `agentId`/`suggestionText`.

GREEN result: `ConversationAdapterContractMatrixTest` ran 28 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 115 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 41: Centralize provider adapter normalization helpers**

Reduce future provider adapter implementation time by moving repeated normalization and attribute boilerplate into one small production helper:

- `ConversationReplyAdapterSupport` centralizes provider normalization, text fallback, optional text attribute trimming, and adapter attribute-map assembly.
- `ConversationReplyAdapterSupportTest` covers provider defaulting/uppercasing, primary-vs-fallback text selection, and optional attribute trimming/skipping.
- `WhatsAppConversationReplyAdapter`, `SocialDmConversationReplyAdapter`, `WebChatConversationReplyAdapter`, and `RcsConversationReplyAdapter` now reuse the shared helper while preserving their provider-specific payload records and attribute keys.
- `scripts/verify-conversation-focus.sh` now includes `ConversationReplyAdapterSupportTest` in the focused backend selector and test-source compilation list.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused test-source compilation failed because `ConversationReplyAdapterSupport` did not exist.

GREEN result: backend focused gate ran 118 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 118 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 42: Restrict adapter fixtures to supported message types**

Keep the fixture-first provider acceleration aligned with the PRD message-type vocabulary:

- `ConversationAdapterContractMatrixTest.contractFixturesUseSupportedMessageTypes()` checks every indexed fixture against the shared supported type set.
- `ConversationAdapterContractSupport.supportedMessageTypes()` defines the allowed fixture values as `TEXT`, `IMAGE`, `INTERACTIVE`, and `UNKNOWN`, matching the PRD enum.
- This prevents future provider slices from introducing channel-local message types such as `BUTTON` or `QUICK_REPLY` while still allowing future image or unknown-message fixtures.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.supportedMessageTypes()` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 29 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 119 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 43: Bind fixture filenames to adapter keys**

Make future provider fixture additions easier to scan and harder to mis-index:

- `ConversationAdapterContractMatrixTest.contractFixtureResourceNamesStartWithAdapterKeyPrefix()` checks every indexed fixture through reusable support.
- `ConversationAdapterContractSupport.assertFixtureResourceNameStartsWithAdapterKeyPrefix(...)` converts `adapterKey` to kebab-case and requires the JSON filename to start with that prefix, for example `social_dm` -> `social-dm-...json`.
- This keeps fixture files, index entries, and adapter keys aligned before dynamic ingress execution, improving traceability when future provider slices add more fixture variants.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertFixtureResourceNameStartsWithAdapterKeyPrefix(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 30 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 120 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 44: Centralize provider ingress request construction**

Reduce the remaining boilerplate in provider adapters by moving common `ConversationIngressReq` construction into shared production support:

- `ProviderConversationReplyPayload` defines the common provider callback fields used by all provider-style conversation adapters: canvas/version/execution ids, user/provider/external/event ids, text, intent, attributes, and occurrence time.
- WhatsApp, Social DM, Web Chat, and RCS payload records now implement `ProviderConversationReplyPayload`.
- `ConversationReplyAdapterSupport.providerIngress(...)` constructs the normalized `ConversationIngressReq` from the common payload fields, message-type decision, fallback text, and already-built adapter attributes.
- `ConversationReplyAdapterSupportTest.buildsProviderIngressRequestFromCommonPayloadFields()` proves provider normalization, message-type selection, fallback text, routing ids, attributes, and occurrence time through the shared helper.
- The existing provider adapter tests and fixture matrix keep each channel's adapter-specific attributes and text/interactive behavior covered after the refactor.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused test-source compilation failed because `ProviderConversationReplyPayload` did not exist.

GREEN result: backend focused gate ran 121 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 121 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 45: Require provider payload records to use the shared provider payload contract**

Keep future provider adapters compatible with the shared ingress construction helper introduced in Step 44:

- `ConversationAdapterContractMatrixTest.providerAdapterPayloadTypesImplementProviderPayloadContract()` checks every discovered provider adapter payload type.
- `ConversationAdapterContractSupport.assertProviderAdapterPayloadTypeImplementsProviderPayloadContract(...)` reuses the concrete record gate, then requires the payload record to implement `ProviderConversationReplyPayload`.
- This prevents future provider slices from adding a concrete payload record that can bind raw JSON but cannot use `ConversationReplyAdapterSupport.providerIngress(...)`.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterPayloadTypeImplementsProviderPayloadContract(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 31 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 122 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 46: Centralize provider interactive marker message-type selection**

Reduce the remaining provider adapter boilerplate by moving interactive marker detection into the shared provider ingress helper:

- `ConversationReplyAdapterSupport.providerIngress(payload, channel, fallbackText, attributes, interactiveMarkers...)` now derives `INTERACTIVE` vs `TEXT` from the marker fields and delegates to the existing common ingress constructor.
- `ConversationReplyAdapterSupportTest.buildsProviderIngressRequestFromInteractiveMarkers()` proves blank primary text can use the fallback display text and produce an `INTERACTIVE` ingress request from marker values.
- WhatsApp, Social DM, Web Chat, and RCS adapters now pass their channel-specific reply/action marker fields to the shared helper instead of each hand-rolling a local `hasAnyText(...)` boolean before constructing ingress.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused test-source compilation failed because `ConversationReplyAdapterSupport.providerIngress(...)` did not have a marker-driven overload.

GREEN result: backend focused gate ran 123 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 123 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 47: Bind expected adapter attributes to representative raw payloads**

Tighten the fixture-first quality gate so expected normalized attributes cannot drift away from the raw provider sample:

- `ConversationAdapterContractMatrixTest.contractFixtureExpectedAttributesBindToRawPayload()` checks every indexed fixture.
- `ConversationAdapterContractSupport.assertExpectedAttributesBindToRawPayload(...)` skips the synthetic `adapter` attribute, then requires every other expected attribute to bind to either a same-named top-level raw payload field or a same-named `rawPayload.attributes` entry.
- String attribute comparisons trim both sides, matching the adapter helper's optional text attribute normalization while still catching hardcoded or misspelled attribute values.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertExpectedAttributesBindToRawPayload(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 32 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 124 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 48: Bind adapter output user id to raw payload user id**

Close the remaining routing gap in the raw ingress contract: `userId` must come from the representative raw payload, not just be any nonblank string:

- `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsUserIdThatDoesNotBindToRawPayload()` adds a negative synthetic adapter that hardcodes `userId` while preserving every other expected routing field.
- `ConversationAdapterContractSupport.assertRawIngressContract(...)` now requires the captured ingress request `userId` to equal `rawPayload.userId`.
- Existing provider fixtures continue to pass, proving WhatsApp, Social DM, Web Chat, and RCS bind customer identity from raw provider samples while the matrix catches future hardcoded identity regressions.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsUserIdThatDoesNotBindToRawPayload()` failed because `assertRawIngressContract(...)` did not throw for a hardcoded adapter `userId`.

GREEN result: `ConversationAdapterContractMatrixTest` ran 33 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 125 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 49: Centralize provider adapter metadata and null-guard boilerplate**

Reduce the repeated provider adapter shell so future channel slices only implement channel-specific ingress mapping:

- `AbstractProviderConversationReplyAdapter<T extends ProviderConversationReplyPayload>` owns stable `adapterKey()`, declared `payloadType()`, and the missing-payload rejection before delegating to `toProviderIngress(...)`.
- WhatsApp, Social DM, Web Chat, and RCS adapters now extend the shared base and only build channel-specific attributes, fallback text, and interactive markers.
- `AbstractProviderConversationReplyAdapterTest` proves adapter metadata exposure, declared payload type, missing-payload rejection before mapping, and delegation for non-null payloads.
- `ConversationAdapterContractMatrixTest.providerAdaptersUseSharedProviderBaseClass()` requires every discovered provider adapter to use the shared base class, keeping future provider slices on the accelerated implementation pattern.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterUsesSharedProviderBaseClass(...)` did not exist.

GREEN result: backend focused gate ran 128 tests with 0 failures, 0 errors, 0 skipped.

- [x] **Step 50: Isolate the focused verifier from unrelated main-source worktrees**

Keep the P2-080 regression gate fast and usable even when unrelated workstreams have dirty or incomplete main-source changes:

- `scripts/verify-conversation-focus.sh` no longer runs full `mvn -pl canvas-engine -DskipTests compile`, which can fail on non-conversation source files before the focused gate starts.
- The script now copies main/test resources, builds the test classpath, and uses Java 21 `javac --release 21 -sourcepath canvas-engine/src/main/java:canvas-engine/src/test/java` to compile only the configured focused backend test sources and the main sources they reference into `target/test-classes`.
- `AbstractProviderConversationReplyAdapterTest` is included in the focused backend selector and source list, so the shared provider base class remains covered by the same gate.
- This preserves focused conversation compilation while avoiding false negatives from unrelated BI, warehouse, audience, or AI source files in a dirty worktree.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: the old verifier failed during full main-source compilation with non-conversation BI/warehouse/audience/AI compile errors before focused conversation tests could run.

GREEN result: backend focused gate ran 128 tests with 0 failures, 0 errors, 0 skipped after switching to sourcepath-focused compilation.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 128 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 51: Move provider ingress construction into the shared adapter template**

Reduce provider adapter implementation to declarative channel-specific mapping hooks:

- `AbstractProviderConversationReplyAdapter.toIngress(...)` now owns adapter attribute assembly, fallback text handling, interactive-marker message-type selection, and normalized provider ingress construction.
- Provider adapters only override `providerAttributes(...)`, `fallbackText(...)`, and `interactiveMarkers(...)`, so future adapters declare their channel-specific optional fields instead of reconstructing `ConversationIngressReq`.
- `AbstractProviderConversationReplyAdapterTest.buildsProviderIngressFromTemplateMethods()` proves the base class invokes the template hooks, preserves source attributes, adds optional attributes, normalizes provider, selects `INTERACTIVE`, and uses fallback display text.
- WhatsApp, Social DM, Web Chat, and RCS adapters were migrated to the template hooks while keeping their existing focused adapter tests and fixture matrix expectations green.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused test-source compilation failed because `AbstractProviderConversationReplyAdapter` still required `toProviderIngress(...)` and did not expose `providerAttributes(...)`, `fallbackText(...)`, or `interactiveMarkers(...)`.

GREEN result: backend focused gate ran 128 tests with 0 failures, 0 errors, 0 skipped.

- [x] **Step 52: Prevent provider adapters from hand-writing ingress construction**

Keep future provider slices on the shared template path:

- `ConversationAdapterContractMatrixTest.providerAdaptersDoNotDeclareCustomIngressConstruction()` checks every discovered provider adapter class.
- `ConversationAdapterContractSupport.assertProviderAdapterDoesNotDeclareCustomIngressConstruction(...)` fails when a concrete provider adapter declares a non-synthetic method returning `ConversationIngressReq`.
- This prevents future channel slices from reintroducing per-adapter ingress construction after Step 51 centralized it in `AbstractProviderConversationReplyAdapter`.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterDoesNotDeclareCustomIngressConstruction(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 35 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 129 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 53: Make provider adapter mapping constructor-declared**

Collapse the remaining provider adapter boilerplate from override hooks into constructor-provided field mappings:

- `AbstractProviderConversationReplyAdapter` now accepts a list of provider attributes, a fallback-text extractor, and interactive marker extractors in its constructor.
- `providerAttribute(...)` gives provider adapters a compact named-field mapping primitive while keeping trimming and blank filtering in the shared base.
- WhatsApp, Social DM, Web Chat, and RCS adapters now only declare their adapter key, payload type, missing-payload message, optional attributes, fallback text, and interactive markers in the constructor.
- `AbstractProviderConversationReplyAdapterTest.buildsProviderIngressFromConstructorMapping()` proves constructor-declared mappings preserve source attributes, add optional attributes, derive `INTERACTIVE`, and use fallback display text without provider-specific hook methods.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only
```

RED result: focused test-source compilation failed because `AbstractProviderConversationReplyAdapter` did not yet expose a constructor for provider attributes, fallback text, and interactive marker mappings.

GREEN result: backend focused gate ran 130 tests with 0 failures, 0 errors, 0 skipped.

- [x] **Step 54: Enforce constructor-only provider adapter mapping**

Turn the constructor mapping shortcut into a durable quality gate:

- `ConversationAdapterContractMatrixTest.providerAdaptersUseConstructorDeclaredProviderMapping()` checks every discovered provider adapter class.
- `ConversationAdapterContractSupport.assertProviderAdapterUsesConstructorDeclaredProviderMapping(...)` fails if a concrete provider adapter declares any non-synthetic method, keeping real provider adapters as constructor-only mapping declarations.
- `AbstractProviderConversationReplyAdapter` now keeps provider attribute, fallback text, and interactive marker mapping methods private, so they are implementation details rather than extension hooks.
- `AbstractProviderConversationReplyAdapterTest.providerMappingHooksArePrivateImplementationDetails()` prevents the base class from re-exposing override hooks later.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh --backend-only
```

RED result: adapter-contract-only focused compilation first failed because `ConversationAdapterContractSupport.assertProviderAdapterUsesConstructorDeclaredProviderMapping(...)` did not exist; backend verification then failed because the provider mapping methods were still protected override hooks.

GREEN result: `ConversationAdapterContractMatrixTest` ran 36 tests with 0 failures, 0 errors, 0 skipped; backend focused gate ran 132 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 132 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 55: Bind provider adapter and payload names to adapter keys**

Keep provider slices traceable across fixtures, Java classes, and payload records:

- `ConversationAdapterContractMatrixTest.providerAdapterClassAndPayloadNamesMatchAdapterKeys()` checks every discovered provider adapter.
- `ConversationAdapterContractSupport.assertProviderAdapterNamesMatchAdapterKey(...)` strips fixed suffixes and normalizes case/underscores so keys like `SOCIAL_DM` map to `SocialDmConversationReplyAdapter` and `SocialDmConversationReplyPayload`.
- This prevents future provider slices from adding adapter or payload class names that do not visually line up with the adapter key used by fixtures and routes.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterNamesMatchAdapterKey(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 37 tests with 0 failures, 0 errors, 0 skipped.

- [x] **Step 56: Decouple adapter-contract-only support from ingress-service compilation**

Make the fastest provider fixture gate reliable even when unrelated or broader conversation main-source classes are not compiled:

- `ConversationAdapterContractSupport.captureRawIngress(...)` now resolves the adapter through `ConversationAdapterCatalog`, converts raw fixture maps directly with the adapter's declared `payloadType()`, and invokes `adapter.toIngress(...)`.
- The contract matrix no longer needs `ConversationAdapterHarness`, Mockito, or `ConversationIngressService`, so `--adapter-contract-only` does not pull ingress, WAIT, workspace, or Lombok-heavy DO sourcepath dependencies into its focused `javac` step.
- `ConversationAdapterHarnessTest.harnessConvertsRawPayloadUsingDeclaredPayloadType()` keeps key-based raw harness conversion covered in the broader backend gate.
- Provider-specific raw conversion test names now reference contract support instead of the harness to keep coverage ownership clear.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh --backend-only
```

RED result: `--adapter-contract-only` failed during focused `javac` because `ConversationAdapterContractSupport` still referenced `ConversationIngressService` through `ConversationAdapterHarness`, causing sourcepath to implicitly compile broader ingress, WAIT, workspace, and Lombok-backed data object sources.

GREEN result: `ConversationAdapterContractMatrixTest` ran 37 tests with 0 failures, 0 errors, 0 skipped; backend focused gate ran 134 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 134 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 57: Keep provider adapters stateless and no-arg constructible**

Prevent future provider slices from turning adapter classes into stateful services:

- `ConversationAdapterContractMatrixTest.providerAdaptersAreStatelessNoArgDefinitions()` checks every discovered provider adapter.
- `ConversationAdapterContractSupport.assertProviderAdapterIsStatelessNoArgDefinition(...)` fails if a provider adapter declares fields, has more than one constructor, has a non-public constructor, or requires constructor parameters.
- This keeps provider adapters as pure mapping declarations that remain fast to instantiate in the matrix and keeps provider dependencies in webhook mappers, controllers, connectors, or services instead of the adapter contract surface.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterIsStatelessNoArgDefinition(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 38 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 135 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 58: Cover provider-specific payload components with raw fixtures**

Make future provider payload additions fixture-first:

- `ConversationAdapterContractMatrixTest.contractFixturesCoverEveryProviderSpecificPayloadComponent()` checks every discovered provider adapter.
- `ConversationAdapterContractSupport.assertProviderSpecificPayloadComponentsCoveredByFixtures(...)` removes the common `ProviderConversationReplyPayload` components, then requires the adapter's indexed raw fixtures to include every remaining provider-specific record component at least once.
- This means adding a provider-only payload field such as `threadId`, `actionId`, or `agentId` forces a representative raw fixture update in the same slice, keeping adapter mappings sample-backed instead of relying on direct Java tests alone.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderSpecificPayloadComponentsCoveredByFixtures(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 39 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 136 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 59: Require provider adapter keys to be normalized constants**

Keep adapter keys stable at the source, not only after catalog normalization:

- `ConversationAdapterContractMatrixTest.providerAdapterKeysAreNormalizedConstants()` checks every discovered provider adapter.
- `ConversationAdapterContractSupport.assertProviderAdapterKeyIsNormalizedConstant(...)` requires `adapterKey()` to be nonblank, already trimmed, and uppercase.
- This prevents future provider adapters from returning lowercase or whitespace-padded keys that still pass through catalog normalization but make route, fixture, and failure-message traceability slower.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterKeyIsNormalizedConstant(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 40 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 137 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 60: Require fixture adapter keys to use canonical aliases**

Keep JSON fixtures consistent with the normalized adapter key vocabulary:

- `ConversationAdapterContractMatrixTest.contractFixtureAdapterKeysUseCanonicalAliases()` checks every indexed fixture.
- `ConversationAdapterContractSupport.assertFixtureAdapterKeyUsesCanonicalAlias(...)` requires fixture `adapterKey` to equal `expectedChannel.toLowerCase(Locale.ROOT)`, so `WEB_CHAT` uses `web_chat` and `SOCIAL_DM` uses `social_dm`.
- Production provider adapters still return uppercase constants from Step 59, while fixtures use a stable lowercase snake alias for raw sample readability and route-like input coverage.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertFixtureAdapterKeyUsesCanonicalAlias(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 41 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 138 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 61: Require provider-specific raw fields to appear in expected attributes**

Make fixture samples prove normalized attribute output for channel-only raw fields:

- `ConversationAdapterContractMatrixTest.contractFixtureProviderSpecificRawFieldsBecomeExpectedAttributes()` checks every indexed fixture.
- `ConversationAdapterContractSupport.assertProviderSpecificRawPayloadFieldsBecomeExpectedAttributes(...)` ignores common `ProviderConversationReplyPayload` fields and requires every remaining top-level raw payload key to appear in `expectedAttributes`.
- This prevents future fixtures from carrying provider-only fields that bind into payload records but are silently dropped from normalized ingress attributes.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderSpecificRawPayloadFieldsBecomeExpectedAttributes(...)` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 42 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 139 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 62: Require provider adapters to be Spring components**

Make contract-discovered provider adapters prove runtime catalog registration:

- `ConversationAdapterContractMatrixTest.providerAdaptersAreSpringComponents()` checks every discovered provider adapter class.
- `ConversationAdapterContractSupport.assertProviderAdapterIsSpringComponent(...)` requires each provider adapter class to be annotated with Spring `@Component`.
- This prevents a future provider adapter from passing classpath contract discovery while being absent from the runtime `ConversationAdapterCatalog` bean list.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.assertProviderAdapterIsSpringComponent()` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 43 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 140 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 63: Require discovered provider adapter keys to be unique**

Make adapter discovery fail directly on channel-key collisions:

- `ConversationAdapterContractMatrixTest.providerAdapterKeysAreUnique()` checks the normalized key declarations for every discovered provider adapter class.
- `ConversationAdapterContractSupport.providerAdapterKeyDeclarations()` returns the full discovered declaration list before registry map indexing can collapse duplicates.
- This prevents a future provider adapter from accidentally reusing an existing channel key and only surfacing later as a runtime catalog startup failure.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: focused test-source compilation failed because `ConversationAdapterContractSupport.providerAdapterKeyDeclarations()` did not exist.

GREEN result: `ConversationAdapterContractMatrixTest` ran 44 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 141 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 64: Bind common raw fixture fields to ingress request fields**

Make raw fixture samples prove common session metadata survives adapter conversion:

- `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsCommonFieldsThatDoNotBindToRawPayload()` guards the contract support with a deliberately bad adapter.
- `ConversationAdapterContractSupport.assertRawIngressContract(...)` now checks `canvasId`, `versionId`, `executionId`, and `intent` whenever those fields appear in `rawPayload`.
- This lets future provider fixtures prove shared conversation/session fields without adding repetitive per-provider adapter unit tests.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsCommonFieldsThatDoNotBindToRawPayload` failed because the contract support did not reject a bad adapter that hardcoded common ingress fields.

GREEN result: `ConversationAdapterContractMatrixTest` ran 45 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 142 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 65: Bind raw fixture occurrence time to ingress request occurrence time**

Make fixture samples prove provider reply timestamps survive adapter conversion:

- `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsOccurredAtThatDoesNotBindToRawPayload()` guards the contract support with a deliberately bad adapter.
- `ConversationAdapterContractSupport.assertRawIngressContract(...)` now checks `occurredAt` whenever that field appears in `rawPayload`.
- The adapter-contract `ObjectMapper` registers `JavaTimeModule`, and `whatsapp-text.json` now carries an ISO `occurredAt` fixture value to prove raw JSON conversion into the provider payload record.
- This prevents future provider fixtures from silently dropping provider event time and relying on ingest-time defaults.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsOccurredAtThatDoesNotBindToRawPayload` failed because the contract support did not reject a bad adapter that hardcoded `occurredAt`. Adding `occurredAt` to `whatsapp-text.json` then exposed that the adapter-contract `ObjectMapper` needed `JavaTimeModule` for raw JSON fixture conversion.

GREEN result: `ConversationAdapterContractMatrixTest` ran 46 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 143 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 66: Stabilize focused verifier provider adapter class discovery**

Make the focused verifier deterministic when adapter classes exist in both `target/classes` and `target/test-classes`:

- `scripts/verify-conversation-focus.sh --adapter-contract-only` now explicitly compiles the provider adapter main source set needed by contract discovery, avoiding stale or missing `target/classes` dependency for provider fixture work.
- Full `scripts/verify-conversation-focus.sh` now runs Maven `compile` before focused test-source compilation, leaving Lombok-processed main classes to Maven instead of broad manual javac sourcepath compilation.
- `ConversationAdapterContractSupport.providerAdapterClasses()` de-duplicates discovered adapter class names across classpath resource roots before checking adapter-key uniqueness.
- This keeps the fastest adapter-contract gate independent of broad ingress-service compilation while making full focused verification less sensitive to stale class files.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: full focused verification first failed during test-source compilation with `NoSuchFileException` for stale provider class files in `target/classes`. A too-broad manual javac compile then exposed Lombok-generated getter failures from implicit sourcepath compilation, and the narrowed provider-source compile exposed duplicate adapter keys from the same adapter classes appearing in both `target/classes` and `target/test-classes`.

GREEN result: adapter-contract-only verification ran `ConversationAdapterContractMatrixTest` with 46 tests and 0 failures, 0 errors, 0 skipped; full focused verification ran 143 backend tests with 0 failures, 0 errors, 0 skipped and 6 frontend test files with 27 tests and 0 failures.

- [x] **Step 67: Require every provider to fixture common session metadata**

Make each provider adapter prove common session metadata, not only the first WhatsApp fixture:

- `ConversationAdapterContractMatrixTest.contractFixturesCoverCommonSessionFieldsForEveryProviderAdapter()` checks every discovered provider adapter.
- `ConversationAdapterContractSupport.assertCommonSessionFieldsCoveredByFixtures(...)` requires each provider's indexed raw fixtures to cover `canvasId`, `versionId`, `executionId`, `intent`, and `occurredAt` at least once.
- `social-dm-text.json`, `web-chat-text.json`, and `rcs-text.json` now include those common session fields, so Step 64 and Step 65 validate actual ingress binding for every provider family.
- This prevents a future provider slice from passing with only channel-specific fixture data while leaving common conversation/session metadata unproven.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `ConversationAdapterContractMatrixTest.contractFixturesCoverCommonSessionFieldsForEveryProviderAdapter` failed for `RCS`, `SOCIAL_DM`, and `WEB_CHAT` because their fixtures did not cover `canvasId`, `versionId`, `executionId`, and `occurredAt`.

GREEN result: `ConversationAdapterContractMatrixTest` ran 47 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 144 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 68: Require exact missing-payload contract messages**

Make missing-payload fixture expectations precise instead of broad substring matches:

- `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsBroadMissingPayloadMessageFixture()` guards the contract support with a deliberately broad fixture value.
- `ConversationAdapterContractSupport.assertRejectsMissingPayload(...)` now requires the adapter exception message to equal the fixture's `missingPayloadMessage`.
- This prevents a future provider fixture from passing with a generic value like `payload` while the adapter actually emits a different diagnostic message.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: `ConversationAdapterContractMatrixTest.rawPayloadIngressContractRejectsBroadMissingPayloadMessageFixture` initially passed under substring matching, proving the broad fixture value was not being rejected.

GREEN result: `ConversationAdapterContractMatrixTest` ran 48 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 145 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 69: Auto-discover provider adapter sources in the focused verifier**

Remove the remaining hardcoded per-provider source list from the fast adapter-contract gate:

- `scripts/verify-conversation-focus.sh` now keeps only the shared adapter support dependencies fixed and discovers `*ConversationReplyAdapter.java` plus `*ConversationReplyPayload.java` from the conversation domain package at runtime.
- `--adapter-contract-only --dry-run` becomes the quick source-list audit for future provider slices, showing that a newly added adapter and payload will be compiled before the matrix runs.
- The full focused selector no longer references handler test files that are absent from the current source tree, so the P2-080 verifier remains isolated to available conversation, provider, adapter, outbox, security, and connector registry tests.
- This prevents a future provider slice from adding a new adapter that the adapter-contract-only verifier silently misses because the script source list was not updated.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | grep -F "ZzzProbeConversationReplyAdapter.java"
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: with a temporary probe adapter/payload source added, the dry-run grep failed because the hardcoded verifier source list did not include the new provider files.

GREEN result: the same dry-run grep found the temporary probe adapter after dynamic discovery was added; after removing the probe files, `ConversationAdapterContractMatrixTest` ran 48 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh
```

Result: backend focused gate ran 138 tests with 0 failures, 0 errors, 0 skipped; frontend focused gate ran 6 test files and 27 tests with 0 failures.

- [x] **Step 70: Derive focused backend selectors from source lists**

Remove the remaining duplicated focused backend test maintenance from the verifier:

- `scripts/verify-conversation-focus.sh` now derives the Surefire `-Dtest` selector from `FOCUSED_BACKEND_TEST_SOURCES`, using only entries whose class names end in `Test`.
- The verifier preflights every focused backend source and dynamically discovered provider adapter source before dry-run or execution, so missing source paths fail immediately with a clear `backend focused source is missing` message instead of after Maven setup.
- `--adapter-contract-only` uses the same selector derivation, so it still resolves to `ConversationAdapterContractMatrixTest` from the adapter-contract source list.
- This leaves future provider/conversation slices with one backend focused test list to update and makes `--dry-run` a reliable maintenance check before running Maven and frontend tests.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only --dry-run | rg -- '-Dtest=.*ConversationVerifierSelectorProbeTest'
scripts/verify-conversation-focus.sh --backend-only --dry-run
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run
```

RED result: with a temporary probe test source added to `FOCUSED_BACKEND_TEST_SOURCES`, the dry-run selector did not include `ConversationVerifierSelectorProbeTest` because `BACKEND_SELECTOR` was still hardcoded.

GREEN result: the same dry-run grep found the temporary probe after selector derivation was added, and a temporary missing source entry failed dry-run preflight with `backend focused source is missing`. After removing the probes, backend-only dry-run derived the full selector from the real focused source list and adapter-contract-only dry-run derived `ConversationAdapterContractMatrixTest`.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

Result: adapter-contract-only verification ran `ConversationAdapterContractMatrixTest` with 48 tests and 0 failures, 0 errors, 0 skipped; full focused verification ran 138 backend tests with 0 failures, 0 errors, 0 skipped and 6 frontend test files with 27 tests and 0 failures.

- [x] **Step 71: Auto-discover adapter contract fixtures**

Remove the remaining fixture-index maintenance from future provider slices:

- `ConversationAdapterContractSupport.fixtureResources()` now uses deterministic discovery of JSON fixture resources under `conversation/adapter-contracts/` instead of reading `index.json`.
- `ConversationAdapterContractMatrixTest.contractMatrixUsesDiscoveredFixturesAsRawPayloadCases()` checks that dynamic raw payload cases are sourced from those discovered fixtures.
- `ConversationAdapterContractMatrixTest.contractFixtureDiscoveryDefinesEveryFixtureResource()` and `contractFixtureResourcesAreUnique()` keep resource discovery traceable and duplicate-free without a separate index file.
- `scripts/verify-conversation-focus.sh` clears `target/test-classes/conversation/adapter-contracts` before copying test resources, preventing deleted or renamed fixtures from surviving as stale auto-discovered target resources.
- `index.json` is no longer required; adding a provider fixture is now a single JSON-file operation plus the same matrix quality gates.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: adding a temporary valid `whatsapp-auto-discovery-probe.json` fixture without changing `index.json` failed `contractFixtureIndexReferencesEveryFixtureResource`, proving fixture slices still had duplicate maintenance.

GREEN result: after switching to deterministic fixture discovery, the same temporary JSON-only fixture was included by the matrix and `ConversationAdapterContractMatrixTest` ran 49 tests with 0 failures, 0 errors, 0 skipped. After removing the temporary fixture and `index.json`, the verifier initially still ran 49 tests because the stale probe remained in `target/test-classes`; clearing the adapter-contract target resource directory before `resources:testResources` restored the real matrix to 48 tests with 0 failures, 0 errors, 0 skipped.

Focused end-to-end verifier regression:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

Result: adapter-contract-only verification ran `ConversationAdapterContractMatrixTest` with 48 tests and 0 failures, 0 errors, 0 skipped; full focused verification ran 138 backend tests with 0 failures, 0 errors, 0 skipped and 6 frontend test files with 27 tests and 0 failures.

- [x] **Step 72: Retire adapter fixture index as a guarded invariant**

Prevent the old fixture-index file from returning as ignored dead maintenance after Step 71 made fixtures discovery-only:

- `ConversationAdapterContractMatrixTest.contractFixtureIndexFileIsRetired()` now fails if `conversation/adapter-contracts/index.json` is present on the test classpath.
- `ConversationAdapterContractSupport.assertFixtureIndexFileIsAbsent()` gives future fixture slices a direct failure message telling them to add JSON fixtures directly under `conversation/adapter-contracts/`.
- This turns the "single JSON fixture file" acceleration rule into an enforced invariant instead of relying on documentation after `index.json` was removed.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: after adding the matrix test, focused `javac` failed because `ConversationAdapterContractSupport.assertFixtureIndexFileIsAbsent()` did not exist. After implementing the helper, a temporary `index.json` fixture file made `contractFixtureIndexFileIsRetired` fail with the retired-index diagnostic, proving the guard catches reintroduced index maintenance.

GREEN result: after removing the temporary `index.json`, adapter-contract-only verification ran `ConversationAdapterContractMatrixTest` with 49 tests and 0 failures, 0 errors, 0 skipped; full focused verification ran 139 backend tests with 0 failures, 0 errors, 0 skipped and 6 frontend test files with 27 tests and 0 failures.

- [x] **Step 73: Auto-discover conversation focused backend tests**

Remove another verifier maintenance point so future conversation-domain and conversation-controller slices are automatically covered by the full focused gate:

- `scripts/verify-conversation-focus.sh` now appends discovered `domain/conversation/*Test.java` sources to the full backend focused test source list.
- The same verifier discovers `web/*Conversation*Test.java`, so new conversation controller tests are picked up without adding another hand-maintained selector entry.
- The fixed source list remains for non-conversation support and integration tests such as security routes, demo sandbox, WAIT, channel connector, outbox, and delivery dispatch.
- `--adapter-contract-only` deliberately keeps its narrow source list, so provider fixture work still runs only the adapter contract matrix while the full gate expands automatically.
- The derived selector and source preflight from Step 70 now operate on the discovered full backend source list, preventing silent test omission when a future conversation slice adds a test file.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --backend-only --dry-run | rg "ConversationVerifierDiscoveryProbeTest"
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: adding a temporary `ConversationVerifierDiscoveryProbeTest.java` under `domain/conversation` and grepping the backend dry-run output failed because the probe was absent from the manually maintained focused source list.

GREEN result: after adding test-source discovery, the same dry-run included the temporary probe plus the previously omitted conversation AI reply, private-domain, routing, workspace, and conversation web controller tests. After removing the probe, adapter-contract-only verification still ran `ConversationAdapterContractMatrixTest` with 49 tests and 0 failures, 0 errors, 0 skipped; full focused verification ran 182 backend tests with 0 failures, 0 errors, 0 skipped and 6 frontend test files with 27 tests and 0 failures.

- [x] **Step 74: Auto-discover conversation focused frontend tests**

Remove the matching frontend verifier maintenance point so future conversation and demo-sandbox UI tests join the focused gate automatically:

- `scripts/verify-conversation-focus.sh` now appends discovered `src/services/conversation*.test.ts(x)` and `src/services/demoSandbox*.test.ts(x)` files to the frontend focused test list.
- The verifier also discovers `src/pages/conversations/*.test.ts(x)` and `src/pages/demo-sandbox/*.test.ts(x)`, so page-level conversation and demo sandbox tests no longer require script edits.
- The fixed frontend list remains for cross-cutting authoring preset coverage, including node library and canvas insert-node tests.
- Frontend test preflight now fails fast when a resolved focused test path is missing, matching the backend source preflight pattern.
- This preserves the fast focused command while preventing newly added conversation UI tests from sitting outside the P2-080 regression gate.

Focused RED/GREEN verification:

```bash
scripts/verify-conversation-focus.sh --frontend-only --dry-run | rg "conversationVerifierDiscoveryProbe"
scripts/verify-conversation-focus.sh --frontend-only
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: adding a temporary `conversationVerifierDiscoveryProbe.test.ts` under `src/pages/conversations` and grepping the frontend dry-run output failed because the probe was absent from the manually maintained frontend test list.

GREEN result: after adding frontend test discovery, the same dry-run included the temporary probe plus the previously omitted `src/pages/conversations/index.test.tsx` and `src/pages/demo-sandbox/index.test.tsx`. After removing the probe, frontend-only verification ran 8 frontend test files with 30 tests and 0 failures; adapter-contract-only verification still ran `ConversationAdapterContractMatrixTest` with 49 tests and 0 failures, 0 errors, 0 skipped; full focused verification ran 182 backend tests with 0 failures, 0 errors, 0 skipped and 8 frontend test files with 30 tests and 0 failures.

- [x] **Step 75: Add a provider scaffold and stale provider-class cleanup**

Make future provider slices start from contract-compliant files instead of copying an existing adapter by hand:

- `scripts/scaffold-conversation-provider.sh` generates a constructor-only provider adapter, payload record, and discovered TEXT/INTERACTIVE adapter-contract fixtures from an adapter key and raw provider value.
- The scaffold supports provider-specific text attributes and configurable interactive marker fields while defaulting to matrix-recognized `actionId` and `actionLabel` fields.
- Generated files are designed to satisfy the existing adapter matrix gates: uppercase adapter key, lowercase snake fixture alias, kebab fixture filenames, `ProviderConversationReplyPayload`, Spring `@Component`, exact missing-payload message, common session metadata in the text fixture, provider-specific raw fields in expected attributes, and TEXT/INTERACTIVE coverage.
- `scripts/verify-conversation-focus.sh` now also clears stale provider adapter/payload classes from `target/test-classes/org/chovy/canvas/domain/conversation` before focused `javac`, so deleted or renamed provider scaffolds cannot keep polluting matrix discovery after source files are removed.

Focused RED/GREEN verification:

```bash
test -x scripts/scaffold-conversation-provider.sh
scripts/scaffold-conversation-provider.sh --adapter-key ZZZ_PROBE --provider probe_provider --attribute threadId --dry-run
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before the scaffold was added, `test -x scripts/scaffold-conversation-provider.sh` failed. After generating and then deleting a temporary `ZZZ_PROBE` provider scaffold, adapter-contract-only verification failed because stale `ZzzProbeConversationReplyAdapter.class` and `ZzzProbeConversationReplyPayload.class` remained in `target/test-classes` and were still discovered without fixtures.

GREEN result: the scaffold dry-run printed the adapter, payload, text fixture, and interactive fixture paths; generating the temporary `ZZZ_PROBE` provider and running adapter-contract-only verification expanded `ConversationAdapterContractMatrixTest` to 51 tests with 0 failures, 0 errors, 0 skipped. After deleting the temporary provider sources/fixtures, the provider test-class cleanup restored adapter-contract-only verification to 49 tests with 0 failures, 0 errors, 0 skipped; full focused verification ran 182 backend tests with 0 failures, 0 errors, 0 skipped and 8 frontend test files with 30 tests and 0 failures.

- [x] **Step 76: Harden provider scaffold field validation**

Prevent the provider scaffold from generating Java or JSON that is known to fail later:

- `scripts/scaffold-conversation-provider.sh` now rejects provider-specific fields that reuse common payload record components such as `text`, `provider`, `eventId`, `attributes`, or `occurredAt`.
- The scaffold rejects Java keywords and literals for generated record component names, repeated `--attribute` values, and identical interactive id/text field names.
- Empty extra text attributes are supported, so a provider can start with only the default `actionId`/`actionLabel` interactive markers.

Verification:

```bash
scripts/scaffold-conversation-provider.sh --adapter-key BAD_FIELD --provider bad --attribute text --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key BAD_KEYWORD --provider bad --attribute class --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key BAD_INTERACTIVE --provider bad --interactive-id-field action --interactive-text-field action --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key BAD_DUP --provider bad --attribute threadId --attribute threadId --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key EMAIL --provider email --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key ZZZ_EMPTY --provider empty_provider
scripts/verify-conversation-focus.sh --adapter-contract-only
rm backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ZzzEmptyConversationReplyAdapter.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ZzzEmptyConversationReplyPayload.java backend/canvas-engine/src/test/resources/conversation/adapter-contracts/zzz-empty-text.json backend/canvas-engine/src/test/resources/conversation/adapter-contracts/zzz-empty-interactive.json
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: before the hardening, scaffold dry-runs accepted `--attribute text`, `--attribute class`, and duplicate `--attribute threadId` values even though they would generate broken Java or duplicate fixture keys. A provider with no `--attribute` failed under `set -u` with `ATTRIBUTES[@]: unbound variable`, making the default scaffold path unusable.

GREEN result: invalid field dry-runs now fail with targeted diagnostics, the `EMAIL` no-attribute dry-run passes, and a temporary generated `ZZZ_EMPTY` provider expanded `ConversationAdapterContractMatrixTest` to 51 tests with 0 failures, 0 errors, 0 skipped. After deleting that temporary provider, adapter-contract-only verification returned to 49 tests with 0 failures, 0 errors, 0 skipped.

- [x] **Step 77: Print provider scaffold next verification command**

Make the scaffold output carry the next quality gate instead of relying on memory:

- `scripts/scaffold-conversation-provider.sh` now prints `scripts/verify-conversation-focus.sh --adapter-contract-only` after both successful dry-run and generation output.
- The generated file list remains unchanged; the extra line is an operator prompt that keeps provider-slice work on the fast matrix gate immediately after scaffolding.

Verification:

```bash
scripts/scaffold-conversation-provider.sh --adapter-key EMAIL --provider email --dry-run | rg -F "scripts/verify-conversation-focus.sh --adapter-contract-only"
bash -n scripts/scaffold-conversation-provider.sh
scripts/scaffold-conversation-provider.sh --adapter-key EMAIL --provider email --dry-run
```

RED result: before this step, the dry-run output listed generated paths but did not include the adapter-contract-only verifier command.

GREEN result: the dry-run output now includes the exact next verification command and the shell syntax check passes.

- [x] **Step 78: Add provider scaffold preflight to the focused verifier**

Keep the scaffold healthy through the same focused gate that future provider slices already run:

- `scripts/verify-conversation-focus.sh` now requires `scripts/scaffold-conversation-provider.sh` to be executable whenever backend verification runs.
- The verifier dry-run prints `provider-scaffold-syntax` and `provider-scaffold-dry-run` commands before the backend compile/test commands.
- Real backend verification runs `bash -n scripts/scaffold-conversation-provider.sh` plus a no-write `VERIFY_SCAFFOLD` dry-run before Maven work, covering the no-extra-attribute scaffold path without generating files.

Verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "provider-scaffold-dry-run"
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "provider-scaffold-syntax"
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, verifier dry-run output did not include any scaffold preflight command, so scaffold regressions required separate manual checks.

GREEN result: verifier dry-run now exposes both scaffold preflight commands, adapter-contract-only verification runs `ConversationAdapterContractMatrixTest` with 49 tests and 0 failures, and full focused verification passes the backend and frontend conversation gates.

- [x] **Step 79: Print provider scaffold files as repo-relative paths**

Make scaffold output easier to read, review, and paste into follow-up commands:

- `scripts/scaffold-conversation-provider.sh` still writes generated files through absolute internal paths, but dry-run and generation output now list repo-relative file paths.
- This keeps future provider-slice output stable across developer machines and avoids leaking local workspace prefixes into PRD notes or review comments.

Verification:

```bash
scripts/scaffold-conversation-provider.sh --adapter-key EMAIL --provider email --dry-run | rg -F "/Users/photonpay/project/canvas"
scripts/scaffold-conversation-provider.sh --adapter-key EMAIL --provider email --dry-run
bash -n scripts/scaffold-conversation-provider.sh
```

RED result: before this step, scaffold dry-run output listed `/Users/photonpay/project/canvas/...` absolute paths for every generated file.

GREEN result: the absolute-path grep now finds no matches, dry-run output lists `backend/...` repo-relative paths, and the shell syntax check passes.

- [x] **Step 80: Reject ambiguous provider scaffold adapter keys**

Keep generated class names, fixture names, and adapter aliases traceable before files are written:

- `scripts/scaffold-conversation-provider.sh` now rejects adapter keys ending with `_`.
- The scaffold also rejects adapter keys containing consecutive underscores.
- Valid uppercase snake keys with digits, such as `B2B_CHAT`, still dry-run and generate successfully.

Verification:

```bash
scripts/scaffold-conversation-provider.sh --adapter-key BAD__KEY --provider bad --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key BAD_ --provider bad --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key B2B_CHAT --provider b2b --dry-run
bash -n scripts/scaffold-conversation-provider.sh
scripts/scaffold-conversation-provider.sh --adapter-key B2B_CHAT --provider b2b
scripts/verify-conversation-focus.sh --adapter-contract-only
rm backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/B2bChatConversationReplyAdapter.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/B2bChatConversationReplyPayload.java backend/canvas-engine/src/test/resources/conversation/adapter-contracts/b2b-chat-text.json backend/canvas-engine/src/test/resources/conversation/adapter-contracts/b2b-chat-interactive.json
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: before this step, `BAD__KEY` and `BAD_` both passed dry-run, producing collapsed class names and double-hyphen fixture filenames.

GREEN result: the ambiguous keys now fail with targeted diagnostics, `B2B_CHAT` still passes dry-run, a temporary generated `B2B_CHAT` provider expanded `ConversationAdapterContractMatrixTest` to 51 tests with 0 failures, and after deleting it adapter-contract-only verification returned to 49 tests with 0 failures.

- [x] **Step 81: Assert scaffold preflight invariants in the focused verifier**

Make the focused verifier enforce the scaffold rules that future provider slices rely on:

- `scripts/verify-conversation-focus.sh` now checks scaffold dry-run output for repo-relative adapter and fixture paths, the next `--adapter-contract-only` command, and absence of the local workspace absolute prefix.
- The backend verifier preflight now asserts representative invalid scaffold inputs fail: consecutive underscore adapter keys, trailing underscore adapter keys, common payload field reuse, and Java keyword attributes.
- Verifier dry-run output exposes these checks through `provider-scaffold-output-check`, `provider-scaffold-invalid-adapter-key`, `provider-scaffold-trailing-underscore-key`, `provider-scaffold-common-field-rejection`, and `provider-scaffold-java-keyword-rejection`.

Verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "provider-scaffold-invalid-adapter-key"
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "provider-scaffold-output-check"
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "provider-scaffold-common-field-rejection"
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: before this step, verifier dry-run output did not expose scaffold output checks or invalid-input checks, and backend verification only ran a syntax check plus one successful scaffold dry-run.

GREEN result: verifier dry-run now exposes the scaffold output and negative-input checks, the verifier shell syntax check passes, and adapter-contract-only verification runs `ConversationAdapterContractMatrixTest` with 49 tests and 0 failures while executing the stronger scaffold preflight.

- [x] **Step 82: Enforce lowerCamel provider scaffold fields**

Prevent generated payload records and fixture attributes from starting with a naming shape that the Java side accepts but review and fixture traceability should reject:

- `scripts/scaffold-conversation-provider.sh` now requires `--attribute`, `--interactive-id-field`, and `--interactive-text-field` values to be lower camel case after Java identifier validation.
- The verifier scaffold preflight now asserts a representative `ThreadId` field fails before backend verification starts.
- Verifier dry-run output exposes the new invariant through `provider-scaffold-lower-camel-field-rejection`.

Verification:

```bash
scripts/scaffold-conversation-provider.sh --adapter-key BAD_FIELD_NAME --provider bad --attribute ThreadId --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key BAD_FIELD_NAME --provider bad --attribute thread_id --dry-run
scripts/scaffold-conversation-provider.sh --adapter-key GOOD_FIELD_NAME --provider good --attribute threadId --interactive-id-field actionId --interactive-text-field actionLabel --dry-run
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "provider-scaffold-lower-camel-field-rejection"
bash -n scripts/scaffold-conversation-provider.sh
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: before this step, scaffold fields such as `ThreadId` and `thread_id` passed validation even though they produced noisy payload components and fixture keys.

GREEN result: invalid provider-specific field names now fail with targeted lowerCamel diagnostics, a valid `threadId`/`actionId`/`actionLabel` dry-run still passes, and a temporary generated provider expanded the adapter contract matrix to 51 passing tests before cleanup.

- [x] **Step 83: Clear stale provider classes from main and test outputs**

Keep temporary scaffold experiments from surviving in either classpath root after their source and fixture files are deleted:

- `scripts/verify-conversation-focus.sh` now clears stale `*ConversationReplyAdapter.class` and `*ConversationReplyPayload.class` files from both `target/classes` and `target/test-classes` before focused `javac`.
- Verifier dry-run output now shows both `backend-clean-provider-main-classes` and `backend-clean-provider-test-classes`, making the two cleanup roots visible during command review.
- This closes the case where a provider generated for a probe was deleted from source but still discovered from `target/classes`.

Verification:

```bash
find backend/canvas-engine/target/classes backend/canvas-engine/target/test-classes \( -name '*GoodFieldName*' -o -name 'good-field-name-*' \) -print 2>/dev/null
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "backend-clean-provider"
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/resources backend/canvas-engine/target/classes backend/canvas-engine/target/test-classes \( -name '*GoodFieldName*' -o -name 'good-field-name-*' \) -print 2>/dev/null
```

RED result: before this step, deleted probe provider classes were removed from `target/test-classes` only, so stale provider adapters compiled under `target/classes` could still pollute matrix discovery.

GREEN result: adapter-contract-only verification now removes both compiled-output roots before test compilation, `ConversationAdapterContractMatrixTest` returns to 49 tests with 0 failures after deleting the probe provider, and no `GoodFieldName` source/resource/class artifacts remain.

- [x] **Step 84: Keep the full focused verifier off unrelated main compilation**

Make the full conversation gate match its focused contract even when unrelated dirty main-source work is temporarily uncompilable:

- `scripts/verify-conversation-focus.sh` no longer runs full `mvn -pl canvas-engine compile` for the full backend gate; it prepares resources and test classpath, then uses focused `javac` with the existing sourcepath.
- The backend gate clears project classes under `target/classes/org/chovy/canvas` before focused `javac`, preventing stale main classes from masking sourcepath regressions.
- Full mode dynamically adds Lombok-annotated main sources to the focused compile set and runs javac with the Lombok processor explicitly, so sourcepath dependencies using `@Data`, `@Builder`, or `@Slf4j` compile without relying on prior Maven output.
- The source list is written to the verifier temp work directory, `${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/sources.txt`, and passed through a javac argfile; dry-run now prints `backend-focused-source-count` and `backend-focused-sources-file` instead of dumping hundreds of source paths.
- Adapter-contract-only mode stays small: its dry-run source count remains 19 and it does not pull the full Lombok source set.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "backend-focused-source"
scripts/verify-conversation-focus.sh --dry-run | rg -F "backend-focused-source"
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, the full focused verifier ran full Maven main compilation and failed on unrelated `SearchMarketingMutationService` source errors before reaching the conversation tests. A first sourcepath-only attempt also exposed that implicit javac sourcepath dependencies do not reliably receive Lombok annotation processing.

GREEN result: the full focused verifier now prepares resources/classpath, writes the focused source argfile, compiles the focused source set with explicit Lombok processing, runs 188 backend conversation-adjacent tests with 0 failures, and runs 8 frontend files / 30 tests with 0 failures. Adapter-contract-only remains at 49 matrix tests with 0 failures.

- [x] **Step 85: Cache the focused backend test classpath safely**

Remove another repeated Maven call from warm provider and conversation verification runs without hiding dependency drift:

- `scripts/verify-conversation-focus.sh` now reuses `${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/test-classpath.txt` when the file exists, is newer than `backend/pom.xml`, `backend/canvas-engine/pom.xml`, and `backend/canvas-cache-sdk/pom.xml`, and every path listed in the classpath still exists.
- If the cache is missing, stale, empty, or points at a missing dependency, the verifier falls back to `mvn -pl canvas-engine -DincludeScope=test -Dmdep.outputFile=... dependency:build-classpath`.
- Dry-run now exposes the decision as either `backend-classpath-cache: hit ...` or `backend-classpath-refresh: cd backend && ...`.
- Actual verification prints `Using cached backend test classpath: ...` on cache hits, so warm runs make the saved Maven step visible.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "backend-classpath-cache"
scripts/verify-conversation-focus.sh --dry-run | rg -F "backend-classpath-cache"
set -e
file="${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/test-classpath.txt"
tmp="${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/test-classpath.txt.cache-probe"
mv "$file" "$tmp"
trap 'mv "$tmp" "$file"' EXIT
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "backend-classpath-refresh"
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, every backend verifier run executed Maven `dependency:build-classpath`, even when Maven only skipped rewriting the existing classpath file.

GREEN result: warm adapter-contract-only dry-run and full dry-run now report a classpath cache hit; a temporary missing-classpath probe reports the refresh command; adapter-contract-only verification runs 49 matrix tests with 0 failures; full focused verification runs 188 backend tests and 8 frontend files / 30 tests with 0 failures.

- [x] **Step 86: Replace Maven resource phases with focused resource sync**

Remove the remaining repeated Maven resource invocations from warm conversation verification while keeping stale fixture protection:

- `scripts/verify-conversation-focus.sh` now syncs `src/main/resources` into `target/classes` directly through a reviewed top-level resource allowlist instead of running `mvn resources:resources`.
- The verifier fails fast if a new main resource root appears outside that allowlist, preventing an unreviewed resource layout from being copied into `target/classes` incorrectly or deleting compiled classes.
- `src/test/resources` now syncs through reviewed test-resource roots under `target/test-classes`, so deleted adapter fixtures cannot survive warm runs without deleting already-verified compiled classes.
- Warm backend verification now reaches focused `javac` without Maven resource or Maven classpath work when the classpath cache is valid.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "backend-main-resources-sync"
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run | rg -F "backend-test-resources-sync"
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, even warm focused backend runs still executed Maven `resources:resources` and `resources:testResources` before focused compilation.

GREEN result: dry-run now exposes direct main/test resource sync steps, adapter-contract-only verification runs 49 matrix tests with 0 failures, and full focused verification runs 188 backend tests plus 8 frontend files / 30 tests with 0 failures while using the cached backend test classpath.

- [x] **Step 87: Keep full backend warm runs on a Lombok class cache**

Avoid recompiling hundreds of Lombok-backed main sources on every full focused backend run while preserving stale-class and stale-fixture guards:

- `scripts/verify-conversation-focus.sh` now treats `target/test-classes` as the warm compiled-class cache for the full backend gate.
- Test resources sync by reviewed roots, currently `conversation`, instead of removing all of `target/test-classes`, so fixture deletion is still deterministic while compiled classes can stay warm.
- The full backend gate writes Lombok cache evidence under `${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/`: `lombok-target-classes.stamp`, `lombok-target-sources.txt`, and `main-java-target-sources.txt`.
- The cache invalidates when the backend test classpath or backend POM inputs are newer, when the Lombok source list changes, when any Lombok source changes, or when any focused watched main-source root changes.
- On cache miss, the verifier cleans `target/test-classes/org/chovy/canvas/*.class`, recompiles the full Lombok-backed source set, and refreshes the evidence files after successful `javac`.
- On cache hit, the full backend focused source count drops to the provider/test source set, currently 52, while adapter-contract-only remains 19 and continues to avoid the Lombok-heavy full source set.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --dry-run | rg -F "backend-lombok-target-classes-cache"
scripts/verify-conversation-focus.sh --dry-run | rg -F "backend-focused-source-count"
scripts/verify-conversation-focus.sh --backend-only
scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, warm full backend verification still explicitly compiled the full Lombok main source set, and a first external cache attempt was slower because it copied a broad implicit class cache back into `target/test-classes`.

GREEN result: after refreshing the target-class evidence, stable watched-source runs can report `backend-lombok-target-classes-cache: hit ...` and reduce the full backend focused source count to the provider/test set, observed at 52 sources and about 13 seconds for a hot backend-only run. If a watched source such as security, delivery, WAIT, conversation, or demo code is newer than the evidence stamp, the verifier deliberately reports refresh, cleans test classes, recompiles the Lombok-backed source set, and still preserves provider class cleanup plus adapter fixture resource refresh.

- [x] **Step 88: Add a scaffold-only verifier gate**

Speed up scaffold-script iteration without weakening provider quality gates:

- `scripts/verify-conversation-focus.sh --scaffold-preflight-only` now runs only the provider scaffold syntax, representative dry-run output, repo-relative path, next-step command, and invalid-input checks.
- The scaffold-only dry-run prints the same `provider-scaffold-*` commands as the backend verifier dry-run, but exits before Java, Maven, Node, backend resource, or frontend test setup.
- This is an early script-edit gate only; provider adapter or fixture changes still use `--adapter-contract-only`, and integration work still finishes with the full focused gate.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --scaffold-preflight-only --dry-run
scripts/verify-conversation-focus.sh --scaffold-preflight-only
```

RED result: before this step, editing scaffold validation or output rules required running a backend verifier path to exercise the scaffold preflight, even though Java/Maven work is unrelated to shell-script-only feedback.

GREEN result: scaffold preflight can now be run as a standalone fast gate, with dry-run output showing only the scaffold commands and the actual preflight proving all scaffold invariants without touching backend or frontend verification setup.

- [x] **Step 89: Let scaffold run the provider contract gate**

Collapse provider bootstrap into one command when a new generated adapter should be proven immediately:

- `scripts/scaffold-conversation-provider.sh --verify` now runs `scripts/verify-conversation-focus.sh --adapter-contract-only` after successful file generation.
- `--dry-run --verify` remains no-write and prints the verification command it would run, so scaffold output can be reviewed without triggering Java work.
- The verifier scaffold preflight now asserts this dry-run path through `provider-scaffold-verify-dry-run`, keeping the new shortcut covered by the same scaffold-only and backend gates.

Verification:

```bash
bash -n scripts/scaffold-conversation-provider.sh
bash -n scripts/verify-conversation-focus.sh
scripts/scaffold-conversation-provider.sh --adapter-key VERIFY_SCAFFOLD --provider verify_provider --verify --dry-run
scripts/verify-conversation-focus.sh --scaffold-preflight-only
scripts/scaffold-conversation-provider.sh --adapter-key ZZZ_VERIFY --provider zzz --verify
rm -f backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ZzzVerifyConversationReplyAdapter.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ZzzVerifyConversationReplyPayload.java backend/canvas-engine/src/test/resources/conversation/adapter-contracts/zzz-verify-text.json backend/canvas-engine/src/test/resources/conversation/adapter-contracts/zzz-verify-interactive.json
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, provider bootstrap still required a manual second command after scaffold generation, so the fastest provider quality gate could be skipped accidentally even though it was printed as the next step.

GREEN result: scaffold generation can now optionally run the adapter contract matrix immediately, dry-run still avoids writes and Java work, scaffold-only preflight covers the shortcut command shape, and adapter-contract-only/full focused verification return to the baseline after removing the probe provider.

- [x] **Step 90: Add a disposable scaffold contract probe**

Prove scaffold templates end to end without leaving probe files behind:

- `scripts/verify-conversation-focus.sh --scaffold-contract-probe` now generates a disposable `ZZZ_VERIFY_PROBE` provider, runs the scaffold `--verify` path so the generated adapter and fixtures pass `--adapter-contract-only`, then removes the probe source, fixture, and target output files.
- The dry-run prints the probe generation command and cleanup action without touching Java, Maven, backend resources, or frontend tests.
- The probe fails fast if matching repo source or fixture files already exist, preventing accidental deletion of a real provider slice, while stale target output for the deterministic probe is cleaned before and after the run.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --scaffold-contract-probe --dry-run
scripts/verify-conversation-focus.sh --scaffold-contract-probe
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/resources backend/canvas-engine/target/classes backend/canvas-engine/target/test-classes \( -name 'ZzzVerifyProbe*' -o -name 'zzz-verify-probe-*' \) -print
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, proving that scaffold templates still generated code and fixtures accepted by the matrix required a manual probe provider, manual adapter-contract run, and manual cleanup.

GREEN result: the disposable probe now runs the generated provider through 51 temporary matrix tests with 0 failures, removes all `ZzzVerifyProbe` and `zzz-verify-probe-*` artifacts, and the baseline adapter-contract-only/full focused gates return to 49 backend matrix tests and 188 backend plus 30 frontend focused tests.

- [x] **Step 91: Add a fixture-only JSON lint gate**

Catch fixture editing mistakes before the Java adapter matrix starts:

- `scripts/verify-conversation-focus.sh --adapter-fixture-lint-only` now parses discovered adapter contract JSON fixtures with Node standard library only, avoiding Java, Maven, backend resources, and frontend setup.
- The lint checks retired `index.json` absence, required metadata, fixture name uniqueness, file prefix traceability, canonical adapter aliases, normalized expected fields, supported message types, required raw routing keys, routing value usability, expected text binding, expected routing binding, `expectedAttributes.adapter`, expected attribute binding, and provider-specific raw-field coverage.
- `scripts/scaffold-conversation-provider.sh` now prints `--adapter-fixture-lint-only` before `--adapter-contract-only` in normal next steps, while `--verify` still runs the full adapter contract matrix after generation.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
bash -n scripts/scaffold-conversation-provider.sh
scripts/verify-conversation-focus.sh --adapter-fixture-lint-only --dry-run
scripts/verify-conversation-focus.sh --adapter-fixture-lint-only
scripts/scaffold-conversation-provider.sh --adapter-key VERIFY_SCAFFOLD --provider verify_provider --dry-run | rg -F -- "--adapter-fixture-lint-only"
scripts/verify-conversation-focus.sh --scaffold-preflight-only
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, a fixture-only edit had no fast JSON/metadata gate before focused Java compilation and the adapter matrix, so simple JSON syntax or metadata errors consumed the same path as adapter behavior regressions.

GREEN result: fixture JSON edits now have an earlier lint-only gate that completes without Java/Maven/Node package setup, scaffold output points to that gate first, and adapter-contract-only/full focused verification remain the required quality gates for generated provider behavior.

- [x] **Step 92: Add a class-cache fixture contract gate**

Keep fixture-only behavior checks fast when Java sources are unchanged:

- `scripts/verify-conversation-focus.sh --adapter-fixture-contract-only` now runs fixture lint, proves the provider/test class cache is fresh, syncs adapter fixture resources, and runs the adapter contract matrix without focused `javac`.
- The class-cache proof requires each focused provider adapter/payload/test class to exist and be newer than its source and backend POM inputs. Shared main support classes may be fresh in `target/classes`, while provider adapter/payload classes must be fresh in `target/test-classes` because the fixture-only path removes provider classes from `target/classes` before Surefire.
- If any class is missing or stale, the gate fails fast and tells the developer to run `scripts/verify-conversation-focus.sh --adapter-contract-only`, preserving the Java-source quality path instead of silently running against stale classes.
- The fixture-only contract path prunes provider adapter/payload test classes whose source no longer exists and removes provider adapter/payload classes from `target/classes`, so stale scaffold experiments cannot pollute matrix discovery even when focused `javac` is skipped.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-fixture-contract-only --dry-run
scripts/verify-conversation-focus.sh --adapter-fixture-contract-only
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh --adapter-fixture-contract-only --dry-run
scripts/verify-conversation-focus.sh --adapter-fixture-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, a pure fixture behavior check always paid the focused `javac` setup cost even when provider adapter classes and adapter contract tests were already fresh.

GREEN result: when the class cache is stale, fixture-contract-only fails fast and requires adapter-contract-only; after adapter-contract-only refreshes the focused classes, fixture-contract-only reports a class-cache hit, skips focused `javac`, syncs fixtures, and runs the 49-test adapter matrix successfully.

- [x] **Step 93: Fail fixture metadata before backend Java work**

Keep obvious fixture mistakes from paying the backend compile and Surefire cost:

- `scripts/verify-conversation-focus.sh` now runs the Node standard-library fixture lint during backend verification after scaffold preflight and before resource sync, classpath refresh, focused `javac`, or Surefire.
- The dry-run output prints `adapter-fixture-lint` in the backend path, so developers can see that `--adapter-contract-only` and the full focused gate start with the same JSON/metadata guard as the lint-only path.
- This preserves the adapter matrix and full focused gate as the behavior gates while making malformed fixture JSON, missing metadata, alias drift, routing-value drift, expected-text drift, and provider-specific raw-field omissions fail before Java work begins.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --dry-run
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, `--adapter-contract-only` and the full focused backend gate could spend time refreshing backend resources, classpath evidence, focused compilation, and Surefire setup before the standalone fixture lint exposed a fixture metadata error.

GREEN result: backend verification now runs fixture lint immediately after scaffold preflight, dry-run output exposes the early lint step, adapter-contract-only still runs the 49-test matrix, and the full focused gate still runs the backend and frontend regression set.

- [x] **Step 94: Narrow the Lombok cache invalidation surface**

Prevent unrelated backend DAL/Lombok work from repeatedly forcing a heavy conversation gate refresh:

- `scripts/verify-conversation-focus.sh` no longer watches the entire `dal` source directory for the conversation Lombok target-class cache. Instead, watched conversation/config/delivery/WAIT/web sources are scanned for direct `org.chovy.canvas.dal.dataobject.*` and `org.chovy.canvas.dal.mapper.*` imports, and those DAL source files are added to the relevant-source evidence dynamically.
- The cache freshness check now compares the relevant watched source list and source mtimes, not the full repository-wide Lombok source list, so unrelated new Lombok files such as marketing-platform probe rows do not invalidate the conversation hot path.
- When the cache is genuinely stale, the verifier still compiles all discovered Lombok main sources during refresh, preserving generated getters/builders/log fields for implicit javac dependencies while keeping the hot-path invalidation scope narrow.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --dry-run
scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --dry-run
```

RED result: before this step, an unrelated DAL/Lombok source addition under `dal` changed the cached Lombok source-list evidence and made the full conversation dry-run expand to 527 focused sources even though no conversation source changed.

GREEN result: after one successful full gate refresh, dry-run reports `backend-lombok-target-classes-cache: hit` and `backend-focused-source-count: 52` despite the unrelated DAL/Lombok worktree changes still existing.

- [x] **Step 95: Defer full Lombok source discovery on cache hits**

Remove hot-path scanning that is only needed on stale refreshes:

- `scripts/verify-conversation-focus.sh` now checks the Lombok target-class cache against the narrowed relevant-source evidence before discovering the repository-wide Lombok source list.
- On cache hit, the full focused gate skips the expensive full main-source Lombok scan and keeps the provider/test compile set at 52 sources.
- On cache miss, the verifier still discovers every Lombok main source and adds that full set to focused `javac`, preserving generated getters, builders, and log fields for implicit dependencies.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
time scripts/verify-conversation-focus.sh --dry-run
scripts/verify-conversation-focus.sh --adapter-contract-only
scripts/verify-conversation-focus.sh
```

RED result: before this step, even a cache-hit dry-run scanned the full backend main tree for Lombok sources and took about 7.3 seconds on the dirty worktree.

GREEN result: cache-hit dry-run now reports `backend-lombok-target-classes-cache: hit`, keeps `backend-focused-source-count: 52`, and drops to about 1.7 seconds while adapter-contract-only still runs the 49-test matrix.

- [x] **Step 96: Add a provider adapter source compile preflight**

Catch provider Java source errors before paying the adapter matrix startup cost:

- `scripts/verify-conversation-focus.sh --adapter-source-compile-only` now runs scaffold preflight, fixture lint, cached classpath resolution, stale provider adapter/payload class cleanup, and focused `javac` for the provider adapter/payload source set plus adapter contract support/test sources.
- The compile-only path skips backend main/test resource sync, broad main-class cleanup, and Surefire. It is a Java-source preflight only and is not a substitute for `--adapter-contract-only` or the full focused gate.
- Dry-run output shows the 19-source focused compile set and explicitly reports `backend-surefire: skipped for adapter source compile preflight`.

Verification:

```bash
bash -n scripts/verify-conversation-focus.sh
scripts/verify-conversation-focus.sh --adapter-source-compile-only --dry-run
time scripts/verify-conversation-focus.sh --adapter-source-compile-only
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: before this step, a provider Java syntax or type error required running the adapter contract matrix path to get focused `javac` feedback.

GREEN result: adapter-source-compile-only now compiles the 19 provider/contract sources, skips Surefire, and reports `conversation adapter source compile passed`; adapter-contract-only remains the 49-test behavior gate.

- [x] **Step 97: Skip annotation processing in adapter-only javac gates**

Avoid paying Lombok annotation-processor startup cost for the provider adapter contract source set:

- `scripts/verify-conversation-focus.sh` now uses `-proc:none` for `--adapter-source-compile-only` and `--adapter-contract-only`, because the provider adapter/payload plus adapter contract source set is Lombok-free.
- The full focused backend gate still uses the explicit Lombok processor, preserving generated getter, builder, and log member coverage when the broader target-class cache needs refresh.
- Dry-run output shows `-proc:none` for adapter-only compile paths, making the faster compiler mode visible before execution.

Verification:

```bash
scripts/verify-conversation-focus.sh --adapter-source-compile-only --dry-run | rg -F -- "-proc:none"
time scripts/verify-conversation-focus.sh --adapter-source-compile-only
scripts/verify-conversation-focus.sh --adapter-contract-only
```

RED result: before this step, adapter-source-compile-only dry-run showed the Lombok `-processorpath` and `-processor` arguments even though the adapter contract source set does not use Lombok.

GREEN result: adapter-source-compile-only dry-run now shows `-proc:none`, the compile-only gate passes in about 2.0 seconds on the hot path, and adapter-contract-only still runs the 49-test matrix successfully.

- [x] **Step 98: Add frontend drill-down gates**

Speed up frontend-only iteration without weakening the full focused frontend gate:

- `scripts/verify-conversation-focus.sh --frontend-logic-only` runs only the conversation/demo API and presentation-helper tests, skipping TSX page smoke tests and authoring coverage.
- `scripts/verify-conversation-focus.sh --frontend-conversation-only` runs conversation/demo service and page tests, skipping the node palette and insert-node authoring cross-checks.
- `scripts/verify-conversation-focus.sh --frontend-only` and the default full focused gate still run the complete frontend set, including authoring tests.

Verification:

```bash
scripts/verify-conversation-focus.sh --frontend-logic-only --dry-run
scripts/verify-conversation-focus.sh --frontend-conversation-only --dry-run
time scripts/verify-conversation-focus.sh --frontend-logic-only
time scripts/verify-conversation-focus.sh --frontend-conversation-only
```

RED result: before this step, both new frontend options failed with `ERROR: unknown argument`, so frontend API/presentation or page smoke iteration had to use the full frontend focused set or hand-type a Vitest subset.

GREEN result: frontend-logic-only runs 4 files and 14 tests in about 1.5 seconds, frontend-conversation-only runs 6 files and 17 tests in about 3.6 seconds, and the full frontend gate remains the integration check that includes authoring tests.

- [x] **Step 99: Add a backend conversation-domain drill-down gate**

Speed up backend domain iteration without replacing the backend integration gate:

- `scripts/verify-conversation-focus.sh --backend-domain-only` compiles all `domain/conversation` main sources plus discovered `domain/conversation/*Test.java` tests with `-proc:none`.
- The domain-only gate still runs scaffold preflight, fixture lint, cached classpath resolution, test-resource sync, provider stale-class cleanup, and the adapter matrix fixtures that live under the domain test suite.
- The domain-only gate skips backend main-resource sync, broad main-class cleanup, frontend tests, and cross-cutting backend tests for security routes, web controllers, WAIT, channel connector, outbox, and delivery dispatch. `--backend-only` and the default full gate still run those integration checks.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-domain-only --dry-run
time scripts/verify-conversation-focus.sh --backend-domain-only
time scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, `--backend-domain-only` failed with `ERROR: unknown argument`, so domain service/routing/workspace/adapter iteration had to pay the full backend focused gate or manually type a selector.

GREEN result: backend-domain-only reports an 85-source no-processor focused compile, runs 21 domain test classes with 126 tests and 0 failures in about 6.8 seconds, while backend-only remains the 191-test integration gate.

- [x] **Step 100: Add a backend conversation API drill-down gate**

Speed up controller/security route iteration without replacing backend integration coverage:

- `scripts/verify-conversation-focus.sh --backend-api-only` compiles the Lombok-free conversation domain main sources plus conversation/demo controller, demo service, and security route sources with `-proc:none`.
- The API-only gate discovers `web/*Conversation*Test.java`, adds `SecurityConfigRouteTest` and `DemoSandboxControllerTest`, and derives the Surefire selector from that source list.
- The API-only gate skips backend main-resource sync, test-resource sync, broad main-class cleanup, frontend tests, and cross-cutting WAIT/outbox/delivery/channel tests. `--backend-only` and the default full gate still run those integration checks.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-api-only --dry-run
time scripts/verify-conversation-focus.sh --backend-api-only
time scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, `--backend-api-only` failed with `ERROR: unknown argument`, so controller/security route iteration had to pay the full backend focused gate or manually type a selector.

GREEN result: backend-api-only reports an 80-source no-processor focused compile, runs 7 API/security/controller test classes with 49 tests and 0 failures in about 5.1 seconds, while backend-only remains the 191-test integration gate.

- [x] **Step 101: Add a backend WAIT/delivery/channel runtime drill-down gate**

Speed up cross-cutting runtime iteration without replacing the backend integration gate:

- `scripts/verify-conversation-focus.sh --backend-runtime-only` compiles the focused WAIT, delivery outbox, channel connector, and runtime helper source set with the Lombok processor enabled.
- The runtime-only gate runs `DeliveryOutboxConsumerTest`, `WaitEventFilterTest`, `WhatsAppCloudApiConnectorTest`, `ChannelConnectorRegistryTest`, and `ReachDeliveryServiceConnectorDispatchTest`.
- The runtime-only gate skips backend main-resource sync, test-resource sync, broad main-class cleanup, provider adapter class cleanup, frontend tests, API/controller tests, conversation domain tests, and adapter matrix tests. `--backend-only` and the default full gate still run those integration checks together.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-runtime-only --dry-run
time scripts/verify-conversation-focus.sh --backend-runtime-only
time scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, `--backend-runtime-only` failed with `ERROR: unknown argument`, so WAIT, outbox, delivery, and channel connector iteration had to pay the full backend focused gate or manually type a selector.

GREEN result: backend-runtime-only reports a 34-source focused compile with the Lombok processor, runs 5 runtime test classes with 16 tests and 0 failures in about 7.2 seconds, while backend-only remains the 191-test integration gate.

- [x] **Step 102: Add a backend conversation schema drill-down gate**

Speed up migration/schema assertion iteration without replacing backend integration coverage:

- `scripts/verify-conversation-focus.sh --backend-schema-only` discovers and compiles only `domain/conversation/*SchemaTest.java` sources with `-proc:none`.
- The schema-only gate still runs scaffold preflight, fixture lint, cached classpath resolution, and backend main-resource sync so classpath migration reads reflect current SQL files.
- The schema-only gate skips test-resource sync, broad main/test class cleanup, provider adapter class cleanup, frontend tests, API/controller tests, domain service/adapter tests, WAIT/outbox/delivery/channel runtime tests, and the adapter matrix. `--backend-only` and the default full gate still run those integration checks together.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-schema-only --dry-run
time scripts/verify-conversation-focus.sh --backend-schema-only
time scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, `--backend-schema-only` failed with `ERROR: unknown argument`, so migration/schema edits had to pay the full backend focused gate or manually type a selector.

GREEN result: backend-schema-only reports a 5-source no-processor focused compile, runs 5 schema test classes with 5 tests and 0 failures in about 5.1 seconds, while backend-only remains the 191-test integration gate.

- [x] **Step 103: Add a backend conversation service drill-down gate**

Speed up core service iteration without running schema, adapter matrix, API/controller, or runtime suites:

- `scripts/verify-conversation-focus.sh --backend-conversation-services-only` discovers `domain/conversation/*ServiceTest.java`, compiles the Lombok-free `domain/conversation` main source set plus those service tests with `-proc:none`, and derives the Surefire selector from that focused service test list.
- The services-only gate still runs scaffold preflight, fixture lint, and cached classpath resolution so shared verifier prerequisites stay guarded.
- The services-only gate skips backend main-resource sync, test-resource sync, broad main/test class cleanup, provider adapter class cleanup, frontend tests, schema tests, adapter/support tests, API/controller tests, and WAIT/outbox/delivery/channel runtime tests. `--backend-domain-only`, `--backend-only`, and the default full gate still run those broader checks together.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-conversation-services-only --dry-run
time scripts/verify-conversation-focus.sh --backend-conversation-services-only
time scripts/verify-conversation-focus.sh --backend-domain-only
time scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, `--backend-conversation-services-only` failed with `ERROR: unknown argument`, so service-only edits had to pay the full domain/backend focused gate or manually type a selector.

GREEN result: backend-conversation-services-only reports a 70-source no-processor focused compile, runs 6 service test classes with 32 tests and 0 failures in about 4.0 seconds, while backend-domain-only and backend-only remain the broader quality gates.

- [x] **Step 104: Add a backend conversation controller drill-down gate**

Speed up controller request/response iteration without running the security route suite:

- `scripts/verify-conversation-focus.sh --backend-conversation-controllers-only` discovers `web/*Conversation*Test.java`, adds `DemoSandboxControllerTest`, compiles the Lombok-free conversation domain, conversation/demo controllers, and demo sandbox service source set with `-proc:none`, and derives the Surefire selector from that focused controller test list.
- The controllers-only gate still runs scaffold preflight, fixture lint, cached classpath resolution, and stale provider adapter class cleanup because the focused source set includes conversation domain adapter classes.
- The controllers-only gate skips backend main-resource sync, test-resource sync, broad main/test class cleanup, `SecurityConfigRouteTest`, frontend tests, schema-only tests, service-only tests outside controller dependencies, adapter matrix behavior coverage, and WAIT/outbox/delivery/channel runtime tests. `--backend-api-only`, `--backend-only`, and the default full gate still run the security route and broader integration checks together.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-conversation-controllers-only --dry-run
time scripts/verify-conversation-focus.sh --backend-conversation-controllers-only
time scripts/verify-conversation-focus.sh --backend-api-only
time scripts/verify-conversation-focus.sh --backend-only
```

RED result: before this step, `--backend-conversation-controllers-only` failed with `ERROR: unknown argument`, so controller-only edits had to pay the API/security route gate or manually type a selector.

GREEN result: backend-conversation-controllers-only reports a 77-source no-processor focused compile, runs 6 controller test classes with 33 tests and 0 failures in about 4.3 seconds, while backend-api-only remains the broader controller plus security route gate.

- [x] **Step 105: Add a frontend authoring drill-down gate**

Speed up conversation authoring preset iteration without running conversation/demo page tests:

- `scripts/verify-conversation-focus.sh --frontend-authoring-only` runs only the fixed node palette and insert-node authoring tests that cover the conversation WAIT preset path.
- The authoring-only gate skips backend verification, conversation/demo API helper tests, conversation/demo presentation tests, and TSX conversation/demo page tests. `--frontend-only` and the default full gate still run authoring plus conversation/demo frontend checks together.
- The authoring-only gate reuses the same `FRONTEND_AUTHORING_TESTS` list as the full focused frontend gate, keeping authoring test maintenance in one place.

Verification:

```bash
scripts/verify-conversation-focus.sh --frontend-authoring-only --dry-run
time scripts/verify-conversation-focus.sh --frontend-authoring-only
time scripts/verify-conversation-focus.sh --frontend-only
time scripts/verify-conversation-focus.sh
```

RED result: before this step, `--frontend-authoring-only` failed with `ERROR: unknown argument`, so node palette or insert-node authoring edits had to pay the full frontend focused gate or manually type a Vitest file list.

GREEN result: frontend-authoring-only runs 2 frontend test files with 13 tests and 0 failures in about 0.9 seconds, while frontend-only remains the broader 8-file, 30-test frontend gate.

- [x] **Step 106: Add a backend conversation webhook drill-down gate**

Speed up WhatsApp/public/internal webhook iteration without running the full API/security controller suite:

- `scripts/verify-conversation-focus.sh --backend-conversation-webhooks-only` compiles the Lombok-free conversation domain plus `ConversationProviderWebhookController` and `PublicConversationWebhookController` with `-proc:none`, then runs only the WhatsApp payload mapper, webhook security service, internal provider webhook controller, and public webhook controller tests.
- The webhook-only gate still runs scaffold preflight, fixture lint, cached classpath resolution, and stale provider adapter class cleanup because the focused source set includes conversation domain adapter classes.
- The webhook-only gate skips backend main-resource sync, test-resource sync, broad main/test class cleanup, security route tests, non-webhook controller tests, service-only tests outside webhook dependencies, runtime tests, frontend tests, and full integration coverage. `--backend-api-only`, `--backend-domain-only`, `--backend-only`, and the default full gate remain the broader quality gates before integration.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-conversation-webhooks-only --dry-run
time scripts/verify-conversation-focus.sh --backend-conversation-webhooks-only
time scripts/verify-conversation-focus.sh --backend-api-only
time scripts/verify-conversation-focus.sh --backend-domain-only
time scripts/verify-conversation-focus.sh --backend-only
time scripts/verify-conversation-focus.sh
```

RED result: before this step, `--backend-conversation-webhooks-only` failed with `ERROR: unknown argument`, so webhook payload, signature, and provider/public callback edits had to pay the API/domain/backend focused gates or manually type a selector.

GREEN result: backend-conversation-webhooks-only reports a 70-source no-processor focused compile, runs 4 webhook-related test classes with 16 tests and 0 failures in about 3.9 seconds, while backend-api-only, backend-domain-only, backend-only, and the full focused gate remain the broader quality gates.

- [x] **Step 107: Add a backend conversation adapter drill-down gate**

Speed up provider adapter and shared adapter harness iteration without running the full conversation domain suite:

- `scripts/verify-conversation-focus.sh --backend-conversation-adapters-only` compiles the Lombok-free conversation domain source set with `-proc:none`, syncs adapter fixture test resources, and runs only adapter harness/catalog/support/base tests, all provider adapter unit tests, and the adapter contract matrix.
- The adapter-only gate still runs scaffold preflight, fixture lint, cached classpath resolution, fixture test-resource sync, and stale provider adapter class cleanup because adapter behavior depends on discovered JSON fixtures and provider class discovery.
- The adapter-only gate skips backend main-resource sync, broad main/test class cleanup, conversation service tests, schema-only tests outside the adapter matrix, API/controller/webhook tests, WAIT/outbox/delivery/channel runtime tests, frontend tests, and full integration coverage. `--adapter-contract-only`, `--backend-domain-only`, `--backend-only`, and the default full gate remain the surrounding quality gates for narrower contract-only and broader domain/integration coverage.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-conversation-adapters-only --dry-run
time scripts/verify-conversation-focus.sh --backend-conversation-adapters-only
time scripts/verify-conversation-focus.sh --adapter-contract-only
time scripts/verify-conversation-focus.sh --backend-domain-only
time scripts/verify-conversation-focus.sh --backend-only
time scripts/verify-conversation-focus.sh
```

RED result: before this step, `--backend-conversation-adapters-only` failed with `ERROR: unknown argument`, so adapter base/support/catalog/harness or provider adapter unit edits had to choose between the narrower matrix-only gate and the full domain/backend focused gates.

GREEN result: backend-conversation-adapters-only reports a 74-source no-processor focused compile, runs 9 adapter-related test classes with 84 tests and 0 failures in about 4.3 seconds, while adapter-contract-only remains the narrower fixture matrix gate and backend-domain-only remains the broader conversation domain gate.

- [x] **Step 108: Preserve warmed main classes in the adapter contract gate**

Keep the fastest adapter matrix gate from breaking later no-processor backend drill-down gates:

- `scripts/verify-conversation-focus.sh --adapter-contract-only` no longer syncs backend main resources or runs broad `target/classes/org/chovy/canvas` cleanup. It still runs scaffold preflight, fixture lint, cached classpath resolution, fixture test-resource sync, stale provider adapter class cleanup, focused provider/contract `javac`, and the adapter contract matrix.
- This preserves Lombok-generated main target classes warmed by broader gates, so `--backend-domain-only`, `--backend-api-only`, and other `-proc:none` drill-down gates do not accidentally recompile Lombok DAL/runtime sources without annotation processing after an adapter matrix run.
- The adapter contract gate still deletes stale provider adapter/payload classes from main and test outputs before focused compilation, so removed scaffold/provider experiments cannot linger in matrix discovery.

Verification:

```bash
scripts/verify-conversation-focus.sh --adapter-contract-only --dry-run
time scripts/verify-conversation-focus.sh --adapter-contract-only
time scripts/verify-conversation-focus.sh --backend-domain-only
time scripts/verify-conversation-focus.sh --backend-only
time scripts/verify-conversation-focus.sh
```

RED result: before this step, `--adapter-contract-only --dry-run` printed `backend-clean-main-classes`, and running `--adapter-contract-only` before `--backend-domain-only` could leave the no-processor domain gate compiling Lombok-backed DAL/runtime sources without generated getters, setters, builders, or log fields.

GREEN result: adapter-contract-only dry-run now omits `backend-main-resources-sync` and `backend-clean-main-classes`, still prints fixture test-resource sync and provider adapter stale-class cleanup, then adapter-contract-only runs 49 matrix tests with 0 failures and the immediately following backend-domain-only run compiles and runs 126 domain tests with 0 failures.

- [x] **Step 109: Add a backend WhatsApp slice drill-down gate**

Speed up the first real channel template without switching between adapter, webhook, and runtime gates:

- `scripts/verify-conversation-focus.sh --backend-whatsapp-only` compiles the Lombok-free conversation domain, WhatsApp provider/public webhook controllers, and the small WhatsApp Cloud API connector source set with `-proc:none`.
- The WhatsApp-only gate runs the WhatsApp adapter, webhook payload mapper, webhook security service, provider webhook controller, public webhook controller, and Cloud API connector tests. It still runs scaffold preflight, fixture lint, cached classpath resolution, and stale provider adapter class cleanup because the source set includes provider adapter classes.
- The WhatsApp-only gate skips backend main-resource sync, test-resource sync, broad main/test class cleanup, non-WhatsApp adapter tests, non-WhatsApp conversation services, security route tests, non-webhook controllers, WAIT/outbox/reach runtime tests, frontend tests, and full integration coverage. `--backend-conversation-adapters-only`, `--backend-conversation-webhooks-only`, `--backend-runtime-only`, `--backend-only`, and the default full gate remain the surrounding quality gates before integration.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-whatsapp-only --dry-run
time scripts/verify-conversation-focus.sh --backend-whatsapp-only
time scripts/verify-conversation-focus.sh --backend-conversation-webhooks-only
time scripts/verify-conversation-focus.sh --backend-runtime-only
time scripts/verify-conversation-focus.sh --backend-only
time scripts/verify-conversation-focus.sh
```

RED result: before this step, `--backend-whatsapp-only` failed with `ERROR: unknown argument`, so WhatsApp inbound/outbound edits had to manually choose among adapter-only, webhook-only, runtime-only, or broader backend gates.

GREEN result: backend-whatsapp-only reports a 75-source no-processor focused compile, runs 6 WhatsApp-related test classes with 23 tests and 0 failures in about 4.0 seconds, while adapter-only, webhook-only, runtime-only, backend-only, and the full focused gate remain the broader quality gates.

- [x] **Step 110: Add a backend focused source compile preflight**

Speed up the "code is still being wired" phase before paying for Surefire:

- `scripts/verify-conversation-focus.sh --backend-source-compile-only` runs the backend scaffold preflight, adapter fixture lint, cached classpath resolution, focused source discovery, and `javac`, then skips backend Surefire. It is a compile-error drill-down only; backend behavior changes still finish with the relevant focused test gate, `--backend-only`, or the full verifier.
- The flag can stand alone for the full conversation-focused backend source set, or combine with existing backend drill-down flags such as `--backend-domain-only`, `--backend-runtime-only`, `--backend-conversation-webhooks-only`, and `--backend-whatsapp-only` to reuse the same source list while skipping tests during early compile iteration.
- The full backend source preflight compiles the conversation-relevant watched main sources plus focused tests with the Lombok processor and `-implicit:none`; no-processor slices keep `-proc:none -implicit:none`. This catches focused compile errors without refreshing the full repository-wide Lombok cache or implicitly compiling unrelated dirty sourcepath classes.
- The source preflight skips backend main/test resource sync, broad main-class cleanup, and Surefire, while still keeping the scaffold and fixture lint guards at the front.

Verification:

```bash
scripts/verify-conversation-focus.sh --backend-source-compile-only --dry-run
time scripts/verify-conversation-focus.sh --backend-source-compile-only
scripts/verify-conversation-focus.sh --backend-domain-only --backend-source-compile-only --dry-run
time scripts/verify-conversation-focus.sh --backend-domain-only --backend-source-compile-only
scripts/verify-conversation-focus.sh --backend-runtime-only --backend-source-compile-only --dry-run
time scripts/verify-conversation-focus.sh --backend-runtime-only --backend-source-compile-only
time scripts/verify-conversation-focus.sh --backend-domain-only
time scripts/verify-conversation-focus.sh --backend-runtime-only
time scripts/verify-conversation-focus.sh --backend-only
time scripts/verify-conversation-focus.sh
```

RED result: before this step, `--backend-source-compile-only` failed with `ERROR: unknown argument`, so broad backend source compile iteration had to run Surefire or manually choose a narrower gate. A first broad draft that refreshed every Lombok source was also rejected because it compiled unrelated dirty approval sources and broke the focused-worktree isolation guarantee.

GREEN result: backend-source-compile-only dry-run reports a 311-source focused backend compile with the Lombok processor and `-implicit:none`, skips Surefire, and does not print backend resource sync. The standalone compile preflight passes in about 4.9 seconds; domain+compile-only passes in about 2.0 seconds with `-proc:none -implicit:none`; runtime+compile-only passes in about 2.1 seconds with Lombok processor and `-implicit:none`.

## Latest Handoff Verification

Latest focused verification on 2026-06-07:

```bash
scripts/verify-conversation-focus.sh --backend-source-compile-only
scripts/verify-conversation-focus.sh --frontend-conversation-only
scripts/verify-conversation-focus.sh
```

Results:

- `--backend-source-compile-only`: focused backend source compilation passed with Java 21.
- The focused verifier now maps compiled main/test `.class` targets from each Java source's declared `package`, not from the source file path alone. This covers path/package mismatches such as `DemoSandboxControllerTest` declaring `org.chovy.canvas.web` while living under the controller test directory.
- The focused backend gate now asserts every selected backend `*Test` source produced a target test class before Surefire runs, so selector drift or focused `javac` omissions fail early instead of silently skipping a test class.
- `--backend-schema-only`: 5 backend conversation schema tests passed after `ConversationRoutingSchemaTest` was aligned with `MigrationTestSupport.readMigration("scrm_routing_sla")`, avoiding classpath-resource drift for newly added focused migrations.
- `--frontend-conversation-only`: 6 frontend test files passed, 21 tests passed after the AI suggestion status Select test was made explicit about the Ant Design duplicate `aria-label` wrapper/input shape.
- Full `scripts/verify-conversation-focus.sh`: backend Surefire ran 191 tests with 0 failures, 0 errors, and 0 skipped; frontend Vitest ran 8 test files with 34 tests passed.
- Fresh handoff rerun at 2026-06-07 11:56 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` again passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Fresh handoff rerun at 2026-06-07 12:31 Asia/Shanghai: full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Handoff boundary tooling refresh at 2026-06-07 12:52 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 74 P2-080 staging entries, 81 deferred-overlap entries, and 441 staged files with no P2-080 pathspec overlap; full `scripts/verify-conversation-focus.sh` also passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 8 files / 34 tests passed.
- Handoff boundary tooling refresh at 2026-06-07 13:04 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 76 P2-080 staging entries, 81 deferred-overlap entries, and 531 staged files with no P2-080 pathspec overlap.
- Core frontend split verification at 2026-06-07 13:18 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 78 P2-080 staging entries, 81 deferred-overlap entries, and 441 staged files with no P2-080 pathspec overlap; full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 10 files / 39 tests passed.
- Handoff boundary current-state refresh at 2026-06-07 13:23 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 78 P2-080 staging entries, 81 deferred-overlap entries, and 0 staged files.
- Core inspection current-state refresh at 2026-06-07 13:29 Asia/Shanghai: `scripts/verify-p2-080-handoff-boundary.sh` passed with 81 P2-080 staging entries, 81 deferred-overlap entries, and 0 staged files; `scripts/verify-conversation-focus.sh --dry-run` passed; full `scripts/verify-conversation-focus.sh` passed with backend Surefire 191 tests, 0 failures, 0 errors, 0 skipped, and frontend Vitest 11 files / 40 tests passed.

## Handoff Summary

Current project status:

- P2-080 Conversational Session Foundation is the active implementation slice for this handoff.
- The slice is focused on durable conversation sessions, inbound message persistence/idempotency, normalized `CONVERSATION_REPLY` WAIT resume, provider adapter normalization, and a core inspection surface for the session foundation.
- `scripts/verify-conversation-focus.sh` is the authoritative focused gate for this slice. Broad Maven or repository-wide checks can still be blocked by unrelated dirty modules and should not be used to accept or reject P2-080 without separate authorization.
- The relevant P2-080 files are still untracked in the current worktree, so treat unrelated dirty state as user-owned and avoid global cleanup unless explicitly requested.

Completed content:

- Added the P2-080 spec, implementation plan, and index rows.
- Added conversation session/message persistence, schema coverage, ingress service behavior, idempotent inbound message handling, and WAIT resume coverage through `CONVERSATION_REPLY`.
- Added P2-080 conversation controller/API support, frontend core API and presentation helpers, demo sandbox tests, and authoring coverage for the conversation WAIT preset.
- Documented workspace/private-domain/routing/AI-reply files as deferred overlap for P2-082D/D2/K/L instead of treating them as the next P2-080 implementation target.
- Added provider adapter acceleration support: adapter catalog/harness/base helpers, discovered JSON contract fixtures, adapter contract matrix, provider scaffold checks, and local WhatsApp, social DM, web chat, and RCS adapter skeletons.
- Added WhatsApp acceleration bridges that stay within this slice: internal webhook payload mapping, public verification/signature shell, Cloud API outbound connector bridge, and delivery receipt mapping.
- Hardened the focused verifier with drill-down gates, fixture linting, dynamic test discovery, package-aware compiled class mapping, and an assertion that selected backend `*Test` sources compiled before Surefire runs.

Next tasks:

- Keep the next work limited to P2-080 evidence, focused regression coverage, and handoff polish.
- Re-run `scripts/verify-conversation-focus.sh` after any code or verifier change, and use the narrower drill-down gates documented below only while isolating failures.
- Do not expand this handoff into Growth Activity/P2-089, BI/CDP/OLAP/Flink, risk-control, content marketing, DSP/search marketing, or global worktree cleanup unless that scope is explicitly re-approved.
- Before integration, run `scripts/verify-p2-080-handoff-boundary.sh`, use `docs/product-evolution/evidence/p2-080-staging-pathspec.txt` as the P2-080 core staging review list, and use `docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt` as the P2-082D/D2/K/L deferred-overlap list. Inspect shared-file diffs manually before staging; neither list is a blind `git add` instruction.

Requirement evidence audit:

- Conversation persistence: `V270__conversation_session_foundation.sql`, `ConversationSessionDO`, `ConversationMessageDO`, their mappers, and `ConversationSessionSchemaTest` cover session/message tables, idempotency, active-session lookup, recent-session lookup, and session-message lookup.
- Inbound reply normalization and idempotency: `ConversationIngressReq`, `ConversationIngressResp`, `ConversationIngressService`, and `ConversationIngressServiceTest` cover normalized inbound payloads, durable message writes, duplicate detection, session updates, and returned session/message ids.
- WAIT resume path: `ConversationIngressService.EVENT_CODE = "CONVERSATION_REPLY"`, `ConversationIngressServiceTest`, and `WaitEventFilterTest` cover calling `WaitResumeService.resumeEventWaits(...)` with structured attributes and resuming only matching active waits.
- Operator/API surface: `ConversationController`, `ConversationWorkspaceController`, `ConversationPrivateDomainController`, provider/public webhook controllers, and their focused web tests cover ingress, recent sessions, messages, workspace/private-domain views, and WhatsApp bridge delegation.
- Provider acceleration: `ConversationReplyAdapter`, `ConversationAdapterCatalog`, `ConversationAdapterHarness`, provider payload records/adapters, JSON fixtures under `conversation/adapter-contracts`, and `ConversationAdapterContractMatrixTest` cover adapter discovery, raw payload conversion, required routing fields, exact attributes, and provider skeleton drift prevention.
- Frontend workspace and authoring: `conversationApi.ts`, `conversationCoreApi.ts`, conversation presentation helpers, `frontend/src/pages/conversations/index.tsx`, conversation/demo tests, `nodeLibrary.ts`, and insert-node tests cover conversation inspection and the `等待会话回复` WAIT preset.
- Focused regression gate: `scripts/verify-conversation-focus.sh` covers backend conversation schema/domain/API/runtime/adapter/webhook/outbox/security checks and frontend conversation/demo/authoring checks without relying on unrelated repository-wide Maven compilation.

## Verification Commands

Use `scripts/verify-conversation-focus.sh` as the primary fast regression gate for this slice. For provider adapter/fixture-only changes, use `scripts/verify-conversation-focus.sh --adapter-fixture-lint-only` as the earliest JSON/metadata check; use `scripts/verify-conversation-focus.sh --adapter-source-compile-only` while iterating on provider Java source compile errors; use `scripts/verify-conversation-focus.sh --backend-source-compile-only` while iterating on broader conversation backend compile errors before paying for Surefire, optionally combined with backend drill-down flags such as `--backend-domain-only`, `--backend-runtime-only`, `--backend-conversation-webhooks-only`, or `--backend-whatsapp-only`; if only fixtures changed and `--adapter-contract-only` has already refreshed the focused classes, use `scripts/verify-conversation-focus.sh --adapter-fixture-contract-only` for the cached matrix path; run `scripts/verify-conversation-focus.sh --adapter-contract-only` whenever Java sources, payload records, adapter templates, or the class cache changed; use `scripts/verify-conversation-focus.sh --backend-conversation-adapters-only` while iterating on adapter support, base adapter rules, catalog/harness behavior, or provider adapter unit behavior that should run the matrix plus adapter unit tests; use `scripts/verify-conversation-focus.sh --backend-whatsapp-only` while iterating on WhatsApp adapter mapping, internal/public webhook handling, webhook security, or Cloud API connector payload construction; use `scripts/verify-conversation-focus.sh --backend-schema-only` while iterating only on conversation migration/schema assertions; use `scripts/verify-conversation-focus.sh --backend-conversation-services-only` while iterating only on conversation service logic such as ingress, routing, workspace, private-domain sync, AI reply, or webhook security behavior; use `scripts/verify-conversation-focus.sh --backend-conversation-webhooks-only` while iterating only on WhatsApp payload mapping, webhook signature verification, or provider/public webhook controller delegation; use `scripts/verify-conversation-focus.sh --backend-conversation-controllers-only` while iterating only on conversation/demo controller request-response delegation; use `scripts/verify-conversation-focus.sh --backend-domain-only` while iterating on conversation domain services, routing, workspace, AI reply, schemas, webhook security, or adapter tests; use `scripts/verify-conversation-focus.sh --backend-api-only` while iterating on conversation controllers, public/provider webhook controllers, demo sandbox conversation ingress, or security route rules; use `scripts/verify-conversation-focus.sh --backend-runtime-only` while iterating on WAIT resume, delivery outbox worker, reach connector dispatch, WhatsApp Cloud connector, or connector registry behavior; then run `--backend-only` or the full focused gate before integrating broader backend changes because those include web, security, WAIT, delivery, channel, adapter, and domain checks together. For frontend-only work, use `scripts/verify-conversation-focus.sh --frontend-logic-only` while iterating on conversation/demo API and presentation helpers, `scripts/verify-conversation-focus.sh --frontend-conversation-only` when TSX conversation/demo page smoke coverage is needed without authoring tests, and `scripts/verify-conversation-focus.sh --frontend-authoring-only` while iterating only on the conversation WAIT authoring preset, node palette, or insert-node behavior; run `--frontend-only` or the full focused gate before integration because those include conversation/demo and authoring coverage together. Backend verifier paths now run fixture lint before Java work, so the standalone lint command remains the quickest drill-down but is also enforced by the adapter and full gates. Adapter-only javac gates use `-proc:none` because their provider/contract source set is Lombok-free; the backend-schema-only, backend-conversation-adapters-only, backend-whatsapp-only, backend-conversation-services-only, backend-conversation-webhooks-only, backend-conversation-controllers-only, backend-domain-only, and backend-api-only gates also use `-proc:none` for their Lombok-free focused source sets; the backend-runtime-only gate keeps the Lombok processor because its delivery/runtime source set relies on generated builders, getters, setters, and log fields; the backend source compile preflight uses `-implicit:none` even when the Lombok processor is enabled so it does not refresh the full Lombok cache or implicitly compile unrelated dirty sourcepath classes; the full gate still keeps explicit Lombok processing for broader cache refreshes. Adapter-contract-only now preserves warmed main target classes by skipping backend main-resource sync and broad main-class cleanup while still syncing adapter fixture test resources and cleaning stale provider adapter/payload classes; this keeps later no-processor gates from accidentally compiling Lombok-backed dependencies without generated members after a matrix run. The full gate's Lombok cache evidence tracks conversation-relevant watched sources and their direct DAL imports rather than every backend Lombok source, while stale refreshes still compile the full Lombok set for quality; full Lombok source discovery is deferred until that stale refresh path so cache hits avoid repository-wide scanning. Use the focused backend and frontend commands in Step 12, plus the WhatsApp bridge checks in Steps 13, 14, 15, 16, and 17, the authoring preset checks in Step 18, the adapter contract matrix checks in Step 19, the matrix completeness gate in Step 20, the fixture coverage gate in Step 23, the fixture-first matrix gate in Step 24, the fixture metadata gate in Step 26, the adapter-contract-only verifier in Step 27, the raw payload routing key gate in Step 28, the fixture resource display-name gate in Step 29, the usable raw routing value gate in Step 30, the normalized expected field gate in Step 31, the runtime harness idempotency routing guard in Step 32, the concrete provider payload record gate in Step 33, the payload routing component gate in Step 34, the raw-payload-to-record binding gate in Step 35, the provider TEXT/INTERACTIVE fixture coverage gate in Step 36, the raw-payload-shape gate in Step 37, the expected-routing binding gate in Step 38, the expected-text gate in Step 39, the exact-attributes gate in Step 40, the adapter-support helper gate in Step 41, the supported-message-type gate in Step 42, the fixture-filename gate in Step 43, the provider-ingress helper gate in Step 44, the provider-payload-contract gate in Step 45, the marker-driven provider-ingress gate in Step 46, the expected-attributes raw-payload binding gate in Step 47, the raw user-id binding gate in Step 48, the shared provider base-class gate in Step 49, the sourcepath-focused verifier gate in Step 50, the provider template-method gate in Step 51, the no-custom-ingress-construction gate in Step 52, the constructor-declared provider mapping gate in Step 53, the constructor-only adapter mapping gate in Step 54, the adapter-key/name traceability gate in Step 55, the adapter-contract-only sourcepath isolation gate in Step 56, the stateless no-arg adapter gate in Step 57, the provider-specific payload fixture coverage gate in Step 58, the normalized adapter-key gate in Step 59, the fixture adapter-key alias gate in Step 60, the provider-specific raw-field attribute gate in Step 61, the Spring component provider-adapter gate in Step 62, the discovered adapter-key uniqueness gate in Step 63, the common raw-field ingress binding gate in Step 64, the raw occurredAt ingress binding gate in Step 65, the provider adapter discovery verifier stability gate in Step 66, the per-provider common session fixture coverage gate in Step 67, the exact missing-payload message gate in Step 68, the dynamic provider-source verifier gate in Step 69, the source-derived selector verifier gate in Step 70, the auto-discovered fixture verifier gate in Step 71, the retired fixture-index guard in Step 72, the conversation backend test discovery verifier gate in Step 73, the conversation frontend test discovery verifier gate in Step 74, the provider scaffold/stale-class cleanup gate in Step 75, the scaffold field-validation gate in Step 76, the scaffold next-verification prompt gate in Step 77, the verifier scaffold-preflight gate in Step 78, the scaffold repo-relative output gate in Step 79, the scaffold adapter-key validation gate in Step 80, the verifier scaffold-invariant assertion gate in Step 81, the scaffold lowerCamel field gate in Step 82, the main/test stale provider class cleanup gate in Step 83, the full focused sourcepath/Lombok argfile gate in Step 84, the backend test-classpath cache gate in Step 85, the focused resource sync gate in Step 86, the full-backend Lombok target-class cache gate in Step 87, the scaffold-only preflight gate in Step 88, the scaffold `--verify` gate in Step 89, the disposable scaffold contract probe gate in Step 90, the fixture-only lint gate in Step 91, the fixture class-cache contract gate in Step 92, the backend early fixture-lint gate in Step 93, the narrowed Lombok cache-invalidation gate in Step 94, the deferred Lombok discovery gate in Step 95, the adapter source compile preflight gate in Step 96, the adapter-only no-processor javac gate in Step 97, the frontend drill-down gates in Step 98, the backend domain drill-down gate in Step 99, the backend API drill-down gate in Step 100, the backend runtime drill-down gate in Step 101, the backend schema drill-down gate in Step 102, the backend conversation service drill-down gate in Step 103, the backend conversation controller drill-down gate in Step 104, the frontend authoring drill-down gate in Step 105, the backend conversation webhook drill-down gate in Step 106, the backend conversation adapter drill-down gate in Step 107, the adapter-contract warmed-main preservation gate in Step 108, the backend WhatsApp slice drill-down gate in Step 109, and the backend source compile preflight in Step 110 as drill-down commands when a narrower failure needs investigation.

## Acceleration Addendum: Adapter Harness

To accelerate later conversational channels without weakening quality, route provider-specific callbacks through a tiny adapter harness:

- `ConversationReplyAdapter<T>` maps a provider payload into `ConversationIngressReq`.
- `ConversationAdapterCatalog` registers all adapter beans by normalized key and rejects duplicate keys at startup.
- Each adapter declares `payloadType()` so raw internal payloads can be converted to the adapter's typed record before normalization.
- `ConversationAdapterHarness` can route by adapter key, convert raw payloads, validate that the adapter produced a non-null ingress request with `userId`, `channel`, `provider`, `externalMessageId`, and `eventId`, then delegate adapter output to `ConversationIngressService`.
- `ProviderConversationReplyPayload`, `AbstractProviderConversationReplyAdapter`, and `ConversationReplyAdapterSupport` are the production-side adapter helpers for shared provider callback fields, stable adapter metadata, payload typing, missing-payload rejection, provider normalization, text fallback, optional string attributes, adapter attribute maps, interactive marker detection, and common ingress request construction. Future provider adapters declare channel-specific optional attributes, fallback text, and interactive marker fields through the shared base constructor only.
- `POST /canvas/conversations/adapters/{adapterKey}/ingress` is the authenticated internal/operator endpoint for adapter-specific payloads. It is not a public webhook receiver and must stay behind provider-specific signature verification for external callbacks.
- `ingestConversationAdapterReply(adapterKey, payload)` is the frontend service helper for internal/operator adapter payload tests so future channel slices do not duplicate endpoint construction.
- `SandboxConversationReplyAdapter` is the first concrete adapter and backs `/demo-sandboxes/{tenantId}/conversation-replies`.
- `WhatsAppConversationReplyAdapter` is the provider-style skeleton: it maps text and interactive replies into `WHATSAPP` ingress requests.
- `WhatsAppWebhookPayloadMapper` and `POST /canvas/conversations/provider-webhooks/whatsapp` form an authenticated internal bridge from WhatsApp Cloud API webhook-like payloads into the existing `WHATSAPP` adapter.
- `WhatsAppWebhookSecurityService` and `GET/POST /public/conversation-webhooks/{tenantId}/whatsapp` add the first public WhatsApp ingress shell: verify-token challenge handling, exact raw-body `X-Hub-Signature-256` validation, and signed delegation into the same `WHATSAPP` adapter harness. This still deliberately excludes send-side provider integration, template approval sync, and tenant-specific credential persistence.
- `WhatsAppCloudApiConnector` adds the first outbound connector bridge under `WHATSAPP:CLOUD_API`, with template/session-text payload construction and fail-closed credential handling. `ReachDeliveryService.dispatchToProvider` now uses resolved REAL connectors from the outbox worker boundary, so connector-routed outbound delivery preserves existing delivery record, retry, and receipt semantics.
- Signed public WhatsApp webhook handling now also maps Cloud API `statuses[]` into `DeliveryOutboxService.recordReceipt(...)`, giving outbound Cloud API sends a local delivery receipt bridge without changing conversation ingress semantics.
- The canvas editor palette now adds a `等待会话回复` authoring preset that creates a normal `WAIT` node preconfigured for `CONVERSATION_REPLY`, making conversational journeys easier to compose while preserving the governed backend node catalog.
- `scripts/verify-conversation-focus.sh` is the reusable focused verifier for this slice, covering backend conversation schema/ingress/WAIT/provider/adapter/outbox tests and frontend conversation/palette tests with compatible Java and Node runtime selection, plus explicit resource copying and sourcepath-focused backend compilation for adapter JSON fixtures and conversation source dependencies. Its full backend gate avoids full Maven main compilation, clears stale project main classes, writes a focused javac source argfile, and explicitly includes Lombok-annotated main sources with the Lombok processor when the target-class evidence is stale, so unrelated dirty main-source modules do not block the conversation gate. It also reuses a valid generated test classpath when it is newer than the backend POM inputs and all dependency entries still exist, falling back to Maven classpath generation on any stale or missing evidence. Main and test resources are synced directly by the script, with reviewed main-resource and test-resource top-level allowlists; test-resource sync refreshes fixture roots without deleting the warmed compiled classes. The full backend gate stores Lombok target-class evidence in `${TMPDIR:-/tmp}/canvas-conversation-focus/canvas-engine/` and, on cache hit, compiles only the provider/test source set before Surefire. Its `--adapter-fixture-lint-only` mode is the earliest JSON/metadata gate, backend verifier paths run that lint before Java work, and `--adapter-contract-only` remains the source-refreshing provider behavior gate that runs only the adapter contract matrix after focused source/resource preparation. Its `--adapter-fixture-contract-only` mode skips focused `javac` only on a fresh provider/test class cache, while `--scaffold-preflight-only` is the narrow script-edit gate for scaffold validation/output rules before provider files exist. Its `--scaffold-contract-probe` mode generates a disposable provider from the current scaffold template, proves it with adapter-contract-only, and removes the probe source, fixtures, and target output. The verifier dynamically discovers provider adapter and payload source files, conversation domain test sources, conversation web controller test sources, and conversation/demo-sandbox frontend service and page tests, derives Surefire selectors from the focused test source list, preflights listed backend sources and frontend test paths, and clears stale adapter-contract target resources so adding a provider, conversation-domain, or conversation-UI slice does not require duplicate script-list edits or fixture-index edits. The matrix now rejects any reintroduced `index.json`, keeping fixture additions to a single discovered JSON file instead of reviving dead index maintenance.
- `scripts/scaffold-conversation-provider.sh` is the provider-slice starter. It generates the constructor-only adapter, payload record, and TEXT/INTERACTIVE fixture pair in the naming and metadata shape expected by the matrix, rejects invalid or non-lowerCamel provider-specific field names before writing files, then either prints `scripts/verify-conversation-focus.sh --adapter-contract-only` or runs it immediately with `--verify` so the generated provider is proven before webhook/controller work starts. The verifier also clears stale provider adapter/payload classes from both main and test compiled outputs before focused compilation, so removed scaffold experiments cannot linger in matrix discovery.
- `ConversationAdapterContractMatrixTest` is the reusable provider-contract quality gate. New provider slices should add one JSON raw payload fixture under `src/test/resources/conversation/adapter-contracts/` to prove conversion, normalized channel/provider/message type, external/event ids, expected attributes, and exact missing-payload rejection; the matrix's dynamic raw payload cases now come from discovered fixture resources and use direct raw-map-to-payload conversion so adapter-contract-only verification stays independent of ingress-service compilation.
- The matrix now discovers provider adapter classes and fails if a provider adapter lacks a contract case or discovered JSON fixture, requires every provider adapter to cover both `TEXT` and `INTERACTIVE` fixture cases, requires each provider adapter to extend `AbstractProviderConversationReplyAdapter`, fails if a provider adapter declares custom `ConversationIngressReq` construction or any other non-synthetic method, requires provider adapters to be stateless public no-arg definitions, requires provider adapter keys to be uppercase trimmed constants, requires fixture adapter keys to use lowercase snake aliases derived from expected channel, requires adapter class and payload record names to match the adapter key, requires each provider adapter to declare a concrete payload record that implements `ProviderConversationReplyPayload` with routing components, requires discovered raw fixtures to cover every provider-specific payload record component at least once, requires provider-specific top-level raw fixture fields to appear in `expectedAttributes`, checks that every fixture raw payload key binds to that payload record, validates discovered fixture resources are unique, validates required fixture metadata, requires fixture filenames to start with the adapter key prefix, verifies raw payload routing keys and nonblank routing values, requires raw payload shape to match the expected message type, restricts fixture message types to the PRD-supported set, binds expected routing fields to the raw payload and adapter key, binds adapter output `userId` to `rawPayload.userId`, verifies expected text content against raw text or interactive display fields, requires exact expected normalized attributes, binds non-`adapter` expected attributes back to raw payload fields, requires normalized expected channel/provider/message-type fields, and includes the fixture resource path in dynamic test display names, turning the acceleration rule into an automated quality gate with direct fixture traceability.
- `SocialDmConversationReplyAdapter` is the social-DM-style skeleton: it maps platform, page, thread, text, and quick reply payloads into `SOCIAL_DM` ingress requests without adding real social platform webhook handling or API calls.
- `WebChatConversationReplyAdapter` is the web-chat-style skeleton: it maps visitor text and action replies into `WEB_CHAT` ingress requests without adding a real widget, anonymous public hosting, or support inbox.
- `RcsConversationReplyAdapter` is the RCS-style skeleton: it maps text and suggestion replies into `RCS` ingress requests without adding real RCS webhook handling, signature verification, or provider API calls.
- `ConversationAdapterContractSupport` is the test-side acceleration fixture. New adapter slices should use it to capture the normalized ingress request from raw payload conversion and to assert exact missing-payload rejection without rebuilding mock harness plumbing.

This lets WhatsApp, social DM, web chat, and RCS work proceed in parallel as small adapter slices. Each slice should add a payload record, a stateless public no-arg constructor-only adapter implementation declaring its stable key, payload type, optional attributes, fallback text, and interactive markers, plus discovered raw fixtures proving normalized channel/provider/session/message/idempotency attributes and rejection of missing routing fields before any provider webhook controller is added.
