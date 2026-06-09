# Runbook Manifest

Purpose: inventory product-evolution operational and sales runbooks that already
exist in this directory.

This manifest describes current files only. Planned runbooks named by future
specs or plans are not treated as existing until the file is created.

| Runbook | Use |
| --- | --- |
| [3000 Concurrency Hardening Runbook](3000-concurrency-hardening-runbook.md) | Operator entry requirements, baseline command, hardening profile execution, stop gates, rollback, degrade actions, and the 4000-readiness block. |
| [Production Runtime Runbook](production-runtime-runbook.md) | Production runtime incident handling for failed execution spikes, DLQ growth, delivery outbox dead rows, trace overflow, Redis registry outages, MySQL pool pressure, emergency actions, and handoff. |
| [Sandbox Demo Sales Guide](sandbox-demo-sales-guide.md) | Demo tenant boundaries, lifecycle walkthrough, sandbox reset command, conversation reply command, and rollback stance for sales enablement. |

## Future Runbook Boundary

Planned future runbook:
`docs/product-evolution/runbooks/4000-concurrency-readiness-runbook.md` is planned
by P2-015 as future work and is not present in the current runbook inventory.
