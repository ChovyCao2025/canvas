# Isolated Worktree Protocol

Date: 2026-06-08

## Purpose

Use this protocol when running more than one code-writing subagent. Without
isolated workspaces, the maximum safe number of simultaneous code-writing
workers is one, and coordinator edits count as that one writer.

## Precondition

The coordinator must first make the coordination and task-pack documents
available to every worker. The safest path is to commit or otherwise snapshot
the planning docs before creating worker worktrees.

Before creating a worktree for a code-writing worker, complete
`docs/program-coordination/backup-and-rollback-runbook.md` through the
pre-rewrite backup manifest. G0B in
`docs/program-coordination/gate-verification-matrix.md` blocks code-writing
dispatch until that manifest exists.

Before creating a worktree for a code-writing worker, the coordinator must add
an active dispatch registry row to
`docs/program-coordination/progress-ledger.md`.

Do not create workers from a dirty base without recording:

```bash
git status --short
```

## Worktree Naming

Use one branch and one worktree per task:

```bash
git worktree add ../canvas-DDD-W02-risk -b work/DDD-W02-risk HEAD
git worktree add ../canvas-DDD-W03-marketing -b work/DDD-W03-marketing HEAD
git worktree add ../canvas-OSG-W03-schema -b work/OSG-W03-schema HEAD
```

Use task ids from
`docs/program-coordination/max-parallel-subagent-execution-plan.md`.

## Worker Rules

Each worker must:

- edit only its assigned write scope
- run the verification commands in its packet
- return changed paths
- return test output summary
- return `NEEDS_CONTEXT` if it needs a coordinator-owned file
- never rebase, reset, or revert unrelated changes

## Return Format

```text
status: DONE, DONE_WITH_CONCERNS, NEEDS_CONTEXT, or BLOCKED
task id:
dispatch id:
branch:
worktree:
base commit:
head commit:
files changed:
contracts changed:
tests run:
verification result:
verification output summary/path:
evidence artifact paths:
risks:
coordinator actions needed:
ledger update:
rollback path:
```

## Integration Order

The coordinator integrates workers in this order:

1. read worker final report
2. inspect changed paths
3. reject or pause if files exceed the assigned scope
4. run the worker's verification command in the worker worktree
5. merge or cherry-pick into the integration branch
6. run the gate commands from `gate-verification-matrix.md`
7. update the ledger dispatch row and reviewer board
8. remove the worktree only after integration evidence is recorded

Example cleanup after successful integration:

```bash
git worktree remove ../canvas-DDD-W02-risk
git branch -d work/DDD-W02-risk
```

## Conflict Handling

If two workers change the same file:

1. stop integrating the second worker
2. identify which worker had legitimate ownership
3. move shared-file changes to the coordinator
4. re-dispatch the losing worker with a narrower scope

Do not ask workers to resolve conflicts in shared coordinator-owned files.
