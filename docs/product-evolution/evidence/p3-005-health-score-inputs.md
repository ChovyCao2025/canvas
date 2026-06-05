# P3-005 Health Score Inputs

## Signal Inventory

| Signal | Current source | Availability | Tenant-scope risk | Operational owner | Validation method |
|---|---|---|---|---|---|
| Active canvas count | canvas list API export | Available | Low | Product Analytics Lead | Compare active canvas count with renewal notes. |
| Publish cadence | canvas version history | Available | Low | Product Analytics Lead | Check median days between publishes for ten enterprise tenants. |
| Support ticket severity | support export | Partial | Medium | Support Operations Lead | Match ticket tenant IDs before aggregation. |
| Training attendance | enablement roster | Partial | Medium | Enablement Lead | Confirm attendee tenant mapping with customer success manager. |

## Data Access Risk

- No raw customer PII is required for the first validation pass.
- Support and training exports must be aggregated by tenant before joining with product usage.
- Cross-tenant views are allowed only for internal customer-success analysis with named owner approval.

## Validation Exit Criteria

- At least three signals can be populated for eight enterprise tenants.
- At least one signal correlates with a renewal or support outcome.
- Customer Success Lead signs off before any runtime health-score implementation spec is created.
