# DDD-C09D Reservation Recovery Note

Date: 2026-06-12
Coordinator: Codex
Dispatch: dispatch-DDD-C09D-conversation-api-compat-20260612-014813
Task: DDD-C09D Conversation API compatibility test seed

## State Classification

CONTINUE. `progress-ledger.md` and `dispatch-state.json` agreed that no active
code-writing dispatch existed after DDD-C09C closure. Bernoulli's read-only
recommendation selected `ConversationApiCompatibilityTest` as the next
exact-scope DDD-C09 compatibility target.

## Reservation

Reserved file:

```text
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
```

Status at reservation: `RESERVED`

Worker at reservation: `pending-spawn`

The dispatch must not be marked `RUNNING` until a real `multi_agent_v1` worker
is spawned and the actual worker nickname/id is recorded in both the ledger and
dispatch state.

## Worker Spawn

Worker spawned at 2026-06-12T01:58:15+08:00:

```text
multi_agent_v1-worker Ptolemy 019eb7d5-6901-7630-9b95-8794f09888da
```

After this spawn, the coordinator moved the active dispatch from `RESERVED` to
`RUNNING`.

## Gate Evidence

Commands run before reservation:

```bash
node tools/program-coordination/check-dispatch-state.mjs .
bash docs/program-coordination/checks/program-coordination-checks.sh .
ls -l docs/program-coordination/evidence/pre-rewrite-backup-manifest.md
git branch --show-current
git rev-parse HEAD
git worktree list
git status --short -- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
test ! -e backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-conversation -Dtest=ConversationApplicationServiceTest
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Observed results:

- Dispatch-state verifier passed.
- Program coordination checks passed.
- Backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`.
- Branch is `main`.
- HEAD is `01aac65697d524f4cf2e92d954db088895631004`.
- Worktree list was reviewed; stale prunable worktrees do not overlap this
  exact active reservation.
- Target path had no pre-existing status and the file did not exist.
- DDD guardrails passed with the known RiskRuleValidator TypeCompatibility
  advisory only.
- `ConversationApplicationServiceTest` passed 4 tests, 0 failures.
- Cutover preflight JSON exited 0 with `presentCount: 2`,
  `missingCount: 5`, `cutoverReady: false`, and
  `ConversationApiCompatibilityTest` missing.

## Next Action

Wait once for Ptolemy's worker return. If the wait times out, audit the
reserved path, evidence, and focused tests instead of repeatedly waiting.

## Rollback Pointer

Remove only:

```text
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
docs/program-coordination/evidence/dispatch-DDD-C09D-conversation-api-compat-20260612-014813/
```
