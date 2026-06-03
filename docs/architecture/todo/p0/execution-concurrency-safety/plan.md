# Plan: Execution Concurrency Safety

1. Add failing concurrency tests first.
   - Circuit breaker CLOSED/OPEN/HALF_OPEN races.
   - Scheduler close/register/add races.
   - ExecutionContext parallel branch output collisions.

2. Refactor circuit breaker state.
   - Use an immutable state record inside `AtomicReference`.
   - Apply CAS loops for all transitions.

3. Clarify ExecutionContext ownership.
   - Either serialize writes through the scheduler or introduce atomic per-node output snapshots and collision-safe flattening.

4. Harden scheduler lifecycle.
   - Make lifecycle state atomic/volatile as appropriate.
   - Ensure close prevents all new registrations and disposes pending tasks deterministically.

5. Replace raw virtual-thread starts with a managed executor.
   - Add shutdown and task tracking.

6. Run unit tests plus stress-style repeated race tests.
