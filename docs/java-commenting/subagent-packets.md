# Java Commenting Subagent Packets

Date: 2026-06-13

## Purpose

This file provides reusable worker packets for splitting Java commenting work
by module or package. Each worker must follow
`docs/java-commenting/README.md`.

The coordinator should give each worker one packet at a time. Do not let two
workers write the same file range concurrently.

## Universal Worker Prompt

```text
You are adding Java comments only.

Read and follow:
- docs/java-commenting/README.md
- this assigned worker packet

Do not change runtime behavior. Do not rename symbols. Do not modify method
bodies except for adding inline comments or preserving existing record
constructor logic during an allowed record-to-class conversion. Do not alter
imports, annotations, or formatting except for comment placement, readability
blank lines allowed by docs/java-commenting/README.md, and imports required by
an allowed record-to-class conversion.

When you encounter a Java record in your assigned write scope, convert it to a
normal class following the "Record Conversion Rule" in
docs/java-commenting/README.md. Do not drop, rename, merge, reorder, or
reinterpret any record component.

When you encounter a Spring-managed bean with multiple constructors, follow the
"Dependency Injection Rule" in docs/java-commenting/README.md. Prefer Spring
Boot constructor injection with Lombok `@RequiredArgsConstructor` for required
dependencies, `@ConfigurationProperties` for grouped configuration, and
`ObjectProvider<T>` or `Optional<T>` for optional dependencies. Do not
consolidate constructors unless all assigned-scope call sites and tests remain
compatible. Otherwise report the file under "needs human confirmation".

Every Java type, field, constructor, and method in your assigned write scope
must receive a Chinese Javadoc comment. Important internal logic must receive
concise Chinese inline comments when the reason is not obvious.

If business meaning is unclear, write a conservative comment and report the
location under "needs human confirmation". Do not invent domain behavior.

Return the required status packet when done.
```

## Required Worker Return

```text
status: DONE, DONE_WITH_CONCERNS, NEEDS_CONTEXT, or BLOCKED
task id:
assigned scope:
files changed:
verification run:
verification result:
verification output summary:
needs human confirmation:
behavior changes: none
risks:
coordinator actions needed:
```

## Packet JC-01: Cache SDK

```text
Task id: JC-01
Mode: comment-only
Assigned scope:
  backend/canvas-cache-sdk/src/main/java/**/*.java
  backend/canvas-cache-sdk/src/test/java/**/*.java
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  backend/canvas-cache-sdk/src/main/java/**/*.java
  backend/canvas-cache-sdk/src/test/java/**/*.java
Verification:
  cd backend
  mvn -pl canvas-cache-sdk test
Must not touch:
  backend/canvas-engine/**
  backend/pom.xml
  Flyway migrations
```

## Packet JC-02: Engine Web/API Layer

```text
Task id: JC-02
Mode: comment-only
Assigned scope:
  backend/canvas-engine/src/main/java/**/web/**/*.java
  backend/canvas-engine/src/main/java/**/controller/**/*.java
  backend/canvas-engine/src/test/java/**/web/**/*.java
  backend/canvas-engine/src/test/java/**/controller/**/*.java
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  same as assigned scope
Verification:
  cd backend
  mvn -pl canvas-engine -DskipTests compile
Must not touch:
  backend/canvas-cache-sdk/**
  backend/canvas-engine/src/main/resources/db/migration/**
```

## Packet JC-03: Engine Node Handlers

```text
Task id: JC-03
Mode: comment-only
Assigned scope:
  backend/canvas-engine/src/main/java/**/handler/**/*.java
  backend/canvas-engine/src/main/java/**/node/**/*.java
  backend/canvas-engine/src/test/java/**/handler/**/*.java
  backend/canvas-engine/src/test/java/**/node/**/*.java
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  same as assigned scope
Verification:
  cd backend
  mvn -pl canvas-engine -DskipTests compile
Must not touch:
  backend/canvas-cache-sdk/**
  backend/canvas-engine/src/main/resources/db/migration/**
```

## Packet JC-04: Engine Domain and Application Services

```text
Task id: JC-04
Mode: comment-only
Assigned scope:
  backend/canvas-engine/src/main/java/**/domain/**/*.java
  backend/canvas-engine/src/main/java/**/service/**/*.java
  backend/canvas-engine/src/main/java/**/application/**/*.java
  backend/canvas-engine/src/test/java/**/domain/**/*.java
  backend/canvas-engine/src/test/java/**/service/**/*.java
  backend/canvas-engine/src/test/java/**/application/**/*.java
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  same as assigned scope
Verification:
  cd backend
  mvn -pl canvas-engine -DskipTests compile
Must not touch:
  backend/canvas-cache-sdk/**
  backend/canvas-engine/src/main/resources/db/migration/**
```

## Packet JC-05: Persistence and Data Objects

```text
Task id: JC-05
Mode: comment-only
Assigned scope:
  backend/canvas-engine/src/main/java/**/dal/**/*.java
  backend/canvas-engine/src/main/java/**/mapper/**/*.java
  backend/canvas-engine/src/main/java/**/repository/**/*.java
  backend/canvas-engine/src/main/java/**/*DO.java
  backend/canvas-engine/src/main/java/**/*Entity.java
  backend/canvas-engine/src/test/java/**/dal/**/*.java
  backend/canvas-engine/src/test/java/**/mapper/**/*.java
  backend/canvas-engine/src/test/java/**/repository/**/*.java
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  same as assigned scope
Verification:
  cd backend
  mvn -pl canvas-engine -DskipTests compile
Must not touch:
  backend/canvas-cache-sdk/**
  backend/canvas-engine/src/main/resources/db/migration/**
```

## Packet JC-06: Platform, Config, and Infrastructure

```text
Task id: JC-06
Mode: comment-only
Assigned scope:
  backend/canvas-engine/src/main/java/**/config/**/*.java
  backend/canvas-engine/src/main/java/**/platform/**/*.java
  backend/canvas-engine/src/main/java/**/infrastructure/**/*.java
  backend/canvas-engine/src/main/java/**/security/**/*.java
  backend/canvas-engine/src/test/java/**/config/**/*.java
  backend/canvas-engine/src/test/java/**/platform/**/*.java
  backend/canvas-engine/src/test/java/**/infrastructure/**/*.java
  backend/canvas-engine/src/test/java/**/security/**/*.java
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  same as assigned scope
Verification:
  cd backend
  mvn -pl canvas-engine -DskipTests compile
Must not touch:
  backend/canvas-cache-sdk/**
  backend/canvas-engine/src/main/resources/db/migration/**
```

## Packet JC-07: Remaining Engine Java Files

```text
Task id: JC-07
Mode: comment-only
Assigned scope:
  backend/canvas-engine/src/main/java/**/*.java not covered by JC-02 through JC-06
  backend/canvas-engine/src/test/java/**/*.java not covered by JC-02 through JC-06
Read scope:
  docs/java-commenting/README.md
Allowed write scope:
  only files explicitly listed by the coordinator before dispatch
Verification:
  cd backend
  mvn -pl canvas-engine test
Must not touch:
  backend/canvas-cache-sdk/**
  backend/canvas-engine/src/main/resources/db/migration/**
Dispatch requirement:
  The coordinator must paste the exact file list before assigning this packet.
  If the exact file list is missing, return NEEDS_CONTEXT.
```

## Coordinator Checklist

Before dispatch:

```text
[ ] Confirm the worker packet has a non-overlapping write scope.
[ ] Paste the goal and the Universal Worker Prompt.
[ ] Paste the selected packet.
[ ] For JC-07, paste the exact file list.
[ ] Tell the worker to return the Required Worker Return packet.
```

After each worker returns:

```text
[ ] Review changed files for comment-only diffs.
[ ] Check that documented fields have one blank line between declarations.
[ ] Check any "needs human confirmation" locations.
[ ] Run or review module verification.
[ ] Dispatch the next non-overlapping packet.
```
