# Program Coordination Evidence

Date: 2026-06-09

This directory stores copied or summarized verification evidence for dispatched
workers and recovery audits.

Before any code-writing dispatch, the coordinator must create the top-level
backup manifest:

```text
docs/program-coordination/evidence/pre-rewrite-backup-manifest.md
```

The manifest points to external backup artifacts. Do not store database dumps,
repository archives, or large copied worktrees in this directory.

Use one directory per dispatch:

```text
docs/program-coordination/evidence/<dispatch-id>/
```

Required contents for code-writing dispatches:

```text
commands.txt
changed-files.txt
verification-output.txt
worker-return.txt
reviewer-return.txt
rollback.txt
```

The coordinator owns this directory. Workers may reference their raw output in
their return packet, but the coordinator decides what evidence is copied here
and records the final path in `dispatch-state.json` and `progress-ledger.md`.
