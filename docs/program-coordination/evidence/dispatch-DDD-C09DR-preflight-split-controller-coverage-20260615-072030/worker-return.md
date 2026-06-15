# DDD-C09DR Worker Return

Worker: Rawls `019ec870-8994-77e3-82e0-15e670ae591d`

Status: completed

Changed files:

- `tools/program-coordination/cutover-compatibility-preflight.mjs`
- `tools/program-coordination/cutover-compatibility-preflight.test.mjs`

Summary:

- Added a split-controller regression test for old `CanvasController` project-folder metadata endpoints being covered by a separate final `CanvasProjectFolderMetadataController`.
- Updated route gap detection to build a global current-controller coverage set by `HTTP_METHOD + normalized path`, with path variables normalized to `{}`.
- Route/family grouping is now used after concrete endpoint coverage filtering, so split final controllers and variable-name differences do not create false family gaps.
- JSON output shape is unchanged.

Worker-reported verification:

- `node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs` passed: 6/6.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0.
- `family:Canvas` false gap disappeared.
- Global `cutoverReady` remains false because current `canvas-web` still has 87 controllers / 785 endpoints versus old 142 controllers / 806 endpoints.

Risk:

- The preflight tool remains a static Spring annotation scanner. Complex constant-composed paths or runtime route registration may still need separate handling.
