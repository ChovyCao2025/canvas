# Plan: Canvas State And Data Consistency

1. Define allowed canvas state transitions.
   - DRAFT -> PUBLISHED
   - PUBLISHED -> OFFLINE / KILLED / ARCHIVED
   - OFFLINE -> PUBLISHED only if explicitly supported
   - KILLED -> terminal

2. Centralize transition checks.
   - Add a state transition service or validator used by publish/offline/kill/archive/clone/update.

3. Split mutable draft fields from runtime policy.
   - Prevent published canvas runtime limits from being edited in place.
   - Store runtime policy on published version or require republish.

4. Guard version cleanup.
   - Do not clear `graphJson` for versions referenced by running executions, waits, rollbacks, or audit requirements.

5. Add repair/reconciliation jobs.
   - Rebuild trigger routes from published versions.
   - Clean stale quota keys safely.

6. Test transition matrix, cleanup safety, and Redis/DB reconciliation behavior.
