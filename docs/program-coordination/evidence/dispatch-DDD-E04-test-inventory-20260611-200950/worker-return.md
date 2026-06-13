# DDD-E04 Test Inventory Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-E04
dispatch id: dispatch-DDD-E04-test-inventory-20260611-200950
worker: multi_agent_v1-explorer Kant 019eb695-366f-7d93-b385-f16e30738dae

## Files Read

- Required coordination/inventory docs listed in the dispatch.
- Old-engine test tree by file enumeration and content search.
- Representative critical files including `ExecutionControllerTest`,
  `MarketingCampaignControllerTest`, and method inventories for canvas,
  execution, marketing, CDP, BI, risk, conversation, auth, and tenant tests.

## Findings

- `canvas-web`: 135 tests from `org/chovy/canvas/web/**` and
  `org/chovy/canvas/controller/**`. Port controller unit coverage to
  `canvas-web`. Replacement needed: a C09 gate suite under
  `org.chovy.canvas.web.compat` for canvas, execution, marketing, CDP, BI,
  risk, and conversation API compatibility.
- `canvas-context-execution`: 149 tests from engine, execution domain, MQ,
  Redis, and health packages. Replacement needed for execution API
  compatibility plus trigger, resume/wait, trace, idempotency, DLQ, and public
  HMAC trigger behavior.
- `canvas-context-canvas`: 40 tests from canvas, project, and approval domains.
  Replacement needed for CRUD, draft save, versioning, publish/offline/archive,
  approval gate, and tenant isolation.
- `canvas-context-marketing`: 89 tests across marketing content, monitoring,
  search, paid media, programmatic, loyalty, policy, template, notification, and
  creator packages.
- `canvas-context-cdp`: 137 tests for CDP, warehouse, analytics, audience, and
  Doris infrastructure.
- `canvas-context-bi`: 60 tests.
- `canvas-context-risk`: 27 tests.
- `canvas-context-conversation`: 22 tests plus web conversation tests in the
  canvas-web row.
- `canvas-platform`: 13 tests.
- `canvas-boot`: 22 tests for config, migration, and performance.
- `canvas-common`: 9 tests.
- Current compatibility signal: no old-engine files named `CompatibilityTest`;
  only 3 `ContractTest` files; only 1 `@SpringBootTest`; no
  `MockMvc`/`WebTestClient`/`MockMvcBuilders` usage.

## Ambiguous Ownership

- 34 tests require coordinator ownership decisions before worker handoff.
- `domain/ai/**`: 11 tests span AI provider/model/prompt/churn/smart timing.
- `domain/meta/**`: 6 tests span system options, AB governance, tag definition,
  and import source.
- `domain/compliance/**`: 3 tests for audit/deletion/PII masking.
- `domain/datasource/**`: 3 tests for datasource config/credential/security
  migration.
- `security/**`: public trigger auth and secret cipher ownership needs
  execution/web versus common/platform decision.
- `infra/cache/**`: cache SDK/common/platform/execution decision needed.
- Auth, demo sandbox, tenant, collaboration preferences, provider write, and
  async task tests each need explicit owner.

## Recommended Coordinator Decisions

- Add exact rows to `docs/ddd-rewrite/inventory/test-ownership.md` before
  code-writing dispatch; do not hand off by glob only.
- Treat old controller tests as source behavior, not C09 gate evidence. Reserve
  a dedicated `canvas-web` compatibility worker before DDD-C09.
- Add `AuthTenantApiCompatibilityTest` or fold auth/tenant assertions into named
  compatibility tests.
- Decide a migration-test strategy: 119 old tests inspect migration/schema text;
  final migration resource ownership must be standardized.
- Require C09 evidence from `cd backend && mvn test -pl canvas-web -Dtest='*CompatibilityTest'`,
  `cd backend && mvn test -pl canvas-boot -Dtest=ModularArchitectureTest`, and
  `cd backend && mvn clean install` before old `canvas-engine` removal.

## Verification Commands Run Or Inspected

- `find backend/canvas-engine/src/test/java -name '*.java' | wc -l` -> 737.
- `rg --files backend/canvas-engine/src/test/java -g '*.java' | sort`.
- Path-to-module ownership count -> 737 classified, 34 ambiguous.
- `rg -l "CompatibilityTest" backend/canvas-engine/src/test/java -g '*.java'`
  -> no output.
- `rg -l "class .*ContractTest|ContractTest"` -> 3 files.
- `rg -l "@SpringBootTest"` -> only `DorisConnectionTest`.
- `rg -l "MockMvc|WebTestClient|MockMvcBuilders|perform\\("` -> no output.
- Inspected G12 and compatibility-plan commands; no Maven/Node test suites
  executed for this read-only inventory dispatch.

## Risks / Cutover Blockers

- DDD-C09 is blocked until real `canvas-web` HTTP compatibility tests exist and
  pass.
- Direct controller tests miss route mapping, HTTP status, error envelope,
  serialization, filters, and security behavior.
- Ambiguous test ownership can cause duplicate ports or orphaned coverage unless
  coordinator resolves the 34 files.
- Migration/schema tests may break after module moves if final migration
  resource ownership is not standardized.
- End-to-end smoke paths in the compatibility plan are not represented by the
  current old-engine test structure and need explicit boot/web integration
  coverage.

