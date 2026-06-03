# Stale And Duplicate Findings

## Stale Or Corrected

- JWT secret startup validation is present in `JwtUtil`; the remaining work is deployment configuration enforcement.
- "Zero tests" is stale. The current repository contains backend and frontend tests.
- Several older route-cleanup concerns were partially addressed by DB-only transaction methods and transaction-external cleanup. Remaining consistency work is captured in P0 packages.

## Duplicates

- CORS, public endpoints, and error disclosure appear in multiple review documents. Active work is consolidated under `p0/security-hardening`.
- Reactor blocking, transactions, Redis/DB consistency, and graceful shutdown overlap across remediation parts 2, 5, 6, and 7. Active work is split by ownership into P0 reactive/transactions, P0 state/data consistency, and P0 production resilience.
- Frontend `any`, state flow, editor size, and effect issues are consolidated under `p1/frontend-canvas-state`.

## Needs External Decision

- Real production RTO/RPO targets.
- Redis HA topology and cross-region recovery.
- Service split timeline.
- Whether to migrate WebFlux to MVC or harden the current WebFlux model first.
