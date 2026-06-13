# G5 First Wave Integration Summary

Date: 2026-06-10 02:12 Asia/Shanghai

## Decision

G5 is open for DDD-W04/W05/W06.

The DDD-W03 `DONE_WITH_CONCERNS` result is accepted for G5 because the remaining
controller compatibility work is not in the DDD-W03 write scope. The owner is
DDD-C09 cutover, or a future coordinator-approved bridge dispatch with explicit
`backend/canvas-web/**` scope. This accepted concern does not block CDP, BI, or
conversation context module migration.

## Evidence

Commands run:

```bash
bash docs/program-coordination/checks/program-coordination-checks.sh .
node tools/program-coordination/check-dispatch-state.mjs .
node --test tools/program-coordination/*.test.mjs
node --test tools/open-source-growth/guardrail-verifier.test.mjs && node tools/open-source-growth/guardrail-verifier.mjs
bash -n docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh && bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
test -f docs/program-coordination/evidence/pre-rewrite-backup-manifest.md && test -d docs/program-coordination/evidence/baseline-ddd-c00-20260609-222624 && git rev-parse HEAD
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform,canvas-context-risk,canvas-context-marketing
```

Result:

```text
program coordination checks passed
dispatch-state verifier returned ok
program coordination tool tests passed; 17 tests
OSG guardrail verifier tests passed; 11 tests
OSG guardrail verifier returned ok
DDD guardrails passed
pre-rewrite backup manifest and DDD-C00 baseline evidence exist
first-wave Maven integration passed: platform 12 tests, risk 38 tests, marketing 18 tests
```

## Follow-Up

DDD-C09 owns final `canvas-web` controller compatibility and boot cutover. Do
not edit `backend/canvas-web/**` from DDD-W04/W05/W06 unless a new coordinator
reservation explicitly names that scope.
