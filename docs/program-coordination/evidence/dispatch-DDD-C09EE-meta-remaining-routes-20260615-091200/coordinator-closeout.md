# DDD-C09EE Coordinator Closeout

Task: port remaining legacy `MetaController` option/schema routes selected by preflight.

Worker:
- Aquinas `019ec8d6-8671-7be3-abd9-f22a022741d7`
- Timed out once with no evidence file and was closed with previous status `running`.
- Coordinator recovered the reserved scope locally to avoid idle waiting.

Changes:
- Extended final `MetaOptionController` with legacy routes:
  - `GET /meta/ai-models`
  - `GET /meta/ai-providers`
  - `GET /meta/ai-templates`
  - `GET /meta/api-definitions`
  - `GET /meta/behavior-strategy-types`
  - `GET /meta/canvas-context-fields`
  - `GET /meta/context-fields`
  - `GET /meta/coupon-types`
  - `GET /meta/event-definitions`
  - `GET /meta/identity-types`
- Extended final `MetaOptionFacade`, `MetaOptionApplicationService`, and `MetaOptionCatalog` with compact deterministic seed data.
- Kept tests focused on route compatibility risk: old envelope, tenant/providerId mapping, key/label options, schema option shape, context field shape, and category forwarding.

Verification:
- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MetaOptionControllerCompatibilityTest test`
  - Failed as expected with `404 NOT_FOUND` for `GET /meta/ai-models?providerId=11`.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MetaOptionControllerCompatibilityTest test`
  - Passed: 4 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile`
  - Passed with reactor `BUILD SUCCESS`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 98 controllers / 810 endpoints.
  - The ten DDD-C09EE `/meta/*` routes were removed from the top gap list.
  - Next top gap: `route:/meta/message-codes`.
- Strict old-coupling scan over touched final files:
  - No `org.chovy.canvas.engine`, old service/mapper/entity/common imports, `canvas-engine`, `MetaService`, `SystemOptionService`, AI registry services, tenant resolver, or `org.chovy.canvas.common.R` matches.
- `git diff --check` over touched files passed.

Accepted concerns:
- This is a compact deterministic compatibility seed, not durable metadata persistence parity.
- Remaining legacy meta routes still exist outside this dispatch: message codes, MQ definitions/topics, reach scenes, tagger tags/values.
- Global cutover remains blocked by controller count parity: canvas-web 98 controllers vs old canvas-engine web 142 controllers.
