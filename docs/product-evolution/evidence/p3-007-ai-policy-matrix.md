# P3-007 AI Policy Matrix

## Policy Matrix

| Use case | Allowed action | Blocked action | Human approval | Audit requirement | Budget control | Evaluation method |
|---|---|---|---|---|---|---|
| Copy draft | Generate draft variants | Auto-send campaign | Required before publish | Prompt, response, approver | Daily tenant cap | Human quality review |
| Segment explanation | Explain existing rule | Modify audience rule | Required before save | Input rule and explanation | Per-request cap | Accuracy review against known examples |
| Journey optimizer | Recommend next step | Auto-edit journey | Required before edit | Recommendation and accepted action | Experiment cap | A/B holdout proposal |

## Data Boundaries

- Do not send raw PII to model providers.
- Tenant ID must be included in audit records, not in prompts.
- Provider logs must be available for compliance review.

## Evaluation Gate

- At least 20 reviewed examples per use case.
- No blocked action may be executed by AI.
- Cost per successful operator action must be estimated before runtime implementation.
