# Official Plugin Support Refactor

date: 2026-06-11
actor: coordinator
scope:

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/**`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/**`

## Purpose

After closing OSG-W07F, the official plugin handlers shared duplicated helper
logic for trimming string config values and defaulting a blank execution user id
to `anonymous`.

This refactor extracts that behavior into
`OfficialPluginSupport` and updates the official webhook, message, coupon,
approval, AI, and risk handlers to use the shared helper without changing their
node output contracts.

## TDD Evidence

RED:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=OfficialPluginSupportTest
```

Result: failed at test compilation because `OfficialPluginSupport` did not
exist.

GREEN:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=OfficialPluginSupportTest
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest='OfficialPluginSupportTest,*Plugin*Test'
```

Result: passed with 38 tests, 0 failures.

## Verification

```bash
node tools/open-source-growth/guardrail-verifier.mjs
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
node tools/program-coordination/check-dispatch-state.mjs .
bash docs/program-coordination/checks/program-coordination-checks.sh .
git diff --check -- backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official docs/program-coordination/progress-ledger.md docs/program-coordination/dispatch-state.json docs/program-coordination/evidence/dispatch-OSG-W07F-official-risk-plugin-20260611-025500/recovery-review.md
direct trailing-whitespace scan over official plugin files and W07F recovery review
```

Result:

- OSG verifier returned `{ "ok": true }`.
- DDD guardrails passed with the known `RiskRuleValidator` TypeCompatibility
  advisory only.
- Dispatch-state verifier returned `{ "ok": true }`.
- Program coordination checks passed.
- Scoped `git diff --check` passed.
- Direct trailing-whitespace scan passed.

## Notes

The official plugin files are still untracked as part of the broader OSG branch,
so scoped verification and direct file scans are recorded until the branch is
staged or committed.
