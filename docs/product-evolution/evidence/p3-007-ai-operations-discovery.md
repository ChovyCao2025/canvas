# P3-007 AI Operations Discovery

## Owners

- AI product owner: Marketing Canvas Product Lead
- Architecture owner: Platform Architect
- Compliance owner: Compliance Lead
- Security reviewer: Security Lead
- Decision date: 2026-07-25

## Target Operator Workflow

- Workflow: campaign copy draft and segment-rule explanation.
- Human approval owner: lifecycle marketing manager.
- Model provider constraints: no raw PII in prompts, provider audit logs required, budget cap required.

## Evidence Register

| Source | Source date | Owner | Confidence | Decision implication |
|---|---:|---|---|---|
| Operator interviews about copy drafting | 2026-06-12 | Product Lead | Medium | Copy assistance has demand but needs approval workflow. |
| Campaign operations bottleneck review | 2026-06-14 | Operations Lead | High | Segment explanation reduces review time more safely than autonomous edits. |
| Cost assumption worksheet | 2026-06-18 | Architecture Owner | Low | Provider cost uncertainty requires budget guard before runtime build. |

## Discovery Conclusion

Recommended gate input: split AI policy/audit foundation from any generative or autonomous workflow. Runtime AI work requires child specs with evaluation and approval gates.
