# DDD-C09CT Worker Return

Status: `DONE_WITH_CONCERNS`

Worker: `Herschel 019ec7bf-4119-7412-bf66-5647f04d4193`

## Result

Herschel was spawned before the dispatch moved to `RUNNING`. After one bounded
wait timed out, the coordinator inspected the exact reserved scope and closed
the worker with previous status `running` to prevent same-file overwrite.

No target files or final packet were available after the timeout. The
coordinator completed the exact reserved scope locally.

## Tests

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasUserApplicationServiceTest`
  passed: 2 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasUserControllerCompatibilityTest test`
  passed: 3 tests, 0 failures.

## Concern

No normal final packet was returned before timeout/shutdown.
