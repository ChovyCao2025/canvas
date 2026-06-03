# Plan: Reactive Threading And Transaction Boundaries

1. Inventory all `.block()`, `.subscribe()`, `Thread.sleep()`, and `@Transactional` occurrences.
2. Classify each occurrence as acceptable, needs scheduler wrapping, needs API redesign, or should become a managed background task.
3. Fix critical runtime paths first:
   - scheduler trigger execution;
   - audience evaluation/fetching;
   - tag import source metadata calls;
   - route mutation waits;
   - canvas publish/offline/kill side effects.
4. Introduce a consistent transaction-side-effect pattern.
   - Prefer DB transaction first, outbox/after-commit event second, idempotent external update third.
5. Add tests.
   - StepVerifier tests for converted reactive flows.
   - Transaction rollback tests proving Redis/scheduler side effects are not applied before commit.
6. Run backend tests and targeted stress checks for boundedElastic saturation.
