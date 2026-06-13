# OSG-W07E Recovery Note

dispatch id: dispatch-OSG-W07E-official-ai-plugin-20260611-022000
task id: OSG-W07E
status: RESERVED
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004

## Reservation

Allowed write scope:

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/ai/**`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/ai/**`
- `docs/open-source/plugins/official/ai.md`

Forbidden write scope:

- `HandlerRegistry`
- `PluginRegistryService`
- `JdbcPluginRepository`
- `backend/pom.xml`
- any other official plugin package
- platform registry, permissions, persistence, or enablement ownership files
- old `canvas-engine` implementation files

## Gate Evidence

- OSG-W07D closed DONE immediately before this reservation with no active
  dispatches.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  W07D closure.
- Scoped `git diff --check` over W07D coordination/evidence paths passed after
  W07D closure.
- W07D final closure verification passed:
  - `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest='OfficialApprovalPluginTest,*Plugin*Test'`
    passed with 22 tests, 0 failures.
  - `node tools/open-source-growth/guardrail-verifier.mjs` passed.
  - `bash docs/program-coordination/checks/program-coordination-checks.sh .`
    passed.
  - `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
    passed with only the known `RiskRuleValidator` TypeCompatibility advisory.
  - `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- Existing G10 seed evidence remains recorded in `dispatch-state.json`
  `lastVerifiedEvidence` for canvas DSL/template and web compatibility checks.

## Pre-Dispatch Path Check

Commands:

```bash
git status --short -- backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/ai backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/ai docs/open-source/plugins/official/ai.md
```

Result: no output; W07E reserved paths had no existing changes before
reservation.

## Next Action

Generate the OSG-W07E worker prompt, spawn the code-writing worker, then move
the dispatch from RESERVED to RUNNING with the actual worker id/nickname.
