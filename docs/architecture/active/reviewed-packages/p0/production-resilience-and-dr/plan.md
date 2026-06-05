# Plan: Production Resilience And Disaster Recovery

1. Add graceful shutdown configuration.
2. Introduce a central execution lifecycle gate.
   - Reject new direct/MQ/scheduled triggers after shutdown begins.
   - Track in-flight executions.

3. Make async chains drainable.
   - Replace untracked `.subscribe()` and raw virtual threads in shutdown-sensitive paths.

4. Define Redis recovery behavior.
   - Persist enough execution context to recover paused/waiting executions.
   - Add route rebuild and context reconciliation commands.

5. Add operational runbook steps.
   - Stop order.
   - Drain timeout.
   - Redis loss recovery.
   - Route rebuild.

6. Validate with targeted tests and a local shutdown script.
