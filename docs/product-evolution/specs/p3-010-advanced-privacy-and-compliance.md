# P3-010 - Advanced Privacy And Compliance Spec

Priority: P3
Sequence: 010
Source: `todo/p3/strategic-opportunities-from-filtered-scope.md#advanced-privacy-and-compliance`
Implementation plan: `../plans/p3-010-advanced-privacy-and-compliance-plan.md`

## Goal

Create an executable discovery and governance slice for advanced privacy and compliance so GDPR, CCPA, PIPL, deletion/export, differential privacy, federated learning, trusted execution, and residency ideas are ranked by evidence before implementation.

## User And Business Value

This gives compliance, security, product, and architecture teams a shared record of demand, regulation profile, data touchpoints, technical risk, audit requirements, proof commands, and approval status before high-risk privacy work starts.

## In Scope

- Privacy capability evidence registry.
- Regulation profile and data-subject workflow assessment metadata.
- Approval gate for child specs.
- Additive Flyway table for compliance evidence.
- Tests that prove incomplete privacy evidence cannot pass review.

## Out Of Scope

- Implementing data deletion/export, differential privacy, federated learning, trusted execution, or data residency runtime behavior.
- Providing legal advice or final compliance certification.
- Editing product-evolution indexes or `EXECUTABLE_PLAN_AUDIT.md`.

## Functional Requirements

1. A privacy evidence record must include capability key, owner, regulation profile, affected data classes, audit artifact requirement, residency impact, threat model note, proof command, rollback note, and decision status.
2. The service must reject approval when proof command, rollback note, affected data classes, or reviewer identity is missing.
3. Privacy computing candidates must remain `RESEARCH_ONLY` or `BLOCKED_PENDING_REVIEW` until an accepted child spec is named.
4. Data-subject request candidates must identify export, deletion, retention, and audit implications before implementation is allowed.
5. This slice must not expose user-facing privacy actions; it records evidence and gates future implementation.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceService.java`
- `backend/canvas-engine/src/main/resources/db/migration/V182__privacy_compliance_evidence.sql`

### Frontend Touchpoints

- None for this discovery slice. UI work is deferred until a child spec defines an operator or compliance workflow.

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V182__privacy_compliance_evidence.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java`

## Dependencies

- Compliance owner for regulation interpretation.
- Security owner for threat model evidence.
- Product owner for customer-demand ranking.
- Architecture owner for data residency and storage impact.

## Risks And Controls

- Compliance overreach: keep status values separate for research, review, approval, rejection, and merged child specs.
- Audit gaps: require audit artifact notes before approval.
- Sensitive-data risk: store evidence summaries and references, not raw personal data.
- Data migration risk: `V182__privacy_compliance_evidence.sql` is additive and can be disabled by stopping registry writes.

## Acceptance Criteria

- `V182__privacy_compliance_evidence.sql` creates an additive privacy evidence table with regulation, data class, proof, rollback, reviewer, and status fields.
- Service tests prove incomplete or unreviewed privacy evidence cannot unlock implementation.
- The plan includes real TDD snippets, commands with expected outputs, rollout notes, and scoped git add and commit commands.
- The slice records governance evidence only; no privacy runtime action is shipped.
