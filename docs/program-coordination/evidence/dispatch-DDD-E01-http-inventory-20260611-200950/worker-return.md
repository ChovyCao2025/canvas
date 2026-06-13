# DDD-E01 HTTP Inventory Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-E01
dispatch id: dispatch-DDD-E01-http-inventory-20260611-200950
worker: multi_agent_v1-explorer Mendel 019eb695-30f2-7ec1-bed0-fbe138e2d53d

## Files Read

- Required coordination and DDD inventory docs listed in the dispatch.
- All Java files under `backend/canvas-engine/src/main/java/org/chovy/canvas/web/**/*.java`.
- 142 Spring controllers found.
- 11 non-controller support/DTO files under web scope, including
  `EventReportAuthService.java`, risk audit/trace/budget/cache support, and
  `risk/dto/*.java`.

## Findings

- Parser expanded annotation array aliases and found 804 concrete verb/path
  endpoints across 142 controllers.
- Duplicate concrete verb/path pairs: none.
- Compatibility levels used:
  - `UI_STRICT`: preserve old verb/path/request/response for frontend/API cutover.
  - `PUBLIC_STRICT`: external/public/auth/webhook compatibility.
  - `ADMIN_STRICT`: preserve internal/admin route shape.
  - `BRIDGE_DECISION`: route must be bridged until owner is decided.
  - `DEV`: test/dev route.
- Compact inventory highlights:
  - AB experiment, governance, admin, AI, analytics, approvals, async tasks,
    API definitions, notifications, ops, plugin registry, system options,
    tenant, and several delivery/metadata routes are `BRIDGE_DECISION`.
  - Canvas controllers under `/canvas/*` and `/admin/projects/*` are mostly
    canvas-owned with `UI_STRICT` / `ADMIN_STRICT`; stats split canvas and
    execution.
  - Execution controllers under `/canvas/execution*`, `/execution-reruns`,
    `/user-input`, and `/canvas/dlq` are execution-owned with `UI_STRICT`.
  - CDP audience/profile/tag/user/webhook and warehouse controllers are
    CDP-owned with `UI_STRICT`.
  - Conversation controllers include UI and public webhook aliases and require
    `UI_STRICT` / `PUBLIC_STRICT`.
  - Marketing, Mautic, paid media, programmatic, search, and contactability
    routes are marketing-owned, with public form/webhook paths requiring
    `PUBLIC_STRICT`.
  - BI controllers under `/canvas/bi/*` include 51 GET, 75 POST, 1 PUT, and 13
    DELETE route methods and are BI-owned with `UI_STRICT`.
  - Risk controllers under `/canvas/risk/*` include 9 GET, 15 POST, and 1 DELETE
    route methods and are risk-owned with `UI_STRICT`.

## Ambiguous Ownership

- Seed-confirmed ambiguous areas: approval, async task, notification,
  auth/tenant administration, plugin registry, AI provider/decision, loyalty,
  message delivery, channel connector, analytics, AB experiment, creator
  collaboration, and ops/admin.
- Additional web cutover ambiguity from route shape/imports: metadata aggregation
  and runtime definitions, including API definitions, event definitions/reporting,
  meta options/node schemas, and MQ definitions.
- `CanvasStatsController` and `HomeOverviewController` read canvas and execution
  metrics and should be split or exposed through a composed web facade.
- Public aliases requiring exact preservation:
  `PublicConversationWebhookController`, `CdpWarehouseE2eCertificationRunController`,
  `AnalyticsController`, and `BiResourceMovementController` have class-level or
  method-level alias paths.

## Recommended Coordinator Decisions

- Decide a single owner or bridge contract for metadata routes: API definitions,
  MQ definitions, event definitions/reporting, meta options, and node schemas.
- Decide delivery/channel ownership: marketing channel behavior versus execution
  node delivery, including delivery receipts and outbox replay routes.
- Decide whether auth, tenant, admin, system, notification, approval, and plugin
  routes become platform APIs or remain separate context candidates.
- Decide AI ownership split: provider/model/template administration versus
  runtime AI decision/prediction use cases.
- For DDD-C09, require compatibility tests that assert every old verb/path
  remains registered in `canvas-web` after old `canvas-engine` removal,
  including alias mappings.

## Verification Commands Run Or Inspected

- `sed -n` on all required coordination and DDD inventory docs.
- `rg --files backend/canvas-engine/src/main/java/org/chovy/canvas/web -g '*.java'`.
- `rg -n "@(RestController|Controller)"`.
- `rg -n "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)"`.
- Read-only Node parser over `web/**/*.java` to extract controllers, mappings,
  request body/path/query/header parameters, and return types.
- Duplicate check over expanded concrete verb/path endpoints: 804 endpoints,
  0 duplicates.
- No Maven/frontend tests run; this was a read-only inventory dispatch.

## Risks / Cutover Blockers

- `BRIDGE_DECISION` controllers cannot be safely moved into final contexts until
  coordinator ownership is recorded.
- Several controllers still expose DOs/mappers directly in request/response
  families; compatibility tests must preserve those shapes or define intentional
  DTO migrations.
- Public/auth/webhook routes need stricter compatibility than normal UI routes
  because external callers may depend on exact headers, raw body handling, and
  aliases.
- Metadata and stats routes are cross-context and likely need facade APIs before
  old `canvas-engine` can be removed.

