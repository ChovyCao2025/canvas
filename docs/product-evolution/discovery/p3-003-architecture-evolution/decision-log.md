# Architecture Evolution Decision Log

| Candidate | Status | Reason |
| --- | --- | --- |
| Service split | Needs Evidence | Monolith pressure must be measured before extraction. |
| Editor canvas alternative | Deferred | Current editor replacement lacks compatibility and performance proof. |
| Event processing CEP | Needs Evidence | Flink-style CEP requires replay and dual-run proof. |
| Multi-cloud deployment | Deferred | No accepted customer or resilience trigger. |
| Serverless execution | Rejected | Current runtime assumptions do not fit serverless without a stronger trigger. |
| Edge runtime | Deferred | Edge placement depends on latency and data boundary evidence. |
| Data residency | Needs Evidence | Privacy and legal owners must define requirements. |
