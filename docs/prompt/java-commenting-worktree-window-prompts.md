# Java Commenting Git Worktree Goals

Date: 2026-06-16

## 用法

每个代码窗口只复制一个 `Goal:` 块。模型必须自己创建或复用 git worktree，自己切换到 worktree 中执行，不要求用户手动 `cd` 或手动执行 `git worktree add`。

## 通用 Goal 模板

把 `ASSIGNED_SCOPE` 替换成模块或包路径即可。

```text
Goal: For ASSIGNED_SCOPE, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Continue until this scope is completed or genuinely blocked.

First handle the worktree yourself. Detect whether the current directory is already a linked git worktree. If it is, continue there. If it is not, derive a safe scope slug from ASSIGNED_SCOPE, use branch `jc/<scope-slug>` and worktree path `.worktrees/jc-<scope-slug>`. If that branch or worktree already exists, reuse it and resume from existing progress. If neither exists, create the worktree under `.worktrees/` and switch your working directory into it before editing. Do not ask me to run `cd`, `git worktree add`, or branch setup commands. Keep progress in `tmp/java-commenting-progress.md`.

Then do the Java commenting work. Read and follow `docs/java-commenting/README.md` and `docs/java-commenting/subagent-packets.md`. Only modify Java files inside ASSIGNED_SCOPE. Add Chinese Javadocs to every Java type, field, constructor, and method. Add concise Chinese inline comments for important non-obvious logic. Keep documented fields and methods readable with the spacing rules from the guide. Convert Java records to normal classes only according to the Record Conversion Rule. Follow Spring Boot dependency injection best practices from the Dependency Injection Rule. Do not drop fields, rename symbols, change behavior, or run whole-file formatters. Report risky record conversion, Spring beans with multiple constructors, and unclear business meaning under `needs human confirmation`. Commit progress in small batches so the work can be resumed.

Run the most focused Maven compile/test command practical for ASSIGNED_SCOPE from inside the worktree. Final report must include: status, worktree, branch, assigned scope, files changed, commits created, verification run, verification result, needs human confirmation, and behavior changes: none.
```

## 可复制 Goals

### canvas-common

```text
Goal: For backend/canvas-common, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-common. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-boot

```text
Goal: For backend/canvas-boot, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-boot. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-flink-jobs

```text
Goal: For backend/canvas-flink-jobs, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-flink-jobs. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-cache-sdk

```text
Goal: For backend/canvas-cache-sdk, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-cache-sdk. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-platform

```text
Goal: For backend/canvas-platform, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-platform. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-conversation

```text
Goal: For backend/canvas-context-conversation, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-conversation. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-marketing

```text
Goal: For backend/canvas-context-marketing, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-marketing. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-risk

```text
Goal: For backend/canvas-context-risk, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-risk. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-canvas

```text
Goal: For backend/canvas-context-canvas, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-canvas. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-execution

```text
Goal: For backend/canvas-context-execution, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-execution. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-cdp

```text
Goal: For backend/canvas-context-cdp, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-cdp. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-context-bi

```text
Goal: For backend/canvas-context-bi, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-context-bi. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-web

```text
Goal: For backend/canvas-web, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-web. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

## canvas-engine 拆分 Goals

### canvas-engine web

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/web, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/web. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine domain cdp

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine domain warehouse

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine domain bi

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine domain risk

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine domain conversation

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine domain marketing

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine engine

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/engine, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/engine. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine infrastructure

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine dal

```text
Goal: For backend/canvas-engine/src/main/java/org/chovy/canvas/dal, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/main/java/org/chovy/canvas/dal. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

### canvas-engine tests

```text
Goal: For backend/canvas-engine/src/test/java, create or reuse a git worktree by yourself, switch your working directory into that worktree, then add complete Chinese Javadocs plus important inline comments without changing behavior. Follow docs/java-commenting/README.md and docs/java-commenting/subagent-packets.md. If the worktree or branch already exists, resume it; if it does not exist, create it under .worktrees/ first. Do not ask me to run cd or git worktree commands. Only modify files inside backend/canvas-engine/src/test/java. Run focused verification from inside the worktree and report status, worktree, branch, commits, verification, needs human confirmation, and behavior changes: none.
```

