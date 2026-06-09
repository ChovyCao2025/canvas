# Discovery Manifest

Purpose: inventory product-evolution discovery packages so future agents can
find strategy evidence without confusing discovery artifacts with runtime
implementation.

These packages create no Flyway migrations, runtime routes, customer-facing UI,
billing behavior, model integrations, or deployment topology changes. A package
may name future child-spec candidates; that is not proof that the child spec
files already exist.

| Package | Scope | Files |
| --- | --- | --- |
| P3-001 Plugin Marketplace | Public plugin marketplace evidence, governance, and capability decisions. | [README](p3-001-plugin-marketplace/README.md), [evidence](p3-001-plugin-marketplace/evidence.json), [governance](p3-001-plugin-marketplace/governance-policy.md), [decision log](p3-001-plugin-marketplace/decision-log.md) |
| P3-002 AI Commerce Bets | Long-term AI commerce and ecosystem bet ranking with approval boundaries. | [README](p3-002-ai-commerce-bets/README.md), [evidence](p3-002-ai-commerce-bets/evidence.json), [governance](p3-002-ai-commerce-bets/governance-policy.md), [decision log](p3-002-ai-commerce-bets/decision-log.md) |
| P3-003 Architecture Evolution | Long-term architecture candidates and proof gates. | [README](p3-003-architecture-evolution/README.md), [evidence](p3-003-architecture-evolution/evidence.json), [proof matrix](p3-003-architecture-evolution/proof-matrix.md), [decision log](p3-003-architecture-evolution/decision-log.md) |
| P3-004 Commercial Billing | Commercial model and billing capability evidence, gates, and decisions. | [README](p3-004-commercial-billing/README.md), [evidence](p3-004-commercial-billing/evidence.json), [governance](p3-004-commercial-billing/governance-policy.md), [decision log](p3-004-commercial-billing/decision-log.md) |

