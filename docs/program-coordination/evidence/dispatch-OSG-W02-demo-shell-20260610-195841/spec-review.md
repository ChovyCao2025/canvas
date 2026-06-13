# OSG-W02 Spec Review

review status: PASS_WITH_CONCERNS

review scope:
OSG-W02 read-only spec review for demo shell, mock catalog, docs, and worker
return.

files reviewed:

- docker-compose.demo.yml
- wiremock/mappings/demo-catalog.json
- docs/open-source/playground.md
- docs/open-source/quickstart.md
- docs/program-coordination/evidence/dispatch-OSG-W02-demo-shell-20260610-195841/worker-return.md
- required coordination/spec docs

requirements checked:

- Scope stayed DOCS_ONLY/demo-shell with no `backend/canvas-engine` bridge edits.
- Demo compose covers MySQL, Redis, WireMock, and RocketMQ without
  production/staging config or real secrets.
- WireMock catalog covers templates, plugins, golden path, and mock provider
  behavior.
- Docs state G10/DOCS_ONLY limits without claiming backend public API stability.
- Worker return contains canonical fields, verification, risks, ledger update,
  and rollback path.

commands inspected or run:

- `docker compose -f docker-compose.demo.yml config` passed.
- `node tools/open-source-growth/guardrail-verifier.mjs` returned `{ "ok": true }`.
- `node --test tools/open-source-growth/guardrail-verifier.test.mjs` passed 11 tests.
- `node tools/program-coordination/check-dispatch-state.mjs .` returned `{ "ok": true }`.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- Demo catalog JSON parse printed `json ok`.
- Scoped `git diff --check` passed.
- Scoped `git status` showed only the reviewed untracked files and no backend
  `canvas-engine` changes.

findings:
No blocking spec findings. The worker output fits the reserved scope and
correctly avoids backend bridge files. Compose renders validly and uses
local/mock dependencies only. Docs correctly describe current G10 limitations.

required fixes:
None.

residual risks:

- Demo compose shares local dev ports, so it conflicts with
  `docker-compose.local.yml` if run concurrently.
- Backend demo profile/seed behavior remains deferred because no
  CURRENT_ENGINE_BRIDGE declaration was assigned and G10 remains blocked.
- The quickstart verify block is not fully paste-safe after
  `cd frontend && npm run test`; this is docs polish, not a spec blocker.

post-review coordinator follow-up:

- The quickstart verify block was changed to `(cd frontend && npm run test)`.
- Quickstart and playground now document that demo and local compose share the
  same local service ports and should not run concurrently unless ports are
  changed.
- Post-review artifact, demo compose config, OSG guardrail, and scoped diff
  checks passed after the docs polish.

ledger update:
Record OSG-W02 spec review as PASS_WITH_CONCERNS. Proceed to quality review; if
quality passes, close OSG-W02 as DONE_WITH_CONCERNS with accepted concerns for
backend demo profile/seed deferral, port-conflict caveat, and quickstart
paste-safety polish if not fixed before closure.
