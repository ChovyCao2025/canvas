# ADR-004 Script Engine Sandbox

status: Proof Required
owner: Execution Engine

## Source Evidence

Optimization reviews flagged Groovy sandbox and metaspace risk and proposed QLExpress or Aviator.

## Current-Code Evidence

`GroovyHandler` uses Groovy `CompilerConfiguration`, `SecureASTCustomizer`, a shell pool, virtual threads, a script cache, timeout control, and output-size limits.

## Decision

Keep Groovy until a compatibility proof shows QLExpress or Aviator can preserve script semantics, validation behavior, timeout control, and sandbox constraints.

## Expected Benefit

Lower sandbox risk and simpler expression governance if an alternative proves compatible.

## Cost

High: existing scripts, node configs, validation rules, compiled cache semantics, and operator expectations can break.

## Rollback

Feature flag the alternative engine per canvas or node. Roll back by routing all script nodes to `GroovyHandler` and evicting alternative compiled caches.

## Proof Command

`mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest`

## Accepted Evidence

Required: script corpus compatibility, malicious-script rejection, timeout behavior, output limit behavior, and performance comparison.

## Child Spec

Required before implementation: `p2-018d-script-engine-sandbox-proof`.

## Dependency Notes

Must not ship with DAG rewrite. Script-engine proof should run against current handler contracts first.
