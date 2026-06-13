# OSG-W07F Recovery Note

dispatch id: dispatch-OSG-W07F-official-risk-plugin-20260611-025500
task id: OSG-W07F
status: RESERVED
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004

## Reservation

Allowed write scope:

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**`
- `docs/open-source/plugins/official/risk-check.md`

Forbidden write scope:

- `HandlerRegistry`
- `PluginRegistryService`
- `JdbcPluginRepository`
- `backend/pom.xml`
- any other official plugin package
- platform registry, permissions, persistence, or enablement ownership files
- old `canvas-engine` implementation files
- `canvas-context-risk` real decision implementation files

## Gate Evidence

- OSG-W07E closed DONE_WITH_CONCERNS immediately before this reservation with
  no active dispatches.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  W07E closure.
- W07E final closure verification passed:
  - `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest='OfficialAiPluginTest,*Plugin*Test'`
    passed with 28 tests, 0 failures.
  - `node tools/open-source-growth/guardrail-verifier.mjs` passed.
  - `bash docs/program-coordination/checks/program-coordination-checks.sh .`
    passed.
  - `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
    passed with only the known `RiskRuleValidator` TypeCompatibility advisory.
  - `node tools/program-coordination/check-dispatch-state.mjs .` passed.
  - Scoped `git diff --check` plus direct trailing-whitespace scan passed.

## Pre-Dispatch Path Check

Command:

```bash
git status --short -- backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/risk backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/risk docs/open-source/plugins/official/risk-check.md
```

Result: no output; W07F reserved paths had no existing changes before
reservation.

## Next Action

Generate the OSG-W07F worker prompt, spawn the code-writing worker, then move
the dispatch from RESERVED to RUNNING with the actual worker id/nickname.
