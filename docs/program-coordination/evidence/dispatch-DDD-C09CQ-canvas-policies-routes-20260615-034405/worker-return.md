# DDD-C09CQ Worker Return

Status: `DONE_WITH_CONCERNS`

Worker: `Halley 019ec7ab-b4d9-7e93-b814-854edb5b11c9`

## Result

Halley was spawned before the dispatch moved to `RUNNING`. After one bounded
wait timed out, the coordinator inspected the exact reserved scope and closed
the worker with previous status `running` to prevent same-file overwrite.

No target files or final packet were available after the timeout. The
coordinator completed the exact reserved scope locally.

## Tests

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingPolicyApplicationServiceTest`
  passed: 3 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingPolicyControllerCompatibilityTest test`
  passed: 3 tests, 0 failures.

## Concern

No normal final packet was returned before timeout/shutdown.
