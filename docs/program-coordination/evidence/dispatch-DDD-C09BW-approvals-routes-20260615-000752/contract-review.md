# DDD-C09BW Contract Review

Date: 2026-06-15
Reviewers:

- Curie `019ec6eb-5abc-7ca2-bebe-26fea6de7e50`
- Gibbs `019ec6eb-f8ab-7653-a38e-fb57f9bbb11e`

## Findings And Actions

- Curie found tenant-scoped lookup bugs when duplicate task/instance ids exist across tenants. Coordinator fixed `requireTask` and `requireInstance` to filter tenant before id.
- Curie found same-role decision authorization was broader than legacy semantics. Coordinator tightened decision authorization to assigned actor or admin role.
- Curie confirmed tests are not ceremonial: they cover route presence, envelope shape, header/default mapping, bad request/forbidden envelopes, admin gating, and state changes.
- Gibbs confirmed platform failures were in `ApprovalApplicationServiceTest` itself, not unrelated testCompile pollution. Coordinator updated assertions to verify behavior instead of fixture-only assumptions, and added `id` alias coverage for instance snapshots.

## Accepted Concerns

- The new implementation is a compact deterministic compatibility seed, not a full durable replacement for `ApprovalWorkflowService` audit, final-approval, cancellation, external-provider, and auto-action semantics.
- Legacy `TenantContextResolver` behavior is broader than this compact controller's header/default mapping and remains a final cutover concern.
- Global route parity remains blocked beyond `/approvals`.
