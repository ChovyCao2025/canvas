# P3-005 Customer Success Discovery

## Owners

- Business owner: Customer Success Lead
- Product owner: Marketing Canvas Product Lead
- Support owner: Support Operations Lead
- Decision date: 2026-07-15

## Target Segment

- Segment: enterprise tenants running more than 20 active canvases
- Service buyer: VP Marketing Operations
- Operator user: lifecycle marketing operator
- Support owner: named customer success manager

## Evidence Sources

| Source | Source date | Owner | Confidence | Decision implication |
|---|---:|---|---|---|
| Renewal notes for enterprise tenant cohort | 2026-06-10 | Customer Success Lead | High | Renewal risk clusters around campaign governance and operator training. |
| Support tickets tagged canvas-operations | 2026-06-12 | Support Operations Lead | Medium | Repeated issues suggest managed-service playbooks may reduce time to resolution. |
| Usage analytics for active canvases | 2026-06-14 | Product Analytics Lead | Medium | Expansion candidates correlate with high canvas count and low publish cadence. |

## Service Catalog MVP Proposal

| Service | Primary user | Outcome | Evidence source | Operating owner |
|---|---|---|---|---|
| Managed campaign review | lifecycle operator | Reduce failed launches | Support tickets | Customer Success Lead |
| Admin training | tenant admin | Improve self-service setup | Renewal notes | Enablement Lead |
| Health-score review | account owner | Identify churn and expansion | Usage analytics | Customer Success Lead |

## Discovery Conclusion

Recommended gate input: split managed-service playbooks from health-score automation. Health-score automation needs a child spec after signal validation.
