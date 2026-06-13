# Pre-Rewrite Backup Manifest

Date: 2026-06-09 22:20:54 Asia/Shanghai
Coordinator: Codex
Reason: DDD modular rewrite plus Open Source Growth execution

## Git State

Branch: main
HEAD: 01aac65697d524f4cf2e92d954db088895631004
Backup tag: backup/pre-ddd-osg-20260609-222054
Bundle path: ../canvas-rewrite-backups/20260609-222054/canvas-pre-rewrite.bundle
Dirty diff path: ../canvas-rewrite-backups/20260609-222054/dirty-worktree.diff
Staged diff path: ../canvas-rewrite-backups/20260609-222054/staged-worktree.diff
Untracked manifest path: ../canvas-rewrite-backups/20260609-222054/untracked-files.txt
Worktree list path: ../canvas-rewrite-backups/20260609-222054/git-worktree-list.txt
External repository copy path: ../canvas-rewrite-backups/20260609-222054/repo-copy/

## Data State

MySQL dump path: not captured
Redis dump path: not captured
Other volume snapshots: none
Skipped stateful backups and reason: DDD-C00 foundation creates Maven module skeletons, package markers, guardrail tests, and inventory files only; it does not depend on or mutate local MySQL, Redis, RocketMQ, Doris, or Flink state.

## Verification

Commands run:

```bash
git branch --show-current
git rev-parse HEAD
git status --short
git worktree list
git diff --binary
git diff --cached --binary
git ls-files --others --exclude-standard
git bundle create ../canvas-rewrite-backups/20260609-222054/canvas-pre-rewrite.bundle --all
git tag -a backup/pre-ddd-osg-20260609-222054 -m "Backup before DDD and OSG rewrite 20260609-222054"
rsync -a --exclude .git --exclude .worktrees --exclude frontend/node_modules --exclude frontend/dist --exclude 'backend/**/target' --exclude logs ./ ../canvas-rewrite-backups/20260609-222054/repo-copy/
```

Result: backup artifacts and annotated tag created successfully.

## Restore Notes

Source restore path: restore from backup tag `backup/pre-ddd-osg-20260609-222054`, bundle `../canvas-rewrite-backups/20260609-222054/canvas-pre-rewrite.bundle`, or repository copy `../canvas-rewrite-backups/20260609-222054/repo-copy/`.
Data restore path: no stateful data backup was captured for DDD-C00; recreate local services from `docker-compose.local.yml` if needed.
Known risks: the manifest captures an already dirty coordinator-owned docs/tools worktree. Restore must preserve unrelated user changes and should use targeted patches or commits instead of destructive resets.
