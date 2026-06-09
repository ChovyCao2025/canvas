# Backup And Rollback Runbook

Date: 2026-06-09

## Purpose

This runbook defines the backup, checkpoint, and rollback contract for the DDD
modular rewrite plus Open Source Growth work. It is required because the program
is intentionally large, parallel, and multi-stage.

The existing plans already require baseline evidence, isolated worktrees,
per-worker rollback paths, dispatch state, and breakpoint recovery. This runbook
adds the missing operational layer: a named backup point before code-writing
work starts, repeatable checkpoints after each integrated wave, and explicit
rollback procedures for failed workers, failed waves, database changes, and
final cutover.

## Core Rule

No code-writing dispatch may start until a pre-rewrite backup manifest exists
and the coordinator has recorded it in both:

- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/dispatch-state.json`

The manifest is committed with the coordination evidence. Large backup artifacts
must stay outside the repository.

Recommended manifest path:

```text
docs/program-coordination/evidence/pre-rewrite-backup-manifest.md
```

Recommended external backup directory:

```text
../canvas-rewrite-backups/<YYYYMMDD-HHMMSS>/
```

Do not place large backup archives, database dumps, or copied worktrees under
`docs/`.

## Backup Layers

| Layer | Required before first code writer | Required before final cutover | Owner |
| --- | --- | --- | --- |
| Git committed state | yes | yes | coordinator |
| Dirty worktree patch and untracked manifest | yes | yes | coordinator |
| External repository copy or bundle | yes | yes | coordinator |
| Local MySQL dump | yes when local data matters | yes if cutover tests use local data | coordinator |
| Redis/RocketMQ/Doris/Flink volume snapshot | optional unless the task depends on their state | yes if cutover validates their stateful data | coordinator |
| Per-worker rollback pointer | yes for every active dispatch | yes for every integrated worker | worker returns, coordinator records |
| Per-wave checkpoint tag or branch | after each integrated wave | before final cutover | coordinator |

## Pre-Rewrite Backup Procedure

Run these commands from the repository root before the first code-writing
worker or coordinator code task.

```bash
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="../canvas-rewrite-backups/${STAMP}"
mkdir -p "$BACKUP_DIR"

git branch --show-current > "$BACKUP_DIR/git-branch.txt"
git rev-parse HEAD > "$BACKUP_DIR/git-head.txt"
git status --short > "$BACKUP_DIR/git-status-short.txt"
git worktree list > "$BACKUP_DIR/git-worktree-list.txt"
git diff --binary > "$BACKUP_DIR/dirty-worktree.diff"
git diff --cached --binary > "$BACKUP_DIR/staged-worktree.diff"
git ls-files --others --exclude-standard > "$BACKUP_DIR/untracked-files.txt"
git bundle create "$BACKUP_DIR/canvas-pre-rewrite.bundle" --all
git tag -a "backup/pre-ddd-osg-${STAMP}" -m "Backup before DDD and OSG rewrite ${STAMP}"
```

If the repository has important untracked local files, copy the repository to
the external backup directory with build outputs excluded:

```bash
rsync -a \
  --exclude .git \
  --exclude .worktrees \
  --exclude frontend/node_modules \
  --exclude frontend/dist \
  --exclude 'backend/**/target' \
  --exclude logs \
  ./ "$BACKUP_DIR/repo-copy/"
```

If local MySQL data matters for verification, capture a logical dump:

```bash
docker exec canvas-mysql sh -lc \
  'mysqldump -uroot -proot --databases canvas_db --single-transaction --routines --triggers --events' \
  > "$BACKUP_DIR/mysql-canvas_db.sql"
```

If Redis state matters for the task, capture a Redis dump:

```bash
docker exec canvas-redis redis-cli SAVE
docker cp canvas-redis:/data/dump.rdb "$BACKUP_DIR/redis-dump.rdb"
```

For RocketMQ, Doris, and Flink, prefer recreatable test fixtures. If their
state is part of the validation, stop the affected service and snapshot its
named Docker volume into the external backup directory before changing code or
schema that depends on that state.

## Backup Manifest Template

Create `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
this shape:

```markdown
# Pre-Rewrite Backup Manifest

Date:
Coordinator:
Reason: DDD modular rewrite plus Open Source Growth execution

## Git State

Branch:
HEAD:
Backup tag:
Bundle path:
Dirty diff path:
Staged diff path:
Untracked manifest path:
Worktree list path:

## Data State

MySQL dump path:
Redis dump path:
Other volume snapshots:
Skipped stateful backups and reason:

## Verification

Commands run:
Result:

## Restore Notes

Source restore path:
Data restore path:
Known risks:
```

The manifest should point to external artifact paths. Do not paste secrets or
large command output into the manifest.

## Per-Worker Rollback Contract

Every code-writing worker must return a concrete rollback path. A valid
rollback path names exact files, branch, and commit range. A vague statement
such as "revert my changes" is not enough.

Required worker return fields are already defined in:

- `docs/program-coordination/collaboration-and-recovery-protocol.md`
- `docs/program-coordination/subagent-worker-packets.md`

The coordinator may mark a worker `DONE` only after the rollback path is
actionable and evidence has been copied or summarized under:

```text
docs/program-coordination/evidence/<dispatch-id>/
```

## Per-Wave Checkpoints

After each wave is integrated and verified, create a checkpoint tag or branch:

```bash
git tag -a "checkpoint/<wave-id>-<YYYYMMDD-HHMMSS>" -m "Verified checkpoint for <wave-id>"
```

Record the checkpoint in the progress ledger event log and in
`dispatch-state.json` last verified evidence.

Required checkpoint points:

- after DDD-C00 foundation
- after DDD first context wave
- after DDD second context wave
- after canvas/execution contract freeze
- after canvas integration
- after execution integration
- before plugin burst
- before final web/boot cutover
- after final cutover verification

## Rollback Procedures

### Abort An Unintegrated Worker

Use this when a worker has not been merged or cherry-picked into the integration
branch.

1. Stop the worker.
2. Mark the dispatch `ABORTED`.
3. Record the reason and rollback pointer in the ledger and JSON state.
4. Remove the worker worktree only after evidence is copied.
5. Delete the worker branch only after the coordinator confirms no evidence is
   needed from it.

Example cleanup:

```bash
git worktree remove ../canvas-DDD-W02-risk
git branch -D work/DDD-W02-risk
```

### Revert An Integrated Worker

Use this when a worker has already been integrated into the coordinator branch.

1. Identify the exact integration commit or commit range.
2. Run the worker's rollback command against only its assigned files or revert
   the integration commit.
3. Re-run the worker's gate and the current wave gate.
4. Record the revert commit, commands, and evidence path.

Use `git revert` for shared branches. Do not use `git reset --hard` on a shared
integration branch unless the user explicitly asks for destructive history
rewrites.

### Revert Coordinator-Owned Foundation Work

Coordinator-owned work may touch root Maven files, boot modules, web adapters,
dispatch state, and coordination docs. Rollback must name the exact coordinator
commit range and must not remove unrelated user changes.

If foundation rollback is needed before workers start, restore to the backup tag
or revert the foundation commits, then re-run:

```bash
bash docs/program-coordination/checks/program-coordination-checks.sh .
node tools/program-coordination/check-dispatch-state.mjs .
```

### Roll Back Database Changes

Applied Flyway migrations must not be edited. If a new migration is wrong:

1. Stop dispatch.
2. Add a forward-fix migration when possible.
3. For local verification data, restore from the MySQL dump if a full reset is
   required.
4. For production-like environments, use the environment's approved snapshot,
   point-in-time recovery, or forward migration procedure.

Local MySQL restore example:

```bash
docker exec -i canvas-mysql sh -lc 'mysql -uroot -proot' < "$BACKUP_DIR/mysql-canvas_db.sql"
```

### Roll Back Final Cutover

The old `canvas-engine` remains the behavior reference until final cutover. Do
not delete or deactivate the old engine path until G12 passes.

If cutover fails:

1. Restore the previous runtime assembly or reactor entry.
2. Keep integrated context modules unless their specific checkpoint failed.
3. Re-run backend and frontend compatibility commands.
4. Record the cutover rollback evidence.
5. Do not start old-engine deletion until a new G12 pass is recorded.

## Reopened Session Recovery

When a new session resumes this program, read this runbook before dispatching
new workers. Then run the recovery sequence in
`collaboration-and-recovery-protocol.md`.

The reopened session must verify:

- backup manifest exists before code-writing dispatch
- active dispatches have rollback pointers
- integrated workers have evidence paths
- wave checkpoint tags or branch names are recorded
- `git status --short` is reconciled with the ledger
- `git worktree list` is reconciled with active or completed worker rows

## Stop Conditions

Stop dispatch immediately when:

- the pre-rewrite backup manifest is missing before code-writing work
- a worker cannot provide an actionable rollback path
- an integrated worker has no evidence path
- a database-affecting task has no data rollback decision
- final cutover attempts to delete old engine code before G12 passes
- a reopened session cannot reconcile backup, ledger, JSON state, and git state
