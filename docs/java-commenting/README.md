# Java Commenting Coordination Guide

Date: 2026-06-13

## Purpose

This guide defines how to coordinate a repository-wide Java commenting pass
without changing runtime behavior. It is designed for work split across
subagents by Maven module or source package.

The main goal should stay short. Put the full rules in this document and point
agents to it instead of pasting a long prompt into the goal.

## Goal Best Practice

A goal is best used as a compact objective and guardrail, not as the full
specification. Keep it short enough that every worker can remember the purpose,
then reference this guide for detailed rules.

Recommended goal:

```text
Add complete English Javadocs and important inline comments to backend Java
sources without changing behavior. Follow docs/java-commenting/README.md and
dispatch work by module using docs/java-commenting/subagent-packets.md.
```

If the tool or workflow supports only a short goal, use:

```text
Document backend Java sources with Javadocs only; no logic changes. Follow
docs/java-commenting/README.md.
```

Do not put the entire commenting standard into the goal. Long goals are harder
to audit, easier to partially ignore, and harder to reuse across workers.

## Scope

Default scope:

- `backend/**/*.java`
- Exclude `target/`, `generated-sources/`, `build/`, `dist/`, and other
  generated output directories.
- Do not modify applied Flyway migrations.
- Do not modify Java logic, signatures, package names, imports, annotations, or
  formatting except where comment placement requires whitespace.

Module order:

1. `backend/canvas-cache-sdk`
2. `backend/canvas-engine`

Within `canvas-engine`, split by package or layer when possible:

- web/controller/API layer
- engine/node handler layer
- domain/application services
- persistence/data objects/mappers
- platform/config/infrastructure
- tests

## Commenting Rules

Every Java type must have a Javadoc comment:

- class
- interface
- enum
- annotation type
- nested type

Every field must have a Javadoc comment:

- `private`, `protected`, and `public` fields
- `static final` constants
- enum values
- injected dependencies
- configuration fields
- DTO fields

Every method must have a Javadoc comment:

- constructors
- public methods
- protected methods
- private methods
- interface methods
- overridden methods
- test helper methods

Important internal logic must have concise inline comments when the reason is
not obvious:

- complex branches
- state transitions
- transaction boundaries
- cache reads, writes, and invalidation
- database writes
- external system calls
- exception handling
- idempotency rules
- performance-sensitive logic
- concurrency or locking assumptions

## Javadoc Format

Use standard Javadoc:

```java
/**
 * One-sentence description of the business purpose.
 *
 * Optional details about rules, constraints, side effects, units, default
 * values, transaction behavior, thread safety, or idempotency.
 */
```

Method Javadocs should use tags only when they add useful information:

```java
/**
 * Executes the given canvas node with the provided runtime context.
 *
 * @param context execution context containing input data and runtime metadata
 * @return execution result produced by the node
 * @throws NodeExecutionException if node execution fails
 */
NodeResult execute(NodeContext context);
```

Rules:

- Do not add `@return` for `void` methods.
- Do not add `@throws` for exceptions that are not declared or practically
  thrown by the method.
- Do not mechanically repeat parameter names.
- Document nullability, allowed values, units, and side effects when known.
- For `@Override` methods, use `{@inheritDoc}` only when the inherited contract
  is sufficient. Add implementation-specific details when behavior differs.

## Field Formatting

When every field has Javadoc, keep one blank line between field declarations:

```java
/**
 * Unique identifier of the canvas.
 */
private Long id;

/**
 * Display name shown to users.
 */
private String name;

/**
 * Current publication status of the canvas.
 */
private CanvasStatus status;
```

Do not collapse documented fields together:

```java
/**
 * Unique identifier of the canvas.
 */
private Long id;
/**
 * Display name shown to users.
 */
private String name;
```

## Inline Comment Format

Use `//` for one-line reasoning:

```java
// Invalidate after commit so rolled-back changes do not evict valid cache data.
TransactionSynchronizationManager.registerSynchronization(...);
```

Use `/* ... */` for multi-line reasoning:

```java
/*
 * Retry only transient failures. Business validation errors must fail fast
 * because retrying them can create duplicate external side effects.
 */
if (isTransient(error)) {
    retry();
}
```

Inline comments should explain why the code exists, not translate the code.

## Quality Rules

Avoid low-value comments:

- `Gets user id.`
- `Sets name.`
- `Loops through list.`
- `Checks if value is null.`
- `Creates object.`

Prefer comments that explain business meaning and constraints:

- `Identifier of the tenant that owns the canvas execution.`
- `Timeout in milliseconds before the engine marks a node as failed.`
- `External event key used to deduplicate webhook callbacks.`
- `Uses database time to keep expiration checks consistent across app nodes.`

When the exact business meaning is unclear:

1. Write a conservative comment based only on the code.
2. Do not invent domain behavior.
3. Add the location to the final "Needs human confirmation" list.

## Behavior Guardrails

This task is documentation-only. Workers must not:

- change method bodies
- rename classes, methods, fields, variables, or packages
- add or remove annotations
- add or remove imports unless a Javadoc link introduces an import, which should
  generally be avoided by using plain text or fully qualified names
- reformat files unrelated to comment placement
- modify tests except to add comments
- modify generated code
- modify applied Flyway migrations

If a file has existing user edits, preserve them. Do not revert unrelated
changes.

## Verification

At the end of each module or worker packet:

1. Run a compile or focused test command when practical.
2. Confirm no non-comment behavior changes were introduced.
3. Report files changed and any locations requiring human confirmation.

Suggested commands:

```bash
cd backend
mvn -pl canvas-cache-sdk test
mvn -pl canvas-engine test
```

For very large batches, at minimum run:

```bash
cd backend
mvn -pl <module> -DskipTests compile
```

## Final Report Format

```text
status: DONE, DONE_WITH_CONCERNS, or BLOCKED
scope completed:
files changed:
verification run:
verification result:
needs human confirmation:
behavior changes: none
```

