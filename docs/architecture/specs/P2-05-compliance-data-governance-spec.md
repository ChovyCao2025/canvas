# Spec: Compliance And Data Governance

Source package: `docs/architecture/todo/p2/compliance-data-governance/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Implemented and locally verified on 2026-06-05. Repository evidence includes a compliance data inventory, audit event matrix, deletion/retention workflow, evidence checklist, masking service, audit-write service, deletion service, controller audit hooks, trace deletion coverage, CDP response masking, suppression SQL-wrapper coverage, and focused tests. Formal certification status still requires external process review.

## Problems Covered

- Audit log table exists but code coverage for audit writes is unclear/incomplete.
- GDPR/PIPL-style delete rights, consent management, data classification, PII masking, retention, and incident response are not implemented as a complete framework.
- DataSource credentials and demo credentials overlap with security hardening.

## Source Coverage

- `archive/reviews/architecture-supplement-review-2026-05.md`: compliance certification readiness.
- `archive/reference/security-considerations.md`: critical and high-priority security gaps.
- `archive/reviews/brownfield-architecture.md`: security integration requirements.

## Acceptance Criteria

- Audit logging covers privileged and canvas lifecycle operations.
  - Status: implemented locally. `AuditEventService` writes masked audit metadata to `canvas_audit_log`, and hooks cover canvas lifecycle, tenant mutations, and data-source credential mutations.
- Data retention and deletion workflows are defined and tested.
  - Status: implemented locally. Workflows are documented and `DataDeletionService` covers governed user-data tables plus `canvas_execution_trace` payload matches.
- PII masking rules are enforced in logs and API responses.
  - Status: implemented locally. `PiiMaskingService` covers metadata, exception payloads, and CDP detail response masking.
- Consent/suppression model is documented and implemented for marketing execution.
  - Status: implemented locally. `MarketingPolicyServiceTest` covers opt-in, opt-out, missing consent, active suppression, no active suppression, and expired/all-channel suppression query matching.
- Security incident response and compliance evidence checklists exist.
  - Status: implemented in `docs/architecture/compliance/compliance-evidence-checklist.md`.

## Evidence

- `docs/architecture/compliance/data-inventory.md`
- `docs/architecture/compliance/audit-event-matrix.md`
- `docs/architecture/compliance/deletion-and-retention-workflows.md`
- `docs/architecture/compliance/compliance-evidence-checklist.md`
- `docs/architecture/evidence/P2-05-compliance-data-governance.md`
