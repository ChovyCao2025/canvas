# P3 - Long-Term Architecture Evolution

## Sources

- `tech-selection-whitepaper.md`

## Why P3

These architecture ideas may matter at scale but should not distract from immediate safety, usability, and operator workflow.

## Work Items

- Evaluate service split into editor, engine, CDP, analytics, admin, and billing after module boundaries are stable.
- Evaluate @antv/x6 migration only after React Flow shortfalls are proven by actual product needs.
- Evaluate Flink CEP behavior-trigger migration after Flink and event pipelines exist.
- Evaluate multi-cloud, serverless, edge, and data residency architecture only for validated deployment requirements.

## Dependencies

- Requires production traffic, customer deployment requirements, or measurable performance bottlenecks.
- Requires existing module and data contracts to be stable enough to split safely.

## Acceptance Criteria

- Long-term architecture concepts are preserved but do not enter active implementation without separate validation.
