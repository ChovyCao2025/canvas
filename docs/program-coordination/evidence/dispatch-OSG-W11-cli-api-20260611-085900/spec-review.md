# OSG-W11 Spec Review

review status: PASS_WITH_CONCERNS

review scope:
OSG-W11 read-only spec compliance review for the scoped CLI and evidence files.

files reviewed:

- `tools/canvas-cli/src/index.mjs`
- `tools/canvas-cli/test/cli.test.mjs`
- `docs/program-coordination/evidence/dispatch-OSG-W11-cli-api-20260611-085900/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-OSG-W11-cli-api-20260611-085900/worker-return.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `docs/program-coordination/gate-verification-matrix.md`

requirements checked:
OSG-W11 write scope, forbidden backend scope, unreserved MarketingOps doc scope,
Canvas DSL v1 CLI/API contract, G10/G11 matrix expectations, accepted
`/canvas/dsl/map` import preview route, local validate/diff preservation, API URL
handling, local HTTP-server API tests, error handling, and help output.

commands inspected or run:

- `git status --short`
- `git status --short -- tools/canvas-cli docs/program-coordination/evidence/dispatch-OSG-W11-cli-api-20260611-085900 docs/open-source/marketingops-as-code.md backend`
- `rg` inspections for OSG-W11 packet, DSL contract, G10/G11 gates, and direct DB references
- `cd tools/canvas-cli && npm test` passed: 10 tests, 0 failures
- `cd tools/canvas-cli && node src/index.mjs --help` passed

findings:
No required-fix findings. CLI import posts `{ document }` to
`POST /canvas/dsl/map`; export and publish use backend HTTP paths with
`--api-url`, `CANVAS_API_URL`, and default `http://localhost:8080`. Validate and
diff remain local and covered by tests. Tests use a local Node HTTP server and
cover request method/path/body, API error handling, API URL precedence, and help
output. No direct DB access was found in the CLI source/tests.

required fixes:
None.

residual risks:
Export/publish backend routes are skeleton paths and remain an accepted concern
because backend route implementation is outside OSG-W11 scope. The current
worktree contains unrelated dirty/untracked `backend/**` paths and an untracked
`docs/open-source/marketingops-as-code.md`; OSG-W11 evidence lists only
`tools/canvas-cli/src/index.mjs` and `tools/canvas-cli/test/cli.test.mjs` as
changed, so this review does not attribute those unrelated paths to OSG-W11.

ledger update:
OSG-W11 PASS_WITH_CONCERNS: CLI API command implementation complies with scoped
requirements; no required fixes. Accepted concern remains final backend
export/publish route confirmation under the appropriate backend/API gate.
