# P2 - Technical Migration Candidates

## Sources

- `tech-selection-whitepaper.md`

## Why P2

The whitepaper contains substantial architecture migration proposals. They should be treated as candidate migration work, not product feature commitments, and each needs its own validation plan before implementation.

## Work Items

- Validate PowerJob migration for dynamic schedule creation and deletion.
- Validate virtual-thread executor replacement for Disruptor in task distribution.
- Validate RocketMQ topic split and delivery Outbox pattern.
- Validate deterministic audience mapping plus Redis BITMAP for collision-free audience membership.
- Validate Spring MVC plus virtual threads and command-style DAG engine as a combined architecture migration.
- Validate Aviator plus QLExpress replacement for Groovy script execution.
- Validate Doris and Flink CDC for reporting and trace data migration after product analytics requirements are stable.

## Fact Corrections

- Apache Doris 4.1.0 exists by 2026-04, so the archived whitepaper's Doris 4.0 wording should be treated as a baseline recommendation, not a latest-version assertion.
- Flink CDC 3.6.0, PowerJob 5.1.2, and @antv/x6 3.1.7 should be rechecked at implementation time.

## Filtered Scope

- Direct @antv/x6 migration is not a P2 default; first improve React Flow where it can satisfy immediate product needs.
- Full service split, serverless, edge, and multi-cloud are P3.

## Dependencies

- Architecture migrations need code-level specs and regression test plans before any implementation.
- Data infrastructure work should follow confirmed analytics and trace data requirements.

## Acceptance Criteria

- Each migration candidate has a separate spec with current-code evidence, risk, rollback, and verification commands.
- No migration is started solely because the whitepaper recommends it.
