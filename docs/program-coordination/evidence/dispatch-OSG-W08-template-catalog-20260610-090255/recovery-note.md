# OSG-W08 Dispatch Note

Date: 2026-06-10

The coordinator registered OSG-W08 as a disjoint P3 sidecar while DDD-W06
continues in `backend/canvas-context-conversation/**`.

Reserved paths:

- `docs/open-source/templates/**`
- `frontend/src/pages/canvas-list/templateCatalog.ts`

Pre-dispatch evidence:

- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `node --test tools/program-coordination/*.test.mjs` passed.
- `node --test tools/open-source-growth/guardrail-verifier.test.mjs` passed.
- `node tools/open-source-growth/guardrail-verifier.mjs` passed.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` passed.

Rollback pointer:

- Revert files under `docs/open-source/templates/**`.
- Revert `frontend/src/pages/canvas-list/templateCatalog.ts`.
