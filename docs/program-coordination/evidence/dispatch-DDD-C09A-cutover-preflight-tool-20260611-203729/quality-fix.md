# DDD-C09A Quality Fix

Date: 2026-06-11

## Fix Scope

Coordinator fix inside the DDD-C09A reserved files only:

- `tools/program-coordination/cutover-compatibility-preflight.mjs`
- `tools/program-coordination/cutover-compatibility-preflight.test.mjs`

## Root Cause

The first implementation treated missing controller source directories as empty
inventories. A partial repository with no old `canvas-engine` baseline path
could therefore satisfy count comparisons when all compatibility test filenames
were present.

## Changes

- Added `present` metadata for old `canvas-engine` web source, current
  `canvas-web` source, and the compatibility test directory in the JSON report.
- Added explicit blockers for missing old baseline and current canvas-web source
  paths.
- Added a regression fixture with a current `canvas-web` controller and all
  required compatibility test filenames but no old baseline path. The fixture
  proves default JSON mode remains exit 0 with `cutoverReady: false`, while
  `--require-ready --json` exits 1 with the same JSON body.

## Verification

- RED: `node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs`
  failed on `reports missing controller source directories as blockers` with
  `true !== false`.
- GREEN: `node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs`
  passed 4/4.
- GREEN: `node --test tools/program-coordination/*.test.mjs` passed 24/24.
- GREEN: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 and reported current DDD-C09 blockers.
- GREEN: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
  exited 1 with JSON and the same blockers.

## Residual Risk

The static endpoint count remains 806 versus the DDD-E01 inventory count of 804.
This is still accepted as a deterministic conservative preflight concern, not a
closure blocker for DDD-C09A.
