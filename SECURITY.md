# Security Policy

## Supported Versions

This repository is under active pre-release development. Security fixes target
the main branch unless maintainers publish a release branch policy.

## Reporting A Vulnerability

Do not open a public issue for a suspected vulnerability. Contact the maintainers
privately through the project security contact configured in the repository, or
use GitHub private vulnerability reporting when it is enabled.

Please include:

- Affected commit, branch, or release.
- Clear reproduction steps.
- Expected and observed impact.
- Logs or screenshots with secrets and personal data removed.
- Whether the issue requires external services, local Docker services, or only
  repository code.

## Sensitive Information

- Do not commit production secrets, provider credentials, customer data, model
  keys, database dumps, or private tokens.
- `CANVAS_JWT_SECRET` must be at least 32 bytes for local development and must
  be rotated if accidentally exposed.
- Demo and test integrations should use WireMock or local mock data.
- Do not mix demo mock settings into production or staging configuration.

## Scope Notes

Public extension/API write operations are gated by G10. Reports about plugin,
template, DSL, CLI, or AI backend behavior should identify whether they apply to
current code, docs-only contracts, or future gated surfaces.
