# Java Commenting Window Progress Template

Date: 2026-06-15

## Usage

Each worktree/window should keep its own progress record. Do not use one shared
progress file for every module, because `canvas-engine` is split across several
windows and concurrent writes would conflict.

Recommended location inside each worktree:

```text
tmp/java-commenting-progress.md
```

The repository already ignores `tmp/`, so this file is local to the worktree and
does not need to be committed.

If you want committed coordination evidence instead, create one committed file
per window under:

```text
docs/java-commenting/progress/<window-id>.md
```

Only the coordinator should edit committed progress files to avoid merge
conflicts.

## Template

```markdown
# Java Commenting Progress

Window:
Worktree:
Branch:
Assigned scope:
Started at:
Last updated:

## Current Status

status: IN_PROGRESS
last completed package:
last completed file:
current file:
last commit:
verification last run:
verification result:

## Completed Batches

| Batch | Scope | Commit | Verification | Notes |
| --- | --- | --- | --- | --- |
| 01 |  |  |  |  |

## Files Completed

- 

## Files Remaining

- 

## Needs Human Confirmation

| File | Line | Reason | Recommended decision |
| --- | ---: | --- | --- |
|  |  |  |  |

## Risky Record Conversions

| File | Status | Notes |
| --- | --- | --- |
|  |  |  |

## Spring Bean Constructor Cleanup Candidates

| File | Status | Notes |
| --- | --- | --- |
|  |  |  |

## Resume Notes

When resuming:

1. Run `git status --short`.
2. Run `git log --oneline -5`.
3. Read this progress file.
4. Continue from `current file` or the first item in `Files Remaining`.
5. Do not rewrite completed files unless fixing verification failures.
```

