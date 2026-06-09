# Whole Project Code Review - 2026-06-07

## Scope

- Repository: `/Users/photonpay/project/canvas`
- Objective: whole-project code review with business-risk awareness, recording issues before fixes.
- Current focus: current worktree changes, especially backend BI, cache SDK, controllers, frontend BI workbench, and integration/runtime paths.

## Review Method

- Prioritize behavior regressions, permission/tenant isolation, data-loss risks, scheduler/runtime behavior, frontend state/API contract mismatches, and verification gaps.
- Record only evidence-backed issues with file/line references.
- Keep this document append-only during review unless a finding is proven invalid by later inspection.

## Coverage Ledger

This ledger tracks review coverage by repository area. `Covered` means the area has file/line-backed findings, resolution evidence, or explicit no-new-finding notes in this report. Any non-`Covered` status in the table would mean the whole-repository review remains open.

| Area | Status | Evidence |
| --- | --- | --- |
| `backend/canvas-engine` Java/domain/controllers/security/migrations | Covered | F-001 through F-003, F-005 through F-049, F-052 through F-055, F-060 through F-061, and F-066 through F-068 cover engine runtime, tenant/security, BI, CDP, marketing, conversation, migrations, risk-control runtime/governance, risk-adjacent routes, message delivery, event ingestion, user-input paths, and risk-node catalog governance. |
| `backend/canvas-cache-sdk` | Covered | F-004 and F-050 cover cache batch-loader null/exception behavior and tests. |
| `frontend/src` | Covered | F-002 and F-051 cover BI chart state and auth-state handling; F-063 covers the risk UI/API contract; F-064 covers growth-activity filter drift; F-065 covers canvas-editor dry-run subject drift; F-066 covers risk-node catalog/config-panel availability drift; F-067 and F-068 cover message-delivery and event-config UI/API contract drift. Marketing platform, monitoring, content hub, marketing forms, marketing preferences, conversations, search marketing, remaining page routes, services, hooks, auth, and shared types received API-contract/state-mutation review with no additional findings beyond those recorded. |
| `backend/canvas-flink-jobs` | Covered | F-056 covers the risk realtime feature pipeline packaging/deployment gap, F-062 covers the Flink-to-backend feature contract gap, and F-069 covers submitter retry semantics. Remaining SQL assets, registry entries, runner tests, checkpoint reporter behavior, and deployment-facing configuration received a pass with no additional findings beyond F-056, F-062, F-069, and F-070. |
| `deploy`, `ops`, `.github/workflows`, `infrastructure`, `rocketmq`, `wiremock` | Covered | F-056 covers Flink deployment registry drift, F-069 covers Kubernetes Job retry behavior, F-070 covers network policy drift, and F-071 covers release image drift. Helm/static manifests, observability alerts/dashboards, CI workflows, local infrastructure, RocketMQ config, and WireMock fixtures received a pass with no additional findings beyond those recorded. |
| `scripts`, `tools`, `sdk/analytics-web` | Covered | F-057 covers the analytics web SDK/backend ingestion contract, F-058 covers Flyway history verifier drift, F-059 covers stale direct-trigger helper scripts, and F-071 covers release image build drift. Shell syntax checks, all `scripts/*.test.sh`, `tools/perf`, `tools/open-source-growth`, `tools/strategy`, MQ producer tests, and the SDK Vitest suite are recorded in Review Log and Verification Notes. |
| `docs` and generated/planning artifacts | Covered | F-059 covers stale direct-trigger docs/helpers, F-068 covers stale ingestion instructions, F-069 and F-070 cover Flink runbook/deployment drift, F-071 covers stress-test/release image drift, and F-072 covers documentation index/link drift. Architecture, product-evolution, runbook, stress-test, canvas-example, DDD rewrite, program-coordination, and superpowers planning docs received targeted link and contract-drift scans. |
| Root configs, agent scaffolds, local artifacts, and nested worktrees | Covered | F-073 covers root build documentation drift, F-074 covers an orphan top-level test tree, and F-075 covers tracked local/generated artifacts. `AGENTS.md`, `CLAUDE.md`, `README.md`, `.gitignore`, `.dockerignore`, `.bmad-core`, `.claude`, `.superpowers`, `logs`, the stray top-level `canvas-engine/`, ignored `backend/com` class files, and `.worktrees` inventory were reviewed for build/security/coverage impact. Nested `.worktrees` are separate Git worktrees and were inventoried as workspace artifacts rather than treated as the release path for this repository root. |

## Findings

### F-001 - High - Scheduled BI delivery can be blocked by new SUBSCRIBE permission enforcement

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerService.java:201` now passes the scheduler effective role into subscription delivery.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java:236` enforces `SUBSCRIBE` access for manual and scheduled subscription runs.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionService.java:211` keeps explicit `DENY` precedence, then allows `SYSTEM` role `SUBSCRIBE` with audit evidence for scheduler delivery.
- Regression coverage exists at `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java:132`.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ApprovalWorkflowServiceTest,ApprovalControllerTest,LarkApprovalSyncSchedulerTest,ApprovalLarkUserIdentityResolverTest,BiDeliveryRuntimeServiceTest,BiDeliveryAttachmentServiceTest,BiSubscriptionControllerTest,BiDeliverySchedulerServiceTest,BiDatasourceHealthSloSummaryTest,BiQueryControllerTest,BiEmbedResourceControllerTest,BiPermissionServiceTest test` passed with 120 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerService.java:201` now passes the scheduler role into `runSubscription`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java:256` calls `enforceDeliveryAccess(..., ACTION_SUBSCRIBE)` before delivery.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionService.java:511` default-allow only includes `VIEW`, `USE`, `QUERY`, `COMPILE`, and `EXECUTE`, not `SUBSCRIBE`.
- Scheduler defaults are `operator=bi-delivery-scheduler` and `role=SYSTEM` in `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerService.java:80`.

**Business risk:**

Existing scheduled subscriptions and alert deliveries can silently fail after deployment unless every subscribed resource has explicit `SUBSCRIBE` allow rules for the scheduler identity or `SYSTEM` role. This affects automated BI report delivery and alerting, not just manual user-triggered runs.

**Recommendation:**

Define scheduler semantics explicitly. Either use a service-account bypass with auditable system context, add migration/default policy for scheduler `SUBSCRIBE`, or use the subscription owner as the effective principal. Add a regression test where production-like `BiPermissionService` denies by default and scheduled delivery remains expected to run or fails with a surfaced reason.

### F-002 - High - Chart filter/sort designer can persist stale values

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `frontend/src/pages/bi/index.tsx:6015` through `6070` now update the chart filter/sort refs in the same handlers that update React state before calling `applySelectedChartQueryDesigner`.
- Regression coverage exists in `frontend/src/pages/bi/index.test.tsx:769` through `789`, asserting `biApi.saveChartDraft` receives the newly edited filter and sort values.

**Evidence:**

- `frontend/src/pages/bi/index.tsx:5723`, `5738`, `5755`, `5767`, and `5782` update React state for chart filter/sort controls.
- `frontend/src/pages/bi/index.tsx:2903` and following lines build query patches from `chartFilterFieldDraftRef.current`, `chartFilterOperatorDraftRef.current`, `chartFilterValueDraftRef.current`, `chartSortFieldDraftRef.current`, and `chartSortDirectionDraftRef.current`.
- `frontend/src/pages/bi/index.tsx:2944` and following lines use the same refs when saving.
- Those refs are reset from the selected chart in `frontend/src/pages/bi/index.tsx:2143`, but the input `onChange` handlers do not update the refs.

**Business risk:**

The UI can display a new filter/sort choice while the saved chart draft contains older values. Chart authors may publish dashboards with incorrect filters or sort order, causing wrong BI output.

**Recommendation:**

Use one source of truth. Either remove the refs and save from state/current selected chart query, or update refs in every handler before calling `applySelectedChartQueryDesigner`. Add a frontend test that changes filter field/value and verifies `biApi.saveChartDraft` receives the new filter.

### F-003 - Medium - SQL dataset FROM validation rejects common valid read-only SELECT shapes

**Status:** Resolved 2026-06-08

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java:53` now defines `SQL_FROM_TOKEN` as a `FROM` keyword detector, without constraining the source grammar after the token.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java:527` through `545` keeps the single read-only `SELECT`, forbidden-token, tenant-column, and `FROM` presence checks while allowing derived tables and quoted source identifiers.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java:113` through `155` covers SQL datasets backed by a derived table source and a quoted source identifier.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=BiDatasetResourceServiceTest test` passed on 2026-06-08 with 20 tests, 0 failures, 0 errors.

**Business risk:**

Valid BI SQL datasets using derived tables, quoted identifiers, dialect-specific table names, or subqueries can be rejected even when they are single read-only `SELECT`s and include the tenant column. This can block onboarding existing warehouse SQL into the semantic layer.

**Recommendation:**

Replace the regex with a parser-aware read-only validation or loosen the source check to detect `FROM` tokens without constraining the next grammar shape. Add tests for derived-table and quoted-identifier SELECTs if the product supports custom SQL beyond simple table scans.

### F-004 - Medium - Tiered cache batch loader null result causes unhandled NPE

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-cache-sdk/src/main/java/org/chovy/cache/TieredCacheImpl.java:474` through `477` now normalizes a null batch-loader result to `Map.of()`.
- Regression coverage exists at `backend/canvas-cache-sdk/src/test/java/org/chovy/cache/TieredCacheTest.java:316` through `322`, asserting a null batch-loader result returns `Optional.empty()` instead of throwing.

**Evidence:**

- `backend/canvas-cache-sdk/src/main/java/org/chovy/cache/TieredCacheImpl.java:441` assigns `Map<K, V> loaded = batchLoader.apply(List.copyOf(misses));`.
- `backend/canvas-cache-sdk/src/main/java/org/chovy/cache/TieredCacheImpl.java:443` immediately calls `loaded.get(key)` for each miss.
- `backend/canvas-cache-sdk/src/main/java/org/chovy/cache/testing/InMemoryTieredCache.java:162` has the same shape for test cache behavior.
- Existing tests cover a successful map-returning loader at `backend/canvas-cache-sdk/src/test/java/org/chovy/cache/TieredCacheTest.java:299`, but do not cover a null loader result.

**Business risk:**

Any batch loader implementation that returns `null` during downstream failure, fallback, or defensive "no data" handling will throw a `NullPointerException` outside the cache's configured loader failure strategy. This can turn a cache miss into a request failure and bypass stale/empty-value degradation policies.

**Recommendation:**

Normalize a null batch loader result to `Map.of()` or route it through the same loader-failure policy used by single-key `loadFromL3`. Add tests for null map, partial map, and exception-throwing batch loader behavior.

### F-005 - High - BI permission administration and request review endpoints lack admin authorization

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:172` through `181` now gates BI permission rule mutation and request review routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `243`, including operator denial for admin writes and continued operator access for self-service permission requests.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/canvas/bi/permissions/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java:88`, `143`, `257`, and `278` expose resource permission upsert, row permission upsert, permission request review, and column permission upsert using only `currentTenant()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestService.java:139` reviews permission requests without checking reviewer role, resource owner, or separation from `requestedBy`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestService.java:163` grants the requested permission on approval by calling `permissionAdminService.upsertResourcePermission`.

**Business risk:**

Any authenticated tenant user can potentially create or delete BI resource, row, and column permission rules, and can approve a pending permission request into an `ALLOW` grant. A normal operator could approve their own `EXPORT`, `SUBSCRIBE`, `EDIT`, or `PUBLISH` access and bypass BI governance.

**Recommendation:**

Add route-level role restrictions for `/canvas/bi/permissions/**` admin/review operations or service-level checks using `TenantContext.role()`. Keep self-service `POST /requests` available to normal users if desired, but require `TENANT_ADMIN` or an explicit BI resource owner/admin role for rule mutations and reviews. Add security route tests showing `OPERATOR` is denied for admin/review endpoints and allowed only for request submission.

### F-006 - High - CDP write-key management is available to any authenticated tenant user

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:183` through `186` now gates CDP write-key creation and deletion to tenant-admin level roles while leaving event ingestion under controller write-key validation.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `249`, including operator denial for write-key creation and authenticated fallback for read-only listing.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/cdp/write-keys/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java:58` creates a new write key after only `tenantContextResolver.currentOrError()`, and `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java:84` disables an existing write key the same way.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java:88` creates an active key without role or ownership checks, and `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java:125` disables keys based only on tenant ID and row ID.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:102` intentionally permits anonymous `POST /cdp/events/track`, so a newly created key immediately grants public ingestion capability for that tenant.

**Business risk:**

Any authenticated operator in a tenant can mint a long-lived CDP ingestion credential or disable existing ingestion credentials. That can enable unauthorized event injection, contaminate customer profiles and warehouse-derived audiences, or disrupt production tracking pipelines.

**Recommendation:**

Restrict `/cdp/write-keys/**` to tenant administrators or a dedicated CDP integration-admin role at route and/or service level. Keep `/cdp/events/track` anonymous only after write-key validation, and add security tests proving `OPERATOR` cannot create or disable keys while authorized admins can.

### F-007 - High - WhatsApp webhook signatures are not bound to the route tenant

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:123` through `127` permit anonymous GET/POST access to both `/public/conversation-webhooks/**` and `/public/conversations/webhooks/**`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java:94` exposes `POST /{tenantId}/whatsapp`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java:100` validates only the raw body and `X-Hub-Signature-256`, then `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java:107` through `111` ingests the mapped payload under the path `tenantId`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityService.java:24` through `30` use a single configured verify token and app secret, with no tenant, phone number, or WhatsApp Business Account binding.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapper.java:99` through `101` copy `display_phone_number`, `phone_number_id`, and `wa_id` into attributes, but no subsequent check compares `phone_number_id` or entry ID to the route tenant before `ConversationAdapterHarness.ingest`.

**Business risk:**

In a multi-tenant deployment sharing one WhatsApp app secret, any valid WhatsApp webhook body signed for one configured phone number can be replayed to another tenant's `/public/conversation-webhooks/{tenantId}/whatsapp` URL. That can inject inbound conversations, interactive replies, or delivery receipts into the wrong tenant and trigger downstream automation on another customer's data.

**Recommendation:**

Resolve the webhook destination from tenant-owned channel configuration, not from the path alone. After signature verification, validate `phone_number_id` or WhatsApp Business Account ID against the tenant before ingesting messages or receipts. Add a regression test where a signed payload for tenant A's phone number is rejected when posted to tenant B's URL.

### F-008 - High - Warehouse operational and privacy mutation endpoints lack admin authorization

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:193` through `194` now gates high-impact `/warehouse/**` POST routes to tenant-admin level roles while preserving the internal realtime checkpoint exception.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `249` and `SecurityConfigRouteTest.java:286` through `296`, proving operator denial for warehouse backfill, read-only status fallback, and anonymous internal checkpoint pass-through.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/warehouse/**` before the authenticated fallback in `SecurityConfig`, except the public internal checkpoint route in `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:64` and `100`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseController.java:77`, `98`, `141`, and `190` expose manual backfill, aggregation, offline-cycle run, and retention cleanup operations using only `currentTenantId()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java:88`, `106`, `125`, `148`, and `161` expose erasure request creation, proof recording, erasure execution, audience bitmap rebuild, and rebuild automation run using only `currentTenantId()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java:70`, `90`, `110`, `130`, `150`, `171`, and `193` expose table contract upsert, live inspection, and remediation planning using only `currentTenant()`.

**Business risk:**

Any authenticated tenant user can trigger expensive warehouse backfills, aggregation runs, retention cleanup, privacy erasure execution, and governance inspections/remediation planning. These operations affect data freshness, compliance evidence, audience materialization, and infrastructure load, so exposing them to normal operators can cause data loss, privacy workflow corruption, or production cost spikes.

**Recommendation:**

Add route-level role restrictions for high-impact `/warehouse/**` mutations, separating safe read-only status endpoints from operational write/control endpoints. Require `TENANT_ADMIN` or a dedicated data-ops/privacy-admin role for backfill, retention cleanup, privacy erasure execution, contract upserts, and live inspections. Add security tests for `OPERATOR` denial on representative warehouse mutation routes.

### F-009 - High - CDP webhook subscriptions can be created and triggered by any authenticated tenant user

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:187` through `192` now gates CDP webhook subscription POST/PUT/DELETE routes, including test delivery, to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for webhook test delivery.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/cdp/webhooks/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java:91` creates active webhook subscriptions after only `tenantContextResolver.currentOrError()`, and `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java:120`, `143`, `156`, `169`, and `183` update, pause/resume, disable, and rotate secrets the same way.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java:203` exposes manual test delivery, and `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookDispatcherService.java:77` through `87` sends the JSON payload to the subscription callback URL.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionValidator.java:20` validates URL shape and event type presence, but does not perform authorization; the controller stores the callback URL and active status at `WebhookSubscriptionController.java:99` through `103`.

**Business risk:**

Any normal authenticated tenant user can configure a public endpoint to receive tenant CDP event payloads and can trigger test deliveries. Even with internal-address SSRF filtering, this creates an unauthorized data exfiltration and integration-governance bypass path for customer event data.

**Recommendation:**

Restrict `/cdp/webhooks/**` mutation and secret/test-delivery routes to tenant administrators or a dedicated integration-admin role. Consider allowing read-only listing to operators only if product requirements permit it. Add security tests proving `OPERATOR` cannot create/update/rotate/test webhook subscriptions.

### F-010 - High - Search marketing provider mutations can be approved and executed by normal users

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- Route gate now requires tenant-admin level access for `POST /canvas/search-marketing/mutations/*/approve` and `POST /canvas/search-marketing/mutations/*/execute` in `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`.
- `SearchMarketingMutationService.approve` rejects same-actor approval before moving a mutation to `READY`.
- Regression coverage: `SecurityConfigRouteTest.highImpactControlPlaneWritesDenyOperator`, `SecurityConfigRouteTest.highImpactControlPlaneWritesAllowTenantAdmin`, and `SearchMarketingMutationServiceTest.rejectsSelfApprovalForLiveSearchProviderMutation`.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/canvas/search-marketing/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java:382` and `401` expose mutation approval and execution using only `currentTenant()`, which delegates to `tenantContextResolver.currentOrError()` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java:713`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java:197` approves or rejects a mutation without checking approver role or separation from `createdBy`; lines `204` through `214` set `APPROVED`, `READY`, and `approvedBy`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java:234` executes approved mutations; lines `251` through `265` build a provider mutation request and call `gateway.execute(request)`.
- Allowed mutation types include `UPDATE_CAMPAIGN_BUDGET`, `ADD_KEYWORD`, `UPDATE_KEYWORD_BID`, and `PAUSE_KEYWORD` in `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java:37` through `42`.

**Business risk:**

Any authenticated tenant user can propose, approve, and execute search marketing provider changes that can modify ad budgets, keyword bids, and campaign state in external platforms. This bypasses spend governance and four-eyes approval, and can cause direct media spend impact or campaign disruption.

**Recommendation:**

Require an advertising-admin or tenant-admin role for approval and execution, and enforce reviewer separation from the mutation creator for non-dry-run provider writes. Add tests proving an `OPERATOR` cannot approve/execute mutations and that self-approval is rejected for live mutations.

### F-011 - High - Creator collaboration provider mutations can be approved and executed by normal users

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- Route gate now requires tenant-admin level access for `POST /canvas/creator-collaboration/mutations/*/approve` and `POST /canvas/creator-collaboration/mutations/*/execute` in `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`.
- `CreatorProviderMutationService.approve` rejects same-actor approval before moving a mutation to `READY`.
- Regression coverage: `SecurityConfigRouteTest.highImpactControlPlaneWritesDenyOperator`, `SecurityConfigRouteTest.highImpactControlPlaneWritesAllowTenantAdmin`, and `CreatorProviderMutationServiceTest.rejectsSelfApprovalForLiveCreatorProviderMutation`.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/canvas/creator-collaboration/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java:139` and `158` expose provider mutation approval and execution using only `currentTenant()`, which delegates to `tenantContextResolver.currentOrError()` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java:219`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationService.java:154` approves or rejects a mutation without checking approver role or separation from `createdBy`; lines `161` through `171` set `APPROVED`, `READY`, and `approvedBy`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationService.java:185` executes approved mutations; lines `199` through `214` build a provider mutation request and call `gateway.execute(request)`.
- Allowed mutation types include `PUBLISH_BRIEF`, `INVITE_CREATOR`, `GENERATE_AFFILIATE_LINK`, `CREATE_DISCOUNT_CODE`, and `REQUEST_CONTENT_AUTHORIZATION` in `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationService.java:38` through `44`.

**Business risk:**

Any authenticated tenant user can approve and execute creator-platform writes that invite creators, publish briefs, generate affiliate links, create discount codes, or request content authorization. This bypasses collaboration governance and can create unauthorized contractual, financial, or brand-facing actions in external creator platforms.

**Recommendation:**

Require creator-campaign admin or tenant-admin authorization for approval and execution, and enforce reviewer separation for live provider writes. Add security and service tests proving normal operators cannot approve/execute creator provider mutations and cannot approve their own live mutations.

### F-012 - High - Programmatic DSP provider mutations can be approved and executed by normal users

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- Route gate now requires tenant-admin level access for `POST /canvas/programmatic-dsp/mutations/*/approve` and `POST /canvas/programmatic-dsp/mutations/*/execute` in `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`.
- `ProgrammaticDspMutationService.approve` rejects same-actor approval before moving a mutation to `READY`.
- Regression coverage: `SecurityConfigRouteTest.highImpactControlPlaneWritesDenyOperator`, `SecurityConfigRouteTest.highImpactControlPlaneWritesAllowTenantAdmin`, and `ProgrammaticDspMutationServiceTest.rejectsSelfApprovalForLiveProgrammaticDspProviderMutation`.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/canvas/programmatic-dsp/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java:188` and `207` expose provider mutation approval and execution using only `currentTenant()`, which delegates to `tenantContextResolver.currentOrError()` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java:244`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationService.java:162` approves or rejects a mutation without checking approver role or separation from `createdBy`; lines `169` through `179` set `APPROVED`, `READY`, and `approvedBy`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationService.java:193` executes approved mutations; lines `212` through `229` build a provider mutation request and call `gateway.execute(request)`.
- Allowed mutation types include `UPDATE_CAMPAIGN_BUDGET`, `UPDATE_LINE_ITEM_BID`, `UPDATE_LINE_ITEM_BUDGET`, `UPDATE_LINE_ITEM_STATUS`, `ASSIGN_TARGETING`, and `ATTACH_DEAL` in `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationService.java:38` through `47`.

**Business risk:**

Any authenticated tenant user can approve and execute DSP provider writes that affect campaign budgets, line-item bids/budgets/status, targeting, and deal attachment. This can directly change paid media spend, delivery pacing, audience targeting, and supply quality without an authorized media-ops approval path.

**Recommendation:**

Require media-ops admin or tenant-admin authorization for approval and execution, and enforce reviewer separation for live provider writes. Add security and service tests proving normal operators cannot approve/execute DSP provider mutations and cannot approve their own live mutations.

### F-013 - Medium - Legacy runtime manual approval fallback is not tenant-bound

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasManualApprovalDO.java` now maps nullable `tenant_id` for legacy runtime approvals.
- `backend/canvas-engine/src/main/resources/db/migration/V353__manual_approval_tenant_scope.sql` adds `canvas_manual_approval.tenant_id`, backfills it from `canvas_execution`, and adds a tenant/status/timeout index.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java` checks `tenant_id` before legacy approval decisions; historical rows without `tenant_id` must match the authenticated tenant and execution ID from the persisted `ExecutionContext` before the approver check or update runs.
- Regression coverage: `CanvasExecutionManagementControllerTest.approveRejectsLegacyManualApprovalWhenExecutionContextBelongsToAnotherTenant`.
- Verification passed under Java 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=CanvasExecutionManagementControllerTest test` reported 2 tests, 0 failures.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java:65` and `82` expose runtime manual approval approve/reject under `/canvas/execution/{executionId}`.
- The unified approval path first calls `ApprovalWorkflowService.decideTargetTask` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java:109` through `117`, which includes tenant and assignee checks.
- If no unified approval task is found, the controller falls back to legacy `canvas_manual_approval` lookup at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java:123` through `129`, filtering only by `executionId` and `PENDING` status.
- The fallback then checks only whether the current username is present in the JSON approvers list at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java:136` through `145`.
- The legacy data object `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasManualApprovalDO.java:25` through `41` stores `executionId`, `canvasId`, and `approvers`, but no tenant ID; the controller does not resolve `canvasId` or `executionId` back to the authenticated tenant before updating.

**Business risk:**

Legacy pending manual approvals can be approved or rejected across tenant boundaries if an execution ID is known and the attacker has the same username as an approver in another tenant. Runtime manual approvals can resume or stop customer journeys, so this can incorrectly advance or block another tenant's campaign execution.

**Recommendation:**

Bind the legacy fallback to tenant before applying a decision. Since `canvas_manual_approval` has no tenant column, resolve `canvasId` or execution context to the authenticated tenant and include that check before the approver check and conditional update. Add a regression test showing same username in tenant B cannot approve tenant A's legacy manual approval.

### F-014 - High - Execution request replay endpoints lack operator/admin authorization

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:195` through `199` now gates single and batch execution-request replay routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for replay.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` falls through to `.anyExchange().authenticated()` for routes not explicitly matched above it.
- No route rule matches `/canvas/execution-requests/**` before the authenticated fallback in `SecurityConfig`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java:152` exposes single-request replay and `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java:201` exposes batch replay using only `currentTenant()` and `currentUsername()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java:164` checks tenant access for single replay, and `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java:222` applies tenant filtering for batch replay, but neither path checks `TenantContext.role()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java:320` through `325` allow `force=true` to bypass the default `FAILED`/`RETRY` replay-only guard.
- After marking a request pending, `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java:175` through `180` and `231` through `237` immediately publish the request back into the execution queue best-effort.

**Business risk:**

Any authenticated tenant user can replay execution requests, including forced replay of non-failed requests. This can duplicate journey actions such as message sends, coupon grants, segment updates, or external API calls, and can create production load spikes through batch replay.

**Recommendation:**

Restrict `/canvas/execution-requests/**/replay` and batch replay to tenant administrators or a dedicated operations role. Consider requiring an explicit admin-only override for `force=true`, with audit metadata and reason validation. Add security tests proving `OPERATOR` cannot replay or force replay execution requests.

### F-015 - High - AI provider credential and model controls are writable by any authenticated tenant user

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:200` through `205` now gates AI provider create, update, and disable routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for provider creation.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` through `178` only gives explicit role gates to `/admin/**`; `/ai/providers/**` is not listed earlier and therefore falls through to `anyExchange().authenticated()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java:61` through `64`, `90` through `93`, and `104` through `109` expose create, update, and disable operations using only the resolved tenant ID.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiProviderModelRegistryService.java:64` through `84`, `100` through `118`, and `125` through `141` persist provider endpoint, enabled state, model list, and API-key digest/mask without any role or ownership check beyond tenant visibility.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java:133` through `137` resolves runtime model calls from this provider registry and treats disabled providers as runtime failures/fallbacks.

**Business risk:**

Any logged-in tenant user can redirect AI workloads to an arbitrary endpoint, register unapproved models, replace provider metadata, or disable a tenant provider. That can exfiltrate prompts/customer context to an attacker-controlled endpoint, increase cost through unauthorized model use, or break AI-driven canvas execution by disabling providers.

**Recommendation:**

Gate `/ai/providers` write endpoints with tenant-admin or dedicated AI-admin roles, and enforce the same role check in service-layer methods for non-HTTP callers. Keep read/model-list endpoints separate if operators need visibility, and add authorization regression tests for create/update/disable.

### F-016 - High - Channel connector delivery mode can be changed by any authenticated tenant user

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:206` through `210` now gates connector mode, health-test, and fallback validation control routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for connector mode changes.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` through `178` leaves `/channels/connectors/**` under the global authenticated fallback.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelConnectorController.java:93` through `99` exposes `POST /channels/connectors/{id}/mode` and calls `service.updateMode(...)` with only tenant ID, connector ID, mode, and reason.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelConnectorController.java:309` through `315` writes the requested connector mode directly, including `DISABLED`, `SANDBOX`, and `REAL`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnector.java:17` through `20` defines modes that distinguish real provider sending from sandbox and disabled states, and `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java:157` through `216` branches delivery behavior on that connector mode.

**Business risk:**

A normal tenant user can disable production delivery, force campaigns into sandbox mode so messages never reach customers, or switch connectors to real mode before provider readiness/governance approval. This is a production delivery control-plane action with direct campaign availability and compliance impact.

**Recommendation:**

Require tenant-admin or delivery-operator administration roles for connector mode changes and any state-changing health/control endpoint. Add route-level security rules and service-layer assertions, then test that ordinary tenant users cannot mutate connector mode.

### F-017 - High - Marketing monitoring provider credentials and webhook secrets lack admin authorization

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:224` through `230` now gates marketing-monitoring provider credential, webhook secret, polling configuration, and poll trigger routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for provider credential writes and refresh paths.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:120` through `123` only explicitly permits public webhook callback routes; `/canvas/marketing-monitoring/**` has no dedicated role rule and falls through to `anyExchange().authenticated()` at `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` through `178`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java:406` through `412`, `451` through `483`, `495` through `522`, and `560` through `583` expose credential upsert, refresh, refresh-due, revoke, disable, OAuth start, and OAuth callback operations using only tenant context and actor username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringWebhookAdminController.java:36` through `40` lets any authenticated tenant user rotate a source webhook secret.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialService.java:88` through `139`, `208` through `245`, and `353` through `390` encrypt/store provider access tokens, refresh tokens, API keys, client secrets, and then perform refresh/revoke HTTP calls.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationService.java:112` through `143` stores OAuth client credentials and `155` through `210` exchanges codes into provider credentials.

**Business risk:**

Any logged-in tenant user can overwrite or disable marketing-monitoring credentials, trigger outbound token refresh/revoke calls, complete OAuth exchanges into tenant credential storage, or rotate webhook secrets and break provider ingestion. This enables credential takeover, monitoring blind spots, and disruption of paid/owned marketing observability.

**Recommendation:**

Split monitoring read operations from credential/webhook-secret administration. Require tenant-admin or integration-admin roles for provider credential, OAuth, webhook-secret, polling-control, and alert-channel mutations, and add negative authorization tests for ordinary tenant users.

### F-018 - High - Paid media audience sync can export audience membership under ordinary user access

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:231` through `234` now gates paid-media audience destination and sync-run creation to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for audience sync runs.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` through `178` leaves `/canvas/paid-media/audience-sync/**` under the global authenticated fallback.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PaidMediaAudienceSyncController.java:47` through `52` lets the caller create or update paid-media destinations, and `63` through `68` lets the caller trigger audience sync runs using only tenant context and actor username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncService.java:85` through `123` persists provider, external account, external audience ID, identifier types, consent channel, and enabled state for a destination.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncService.java:130` through `151` creates a run and processes the requested users after tenant checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncService.java:260` through `298` reads CDP profiles, checks consent, hashes configured identifiers, and writes eligible member rows for the destination.

**Business risk:**

Any authenticated tenant user can define a paid-media destination and generate exportable audience membership for arbitrary user IDs they submit. Even though identifiers are hashed, this is still sensitive advertising audience data and can create unauthorized campaign targeting, privacy/compliance exposure, and leakage of audience eligibility into downstream systems once real provider dispatch is attached.

**Recommendation:**

Restrict destination management and sync-run creation to tenant admins or paid-media operators. Require audience-level permission checks before syncing a source audience, audit the requested user set, and add negative tests proving normal tenant users cannot create destinations or trigger sync runs.

### F-019 - High - Loyalty points can be earned and redeemed for arbitrary users by any authenticated tenant user

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:235` through `238` now gates loyalty earn and redeem routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `235`, proving operator denial and tenant-admin allowance for redemption.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` through `178` leaves `/canvas/loyalty/**` under the global authenticated fallback.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/LoyaltyController.java:57` through `62` exposes `POST /canvas/loyalty/users/{userId}/earn`, and `75` through `80` exposes `POST /canvas/loyalty/users/{userId}/redeem`; neither path compares `userId` with the authenticated principal or checks an admin/service role.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/loyalty/LoyaltyService.java:72` through `94` applies positive points to the target account and writes an `EARN` journal row.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/loyalty/LoyaltyService.java:102` through `129` deducts points, inserts a redemption record, and writes a `REDEEM` journal row.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/loyalty/LoyaltyService.java:228` through `243` and `253` through `265` persist journal/redemption records for the supplied target user.

**Business risk:**

A logged-in tenant user can credit points to any customer account or redeem another customer's points by choosing their `userId`. Loyalty balances are financial/benefit liabilities; unauthorized mutation can create direct reward cost, customer harm, and audit integrity issues.

**Recommendation:**

Split self-service customer operations from operator/admin adjustments. For self-service, bind `{userId}` to the authenticated customer identity; for administrative earn/redeem adjustments, require tenant-admin or loyalty-operator roles plus reason/audit validation. Add tests for cross-user earn/redeem attempts.

### F-020 - High - Growth reward grants and activity lifecycle controls lack privileged authorization

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:239` through `243` now gates growth activity creation and lifecycle/control routes to tenant-admin level roles.
- Regression coverage exists in `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:185` through `249`, proving operator denial for creation and publish while preserving authenticated read fallback.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed with 22 tests.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:176` through `178` leaves `/canvas/growth-activities/**` under the global authenticated fallback.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java:129` through `133` creates or updates activities, `227` through `232` creates or updates reward pools, and `494` through `528` publishes, pauses, or closes activities using only tenant context and actor username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java:259` through `264`, `276` through `281`, `294` through `301`, and `313` through `318` expose reward grant create, retry, reconcile, and cancel operations without role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantService.java:86` through `124` validates a reward pool, reserves inventory/budget, and inserts a `RESERVED` grant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantService.java:173` through `188`, `199` through `205`, and `260` through `287` retry, cancel, or reconcile grants and update provider response/status.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantService.java:307` through `333` mutates pool reserved/granted inventory and amount counters during grant transitions.

**Business risk:**

Any authenticated tenant user can publish or stop growth campaigns and manipulate reward grant state/inventory. That can reserve rewards without approval, cancel legitimate rewards, mark provider outcomes incorrectly, distort cost/reporting counters, or launch campaigns before readiness and compliance checks are complete.

**Recommendation:**

Apply role gates to growth activity lifecycle, reward pool, reward grant, and provider reconciliation endpoints. Use tenant-admin/marketing-operator roles for configuration, and a narrower service/provider callback role for reconciliation. Add tests that normal tenant users cannot publish activities or mutate reward grants.

### F-021 - High - Delivery receipt callback becomes unauthenticated when the shared secret is unset

**Status:** Resolved 2026-06-08

**Resolution evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java:51` through `68` now calls `requireValidSecret` before validation or `recordReceipt`, and throws `AccessDeniedException` if `canvas.delivery.receipt.secret` is blank.
- Regression coverage exists at `backend/canvas-engine/src/test/java/org/chovy/canvas/web/DeliveryReceiptControllerTest.java:54` through `62`, asserting a blank configured secret rejects before interacting with `DeliveryOutboxService`.

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:110` permits anonymous `POST /delivery/receipts`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java:49` through `63` accepts provider, provider message ID, receipt type, idempotency key, and raw payload, then calls `outboxService.recordReceipt(...)`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java:66` through `69` returns successfully without any authentication if `canvas.delivery.receipt.secret` is blank.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java:291` through `305` records the receipt and updates the current outbox receipt status.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java:362` through `368` locates the outbox only by provider and provider message ID, not by tenant or a tenant-bound signature claim.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java:528` through `536` inserts the callback log with the receipt type supplied by the request.

**Business risk:**

If the secret is missing in any environment, anonymous callers can forge delivery receipts for known provider message IDs and mark messages delivered, failed, clicked, or otherwise status-changed. Because lookup is global by provider message ID, a forged callback can affect any tenant's outbox record that matches the guessed provider ID.

**Recommendation:**

Fail closed when `canvas.delivery.receipt.secret` is blank, and replace the raw shared-secret header with timestamped HMAC over the raw body. Include tenant or provider account identity in the callback path/signature scope where possible, and add startup validation plus tests proving blank/missing secrets reject callbacks.

### F-022 - High - A/B experiment configuration and governance decisions lack privileged authorization

**Status:** Resolved 2026-06-08

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:243` through `250` now gates POST, PUT, and DELETE routes under `/canvas/ab-experiments` and `/canvas/ab-experiments/**` to tenant-admin level roles.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:210` through `217` proves `OPERATOR` is denied for experiment create/update/delete, group mutation, and governance evaluation routes.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:245` through `252` proves `TENANT_ADMIN` can use the same mutation and governance routes.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:267` through `268` keeps read-only experiment list/group routes on the authenticated fallback for ordinary operators.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed on 2026-06-08 with 23 tests, 0 failures, 0 errors.

**Business risk:**

Any authenticated user can create enabled experiments, alter split variants, disable groups, delete experiments, and write governance decision records. That can change live journey allocation logic, invalidate experiment evidence, prematurely mark winner candidates, or disrupt campaigns that depend on stable A/B assignments and audit-backed launch controls.

**Recommendation:**

Require tenant-admin or a dedicated experiment-operator role for experiment and group mutations, and require an experiment-governance role for evaluation/writeback decision endpoints. Add tenant ownership to experiment records or enforce project/canvas ownership before mutation. Add route-level authorization tests for normal tenant users and service tests proving governance decisions cannot be written without the required role.

### F-023 - High - Marketing policy admin endpoints mutate global contactability records without tenant or admin checks

**Status:** Resolved 2026-06-08

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:251` through `255` now gates `/canvas/policies/consent`, `/canvas/policies/suppression`, and `/canvas/policies/channel` POST routes to tenant-admin level roles.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPolicyAdminController.java:35` through `38` injects `TenantContextResolver`; lines `65` through `180` resolve the current tenant, apply tenant predicates on state/read and upsert lookups, and write the resolved tenant ID onto consent, suppression, and channel rows.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java:55` through `132` adds tenant-aware consent, suppression, and channel availability checks with tenant predicates while keeping existing overloads for legacy callers.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java:269` through `276` passes `DeliveryRequest.tenantId()` into runtime consent, suppression, and channel checks before allowing outbound delivery.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:218` through `220` proves `OPERATOR` is denied for policy mutation routes; lines `256` through `258` prove `TENANT_ADMIN` is allowed.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingPolicyAdminControllerTest.java` asserts inserted consent rows use the current tenant context.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java` asserts tenant-aware consent, suppression, and channel queries include `tenant_id` predicates.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingPolicyAdminControllerTest,MarketingPolicyServiceTest,ReachDeliveryServicePolicyTest,SecurityConfigRouteTest test` passed on 2026-06-08 with 38 tests, 0 failures, 0 errors.

**Business risk:**

An ordinary authenticated user can opt arbitrary customers in or out, create cross-channel suppressions, or change channel addresses and availability. Because runtime policy checks are not tenant-scoped, these writes can block legitimate outbound campaigns, enable messaging after opt-out, redirect contact addresses, and create cross-tenant compliance exposure.

**Recommendation:**

Move policy administration behind tenant-admin/compliance roles and bind every consent, suppression, and channel query/write to the resolved tenant. Do not use tenant `0L` for user-level policy records. Add tests proving users cannot mutate another user's contactability state and that delivery policy checks include tenant predicates.

### F-024 - High - Marketing integration contracts and probes can be modified or executed by any authenticated tenant user

**Status:** Resolved 2026-06-08

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:256` through `259` now gates POST and DELETE routes under `/canvas/marketing-integrations/**` to tenant-admin level roles.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:221` through `228` proves `OPERATOR` is denied for contract upsert/archive, probe-run record, probe record, and production scan trigger routes.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:267` through `274` proves `TENANT_ADMIN` can use those mutation routes.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java:288` through `295` keeps read-only contract, audit, probe, and probe-run listing routes on the authenticated fallback for ordinary operators.
- Existing controllers continue to resolve tenant context and pass actor username into contract/probe services before reaching the now-gated mutation handlers.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest test` passed on 2026-06-08 with 23 tests, 0 failures, 0 errors.

**Business risk:**

Integration contracts describe production provider endpoints, auth mode, readiness, SLO tier, and schema expectations. A normal tenant user can point contracts at the wrong API root, mark unsafe integrations active, archive production dependencies, or forge probe results that drive alerts/readiness. That can break reward/campaign integrations, hide provider outages, or approve production paths without operational review.

**Recommendation:**

Restrict contract upsert/archive and probe execution to tenant-admin or integration-operator roles, with a separate service principal for automated probe ingestion. Treat production contract status/environment/API root changes as privileged change-management actions, and add authorization tests for all mutation routes plus audit assertions showing the authorized actor.

### F-025 - High - Computed CDP tag and profile jobs can be defined and executed by any authenticated tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/cdp/computed-tags/**` or `/cdp/computed-profile-attributes/**`, so these endpoints fall through to authenticated access.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java:69` through `73` creates computed tag definitions using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java:101` through `125` activates and pauses computed tag definitions without a role check.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java:137` through `141` runs an active computed tag job on demand.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java:82` through `98` inserts computed tag definitions and dependencies; `108` through `129` activates or pauses definitions.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java:155` through `178` starts a run and records success/failure counts.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java:198` through `239` scans active profiles and writes tags through `cdpTagService.setTag(...)` when the run mutates data.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java:54` through `58` creates computed profile attributes; `86` through `110` activates or pauses them; `122` through `126` runs them on demand.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java:70` through `83` inserts computed profile definitions, `93` through `110` activates or pauses them, and `229` through `260` updates user profile properties and writes change logs during mutable evaluation.

**Business risk:**

Computed CDP definitions and runs can change segmentation inputs, eligibility tags, and customer profile attributes across all active profiles in a tenant. A normal user can introduce or activate broad rules, run them immediately, and mutate customer data that downstream journeys, BI, paid-media sync, AI decisions, and compliance filters rely on.

**Recommendation:**

Require data-admin or CDP-operator roles for creating, activating, pausing, and running computed CDP jobs. Add approval or dry-run enforcement for high-cardinality mutations, and add route tests proving ordinary tenant users cannot create or run computed tag/profile jobs. Keep preview/read endpoints separately permissioned if analysts need read-only access.

### F-026 - High - Batch CDP tag operations are globally unscoped and unauthorised

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/cdp/tag-operations/**`, so batch tag operations only require authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpTagOperationController.java:43` through `47` creates a batch tag operation without resolving tenant context or caller role.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpTagOperationController.java:57` through `74` lists and reads operation records without tenant filtering, and `85` through `88` retries failed operations without tenant or role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpBatchTagReq.java:15` through `37` lets the request supply operation type, tag code/value, up to 10,000 user IDs, reason, and operator.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java:47` through `66` persists the operation and immediately submits background work for the supplied users.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java:103` through `132` executes each user mutation and calls tenantless `tagService.removeTag(...)` or `tagService.setTag(...)`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java:40` through `44` and `101` through `104` route tenantless tag writes/removals to the nullable-tenant overloads.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java:233` through `247` only adds tenant predicates when `tenantId` is non-null, so the batch path queries and writes current tags/history without tenant scope.

**Business risk:**

Any authenticated user can submit or retry bulk tag writes/removals for arbitrary user IDs, and those operations are not tenant-bound. This can mass-change segmentation, personalization, suppression, reward eligibility, or campaign targeting data across tenants, while the caller can spoof the `operator` field in the request.

**Recommendation:**

Resolve tenant context in the controller and remove tenantless write paths from externally reachable batch operations. Require a CDP data-admin role, derive operator from authenticated context, and filter/list/retry operations by tenant. Add tests covering cross-tenant user IDs, spoofed operator values, and normal-user rejection for create/retry.

### F-027 - High - Marketing preference center lets any authenticated tenant user change arbitrary customer contactability

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/canvas/marketing-preferences/**`, so the preference center only requires authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPreferenceCenterController.java:62` through `72` updates consent for the `userId` path variable without checking whether the caller is that customer or an operator/admin.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPreferenceCenterController.java:85` through `95` updates channel address, enabled, verified, and metadata for arbitrary `userId`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPreferenceCenterController.java:108` through `133` creates suppressions or deactivates suppressions using only tenant context.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterService.java:68` through `90` upserts tenant-scoped consent rows for the supplied user ID and channel.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterService.java:96` through `116` upserts customer channel address/reachability for the supplied user ID.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterService.java:122` through `148` inserts or deactivates tenant-scoped suppression records.

**Business risk:**

Even with tenant predicates, an ordinary tenant user can change another customer's opt-in/opt-out state, channel address, channel verification, and suppression records. That can disable legitimate customer communications, re-enable marketing after opt-out, redirect messages to attacker-controlled addresses, or alter compliance evidence for consent and suppression handling.

**Recommendation:**

Split self-service preference routes from operator/admin routes. Self-service updates should bind the path user to an authenticated customer identity or signed preference-token, while operator updates should require compliance/support roles and audited reason codes. Add authorization tests for cross-user preference mutation and suppression deactivation.

### F-028 - High - Marketing campaign master data and launch dependencies are writable by any authenticated tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/canvas/marketing-campaigns/**`, so campaign write routes only require authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingCampaignController.java:46` through `50` upserts campaign master records using only tenant context and actor username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingCampaignController.java:79` through `83` links resources to campaigns, and `124` through `130` deletes campaign links, without role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignService.java:59` through `97` creates or updates campaign status, objective, primary channel, budget, currency, schedule, owner team, and brief.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignService.java:131` through `168` creates or updates launch dependency links, including `dependencyRole`, `linkStatus`, and `requiredForLaunch`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignService.java:196` through `294` computes readiness from campaign status and linked launch-required resources, and `312` through `317` deletes campaign links.

**Business risk:**

Campaign master data drives budget, schedule, launch readiness, and required dependency checks. Any authenticated tenant user can mark campaigns active, change budgets or dates, add fake active launch dependencies, or remove blockers from readiness evaluation. This can let campaigns launch without required content, measurement, provider, or compliance dependencies.

**Recommendation:**

Require marketing-operator or tenant-admin roles for campaign creation/status/budget changes and launch dependency link changes. Treat readiness-affecting link status and `requiredForLaunch` fields as controlled governance data. Add route authorization tests and audit assertions for campaign status and launch dependency mutations.

### F-029 - High - Private-domain conversation snapshot ingestion is exposed to ordinary authenticated users

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/canvas/conversations/private-domain/**`, so snapshot ingestion only requires authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java:46` through `53` accepts `POST /sync-runs` and calls `service.ingestSnapshot(...)` using only tenant context and actor username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncService.java:78` through `121` imports contacts, groups, group members, and writes a sync-run record from the supplied command.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncService.java:209` through `247` upserts private contacts, owners, and conversation contact profiles from the submitted snapshot.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncService.java:318` through `346` upserts private-domain groups, and `349` onward upserts group members.

**Business risk:**

Private-domain sync ingestion can create or overwrite contact profiles, owner mappings, group metadata, member counts, tags, and raw provider payloads. A normal tenant user can inject fake contacts or groups, change ownership attribution, poison service-agent work queues, or pollute downstream private-domain segmentation and conversation analytics.

**Recommendation:**

Restrict snapshot ingestion to trusted connector service principals or integration-operator roles, with provider-scoped credentials/signatures. Keep read-only contact/group queries separately permissioned. Add tests proving normal users cannot ingest snapshots and that connector calls are bound to the configured provider and tenant.

### F-030 - High - Audience definitions and recomputation can be changed by any authenticated tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/canvas/audiences/**`, so audience mutation and compute routes only require authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java:167` through `180` creates an audience, registers scheduling, and enqueues initial compute using only current user and tenant context.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java:184` through `204` updates an audience, refreshes its scheduler, and enqueues recompute without a role check.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java:207` through `215` deletes an audience, cancels scheduling, and deletes compute artifacts.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java:219` through `244` manually queues audience compute for the selected audience.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java:147` through `165` inserts, updates, and deletes audience definitions, stats, and bitmaps.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java:88` through `130` computes audience membership and overwrites the persisted bitmap used by runtime membership checks.

**Business risk:**

Audiences drive targeting, journey enrollment, paid-media export, and analytics. A normal tenant user can create broad audience rules, update existing definitions, delete audiences, or trigger recomputation that overwrites the runtime bitmap. That can enroll unintended customers, drop valid customers from campaigns, or break downstream activation and reporting.

**Recommendation:**

Require audience-manager or tenant-admin roles for create/update/delete/compute routes. Separate read-only analyst permissions from mutation and recomputation permissions. Add tests proving ordinary tenant users cannot mutate audiences or trigger compute, and audit who changed definitions or refreshed runtime membership.

### F-031 - High - Realtime audience membership updates are exposed to ordinary authenticated users

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/cdp/realtime-audiences/**` or `/cdp/audiences/**`, so realtime audience endpoints only require authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java:37` through `47` accepts realtime audience events and calls `service.processEvent(...)` using only tenant context.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java:71` through `101` evaluates the event, reserves an event log, adds/removes the user from the persisted bitmap, and saves it.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java:288` through `325` persists realtime audience event logs for add/remove operations.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java:59` through `63` lets any authenticated caller create a realtime audience snapshot.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java:158` through `172` loads the bitmap, records estimated size, and inserts a snapshot record.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java:94` through `136` resolves tenant context but calls `service.overlap(leftId, rightId)`, `service.merge(leftId, rightId)`, and `service.exclude(baseId, excludedId)` without passing tenant ID.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java:110` through `146` and `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java:88` through `107` load bitmaps solely by audience ID to calculate overlap, merge, and exclude sizes.

**Business risk:**

Any authenticated tenant user can submit realtime events that add or remove arbitrary users from an audience bitmap, changing eligibility immediately for downstream journeys and activation. The set-operation endpoints also allow probing bitmap cardinalities by audience ID without tenant validation, exposing cross-tenant audience size/overlap signals where IDs are guessable.

**Recommendation:**

Restrict realtime membership updates to trusted CDP event ingestion service principals or signed connector routes, not ordinary UI users. Require tenant-bound validation for snapshot and set-operation endpoints, and pass tenant ID into overlap/merge/exclude. Add tests for normal-user rejection, cross-tenant audience IDs, and forged realtime event updates.

### F-032 - High - MQ rejected-message replay is globally unscoped and operator-unguarded

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:153` through `178` has no explicit rule for `/canvas/mq-trigger-rejected/**`, so rejected-message replay only requires authentication.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java:66` through `79` lists rejected MQ records without resolving tenant context or filtering by tenant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java:90` through `98` reads rejected record details by ID without tenant filtering.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java:109` through `149` replays a rejected record by ID, parses the original message body, looks up current routes by topic, enqueues execution requests, and best-effort publishes them.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java:176` through `181` only validates that `userId`, `messageCode`, and payload are present before replay.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java:211` through `219` immediately dispatches replayed execution requests through `CanvasDisruptorService`.

**Business risk:**

Any authenticated user can inspect rejected MQ payloads and replay them into every canvas currently routed for the rejected topic. Because the endpoint is not tenant-bound and does not require an operator role, users can re-trigger historical customer journeys, resend messages, reapply rewards/tags, or leak rejected payload data from other tenants if record IDs or topics are discoverable.

**Recommendation:**

Bind rejected MQ records to tenant/project at ingestion time and require an operations role for list/detail/replay. Revalidate the target canvas tenant and route tenant during replay, require an explicit replay reason, and add tests proving normal users cannot view or replay rejected records across tenants.

### F-033 - High - Direct CDP user tag writes are available to any authenticated tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:182` through `239` adds explicit admin gates for selected CDP write keys/webhooks and other control-plane routes, but there is no explicit rule for `/cdp/users/**`; it falls through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java:137` through `143` lets a caller add or update a tag for any `userId` path variable using only tenant context.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java:155` through `162` lets a caller remove any tag from any `userId` in the tenant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java:47` through `99` writes current tag state and tag history, ensures the user exists, accepts request-supplied source/operator/idempotency fields, and sets the current tag `createdBy` from the request.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java:107` through `120` marks current tags removed and writes removal history.

**Business risk:**

CDP tags are used for segmentation, computed audiences, paid-media sync, reward eligibility, and personalization. Any authenticated tenant user can manually add, overwrite, or remove tags for arbitrary customers, including spoofing the operator value in the request. That can move customers into or out of campaigns and distort audit history.

**Recommendation:**

Gate direct user tag mutation behind a CDP data-admin/operator role, derive operator and source type from authenticated context for UI writes, and add audit fields that cannot be client-spoofed. Add tests proving normal tenant users can read permitted profile data but cannot mutate another customer's tags.

### F-034 - High - Global tag and identity metadata can be changed by any authenticated user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:182` through `239` has no explicit rule for `/canvas/tag-definitions/**` or `/canvas/identity-types/**`, so these metadata write routes fall through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagDefinitionController.java:43` through `64` creates, updates, and deletes tag definitions without tenant context or role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagDefinitionController.java:95` through `130` creates, updates, and deletes tag value definitions without tenant context or role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionService.java:53` through `80` inserts, updates, or deletes tag definitions, including `enabled`, `manualEnabled`, value type, write policy, category, and owner defaults.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionService.java:93` through `142` inserts, updates, or deletes allowed tag values.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IdentityTypeController.java:54` through `88` creates, updates, and deletes identity types without tenant context or role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityTypeService.java:46` through `74` inserts, updates, and deletes identity metadata, including `enabled`, `allowImport`, `multiValue`, `priority`, and `participateMapping`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java:79` through `82` relies on importable identity types and enabled tag definitions when importing tags.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java:98` through `128` filters CDP audience facts using enabled tag codes and enabled identity types.

**Business risk:**

Tag and identity metadata are global control-plane data for imports, manual tagging, audience source fields, and downstream customer targeting. A normal authenticated user can enable/disable tags or identity types, change value semantics, open manual tagging, remove allowed values, or change identity importability, affecting segmentation and import behavior across tenants.

**Recommendation:**

Require tenant-admin or CDP governance roles for tag/identity metadata writes, and either tenant-scope the metadata or explicitly treat it as super-admin global configuration. Add tests for normal-user rejection and cross-tenant impact, and add change audit for enabled/manual/importability fields.

### F-035 - High - Built-in plugin enablement is globally writable by any authenticated user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:182` through `239` has no explicit rule for `/canvas/plugins/**`, so plugin enablement falls through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java:50` through `57` lets a caller enable or disable a plugin by key without tenant context or role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java:48` through `62` validates only plugin existence and minimum Canvas version, then writes the enablement command.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java:61` through `68` updates `built_in_plugin_registry.enabled` globally by `plugin_key`.
- `backend/canvas-engine/src/main/resources/db/migration/V161__plugin_integration_foundations.sql:1` creates the global `built_in_plugin_registry` table.

**Business risk:**

Plugin enablement is a global runtime capability switch. Any authenticated user can turn built-in integrations or extension points on or off for all tenants, potentially exposing experimental functionality, disabling production capabilities, or changing canvas behavior without tenant-admin or platform governance approval.

**Recommendation:**

Restrict plugin enablement to super-admin/platform-admin roles, or make plugin state tenant-scoped if tenant operators should control it. Add audit records for enablement changes and tests proving ordinary tenant users cannot modify global plugin state.

### F-036 - High - Canvas DLQ replay and deletion are globally unscoped and operator-unguarded

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:247` through `248` sends routes without explicit matchers to `authenticated()`; there is no explicit matcher for `/canvas/dlq/**`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java:53` through `68` lists DLQ records ordered globally and only filters by optional `canvasId`; it does not resolve tenant context.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java:78` through `114` replays any DLQ record by ID, parses the stored payload, and calls `CanvasExecutionService.trigger(...)` with the stored `canvasId`, `userId`, trigger metadata, and a new replay message ID.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java:123` through `127` deletes any DLQ record by ID with `deleteById`.

**Business risk:**

Any authenticated user can inspect failed execution payloads, replay them into canvases, or delete operational evidence across tenants if IDs are discoverable. Replays can resend messages, reapply rewards or tags, and perturb customer journeys, while deletion removes the audit trail needed for incident response.

**Recommendation:**

Persist tenant/project ownership on DLQ records and enforce it in list, replay, and delete. Require an operations/admin role, record replay/delete reasons and actors, and add tests for normal-user denial plus cross-tenant ID rejection.

### F-037 - High - Execution rerun dry-runs can target canvases outside the caller tenant

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:247` through `248` sends `/execution-reruns/**` to the generic authenticated fallback; the route is not covered by the admin replay gates for `/canvas/execution-requests/**`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionRerunController.java:43` through `45` accepts any `canvasId` path variable and forwards caller tenant context plus the requested canvas ID to `TestUserRerunService.rerun`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/TestUserRerunService.java:137` through `167` only requires admin role for `ADMIN_REPLAY`; `DRY_RUN` and `SKIP_SIDE_EFFECTS` are available to normal authenticated users and still call `executionService.triggerDryRun(...)`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoader.java:38` through `44` loads the dry-run canvas by ID only, without checking that the canvas tenant matches the caller tenant passed into the rerun service.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java:172` through `239` executes the dry-run graph, ensures a CDP user, inserts an execution record, and updates execution status/result.

**Business risk:**

A tenant user can dry-run or skip-side-effect-run another tenant's canvas by guessing IDs, causing cross-tenant execution records and CDP user creation while probing graph behavior and returned execution results. If node handlers do not consistently honor dry-run semantics, this also becomes a path to unintended external side effects.

**Recommendation:**

Verify canvas tenant ownership before any rerun mode, and require explicit canvas edit/operator permission for dry-runs. Treat supplied `graphJson` as privileged draft content, restrict it to users who can edit that canvas, and add tests for cross-tenant canvas IDs and normal-user `ADMIN_REPLAY` denial.

### F-038 - High - Marketing form control-plane writes let ordinary users create public CDP mutation and trigger endpoints

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:247` through `248` has no explicit admin rule for `/canvas/marketing-forms/**`, so form creation, update, and activation fall through to authenticated access.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java:76` through `114` lets any authenticated tenant user create forms, change form JSON, and activate or deactivate forms.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java:167` through `176` accepts request-supplied `createdBy` instead of deriving it from the authenticated user.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingFormService.java:123` through `137` persists `publicKey`, field schema, submit action JSON, status, and creator.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingFormService.java:237` through `285` lets anonymous public submissions update or create CDP profiles, channels, consent, and submission records for the form's tenant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingFormService.java:425` through `447` publishes a canvas trigger when the form submit action contains `canvasId` and `triggerEventCode`.

**Business risk:**

A normal tenant user can create or alter a public form that becomes an anonymous endpoint for customer profile, channel, and consent mutation, and can wire submissions to canvas triggers. This can poison CDP data, opt customers in or out, and launch journeys without marketing-admin approval; spoofed `createdBy` also weakens auditability.

**Recommendation:**

Restrict form definition writes and activation to marketing/admin roles, derive creator/operator from authentication, and validate submit actions against canvases the tenant is allowed to trigger. Add tests for normal-user rejection, action `canvasId` tenant matching, and immutable audit actor handling.

### F-039 - High - Authenticated users can forge conversation ingress and resume customer wait states

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:247` through `248` has no explicit service-principal or admin rule for `/canvas/conversations/ingress` or `/canvas/conversations/adapters/*/ingress`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java:57` through `83` allows any authenticated tenant user to submit raw or adapter-mapped inbound conversation events.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java:62` through `107` writes sessions/messages and calls `waitResumeService.resumeEventWaits(...)` for `CONVERSATION_REPLY` using request-supplied `userId`, text, intent, execution ID, and attributes.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java:248` through `273` copies request attributes into the resume event payload and includes request-supplied canvas/version/execution identifiers.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarness.java:56` through `65` converts raw payloads for adapter ingress and forwards them to the same ingestion path.

**Business risk:**

Any authenticated user in a tenant can impersonate a customer's inbound reply, alter conversation history, and resume canvases waiting for `CONVERSATION_REPLY`. Forged intents or attributes can advance journeys, satisfy approval-like waits, or steer downstream personalization and support workflows without a provider signature or trusted connector boundary.

**Recommendation:**

Move ingress writes behind signed provider callbacks, trusted connector service credentials, or a narrow conversation-operator role. Derive provider/source metadata from the authenticated connector, reject caller-supplied execution identifiers unless bound to an active session, and add tests proving ordinary UI users cannot forge ingress events.

### F-040 - High - Conversation workspace routing and assignment controls are writable by any tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:247` through `248` has no explicit role rule for `/canvas/conversations/workspace/**`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java:115` through `170` lets any authenticated tenant user assign work items, change status/priority, create SOP tasks, and complete tasks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java:207` through `269` lets any authenticated tenant user upsert routing agents/rules, route work items, and evaluate SLA breaches.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceService.java:149` through `215` changes assignee/team/status/priority and records audit using the caller name, but only checks tenant ownership.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingService.java:76` through `157` creates or updates routing agents and rules, including status, capacity, skills, target team, SLA minutes, enabled flag, and sort order.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingService.java:159` through `300` routes work items, increments agent load, creates SLA breaches, and escalates priority after scanning tenant work items.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceService.java:399` through `418` validates tenant ownership but does not enforce assignee, supervisor, or admin authorization.

**Business risk:**

Support routing and SLA controls determine who handles customer conversations and when escalations occur. A normal tenant user can reassign work, close or reprioritize cases, rewrite routing rules, spoof agent availability/capacity, and trigger SLA escalations, causing missed customer responses or hiding unresolved incidents.

**Recommendation:**

Separate workspace permissions into agent, supervisor, and routing-admin capabilities. Enforce assignee/self-service checks for task completion and status changes, supervisor/admin checks for assignment and routing config, and add tests for unauthorized assignment, rule updates, and SLA evaluation.

### F-041 - High - BI datasource onboarding and credential controls are writable by ordinary authenticated users

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:170` through `239` gates selected BI permission routes, but there is no explicit route gate for `/canvas/bi/datasources/**`; these endpoints fall through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java:137` through `145` creates a BI datasource onboarding record using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java:161` through `257` accepts uploaded files and can materialize them into BI datasource/dataset state for the tenant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java:269` through `319` updates datasource onboarding records and rotates datasource credentials without checking tenant-admin or data-admin role.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java:348` through `377` lets callers preview API data and sync schema snapshots for a datasource.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingService.java:105` through `155` persists datasource connector type, URL, username, encrypted password, connector config, enabled flag, and creator.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceRuntimeService.java:968` through `977` verifies tenant ownership of a datasource but does not enforce caller role or datasource governance permission.

**Business risk:**

BI datasources carry database/API connection details, credentials, schema metadata, and uploaded data used for dashboards and self-service analysis. A normal tenant user can add or change connectors, rotate credentials, upload and materialize files, call external APIs for previews, and refresh schemas, potentially exfiltrating data, poisoning analytical datasets, or breaking production BI resources.

**Recommendation:**

Require tenant-admin/data-admin permission for datasource create/update/upload/materialize/credential rotation/schema sync. Keep read-only catalog/list access separate from credential-bearing operations, enforce datasource-level admin grants in services, and add route/security tests for normal-user rejection.

### F-042 - High - BI resource creation and archival bypass resource permission checks

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:170` through `239` does not explicitly gate BI resource write routes such as `/canvas/bi/charts/resources/**`, `/canvas/bi/dashboards/resources/**`, `/canvas/bi/datasets/resources/**`, or `/canvas/bi/portals/resources/**`; they fall through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java:106` through `120` lets authenticated users save chart drafts, while `162` through `166` archives charts using only tenant context.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java:102` through `117` saves dashboard drafts, `219` through `261` imports dashboard packages, and `273` through `277` archives dashboards using only tenant context and caller identity.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java:222` through `261` calls `requirePermission(...)` with `resourceId = null` when a chart does not already exist, then upserts a new chart.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java:185` through `223` has the same `existing == null ? null : existing.getId()` pattern before upserting dashboards and deleting/replacing widgets.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java:374` through `407` imports a dashboard package and delegates to `saveDraft(...)` without requiring a workspace-level create/import permission.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java:489` through `498` and `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java:573` through `582` skip `permissionGuard.require(...)` whenever `resourceId` is null.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java:446` through `461` archives a dashboard without calling `requirePermission(...)`; the chart, dataset, portal, spreadsheet, and big-screen services show the same unguarded archive pattern in `rg` results.

**Business risk:**

Resource permissions protect existing BI assets but do not protect workspace-level creation/import, and archive operations bypass the guard entirely. Any authenticated tenant user can create new charts/dashboards/datasets/portals or import packages that reference sensitive datasets, and can archive shared assets, disrupting reporting and bypassing intended BI governance.

**Recommendation:**

Introduce explicit workspace-level `CREATE`, `IMPORT`, and `ARCHIVE` permissions for BI resources, and enforce them when `resourceId` is null or when archiving. Add route-level admin/editor gates where appropriate, and add tests for normal-user creation/import/archive denial across chart, dashboard, dataset, and portal resource types.

### F-043 - High - AI prompt templates can be created or disabled by any authenticated tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:198` through `203` gates `/ai/providers/**`, but there is no explicit rule for `/ai/prompt-templates/**`; these routes fall through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java:72` through `75` creates prompt templates for the caller tenant without role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java:102` through `107` disables tenant-owned templates without role checks.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java:119` through `137` renders and evaluates prompts using tenant context, and `148` through `151` exposes recent evaluation audits without tenant filtering.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java:50` through `65` stores arbitrary prompt body, category, output schema, default variables, and enabled state in the runtime template registry.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java:113` through `117` allows request-supplied `promptOverride` to replace registered template text during rendering.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java:161` through `166` enforces tenant visibility but not author/editor/admin authorization.

**Business risk:**

Prompt templates influence generated marketing copy, scoring, BI planning, and other AI-assisted decisions. Ordinary users can introduce or disable templates that alter model instructions and output schemas, and global evaluation audits may leak prompt-evaluation activity across tenants. Malicious or poorly governed prompts can cause unsafe content, data leakage through prompt composition, or incorrect automated decisions.

**Recommendation:**

Require AI-admin or template-editor permission for template create/disable and restrict prompt overrides to trusted test/evaluation roles. Tenant-scope evaluation audits, add immutable author/audit metadata, and add tests for normal-user mutation denial and cross-tenant audit isolation.

### F-044 - High - Demo sandbox endpoints allow cross-tenant install, reset, and forged replies

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:170` through `239` has no explicit rule for `/demo-sandboxes/**`, so demo sandbox writes fall through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java:59` through `64` resolves the authenticated context but ignores it, then installs a sandbox for the request-body `tenantId`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java:76` through `81` resets the path `tenantId` without checking it matches the authenticated tenant or that the caller is an admin.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java:109` through `118` submits sandbox conversation replies for the path `tenantId` via `ConversationAdapterHarness`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java:121` through `134` accepts caller-supplied canvas ID, version ID, execution ID, user ID, event IDs, text, intent, and attributes for the sandbox reply payload.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java:33` through `47` upserts an ACTIVE sandbox marker and expiry for the supplied tenant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java:54` through `63` records reset state for the supplied tenant.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/JdbcDemoSandboxRepository.java:21` through `43` upserts `demo_sandbox`, while `66` through `71` writes reset metadata by tenant ID.

**Business risk:**

Demo sandbox operations are tenant lifecycle controls, but any authenticated user can install or reset another tenant's sandbox by supplying a tenant ID. The reply endpoint also lets users forge cross-tenant conversation events that can feed the same wait-resume path documented in F-039, polluting demo/customer journeys and audit state.

**Recommendation:**

Restrict sandbox install/reset to platform or tenant admins, bind the target tenant to authenticated context unless the caller is a super-admin, and require signed/internal-only access for sandbox reply simulation. Add tests for cross-tenant tenantId rejection and normal-user denial.

### F-045 - High - MQ message definition writes are authenticated-only and can rewrite trigger routing

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:170` through `239` has no explicit rule for `/canvas/mq-definitions/**`, so MQ definition writes fall through to `authenticated()` at `247` through `248`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MqDefinitionController.java:48` through `55` lets any authenticated caller create a message definition, inserts the supplied body, and immediately calls `routeRefreshService.rebuildMqRoutes()`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MqDefinitionController.java:59` through `68` lets any authenticated caller update an arbitrary definition ID and rebuild routes.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MqDefinitionController.java:72` through `79` lets any authenticated caller delete an arbitrary definition ID and rebuild routes.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java:56` through `99` rebuilds runtime trigger-route caches for all published canvases from the current published graph definitions.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinitionService.java:27` through `47` resolves runtime topics from enabled message definitions before falling back to node config.
- `frontend/src/pages/mq-config/index.tsx:145` through `170` exposes the same create/update/delete endpoints from the MQ configuration UI.

**Business risk:**

MQ message definitions are runtime routing metadata. An ordinary authenticated tenant user can create, edit, or delete message-code mappings and force a route rebuild, causing published MQ-trigger canvases to stop consuming expected topics or consume attacker-selected topics. This can misroute production journeys, create duplicate/phantom triggers, or interrupt order/payment/customer-event workflows that depend on MQ_TRIGGER and SEND_MQ semantics.

**Recommendation:**

Restrict `/canvas/mq-definitions` mutations to tenant-admin or operations roles, add service-level authorization before insert/update/delete, validate tenant ownership or make the table explicitly global with super-admin-only writes, and add route-security regression tests denying normal users. Consider audit logging and change review because edits affect already-published runtime routes.

### F-046 - High - BI publish approvals can be self-approved by ordinary authorized publishers

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:204` through `273` gates selected BI permission, datasource-adjacent, and high-impact control-plane routes, but it does not include `/canvas/bi/resources/publish-approvals/**`; these endpoints fall through to authenticated access at `SecurityConfig.java:282`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPublishApprovalController.java:99` through `105` lets any authenticated tenant user request a BI publish approval using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPublishApprovalController.java:118` through `129` lets any authenticated tenant user review an approval using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalService.java:158` through `181` updates a pending approval to the requested review status and records `reviewedBy`, but it does not check reviewer role, resource ownership, resource permission, or separation from `requestedBy`.
- BI resource publish paths rely on approved approval records for non-admin publishers, for example `BiChartResourceService.java:336` through `339`, `BiDashboardResourceService.java:298` through `301`, and `BiDatasetResourceService.java:393` through `396`.

**Business risk:**

The approval layer intended to gate BI publishing can be bypassed by any user who can reach publish with a valid resource permission: they can create a pending approval and approve it themselves before publishing. This weakens four-eyes governance for dashboards, charts, datasets, and portals, and can expose unreviewed SQL, sensitive datasets, or incorrect BI assets to consumers.

**Recommendation:**

Require tenant-admin, BI approver, or resource-owner reviewer permissions for `/canvas/bi/resources/publish-approvals/*/review`, and reject self-review for non-admin users. Tie approval review to the same resource permission model used by publish, and add tests proving normal users cannot approve their own BI publish requests.

### F-047 - High - Quick Engine capacity and pool policies are writable by any authenticated tenant user

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:204` through `273` has no explicit rule for `/canvas/bi/capacity/**`, so those endpoints fall through to authenticated access at `SecurityConfig.java:282`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiCapacityController.java:126` through `134` writes Quick Engine alert/capacity policy using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiCapacityController.java:146` through `154` writes tenant pool policy using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityService.java:583` through `633` persists enabled state, capacity limit rows, warning/critical thresholds, notification channels, and receivers without a role check.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityService.java:644` through `697` persists pool key, max concurrent queries, queue limit, queue timeout, and pool weight without a role check.

**Business risk:**

Any tenant user can weaken BI capacity controls, disable or redirect alerting, raise concurrency/queue limits, or reduce queue limits/timeouts enough to deny service to legitimate BI workloads. These settings affect shared query execution capacity, cost, operational alerting, and dashboard availability.

**Recommendation:**

Gate `/canvas/bi/capacity/**` mutation routes to tenant-admin or BI-ops roles and enforce the same check in `BiQuickEngineCapacityService`. Keep read-only capacity summaries separately available if needed, and add route tests denying ordinary operators for alert-policy and tenant-pool-policy writes.

### F-048 - High - BI embed tickets can be minted for arbitrary resources by ordinary authenticated users

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:146` through `150` permits anonymous verification, query, dashboard-resource, runtime-state, and portal-resource embed routes; `POST /canvas/bi/embed-tickets` itself has no privileged route gate and falls through to authenticated access at `SecurityConfig.java:282`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java:629` through `636` creates an embed ticket from the request body using only current tenant and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketService.java:135` through `175` signs the request-supplied `resourceType`, `resourceKey`, scope, filters, parameters, domains, TTL, and access limits, but does not check that the resource exists or that the caller has `VIEW`, `EMBED`, or sharing permission for that resource.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiEmbedResourceController.java:97` through `113` and `162` through `180` serve dashboard and portal resources to anonymous callers after ticket verification.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java:678` through `699` executes embed BI queries under the ticket tenant/user after verifying only ticket scope and origin.

**Business risk:**

Any logged-in tenant user can create short-lived externally usable embed tickets for guessed or known BI dashboard/portal keys and allowed domains they choose. The public embed endpoints then trust those tickets, potentially exposing BI resource definitions and query results outside the tenant UI without resource-level sharing approval.

**Recommendation:**

Require explicit `EMBED` or `VIEW` permission on the target resource before ticket creation, and restrict external-ticket creation to BI admins or resource owners. Validate resource existence and published/shareable state during ticket minting, not only during public resource access. Add tests where a normal user without resource permission cannot mint or use an embed ticket for a dashboard.

### F-049 - High - Datasource credential sanitization migration writes plaintext passwords into encrypted credential columns

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/resources/db/migration/V354__sanitize_demo_datasource_credentials.sql:18` through `24` updates `data_source_config.password` directly to the plaintext value `canvas_demo_local_password`.
- New datasource writes use `DataSourceConfigService.create`, which encrypts `body.getPassword()` before insert at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/datasource/DataSourceConfigService.java:76` through `80`; password rotation also encrypts at `DataSourceConfigService.java:103` through `115`.
- `DataSourceCredentialCipher.encrypt` prefixes encrypted datasource credentials with `enc:v1:` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/datasource/DataSourceCredentialCipher.java:54` and `71` through `88`.
- Runtime still accepts plaintext because `DataSourceCredentialCipher.decrypt` returns non-`enc:v1:` values unchanged at `DataSourceCredentialCipher.java:93` through `99`, and `SecretCipher.decrypt` similarly returns non-`v1:` values unchanged at `backend/canvas-engine/src/main/java/org/chovy/canvas/security/SecretCipher.java:95` through `98`.
- BI runtime explicitly falls back to the legacy `SecretCipher`/plaintext path for non-`enc:v1:` values at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceRuntimeService.java:1096` through `1103`.

**Business risk:**

The migration intended to remove unsafe `root/root` demo credentials also reintroduces plaintext datasource credentials at rest. Anyone with database read access, backup access, slow-query logging, or dump artifacts can recover the demo datasource password directly, and this creates a precedent that conflicts with the encrypted credential storage contract used by normal create/update paths.

**Recommendation:**

Do not write replacement passwords directly in SQL unless they are encrypted in the same storage format expected by the application. Prefer disabling matching demo datasources, rotating through application code, or using an environment-provided encrypted value with a migration guard. Add a migration policy test that rejects writes of literal `password = '...'` to credential columns.

### F-050 - Medium - Tiered cache batch-loader exceptions bypass loader failure and stale fallback policies

**Status:** Resolved 2026-06-08

**Evidence:**

- `backend/canvas-cache-sdk/src/main/java/org/chovy/cache/TieredCacheImpl.java:442` through `450` now wraps `batchLoader.apply(...)` and routes each missed key through `handleLoaderFailure(key, staleFor(key), e)` before writing the current L1 version.
- Single-key L3 loading handles loader exceptions at `backend/canvas-cache-sdk/src/main/java/org/chovy/cache/TieredCacheImpl.java:776` through `797`, incrementing loader-failure metrics and applying stale, `RETURN_STALE`, or `RETURN_EMPTY` behavior.
- `backend/canvas-cache-sdk/src/test/java/org/chovy/cache/TieredCacheTest.java:327` through `363` covers exception-throwing batch loaders for `RETURN_EMPTY` and stale-on-error fallback.
- Verification: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-cache-sdk -Dtest=TieredCacheTest test` passed on 2026-06-08 with 25 tests, 0 failures, 0 errors.

**Business risk:**

A batch data loader failure can still bubble out of `getAll` even when the cache is configured to return stale or empty results for loader failures. Bulk callers can fail an entire request instead of degrading per key, and loader-failure metrics/stale fallback will be inconsistent between single-key and batch cache access.

**Recommendation:**

Route batch loader execution through the same failure-handling policy as `loadFromL3`, ideally per missed key where a stale value exists. Add tests for `RETURN_EMPTY`, `RETURN_STALE`, stale-while-revalidate, and default `THROW` behavior on exception-throwing batch loaders.

### F-051 - Medium - Frontend unauthorized handling clears storage but leaves authenticated React state alive

**Status:** Resolved 2026-06-08

**Evidence:**

- `frontend/src/services/api.ts:106` through `117` clears `canvas_token` and `canvas_user` from `localStorage` and dispatches `canvas:unauthorized` when axios classifies a response as unauthorized.
- `frontend/src/services/api.ts:120` through `129` repeats the same storage cleanup/event dispatch for the shared `apiClient` unauthorized callback.
- `frontend/src/context/AuthContext.tsx:87` through `99` now wraps `logout` in a stable callback and subscribes `AuthProvider` to `canvas:unauthorized`, clearing both storage and React `user` state through the provider-owned path.
- `frontend/src/context/AuthContext.test.tsx:71` through `97` starts with a stored admin user, dispatches `canvas:unauthorized`, and verifies the rendered auth state becomes anonymous and both stored auth keys are removed.
- Verification: `npm run test -- AuthContext.test.tsx` passed on 2026-06-08 with 2 tests, 0 failures.

**Business risk:**

After token expiry or revocation, the UI navigates to the login page but the in-memory auth state can still represent the previous user until a full reload or explicit logout. Users can still see admin menus or re-enter protected routes from the same SPA session, causing repeated 401 loops and misleading role-based UI state even though the backend has rejected the session.

**Recommendation:**

Move unauthorized handling into `AuthProvider` or expose a logout callback to `UnauthorizedRedirect` so the same event clears both storage and React state. Add a test that starts with a stored user, dispatches `canvas:unauthorized`, and verifies `RequireAuth` denies protected routes and admin flags are reset.

### F-052 - High - Renumbering Flyway migrations deletes already tracked scripts and can break upgraded databases

**Status:** Open

**Evidence:**

- The current diff deletes `backend/canvas-engine/src/main/resources/db/migration/V91__sanitize_demo_datasource_credentials.sql`, `V92__enforce_core_tenant_not_null.sql`, `V272__sanitize_demo_datasource_credentials.sql`, and `V273__enforce_core_tenant_not_null.sql`.
- `git ls-tree HEAD` shows all four deleted scripts are tracked in the base revision, alongside the separate `V91__data_security_and_tenant_isolation.sql` and `V92__execution_context_cold_backup.sql` migrations.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java:91` through `106` now asserts those old files do not exist and only the renumbered `V354__sanitize_demo_datasource_credentials.sql` and `V355__enforce_core_tenant_not_null.sql` exist.
- `backend/canvas-engine/src/main/resources/db/migration/V354__sanitize_demo_datasource_credentials.sql:18` through `35` and `V355__enforce_core_tenant_not_null.sql:18` through `57` reintroduce the same migration bodies under new version numbers.

**Business risk:**

Any environment that has already applied one of the deleted versioned migrations will fail Flyway validation because the migration file recorded in `flyway_schema_history` is missing from the application artifact. Renumbering and deleting versioned migrations after they are tracked also creates uncertainty about which environments have the old versus new version, making production rollout and rollback brittle.

**Recommendation:**

Do not delete or rewrite versioned migrations that may have been applied. Keep the old scripts in place, add new follow-up migrations only for additional work, or perform an explicit Flyway repair plan with environment inventory and release notes. Change the migration policy test to prevent deletion of previously tracked migrations instead of asserting the old files are absent.

### F-053 - Medium - BI embed ticket verification consumes the same access budget needed by render endpoints

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketService.java:77` sets `DEFAULT_MAX_ACCESS_COUNT` to `1`, and `BiEmbedTicketService.java:402` through `409` uses that default when callers omit `maxAccessCount`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java:647` through `655` exposes `POST /canvas/bi/embed-tickets/verify`, but it calls `embedTicketService.verifyForUse(...)`, which consumes persistent or in-memory access state at `BiEmbedTicketService.java:207` through `215`.
- The same consuming method is called by dashboard resource load at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiEmbedResourceController.java:106` through `112`, dashboard runtime-state load at `BiEmbedResourceController.java:136` through `145`, portal resource load at `BiEmbedResourceController.java:172` through `182`, and each embed query at `BiQueryController.java:687` through `697`.
- The embed page calls verify first, then resource/runtime/query endpoints with the same ticket at `frontend/src/pages/bi/embed.tsx:87` through `132`; the workbench-generated dashboard ticket compensates by setting `maxAccessCount` at `frontend/src/pages/bi/biWorkbench.ts:1847` through `1849`, but raw API clients and older callers that rely on the backend default get only one consumable use.

**Business risk:**

The public API contract is easy to misuse: a caller can create a valid ticket without `maxAccessCount`, call the documented verify endpoint to inspect it, and immediately burn the only allowed use before the embed page can fetch its resource or execute queries. This creates intermittent "ticket replayed or rate-limited" failures for integrations that do a preflight verify or render more than one endpoint per page load.

**Recommendation:**

Separate non-consuming verification from consuming render/query use, or raise the default access budget to match the minimum render flow. Document which endpoints consume access and add an integration test that creates a ticket with omitted `maxAccessCount`, opens the embed flow through verify, resource, runtime-state, and one query, and verifies the intended contract.

### F-054 - High - BI resource move and ownership transfer bypass resource governance

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:249` through `251` sends routes without explicit matchers to the authenticated fallback; there is no explicit rule for `/canvas/bi/resources/locations`, `/canvas/bi/resources/move`, or `/canvas/bi/resources/transfer`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceMovementController.java:42` through `49` exposes resource move operations using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceTransferController.java:42` through `49` exposes ownership transfer using only tenant context and username.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementService.java:70` through `90` validates that the resource exists and is not archived, then upserts the requested folder/sort location without checking role, owner, or BI resource permission.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferService.java:69` through `88` validates that the resource exists and is not archived, then upserts the requested owner user without checking role, current ownership, or transfer permission.
- Existing service tests such as `BiResourceMovementServiceTest.java:65` through `80` and `BiResourceTransferServiceTest.java:64` through `75` prove the writes persist tenant-scoped rows, but they do not cover normal-user denial or owner/admin authorization.

**Business risk:**

Any authenticated tenant user can move shared BI assets into different folders, reorder them, or transfer ownership to another user. If ownership and location feed navigation, curation, review responsibility, or future permission decisions, a normal analyst can disrupt shared reporting, hide important assets, or take over governance metadata without editor/admin approval.

**Recommendation:**

Gate move and transfer routes with tenant-admin/resource-admin authorization, and enforce resource-level `EDIT`/`ADMIN` or owner checks inside `BiResourceMovementService` and `BiResourceTransferService`. Add route tests proving `OPERATOR` is denied for move/transfer and service tests proving non-owner transfer attempts fail.

### F-055 - High - User input submission is authorized only by guessable response ID

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:268` through `276` has no explicit matcher for `/user-input/**`, so `POST /user-input/responses/{responseId}/submit` falls through to the generic authenticated fallback.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/UserInputController.java:41` through `45` accepts only the path `responseId` and request body, then delegates directly to `UserInputService.submit(responseId, req)`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/UserInputService.java:122` through `145` loads and completes the pending response by `responseId`; the update predicate checks only `id` and `status`, not tenant, target user, submitter, or a signed public token.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/UserInputService.java:148` through `156` implements `requireResponse` with `selectById(responseId)`, so the initial lookup is not tenant-scoped.
- Existing tests cover happy-path submit and duplicate handling in `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/UserInputServiceTest.java:90` through `135`, and controller delegation in `backend/canvas-engine/src/test/java/org/chovy/canvas/web/UserInputControllerTest.java:18` through `34`, but they do not cover cross-tenant, wrong-user, unsigned-link, or unauthorized-submit denial.

**Business risk:**

Any authenticated user who can guess or obtain a pending response ID can submit arbitrary input and resume that execution, including across tenant boundaries if IDs are globally enumerable. For customer-facing forms this also creates an awkward contract: the route is not truly public, but once authenticated it is not bound to the intended recipient or form invitation.

**Recommendation:**

Bind submission to the response tenant and intended user, or issue a signed, short-lived form token and require it on submit. Include tenant/user/token predicates in both the read and conditional update, and add negative tests for wrong tenant, wrong user, expired token, and replayed submission.

### F-056 - High - Registered risk Flink pipeline depends on connectors that are not packaged or deployed

**Status:** Open

**Evidence:**

- `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkPipelineRegistry.java:16` and `62` through `72` register `risk_realtime_features` as a runnable pipeline with SQL asset `sql/risk_realtime_features.sql`.
- `backend/canvas-flink-jobs/src/main/resources/sql/risk_realtime_features.sql:18` through `20` declares a `rocketmq` source connector, and lines `23` through `33` declare a `redis` sink connector.
- `backend/canvas-flink-jobs/pom.xml:37` through `54` packages Doris, MySQL CDC, MySQL, and Jackson dependencies, but no RocketMQ or Redis Flink table connector.
- `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/CanvasFlinkModulePackagingTest.java:12` through `20` only asserts MySQL CDC and Doris connector packaging, so the new connector requirements are not guarded.
- `deploy/helm/canvas/values.yaml:123` through `128` renders submitter Jobs only for the four warehouse DAG pipelines and omits `risk_realtime_features`, so the newly registered risk pipeline is not launched by the default Helm values either.
- `scripts/verify-flink-production-deployment.sh:7` through `13` defines the production preflight `REQUIRED_PIPELINES` as the same four warehouse DAG pipelines, with no `risk_realtime_features` entry; running the script passes while reporting `PASS static submitter defines all 4 required pipeline jobs`.

**Business risk:**

The risk-control realtime feature job is discoverable through the registry and tests, but a real Flink submission will fail when Table API resolves the `rocketmq` or `redis` connector factories unless those connector JARs are supplied out of band. If the feature is expected to power online risk decisions, production can ship with the registry and SQL present while no risk feature stream actually starts or updates Redis/Doris.

**Recommendation:**

Package supported RocketMQ and Redis Flink SQL connectors in `canvas-flink-jobs`, or change the SQL to use connectors already available in the runtime image. Add packaging tests that assert every connector identifier used by registered SQL assets is backed by a dependency or documented mounted plugin. Decide whether `risk_realtime_features` should be in Helm/K8s pipeline lists; if yes, add it with required secrets and live verifier coverage, and if no, keep it out of the registry or mark it experimental.

### F-057 - High - Analytics web SDK exposes event types the backend rejects

**Status:** Open

**Evidence:**

- `sdk/analytics-web/src/index.ts:30` declares SDK event types as `track`, `identify`, `page`, `group`, and `alias`.
- `sdk/analytics-web/src/index.ts:146` through `179` enqueue `identify`, `group`, and `alias` events, and `sdk/analytics-web/src/index.ts:154` through `155` enqueues `page` events.
- `sdk/analytics-web/src/index.ts:234` through `240` flushes the queued mixed event batch directly to the configured ingestion URL with Basic Auth.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/TrackEventReq.java:19` through `29` receives the SDK envelope as `TrackEventReq`, but `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java:135` through `138` lowercases `type` and rejects everything except `track` with `only track events are supported`.
- SDK tests assert the client can queue and send the extra event types at `sdk/analytics-web/src/index.test.ts:59` through `86`, but they do not exercise backend ingestion compatibility for `identify`, `page`, `group`, or `alias`.

**Business risk:**

Applications integrating the SDK will naturally call `identify`, `page`, `group`, or `alias` because those are first-class SDK APIs. Those calls can accumulate in the same persisted queue as valid `track` events and be sent to `/cdp/events/track`, where the backend reports them as invalid instead of ingesting identity, pageview, group, or alias semantics. This creates a silent analytics data gap and can leave customer identity stitching or page attribution unusable even though the SDK appears to support it.

**Recommendation:**

Align the contract in one direction. Either implement backend support for the advertised event types, including persistence and downstream semantics, or remove/disable those SDK APIs until the ingestion endpoint supports them. Add a cross-module contract test or fixture that posts an SDK-shaped batch containing each supported event type to `CdpEventIngestionService` and verifies the expected accepted/rejected behavior.

### F-058 - High - Flyway history verifier still expects the tenant repair at V355 instead of V356

**Status:** Open

**Evidence:**

- The current migration directory contains `backend/canvas-engine/src/main/resources/db/migration/V356__enforce_core_tenant_not_null.sql`, while `V355__enforce_core_tenant_not_null.sql` is absent.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java:73` through `78` explicitly states that `V355` is already tracked by a different migration and the core tenant NOT NULL repair must use `V356`.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CoreTenantNotNullMigrationTest.java:13` through `24` reads and validates `V356__enforce_core_tenant_not_null.sql`.
- `scripts/verify-flyway-history.sh:4` and `85` still document and query only `V91`, `V92`, `V93`, `V272`, `V273`, `V354`, and `V355`, with no `V356` row included.
- `scripts/verify-flyway-history.sh:165` through `168` treats `V355` as the expected `enforce core tenant not null` migration, and `scripts/verify-flyway-history.test.sh:35` through `43` hard-codes a passing fixture where `355	enforce core tenant not null	1`.
- Verification: `bash scripts/verify-flyway-history.test.sh` passes, which proves the shell test currently guards the stale V355 contract rather than the actual V356 migration contract.

**Business risk:**

The runbook-level Flyway repair verifier can give false guidance during staging or production repair checks. A database that correctly applied the current `V356__enforce_core_tenant_not_null.sql` is invisible to this script, while a stale fixture that claims `V355` owns the tenant NOT NULL repair is accepted. That weakens the release team's ability to distinguish safe repaired histories from version-drifted histories during a migration incident.

**Recommendation:**

Update `scripts/verify-flyway-history.sh` and its fixture tests to query and validate `V356` for the core tenant NOT NULL repair. If `V355` is reserved for another tracked migration, validate that expected description separately instead of overloading it. Re-run the shell test and the Java migration policy tests together so the script and application migration contract cannot diverge again.

### F-059 - Medium - Direct-trigger helper scripts still send unsigned requests after HMAC hardening

**Status:** Open

**Evidence:**

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java:91` through `128` documents `POST /canvas/execute/direct/{canvasId}` as a machine-to-machine trigger protected by HMAC headers and calls `parseSignedBody`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java:245` through `255` calls `publicTriggerAuthService.verify(...)` before parsing the direct-call body.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/security/CanvasHmacVerifier.java:45` through `66` rejects blank/short secrets, missing `X-Canvas-Timestamp`, missing `X-Canvas-Signature`, expired timestamps, and invalid signatures.
- `docs/architecture/evidence/P1-02-api-contract-inventory.md:69` states that direct execution requires `X-Canvas-Timestamp` and `X-Canvas-Signature`.
- `scripts/direct-call.sh:2` still labels the direct trigger as unauthenticated internal usage, and `scripts/direct-call.sh:14` through `17` sends only `Content-Type` with no HMAC headers.
- `scripts/direct_execute.sh:19` through `26` also posts to `/canvas/execute/direct/{canvasId}` with only `Content-Type`, and `docs/canvas-examples/CanvasUse.md:9` through `18` still instructs users to run `scripts/direct-call.sh`.

**Business risk:**

The direct-trigger examples and smoke helpers no longer match the hardened runtime contract. Developers or delivery engineers following the checked-in docs will get 401 responses for valid local canvases, which can be misdiagnosed as an engine regression. In the worst case, teams may weaken or bypass HMAC locally to make stale scripts work, drifting further from production behavior.

**Recommendation:**

Update `scripts/direct-call.sh`, `scripts/direct_execute.sh`, and `docs/canvas-examples/CanvasUse.md` to sign the exact JSON body with `CANVAS_PUBLIC_TRIGGER_SECRET` or `CANVAS_EVENT_REPORT_SECRET`, matching `CanvasHmacVerifier`'s `timestamp + "\n" + rawBody` contract. Avoid printing the raw secret. Add a shell fixture test that stubs `date`/`openssl` or reuses the Node signing helper to assert the scripts emit `X-Canvas-Timestamp` and `X-Canvas-Signature`.

### F-060 - High - Risk strategies and lists are production beans backed only by in-memory maps

**Status:** Open

**Evidence:**

- Production configuration wires `RiskStrategyService` as the active strategy reader used by `RiskDecisionService` at `backend/canvas-engine/src/main/java/org/chovy/canvas/config/RiskControlConfiguration.java:67` through `80`, then creates that service with no persistence mappers at `RiskControlConfiguration.java:95` through `100`.
- `RiskStrategyService` stores all strategy state in `private final Map<Key, StrategyState> strategies = new HashMap<>()` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyService.java:27` through `35`; draft creation appends only to this map at `RiskStrategyService.java:48` through `72`.
- Runtime active-strategy lookup also scans the same in-memory map at `RiskStrategyService.java:207` through `217`, so a process restart or second app instance has no active strategy unless it was created in that JVM.
- Production configuration similarly creates `RiskListService` with no `RiskListMapper` or `RiskListEntryMapper` at `RiskControlConfiguration.java:107` through `111`.
- `RiskListService` stores lists and entries in `LinkedHashMap` fields at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListService.java:20` through `26`, and `createList`/`addEntry` write only those maps at `RiskListService.java:42` through `68`.
- The migration creates durable `risk_scene`, `risk_strategy`, `risk_strategy_version`, `risk_list`, and `risk_list_entry` tables in `backend/canvas-engine/src/main/resources/db/migration/V357__risk_control_rule_engine_foundation.sql:1` through `84`, but the production beans above do not use those tables.

**Business risk:**

Risk-control governance appears to have a persisted table model, but live strategy versions and list entries are lost on restart and are not shared across multiple backend instances. Online decisions can fall back to missing-strategy behavior after deployment, while operators may believe an approved and activated strategy is still serving. Manual black/white/gray list updates can also disappear before they affect runtime decisions.

**Recommendation:**

Replace the in-memory governance services with JDBC/MyBatis-backed repositories for strategy, version, list, and entry state, or clearly mark the current services as test-only and keep production routes disabled. Add integration tests that create, approve, activate, restart/rebuild the service from the database, and verify `RiskActiveStrategyReader.findActiveStrategy` plus list lookup still return the saved state.

### F-061 - High - Risk decision persistence violates the V357 NOT NULL contract

**Status:** Open

**Evidence:**

- `risk_decision_run.subject_hash` and `risk_decision_run.mode` are `NOT NULL` in `backend/canvas-engine/src/main/resources/db/migration/V357__risk_control_rule_engine_foundation.sql:88` through `107`.
- `RiskDecisionRunDO` has a `subjectHash` field at `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskDecisionRunDO.java:17` through `24`.
- `JdbcRiskDecisionLedger.toRow` fills tenant, request, scene, strategy, decision, score, band, mode, status, input, output, and timestamp, but never calls `row.setSubjectHash(...)` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/JdbcRiskDecisionLedger.java:88` through `106`.
- Missing active strategies are intentionally converted into a `RiskCompiledStrategy` with `mode = null` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionService.java:77` through `82`, and that mode is copied into the response at `RiskDecisionService.java:175` through `181`.
- The current ledger test asserts many inserted columns at `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/JdbcRiskDecisionLedgerTest.java:67` through `83`, but it does not assert `row.getSubjectHash()` is populated or non-null.

**Business risk:**

Every first-time online risk decision can fail at the database insert boundary because a required subject hash is omitted. The fallback path for a missing active strategy can also attempt to insert a null mode into a NOT NULL column. This turns risk evaluation from a fail-policy-controlled decision into a persistence exception, breaking canvas risk nodes and the public evaluate API at the exact point where a traceable decision should be recorded.

**Recommendation:**

Carry a deterministic, non-PII subject hash through `RiskDecisionRequest`/`RiskDecisionRunRecord` or compute it inside `JdbcRiskDecisionLedger` before insert. Ensure missing-strategy decisions use an explicit runtime mode such as `ENFORCE` or `PAUSED` that satisfies the schema. Add a real schema-backed persistence test, not only a mocked mapper capture, that inserts an evaluation row under V357 and verifies the NOT NULL columns and subject index are valid.

### F-062 - High - Flink realtime risk features cannot be read by the backend feature resolver

**Status:** Open

**Evidence:**

- Backend feature lookup asks the catalog for a subject field, then builds `subjectHash = "hash-" + rawSubject` before reading Redis at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureResolver.java:40` through `57`.
- `RedisRiskFeatureStore` expects Redis values to be JSON envelopes like `{"type":"NUMBER","value":3}` and returns `Optional.empty()` for payloads without a recognized `type` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RedisRiskFeatureStore.java:27` through `69`.
- The backend catalog only defines `user.fail_count_1d`, `user.has_chargeback`, and `user.segment` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureCatalogService.java:6` through `16`.
- The Flink SQL Redis sink declares a key pattern `risk:feature:{tenantId}:{featureKey}:{subjectHash}` at `backend/canvas-flink-jobs/src/main/resources/sql/risk_realtime_features.sql:23` through `33`, but writes `CAST(feature_value AS STRING)` as a plain string at `risk_realtime_features.sql:53` through `54`.
- The SQL populates `subject_hash` with raw `user_id`, `device_id`, or `ip` values and emits additional feature keys such as `user.success_count_1d`, `device.change_user_1d`, `ip.change_user_1h`, and `benefit.issue_amount_1d` at `risk_realtime_features.sql:56` through `83`.

**Business risk:**

Even if the risk realtime Flink job is successfully deployed, backend risk decisions will not resolve its Redis features: keys use raw identifiers rather than the backend's `hash-...` convention, values are not in the backend's type envelope, and most emitted feature names are absent from the catalog. Rules depending on those online features will see them as missing and route through fail-policy behavior, which can either over-review legitimate users or fail open on abuse signals.

**Recommendation:**

Define a single feature-store contract shared by Flink and backend: subject-hash algorithm, Redis key format, value envelope, TTL semantics, and feature catalog entries. Add a cross-module fixture where the SQL-produced key/value for `user.fail_count_1d` is read by `RedisRiskFeatureStore` and `RiskFeatureResolver`, plus tests for every feature key emitted by `risk_realtime_features.sql`.

### F-063 - High - Risk Studio frontend is wired to non-existent and mismatched backend contracts

**Status:** Open

**Evidence:**

- The frontend calls `GET /canvas/risk/scenes` from `frontend/src/services/riskApi.ts:171` through `173`, but the current risk controllers expose `/canvas/risk/strategies`, `/canvas/risk/lists`, `/canvas/risk/decisions`, and `/canvas/risk/lab` only at `RiskStrategyController.java:27` through `41`, `RiskListController.java:27` through `40`, `RiskDecisionController.java:30` through `62`, and `RiskLabController.java:23` through `40`.
- Risk Studio initializes static fallback scenes, strategies, versions, rule groups, list entries, simulations, and traces at `frontend/src/pages/risk/index.tsx:80` through `150`, and falls back to those values when `listScenes` fails at `index.tsx:174` through `198`.
- The frontend strategy command uses `displayName`, `mode`, and `snapshot` at `frontend/src/services/riskApi.ts:39` through `46`, while the backend expects `name`, `riskLevel`, and `definitionJson` at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyCommand.java:3` through `9`.
- The frontend list-entry command uses `subjectKey`, optional `attributes`, and no `subjectType` at `frontend/src/services/riskApi.ts:118` through `123`, while the backend expects `rawSubject`, `subjectType`, `reason`, `source`, and effective windows at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListEntryCommand.java:7` through `14`.
- The frontend decision response type expects an `action` field at `frontend/src/services/riskApi.ts:92` through `108`, while the backend DTO emits `decision` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionEvaluateResponse.java:5` through `21`.
- Risk Studio mutates local UI state even when backend activation, rollback, or simulation calls fail at `frontend/src/pages/risk/index.tsx:269` through `324`; adding a list entry does not call `riskApi.addListEntry` at all at `index.tsx:242` through `245`.

**Business risk:**

Risk operators can see a functional workbench backed by mock data, submit payloads the backend cannot deserialize into the expected semantics, and receive local "activated", "rolled back", or "simulation running" state after a failed server call. For a risk-control surface, that can create false confidence that a production strategy, list entry, or rollback is live when the backend state is unchanged.

**Recommendation:**

Align the TypeScript service contracts with the Java records or add backend DTO adapters for the UI shape. Implement the missing scene/list/query endpoints needed by the workbench, remove static fallback data from authenticated production flows, and only mutate local activation/rollback/simulation/list state after successful API responses. Add contract tests that exercise `riskApi` payloads against controller-level request records and frontend tests proving failed mutations do not update the displayed live state.

### F-064 - Medium - Growth activity filters are shown and tested in the UI but ignored by the backend

**Status:** Open

**Evidence:**

- The frontend query type advertises `campaignId`, `ownerTeam`, `readinessStatus`, `scheduleStatus`, and `grantHealth` filters in addition to type/status at `frontend/src/services/growthActivityApi.ts:5` through `29`, and sends them as query parameters from `growthActivityApi.listActivities` at `growthActivityApi.ts:558` through `561`.
- The Growth Activities page renders controls for activity ID, owner, readiness, schedule, and grant health at `frontend/src/pages/growth-activities/index.tsx:680` through `734`, then includes those values in `applyFilters` at `index.tsx:193` through `205`.
- Frontend tests lock in those extra filters by expecting `listActivities` to receive them at `frontend/src/pages/growth-activities/index.test.tsx:101` through `123`, and the service test asserts the same HTTP query shape at `frontend/src/services/growthActivityApi.test.ts:24` through `54`.
- The backend controller only accepts `activityType`, `status`, and `limit` for `GET /canvas/growth-activities` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java:128` through `135`.
- The service query likewise filters only by tenant, activity type, status, and limit at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityService.java:110` through `124`, with no hidden filtering for campaign, owner, readiness, schedule, or grant health.

**Business risk:**

Operators can narrow the Growth Activities UI by Campaign, owner, launch readiness, schedule, or reward-grant health and still receive unfiltered backend results. This is especially risky for launch readiness and grant health triage because the workbench can show activities that do not match the selected operational condition, making teams act on the wrong activity set or miss failing reward campaigns.

**Recommendation:**

Either implement the advertised filters end-to-end in `GrowthActivityController`/`GrowthActivityService`, including derived readiness/schedule/grant-health semantics, or remove the controls and TypeScript query fields until the backend contract exists. Add backend controller/service tests for each supported filter and a frontend contract test that fails if the page sends parameters the backend does not accept.

### F-065 - Medium - Canvas editor dry-run ignores the UI-selected test user

**Status:** Open

**Evidence:**

- The canvas editor dry-run modal labels an editable `用户 ID` field and binds it to `testUserId` at `frontend/src/pages/canvas-editor/CanvasWorkflowModals.tsx:47` through `48`.
- The dry-run workflow initializes that field to `user_test_001`, builds the current unsaved graph JSON, and calls `canvasApi.dryRun(canvasId, testUserId, ...)` at `frontend/src/pages/canvas-editor/useCanvasTestRunWorkflow.ts:42` through `55`.
- The frontend API client posts `{ userId, inputParams, graphJson }` to `POST /canvas/execute/dry-run/{id}` at `frontend/src/services/api.ts:665` through `670`.
- The backend dry-run request body includes a documented `userId` field at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java:277` through `282`, but the endpoint calls `currentUserId()` and passes the authenticated JWT subject to `executionService.triggerDryRun` at `ExecutionController.java:197` through `208`.
- The existing controller test locks in this mismatch by naming the case `dryRunUsesAuthenticatedSubjectInsteadOfRequestUserId` and verifying `"forged-user"` is ignored in favor of `"auth-user-7"` at `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerTest.java:38` through `63`.
- The engine dry-run path uses the supplied `userId` as the execution context subject at `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java:172` through `180`, and the test-user rerun service intentionally forwards a request/test-user `userId` to that same dry-run entry point at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/TestUserRerunService.java:109` through `140`.

**Business risk:**

Operators can enter a target customer or saved test-user ID in the canvas editor and believe consent, suppression, profile, branch, channel, AI, and personalization logic are being evaluated for that subject. In reality, the backend executes the dry-run under the logged-in operator identity, so test results can look valid while the customer-facing journey would take a different path.

**Recommendation:**

Choose one product contract and make it explicit. If the editor is meant to test arbitrary customer/test-user subjects, use `req.getUserId()` after validating the operator's canvas/test-user access and add a controller test proving that value reaches `CanvasExecutionService.triggerDryRun`. If browser dry-run must always run as the authenticated operator, remove the `用户 ID` input and `userId` request field from the editor flow and route customer-subject testing through `TestUserRerunService`.

### F-066 - High - Risk decision node is implemented but not governed or registered for the editor

**Status:** Open

**Evidence:**

- `NodeType` now exposes `RISK_DECISION` at `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java:48` through `51`, and `RiskDecisionHandler` is a Spring component registered with `@NodeHandlerType(NodeType.RISK_DECISION)` at `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RiskDecisionHandler.java:23` through `25`.
- The node-governance test still expects the governed product node set without `RISK_DECISION` at `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java:19` through `38`; running `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=NodeTypeGovernanceTest test` fails because `RISK_DECISION` is unexpected.
- The final governed registry migration deletes any node types outside an allowlist that also omits `RISK_DECISION` at `backend/canvas-engine/src/main/resources/db/migration/V90__register_commit_action_node.sql:1` through `4`.
- The risk foundation migration creates the risk runtime/governance tables at `backend/canvas-engine/src/main/resources/db/migration/V357__risk_control_rule_engine_foundation.sql:1` through `126`, but it does not add a `node_type_registry` row for `RISK_DECISION`.
- The editor node library loads draggable nodes only from `/meta/node-types` at `frontend/src/components/node-panel/index.tsx:48` through `54`, then serializes the selected registry row into the drag payload at `index.tsx:136` through `151`.
- The canvas editor and config panel already contain `RISK_DECISION` defaults and fallback schema at `frontend/src/pages/canvas-editor/index.tsx:516` through `526` and `frontend/src/components/config-panel/index.tsx:1528` through `1554`, so the frontend partially expects the node to exist once a registry item is available.

**Business risk:**

Risk-decision routing appears to be a canvas node feature, but operators cannot add it through the governed node library because the metadata registry does not expose it. At the same time, full backend test verification is blocked by a stale governance expectation. This can ship a risk-control backend that is callable through lower-level APIs while the journey editor cannot actually place the node, leaving fraud/risk gates out of customer journeys.

**Recommendation:**

Decide whether `RISK_DECISION` is launch-ready. If yes, update the governed node catalog, add a `node_type_registry` migration with config/output/outlet schemas, update `NodeTypeGovernanceTest`, and add an editor/metadata contract test proving `/meta/node-types` includes the draggable risk node. If not, remove or feature-flag the handler/frontend defaults until the node is intentionally released, and keep the governance test green.

### F-067 - High - Message delivery outbox admin API is not tenant-scoped

**Status:** Open

**Evidence:**

- `SecurityConfig` only role-gates `/message-deliveries` and `/message-deliveries/**` at `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:141` through `143`; it does not bind requests to the current tenant.
- `MessageDeliveryController.list` accepts an optional caller-supplied `tenantId` and passes it directly into `DeliverySearchCriteria` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageDeliveryController.java:60` through `74`.
- Detail, receipts, and replay all use global outbox IDs without tenant context at `MessageDeliveryController.java:86` through `91`, `103` through `106`, and `118` through `126`.
- `DeliveryOutboxService.search` builds SQL directly from criteria at `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java:266` through `283`; `buildWhere` adds a tenant clause only if the request provided one at `DeliveryOutboxService.java:411` through `427`.
- Receipt history filters only by `outbox_id` at `DeliveryOutboxService.java:286` through `291`, and `replayDead` updates by `id` and `status` only at `DeliveryOutboxService.java:199` through `207`.
- The frontend service exposes `tenantId?: number` on search params and provides list/detail/receipts/replay/reconcile calls at `frontend/src/services/messageDeliveryApi.ts:111` through `176`, while the page exposes detail, replay, and reconcile actions at `frontend/src/pages/message-delivery/index.tsx:132` through `159`.

**Business risk:**

A tenant admin or operator with the message-delivery route role can omit `tenantId` to list all tenants' outbox rows, supply another tenant ID, read provider payload and receipt history by global ID, or replay another tenant's dead delivery. That leaks customer/user/provider message data and can resend customer-facing messages across tenant boundaries.

**Recommendation:**

Resolve tenant scope from `TenantContext` in the controller and pass it as a required service argument for list, detail, receipt history, replay, and reconcile. Allow explicit cross-tenant search only for a separate super-admin path. Add route/controller/service tests proving tenant A cannot list, read, or replay tenant B outbox rows by omitting or forging `tenantId`.

### F-068 - Medium - Event configuration page shows stale and split ingestion instructions

**Status:** Open

**Evidence:**

- The Event Config page shows a copyable example for `POST /canvas/events/report` with only the JSON body at `frontend/src/pages/event-config/index.tsx:305` through `315`.
- That endpoint exists, but the controller verifies HMAC before parsing the body at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java:154` through `164`.
- `EventReportAuthService` delegates raw-body HMAC verification at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventReportAuthService.java:47` through `49`, and `CanvasHmacVerifier` requires a configured 32-byte secret plus `X-Canvas-Timestamp` and `X-Canvas-Signature` at `backend/canvas-engine/src/main/java/org/chovy/canvas/security/CanvasHmacVerifier.java:45` through `66`.
- The same page manages CDP Write Keys at `frontend/src/pages/event-config/index.tsx:137` through `197`, and `cdpEventApi` uses `/cdp/write-keys` at `frontend/src/services/cdpEventApi.ts:279` through `287`.
- The actual CDP SDK ingestion endpoint is `POST /cdp/events/track` at `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java:39` through `45`, authenticated by Basic write key validation at `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java:40` through `49`.
- The analytics SDK posts Basic Auth to its configured `serverUrl` at `sdk/analytics-web/src/index.ts:234` through `240`.

**Business risk:**

Operators are shown a JSON-only example that will 401 against `/canvas/events/report`, while the same screen's Write Key management belongs to `/cdp/events/track`. Integrators can wire browser SDK tracking into the behavior-trigger endpoint or try to remove HMAC from the Canvas trigger endpoint to match the UI, causing failed tracking, missed journey triggers, or weakened public-trigger security.

**Recommendation:**

Split the UI documentation into "Canvas behavior trigger" and "CDP SDK tracking". Show `X-Canvas-Timestamp` and `X-Canvas-Signature` for `/canvas/events/report`, and show `/cdp/events/track` with Basic write-key auth for SDK tracking. Add a frontend contract test or docs fixture that fails when the page omits required headers for HMAC-protected examples.

### F-069 - High - Flink submitter retries can duplicate streaming jobs after checkpoint-report failure

**Status:** Open

**Evidence:**

- `CanvasFlinkJobMain.run` submits all rendered SQL statements, then reports startup checkpoint evidence in the same `try` block at `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkJobMain.java:52` through `55`.
- If the startup report fails, the same catch path calls `reportFailure` and rethrows at `CanvasFlinkJobMain.java:56` through `59`, while `reportFailure` tries another checkpoint report at `CanvasFlinkJobMain.java:80` through `88`.
- `CanvasFlinkCheckpointReporter.report` throws for non-2xx responses, IO failures, and interrupts at `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/CanvasFlinkCheckpointReporter.java:81` through `105`.
- Helm and static submitters both run `flink run -d` and allow Kubernetes Job retries with `backoffLimit: 3` at `deploy/helm/canvas/templates/flink-job-submitter.yaml:16` through `17` and `103` through `107`, and `deploy/k8s/canvas-flink-job-submitter.yaml:13` through `14` and `105` through `109`.
- The production runbook also uses detached `flink run -d` for restore submissions at `docs/runbooks/flink-production-deployment.md:151` through `158`.

**Business risk:**

The failure mode after SQL submission is different from a pre-submit validation failure. If the INSERT statements have already started detached Flink jobs and only the Canvas checkpoint callback is unavailable or blocked, the client exits non-zero and the Kubernetes submitter retries. That can start duplicate CDC/aggregation jobs for the same pipeline, duplicating Doris writes, competing for checkpoints, and making cutover evidence ambiguous.

**Recommendation:**

Separate "SQL submitted" from "startup evidence reported". After a successful detached submission, do not fail the submitter solely because the advisory startup report failed; instead emit local logs/metrics and let runtime checkpoint evidence determine readiness. Add a regression test where `executor.execute` succeeds, `checkpointSink.report` throws, and the launcher either returns a distinct warning result or fails before any SQL is submitted. Consider deterministic job names or a pre-submit duplicate-running-job guard per pipeline.

### F-070 - High - Flink network policies can block required production traffic

**Status:** Open

**Evidence:**

- The static `canvas-engine` NetworkPolicy allows ingress to backend port 8080 only from `ingress-nginx` and `monitoring` namespaces at `deploy/k8s/canvas-engine-network-policy.yaml:18` through `29`.
- The static Flink NetworkPolicy explicitly allows Flink egress to the backend pod on 8080 for checkpoint reporting at `deploy/k8s/canvas-flink-network-policy.yaml:57` through `64`, but NetworkPolicy is bidirectional; the backend ingress policy above still does not allow that source.
- The static Flink secret points checkpoint reports to `http://canvas-engine.canvas.svc.cluster.local:8080/warehouse/realtime/pipelines/checkpoints` at `deploy/k8s/canvas-flink-secret.example.yaml:22` through `25`.
- The Helm Flink NetworkPolicy selects every pod with the chart-wide `app.kubernetes.io/name` and `app.kubernetes.io/instance` labels at `deploy/helm/canvas/templates/flink-network-policy.yaml:11` through `17`, while backend, frontend, JobManager, and TaskManager pods all share those labels via `canvas.labels` at `deploy/helm/canvas/templates/_helpers.tpl:15` through `20`.
- That Helm policy then opens only Flink REST/RPC ingress ports 8081 and 6123 at `deploy/helm/canvas/templates/flink-network-policy.yaml:18` through `36`, and its intended backend source selector uses `app.kubernetes.io/component: canvas-engine` at `flink-network-policy.yaml:26` through `28`, while the backend component label is `backend` at `deploy/helm/canvas/templates/deployment.yaml:27` through `30`.

**Business risk:**

Static Kubernetes deployment can bring up Flink and Engine pods but block the checkpoint callback that the jobs use to report pipeline state. Helm deployments with `.Values.flink.enabled=true` can accidentally apply a Flink-only ingress/egress policy to backend and frontend pods, breaking ingress traffic and backend dependency access while the rendered manifests still look valid.

**Recommendation:**

Scope the Helm Flink NetworkPolicy `podSelector` to Flink components only, for example by matching `app.kubernetes.io/component in (flink-jobmanager, flink-taskmanager, flink-job-submitter)` through separate policies. Add reciprocal backend ingress rules allowing Flink submitter/runtime pods to call `/warehouse/realtime/pipelines/checkpoints`. Add static policy tests or `helm template` assertions that backend/frontend pods are not selected by the Flink policy and that the checkpoint path has both egress and ingress permission.

### F-071 - Medium - Release image build defaults to the perf Dockerfile

**Status:** Open

**Evidence:**

- `scripts/release/build-image.sh` defaults `CANVAS_DOCKERFILE` to `backend/canvas-engine/Dockerfile.perf` at `scripts/release/build-image.sh:9` through `12`.
- The `canvas-ci` container-build job calls that script directly with only `CANVAS_IMAGE_TAG` at `.github/workflows/canvas-ci.yml:93` through `98`.
- The separate CI smoke job builds the normal backend image with `docker build -f backend/canvas-engine/Dockerfile backend` at `.github/workflows/ci.yml:95` through `99`.
- The normal Dockerfile performs a multi-stage Maven build from the backend context and sets `-Dspring.profiles.active=prod` in image-level `JAVA_OPTS` at `backend/canvas-engine/Dockerfile:1` through `28`.
- `Dockerfile.perf` is a prepackaged-jar runtime image, has no profile default, and was introduced for local capacity testing at `backend/canvas-engine/Dockerfile.perf:1` through `22`; docs still describe it as the local capacity/perf image path at `docs/stressTest/local-capacity-runbook.md:67`.

**Business risk:**

The release script and one CI workflow build a different image contract than the production Dockerfile smoke-tested elsewhere. A release can pass through `build-image.sh` with a perf-oriented Dockerfile that omits the normal build-stage checks and image-level prod profile default, while the standard Dockerfile can drift or break independently. That weakens confidence that the image promoted by release governance matches the image validated by production-operability docs and smoke CI.

**Recommendation:**

Make `backend/canvas-engine/Dockerfile` the default release Dockerfile, or rename/document the perf Dockerfile as the intentional release runtime image and update all CI/docs to smoke-test the same path. Add a release-script test asserting the default Dockerfile value, and keep one canonical backend image build command across `ci.yml`, `canvas-ci.yml`, and `docs/architecture/evidence/runbooks/release-deployment.md`.

### F-072 - Medium - Documentation indexes point to moved or missing sources

**Status:** Open

**Evidence:**

- The root docs index still links product strategy and product-evolution documents at their old top-level paths at `docs/INDEX.md:69` through `91`, including `./product-evolution/product-strategy-dual-track-2026-05-31.md` and `./product-evolution/mautic-comparison-2026-06.md`.
- The product-evolution todo index says those original sources were archived under `../archive/2026-06-03/` at `docs/product-evolution/todo/INDEX.md:7` and records the archive path for representative sources at `docs/product-evolution/todo/INDEX.md:74` through `89`, so the root index no longer points at the maintained lineage path.
- The root docs index also links a removed `product-docs` directory at `docs/INDEX.md:94` through `95`.
- The root docs index links the old code-review reports at `docs/INDEX.md:100` through `123`, while the current tree stores those original reports under `docs/code-review/archive/original-reports-2026-06-03/`.
- The old evolution index still links direction files relative to `docs/optimization/todo/plans/` at `docs/optimization/todo/plans/evolution-index/README.md:20` through `26`, but those direction documents now live under `docs/optimization/todo/specs/`.
- The same old evolution index points to root-level strategy/audit files at `docs/optimization/todo/plans/evolution-index/README.md:258` through `269`, including `../../../../product-strategy-discussion-2026-05.md` and `../../../../deep-code-audit-2026-05-31.md`, which are no longer present at those paths.

**Business risk:**

Engineers, reviewers, and AI agents using the documented entry points can land on dead links for product strategy, prior code-review evidence, and optimization lineage. That weakens traceability from current specs/plans back to source research and can cause remediation work to skip historical audit evidence or treat archived decisions as missing.

**Recommendation:**

Update `docs/INDEX.md` to point to the active product-evolution indexes and archived source paths, or explicitly mark retired sections as archived with working archive links. Either remove the obsolete `docs/optimization/todo/plans/evolution-index/README.md` from active navigation or rewrite its links to the current `specs`, `archive`, and `code-review/archive` locations. Add a lightweight Markdown local-link check to CI so repository moves cannot leave the main documentation index broken again.

### F-073 - Medium - Root deployment docs use a backend Docker build context that no longer matches the Dockerfile

**Status:** Open

**Evidence:**

- The single-machine deployment section tells operators to run `docker build -t canvas-engine:latest ./backend/canvas-engine` from the repository root at `README.md:117` through `120`.
- The production Compose section repeats the same backend image build command at `README.md:319` through `321`.
- The current backend Dockerfile expects the Docker build context to contain `pom.xml`, `canvas-cache-sdk/pom.xml`, `canvas-engine/pom.xml`, and both module source trees at `backend/canvas-engine/Dockerfile:3` through `12`.
- The CI smoke build uses the backend directory as context with an explicit Dockerfile path at `.github/workflows/ci.yml:95` through `99`, which matches the Dockerfile's `COPY canvas-cache-sdk/...` and `COPY canvas-engine/...` assumptions.
- The separate agent guidance still says `docker compose up -d` and claims the migration directory has "81 files V1-V81" at `CLAUDE.md:27` through `31`, while `AGENTS.md:9` through `13` correctly points local infrastructure at `docker-compose.local.yml`.

**Business risk:**

An operator following the root README's backend image command will build with `backend/canvas-engine` as the context, so the Dockerfile cannot copy `canvas-cache-sdk` or the parent Maven module layout it needs. That blocks Docker single-machine and production-Compose deployment from the documented path, and conflicting top-level guidance can send automation agents toward stale startup and migration assumptions.

**Recommendation:**

Make the README use the same canonical command as CI, for example `docker build -f backend/canvas-engine/Dockerfile backend`, or change the Dockerfile to support the documented context. Update `CLAUDE.md` to match `AGENTS.md` for local compose startup and remove stale migration-count language. Add a lightweight docs/CI check that validates the README Docker build command against the canonical image build path.

### F-074 - Medium - Orphan top-level canvas-engine test is outside the Maven reactor

**Status:** Open

**Evidence:**

- A test exists under the repository root at `canvas-engine/src/test/java/org/chovy/canvas/controller/FontResourceControllerTest.java:11` through `38`.
- That test instantiates `FontResourceController` directly at `FontResourceControllerTest.java:15` and `32`, but a search of `backend/canvas-engine/src/main/java` and `backend/canvas-engine/src/test/java` finds no `FontResourceController` production or test class.
- The repository root has no `pom.xml`, while the actual Maven reactor is `backend/pom.xml` and lists only `canvas-cache-sdk`, `canvas-engine`, and `canvas-flink-jobs` modules at `backend/pom.xml:19` through `23`.
- The actual application module is `backend/canvas-engine` with parent `../pom.xml` at `backend/canvas-engine/pom.xml:7` through `12`, so the root-level `canvas-engine/src/test` tree is not part of the normal backend test commands documented in `AGENTS.md:15` through `28`.

**Business risk:**

This looks like regression coverage for a font resource endpoint, but it is not compiled or run by the documented Maven reactor. If the endpoint is intended to exist, production can lose the font route without a failing test; if it is not intended, the orphan tree creates false coverage signals and broadens whole-repository scans with dead code.

**Recommendation:**

Either move the test and the corresponding `FontResourceController` implementation into `backend/canvas-engine` with a focused module test, or delete the orphan root-level `canvas-engine/` tree. Add a repository guard that fails when Java sources appear outside the declared backend Maven modules unless they are explicitly documented as examples.

### F-075 - Medium - Local/generated artifacts are tracked despite ignore rules

**Status:** Open

**Evidence:**

- `.gitignore` excludes `.DS_Store`, `*.log`, `logs/`, `.superpowers/`, and `.worktrees/` at `.gitignore:1` through `4`, `27` through `29`, and `40` through `43`.
- `.dockerignore` also excludes `.DS_Store`, logs, pid files, temp files, and local environment files at `.dockerignore:5` and `.dockerignore:13` through `22`.
- The Git index still contains 21 matching local/generated artifacts, including `.DS_Store`, `logs/error.log`, `.claude/scheduled_tasks.lock`, and multiple `.superpowers/brainstorm/**` files.
- `.claude/scheduled_tasks.lock:1` stores a local session id, pid, process start time, and acquisition timestamp.
- `.superpowers/brainstorm/25798-1778997192/state/events:1` through `3` records local UI click telemetry text and timestamps, while `.superpowers/brainstorm/58083-1778932886/state/server.pid:1` and `.superpowers/brainstorm/75959-1779021861/state/server-stopped:1` store local process/runtime state.
- `.superpowers/brainstorm/58083-1778932886/content/version-history.html:1` through `33` is generated brainstorming/demo HTML scaffolding rather than application source.

**Business risk:**

The repository's ignore policy says these files are local or generated, but they are already tracked. That pollutes reviews and future diffs, can leak local session/process metadata, and weakens confidence that logs, brainstorm artifacts, or other generated state will stay out of commits when they contain real customer, credential, or incident details later.

**Recommendation:**

Remove the tracked local/generated artifacts from the index while keeping the ignore rules, and decide whether `.bmad-core`/`.claude` are intentional project assets or should be documented as local agent tooling. Add a CI guard using `git ls-files` against the ignore patterns for logs, `.DS_Store`, `.superpowers`, pid/state files, and generated class files.

## Verification Notes

- Frontend targeted tests passed: `npm run test -- --run src/pages/bi/biWorkbench.test.ts src/services/biApi.test.ts` reported 2 files and 127 tests passed.
- Backend targeted tests passed under Java 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-engine -Dtest=BiDeliverySchedulerServiceTest,BiDeliveryRuntimeServiceTest,BiDeliveryAttachmentServiceTest,BiChartReferenceImpactServiceTest,BiDatasetResourceServiceTest` reported 5 test classes, 59 tests, 0 failures.
- Frontend BI/API targeted tests passed: `npm run test -- --run src/pages/bi/index.test.tsx src/pages/bi/biWorkbench.test.ts src/services/biApi.test.ts` reported 3 files, 176 tests, 0 failures. Duration was 255.26s, with `index.test.tsx` dominating runtime.
- Backend route security tests passed under Java 21 after gating documentation routes by production-like profile: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest,SecurityConfigRoleTest test` reported 30 tests, 0 failures.
- Tooling Node tests passed: `node --test tools/perf/*.test.mjs tools/open-source-growth/*.test.mjs tools/strategy/*.test.mjs` reported 181 tests, 0 failures.
- Tooling Java MQ producer tests passed under Java 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test` in `tools/perf/mq-producer` reported 9 tests, 0 failures.
- Backend risk-control targeted tests passed under Java 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest='Risk*Test,Risk*ControllerTest,RiskDecisionHandlerTest' test` reported 148 tests, 0 failures, 0 errors. These tests currently do not cover schema-backed risk-decision inserts, persistence across service rebuilds, or Flink-produced Redis feature payloads.
- Frontend risk/API targeted tests passed: `npm run test -- --run src/services/riskApi.test.ts src/pages/risk/riskWorkbench.test.ts src/types/canvasSchemas.test.ts` reported 3 files and 29 tests passed. These tests currently assert the frontend's stale risk API contract rather than a backend-compatible contract.
- Backend node-governance targeted test currently fails under Java 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=NodeTypeGovernanceTest test` reports `RISK_DECISION` as an unexpected governed node type, matching F-066.
- Flyway history verifier shell tests passed but currently guard stale V355 expectations: `bash scripts/verify-flyway-history.test.sh` printed `verify-flyway-history tests passed`.
- Release Flyway policy currently fails on the missing tracked migrations in F-052: `bash scripts/release/check-flyway-migration.sh` reports missing `V91__sanitize_demo_datasource_credentials.sql`, `V92__enforce_core_tenant_not_null.sql`, `V272__sanitize_demo_datasource_credentials.sql`, and `V273__enforce_core_tenant_not_null.sql`.
- Release pre-deploy dry-run currently fails for the same Flyway policy reason after profile validation: `bash scripts/release/pre-deploy-check.sh --dry-run`.
- Flink production preflight passed while confirming only four required submitter jobs: `bash scripts/verify-flink-production-deployment.sh` passed with promtool and helm checks skipped because those CLIs are not installed.
- Backend route security tests passed under Java 21 after adding high-impact tenant control-plane route gates: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-engine -Dtest=SecurityConfigRouteTest` reported 21 tests, 0 failures.
- Backend route and delivery receipt tests passed under Java 21 after making receipt shared-secret validation fail closed: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-engine -Dtest=SecurityConfigRouteTest,DeliveryReceiptControllerTest` reported 24 tests, 0 failures.
- Backend legacy runtime approval tenant-binding tests passed under Java 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=CanvasExecutionManagementControllerTest test` reported 2 tests, 0 failures.
- Shell syntax check passed: `for f in scripts/*.sh scripts/release/*.sh; do bash -n "$f"; done` completed with no syntax errors.
- Script self-tests passed: `for f in scripts/*.test.sh; do bash "$f"; done` reported passing quickbi, Flyway history, Lark approval, marketing-content release/upload, and quickbi focus verification script tests.
- Analytics web SDK tests passed: `npm test -- --run src/index.test.ts` in `sdk/analytics-web` reported 1 file and 8 tests passed.
- Documentation local-link scan found 78 missing local Markdown links outside `docs/code-review/archive`, with the active root index and old evolution index examples captured in F-072.
- Root/hidden inventory scan found tracked local/generated artifacts and separate nested `.worktrees`: `git ls-files` includes `.DS_Store`, `logs/error.log`, `.claude/scheduled_tasks.lock`, and `.superpowers/brainstorm/**`, while `rg --files -uu` shows `.worktrees` as separate nested worktree snapshots rather than the root release path.
- Root build-documentation review found the README Docker build command diverges from the CI backend image command and Dockerfile context contract, captured in F-073.
- Root Java-source inventory found one orphan source file outside the backend Maven reactor, `canvas-engine/src/test/java/org/chovy/canvas/controller/FontResourceControllerTest.java`, captured in F-074.
- Final coverage ledger consistency check passed: a no-open-scope marker scan found no unfinished ledger rows and no stale "still needs broader review" prose outside this verification summary.
- Final report whitespace check passed: `perl -ne 'if (/[ \t]$/) { print "$ARGV:$.: trailing whitespace\n"; $bad=1 } END { exit($bad ? 1 : 0) }' docs/code-review/2026-06-07-whole-project-review.md`.
- Final root build/static evidence check confirmed the documented bad README context lacks `canvas-cache-sdk`, the canonical backend build context has it, there is no root `pom.xml`, the only root-level `canvas-engine` file is the orphan font test, and 21 tracked files still match the local/generated artifact set recorded in F-075.
- Environment note: default shell Java is Java 8, while repository backend requires Java 21. Use `JAVA_HOME=$(/usr/libexec/java_home -v 21)` for backend verification.

## Mitigation Notes

- 2026-06-08: Added tenant-admin route gates for representative high-impact write paths behind F-005, F-006, F-008, F-009, F-010, F-011, F-012, F-014, F-015, F-016, F-017, F-018, F-019, and F-020. This closes the ordinary authenticated-user route fallback for those HTTP writes and preserves self-service BI permission request submission plus read-only authenticated fallbacks.
- 2026-06-08: Made delivery receipt shared-secret validation fail closed when `canvas.delivery.receipt.secret` is blank, covering F-021's unauthenticated blank-secret path.
- 2026-06-08: Added service-level reviewer separation for F-010, F-011, and F-012 live provider mutation approvals, and expanded route tests so approve and execute endpoints for SEM, creator, and DSP are all denied to `OPERATOR` and allowed to `TENANT_ADMIN`.
- 2026-06-08: Added tenant binding to the legacy runtime manual approval fallback for F-013, including a nullable `tenant_id` column/backfill and fail-closed `ExecutionContext` fallback for historical rows.
- 2026-06-08: Gated Swagger/OpenAPI documentation routes behind `publicDocumentationEnabled(Environment)`, so production-like profiles use the authenticated fallback while local profiles remain anonymous for developer access.

- Remaining service/business follow-up: F-021 still needs stronger timestamped/body-bound HMAC if provider replay protection is required. F-007 and F-025 onward remain open, with F-052 additionally blocked on restoring the currently deleted tracked Flyway migrations before its policy guards can pass.

## Review Log

- 2026-06-07: Initial findings recorded for BI scheduler permission behavior, chart designer state persistence, and SQL dataset validation.
- 2026-06-07: Added cache SDK batch-loader resilience finding after reviewing `TieredCacheImpl` and test cache behavior.
- 2026-06-07: Re-ran backend verification with Java 21 and expanded frontend BI/API test verification.
- 2026-06-07: Added BI permission governance authorization finding after checking service, controller, and global security config.
- 2026-06-07: Added CDP write-key governance finding after checking security config, controller, and write-key service authorization boundaries.
- 2026-06-07: Added WhatsApp conversation webhook tenant-binding finding after checking public route, signature service, payload mapper, and ingestion path.
- 2026-06-07: Added warehouse operational/privacy authorization finding after checking security config and representative mutation controllers.
- 2026-06-07: Added CDP webhook subscription governance finding after checking route security, subscription controller, validator, and dispatcher.
- 2026-06-07: Added search marketing provider-write approval/execution authorization finding after checking controller, service, and security fallback.
- 2026-06-07: Added creator collaboration provider-write approval/execution authorization finding after checking controller, service, gateway call, and mutation types.
- 2026-06-07: Added programmatic DSP provider-write approval/execution authorization finding after checking controller, service, gateway call, and mutation types.
- 2026-06-07: Added legacy runtime manual approval tenant-binding finding after checking unified approval path, fallback query, and legacy approval data object.
- 2026-06-07: Added execution request replay authorization finding after checking replay controller, tenant filtering, force behavior, and dispatch path.
- 2026-06-07: Added AI provider, channel connector mode, and marketing monitoring credential/webhook-secret authorization findings after checking route security, controllers, and service-layer side effects.
- 2026-06-07: Added paid-media audience sync, loyalty points, and growth reward authorization findings after checking route security, controllers, and state-changing service methods.
- 2026-06-07: Added delivery receipt callback authentication finding after checking anonymous route handling, secret fallback, and outbox receipt update lookup.
- 2026-06-08: Added route-level tenant-admin gates and regression coverage for high-impact control-plane write endpoints.
- 2026-06-08: Added fail-closed delivery receipt shared-secret validation and regression coverage for blank configured secrets.
- 2026-06-07: Added A/B experiment, marketing policy admin, and marketing integration contract/probe authorization findings after checking route security, controllers, runtime consumers, and service side effects.
- 2026-06-07: Added computed CDP job and batch tag-operation authorization findings after checking route security, controllers, service mutation paths, and tenant scoping.
- 2026-06-07: Added marketing preference, campaign, and private-domain sync authorization findings after checking route security, arbitrary user/resource mutation paths, and ingestion side effects.
- 2026-06-07: Added static and realtime audience authorization findings after checking audience definition, compute, bitmap, event-ingestion, snapshot, and set-operation paths.
- 2026-06-07: Added MQ rejected-message replay authorization finding after checking rejected record list/detail/replay routes and execution request dispatch side effects.
- 2026-06-08: Added direct CDP tag mutation, tag/identity metadata, and plugin enablement findings after checking current route security and service persistence side effects.
- 2026-06-08: Added DLQ replay/delete, execution rerun tenant ownership, marketing form control-plane, conversation ingress, and conversation workspace routing findings after checking current security fallback, controllers, and service side effects.
- 2026-06-08: Added BI datasource governance, BI resource creation/archive, AI prompt template, and demo sandbox findings after checking current route security, controller entry points, and service-layer permission behavior.
- 2026-06-08: Added MQ message definition routing-governance finding after checking current security fallback, definition controller mutations, route rebuild side effects, and runtime topic resolution.
- 2026-06-08: Added BI publish approval self-review, Quick Engine capacity policy, and BI embed ticket minting findings after checking current security fallback, controller entry points, service-layer role checks, and public embed consumption paths.
- 2026-06-08: Marked the null batch-loader NPE finding resolved after confirming implementation and test coverage, and added plaintext datasource credential migration plus batch-loader exception policy findings.
- 2026-06-08: Added frontend unauthorized-state finding after checking API interceptors, event listener, AuthProvider state lifecycle, route guards, and existing AuthProvider tests.
- 2026-06-08: Added Flyway migration deletion and BI embed ticket access-budget findings after checking current diff, migration policy tests, ticket defaults, consuming verification paths, and embed page call sequence.
- 2026-06-08: Marked the chart designer stale filter/sort persistence finding resolved after confirming refs are updated with state and existing BI page coverage asserts saved filter/sort payloads.
- 2026-06-08: Marked the delivery receipt blank-secret authentication finding resolved after confirming fail-closed controller behavior and regression coverage; stronger timestamped/body-bound HMAC remains a separate hardening follow-up.
- 2026-06-08: Added BI resource movement and ownership-transfer governance finding after checking route fallback, controllers, services, and existing service tests.
- 2026-06-08: Added user-input submission authorization finding after checking route fallback, controller delegation, response lookup/update predicates, and existing tests.
- 2026-06-08: Added whole-repository coverage ledger and Flink risk realtime feature pipeline finding after checking pipeline registry, SQL connector usage, job packaging dependencies, tests, and Helm pipeline rendering.
- 2026-06-08: Added analytics web SDK/backend ingestion contract finding after checking SDK event APIs, flush payload, backend DTO, ingestion service type validation, and SDK-only tests.
- 2026-06-08: Completed a no-new-finding pass over tooling after checking perf cleanup/verifier/runner/reporting scripts, open-source growth guardrails, strategy evidence validators, MQ producer JSON/argument handling, and passing the Node plus MQ producer test suites.
- 2026-06-08: Added Flyway history verifier drift finding after checking the migration directory, Java migration policy tests, shell verifier query/fixtures, and the passing stale shell test.
- 2026-06-08: Ran release/profile/Flink production scripts; profile validation passed, release Flyway and pre-deploy dry-run fail on F-052 missing tracked migrations, and Flink production preflight passes with the four-pipeline set that supports F-056.
- 2026-06-08: Added stale direct-trigger helper finding after checking direct helper scripts, canvas example docs, HMAC verifier behavior, execution controller signing path, and the API contract inventory.
- 2026-06-08: Added risk-control persistence, risk-decision schema, realtime-feature contract, and Risk Studio API drift findings after checking risk controllers, production bean wiring, governance/runtime services, V357, Redis/Flink feature paths, frontend service types, and risk workbench state transitions.
- 2026-06-08: Ran backend and frontend risk-targeted tests; both passed, but the passing coverage confirms the current mock/in-memory/frontend-contract assumptions rather than the production persistence and cross-module contracts flagged in F-060 through F-063.
- 2026-06-08: Completed remaining frontend route/service review, adding message-delivery tenant-scope and event-config ingestion-instruction findings after checking UI actions, generated API clients, backend controllers, and ingestion auth services.
- 2026-06-08: Completed Flink runner, pipeline registry, SQL asset, checkpoint reporter, Kubernetes submitter, Helm/static NetworkPolicy, and production runbook review, adding duplicate-submit and network-policy findings while confirming the existing risk realtime pipeline gap remains covered by F-056/F-062.
- 2026-06-08: Completed deploy/ops/CI/local-infrastructure review, adding release Dockerfile drift while finding no additional findings in alert/dashboard metric naming, RocketMQ local config, WireMock fixtures, CODEOWNERS, or PR template.
- 2026-06-08: Completed scripts, tooling, and analytics SDK review. Shell syntax, all script self-tests, SDK Vitest, existing Node tooling tests, and MQ producer tests passed; no additional findings beyond F-057 through F-059 and F-071.
- 2026-06-08: Completed documentation and generated/planning artifact review, adding documentation index/link drift after scanning root docs navigation, product-evolution indexes, canvas examples, stress-test runbooks, Flink/risk/Lark/marketing-content runbooks, DDD rewrite packets, and program-coordination docs.
- 2026-06-08: Added growth-activity filter contract finding after checking frontend query types, page filter controls/tests, backend controller parameters, and service query predicates; completed no-new-finding API/state passes over marketing platform, marketing monitoring, content hub, marketing forms, marketing preferences, conversations, and search marketing.
- 2026-06-08: Added canvas-editor dry-run subject finding after checking the dry-run modal/workflow/API client, backend request DTO, controller subject selection, existing controller test, engine dry-run context creation, and test-user rerun service.
- 2026-06-08: Added risk-decision node catalog governance finding after checking NodeType, handler registration, node registry migrations, editor node library loading, config-panel fallback schema, and the failing `NodeTypeGovernanceTest` verification.
- 2026-06-09: Completed root-level and hidden artifact review after the coverage audit exposed areas outside the main ledger. Added README/Docker build-context drift, orphan root `canvas-engine` test tree, and tracked local/generated artifact findings; `.bmad-core`, `.claude`, `.superpowers`, `logs`, `.worktrees`, root ignore files, and ignored `backend/com` class artifacts were included in the coverage inventory.
