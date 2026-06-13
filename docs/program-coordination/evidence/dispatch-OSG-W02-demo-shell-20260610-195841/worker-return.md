# OSG-W02 Worker Return

status: DONE_WITH_CONCERNS
task id: OSG-W02
dispatch id: dispatch-OSG-W02-demo-shell-20260610-195841
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 with dirty worktree changes

files changed:

- docker-compose.demo.yml
- wiremock/mappings/demo-catalog.json
- docs/open-source/playground.md
- docs/open-source/quickstart.md

contracts changed: none

tests run:

- RED: inline Node artifact check failed as expected with `missing docker-compose.demo.yml`.
- GREEN: same inline Node artifact check passed with `{ "ok": true }`.
- `docker compose -f docker-compose.demo.yml config`
- `node tools/open-source-growth/guardrail-verifier.mjs`
- `node --test tools/open-source-growth/guardrail-verifier.test.mjs`
- `node tools/program-coordination/check-dispatch-state.mjs .`
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
- `git diff --check -- docker-compose.demo.yml wiremock docs/open-source/playground.md docs/open-source/quickstart.md docs/program-coordination/dispatch-state.json docs/program-coordination/progress-ledger.md docs/program-coordination/evidence/dispatch-OSG-W02-demo-shell-20260610-195841/recovery-note.md`
- `node -e "JSON.parse(require('fs').readFileSync('wiremock/mappings/demo-catalog.json','utf8')); console.log('demo catalog json ok')"`
- Post-spec-review artifact check passed and confirmed the quickstart verify
  block uses `(cd frontend && npm run test)` and both quickstart/playground
  document the shared-port caveat.

verification result: passed

verification output summary/path:

- Demo compose rendered successfully and includes MySQL, Redis, WireMock, RocketMQ namesrv, and RocketMQ broker services.
- OSG guardrail verifier returned `{ "ok": true }`.
- OSG guardrail verifier tests passed with 11 tests.
- Dispatch-state verifier returned `{ "ok": true }`.
- Program coordination checks passed.
- Scoped diff whitespace check passed.
- WireMock demo catalog JSON parsed successfully.
- Post-spec-review verification passed after paste-safety and port-conflict
  documentation polish.

evidence artifact paths:

- docs/program-coordination/evidence/dispatch-OSG-W02-demo-shell-20260610-195841/recovery-note.md
- docs/program-coordination/evidence/dispatch-OSG-W02-demo-shell-20260610-195841/worker-return.md

risks:

- This dispatch intentionally stayed DOCS_ONLY/demo-shell and did not add `application-demo.yml`, backend seed code, frontend routes, or backend public API commands because no CURRENT_ENGINE_BRIDGE declaration was assigned and G10 remains blocked.
- `docker-compose.demo.yml` exposes the same default local ports as the dev stack; the quickstart and playground now document that users must not run it concurrently with `docker-compose.local.yml` unless they change ports.
- Because `docs/open-source/**` is untracked in the current dirty worktree, scope attribution relies on the active reservation and scoped status checks.

coordinator actions needed:

- Run read-only spec and quality review.
- Decide whether to accept the DOCS_ONLY limitation as an OSG-W02 concern or defer backend demo profile/seed work to a later bridge/G10 task.

ledger update:

- Move OSG-W02 from RUNNING to RETURNED, then REVIEWING after reviewer assignment.
- If review passes, close as DONE_WITH_CONCERNS with accepted concerns for backend demo profile/seed deferral and port-conflict caveat.

rollback path:

- Remove docker-compose.demo.yml.
- Remove wiremock/mappings/demo-catalog.json.
- Remove docs/open-source/playground.md.
- Revert the OSG-W02 section edits in docs/open-source/quickstart.md.
