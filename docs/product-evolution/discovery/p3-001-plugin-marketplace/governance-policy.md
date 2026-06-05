# Plugin Marketplace Governance Policy

## Required Gates

- Security review covers signing, permissions, tenant isolation, dependency vulnerabilities, vulnerability response, and takedown.
- Developer experience review covers SDK versioning, sample plugin maintenance, compatibility matrix, and deprecation windows.
- Support review covers partner contact, customer escalation, incident ownership, SLAs, and support limits.
- Commercial review covers paid listing eligibility, marketplace fees, revenue share, and partner obligations.

## Accepted Capability Rules

- Accepted capabilities must link a child spec path.
- Accepted capabilities must name an owner who can fund and operate the child spec.
- Accepted capabilities must keep P2-002 plugin foundations in their dependency list.
- Accepted capabilities must include a proof command that can be run from the repository root.

## Security And Tenant Safety

Public plugin workflows must not proceed until package signing, permission review, tenant isolation, install boundaries, and vulnerability handling are proven by a child spec. Any later runtime implementation must preserve tenant-scoped access and allow a plugin key to be blocked or removed when a vulnerability or abuse report is confirmed.

## Support And Takedown

Partner support must define escalation routing, partner ownership, customer-facing SLAs, and incident communication. Takedown must cover abuse, security vulnerability, broken integrations, customer notice, and install blocking for affected plugin keys.

## No Runtime Rollout

This P3 slice has no Flyway migration and no runtime route. Rollback is a documentation rollback: revert the discovery package commit or leave capabilities below `Accepted For Child Spec`.
