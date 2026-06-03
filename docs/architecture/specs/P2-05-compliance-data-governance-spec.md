# Spec: Compliance And Data Governance

Source package: `docs/architecture/todo/p2/compliance-data-governance/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Partially confirmed. Repository evidence confirms missing technical hooks; formal certification status requires external process review.

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
- Data retention and deletion workflows are defined and tested.
- PII masking rules are enforced in logs and API responses.
- Consent/suppression model is documented and implemented for marketing execution.
- Security incident response and compliance evidence checklists exist.
