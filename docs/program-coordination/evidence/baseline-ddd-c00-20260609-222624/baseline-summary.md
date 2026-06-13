# DDD-C00 Baseline Evidence

Date: 2026-06-09 22:26:24 Asia/Shanghai
Task: DDD-C00 coordinator foundation baseline
Branch: main
HEAD: 01aac65697d524f4cf2e92d954db088895631004

## Dirty Worktree

Snapshot: `git-status-short.txt`
Result: coordinator-owned docs/tools files were dirty before DDD-C00 code-writing began; no business source rewrite files were dirty.

## Backend Baseline

Command:

```bash
cd backend && mvn clean install
```

Default runtime result: failed before tests because Maven used Java 8, which does not support the configured `--release` compiler flag.
Log: `backend-mvn-clean-install.log`

Retried command with Java 21:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 21)" PATH="$JAVA_HOME/bin:$PATH" \
  bash -lc 'cd backend && mvn clean install'
```

Java 21 result: failed in `canvas-engine` tests; 3063 tests run, 3 failures, 0 errors, 1 skipped.
Log: `backend-mvn-clean-install-java21.log`

Known failing tests:

- `RuntimeMigrationEvidenceTest.recordsTraceMysqlWritePath`
- `NodeTypeGovernanceTest.exposesOnlyGovernedProductNodeTypes`
- `FlywayMigrationPolicyTest.mergedDuplicateVersionRepairsRemainExplicitAndExecutable`

## Frontend Baseline

Command:

```bash
cd frontend && npm run build && npm run test
```

Result: `npm run build` failed before tests because the active Node is 18.20.8 and the installed Vite requires Node 20.19+ or 22.12+; `npm run test` was skipped.
Logs: `frontend-npm-build.log`, `frontend-npm-test.log`

## Baseline Decision

G3 baseline evidence is captured with pass/fail status. DDD-C00 may proceed because the next foundation verification uses targeted Maven commands with Java 21 and does not depend on frontend runtime state.
