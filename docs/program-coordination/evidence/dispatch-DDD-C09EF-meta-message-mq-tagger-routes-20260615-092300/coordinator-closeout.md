# DDD-C09EF Coordinator Closeout

Task: port remaining legacy `MetaController` message/MQ/reach/tagger routes.

Worker:
- Ptolemy `019ec8df-f63b-7623-998b-138d08e9b033`
- Timed out once with no evidence file and was closed with previous status `running`.
- Coordinator recovered the reserved scope locally to avoid idle waiting.

Changes:
- Extended final `MetaOptionController` with legacy routes:
  - `GET /meta/message-codes`
  - `GET /meta/mq-definitions`
  - `GET /meta/mq-topics`
  - `GET /meta/reach-scenes`
  - `GET /meta/tagger-tags`
  - `GET /meta/tagger-tag-values`
- Extended final `MetaOptionFacade`, `MetaOptionApplicationService`, and `MetaOptionCatalog` for MQ definition and tagger lookups.
- Kept one focused compatibility test covering old envelope, message-code type switch, MQ `value/label/requestSchema`, reach/MQ topic category forwarding, and tagger `type` / `tagCode` mapping.

Verification:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MetaOptionControllerCompatibilityTest test`
  - Passed: 5 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile`
  - Passed with reactor `BUILD SUCCESS`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 98 controllers / 816 endpoints.
  - DDD-C09EF meta routes were removed from the top gap list.
  - Next top gap: `route:/platform`.
- Strict old-coupling scan over touched final files:
  - No `org.chovy.canvas.engine`, old service/mapper/entity/common imports, `canvas-engine`, old meta services, tag definition service, tenant resolver, or `org.chovy.canvas.common.R` matches.
- `git diff --check` over touched files passed.

Accepted concerns:
- This remains a compact deterministic metadata seed, not durable metadata/tagger integration parity.
- Global cutover remains blocked by controller count parity: canvas-web 98 controllers vs old canvas-engine web 142 controllers.
