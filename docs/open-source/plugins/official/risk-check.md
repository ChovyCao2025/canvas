# Official Risk Check Plugin

The official risk check plugin contributes the `risk.check` action node type
through the execution-owned `NodeHandler` extension point. Runtime discovery is
handled by `NodeHandlerRegistry`; registry metadata, manifest validation,
permissions, compatibility, persistence, and enablement remain platform-owned.

## Manifest

```json
{
  "id": "canvas-plugin-risk",
  "name": "Risk Check Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler"],
  "permissions": ["profile:read"],
  "nodes": ["risk.check"],
  "templates": ["dormant-user-winback", "risk-blocked-outreach"],
  "configSchema": {
    "type": "object",
    "required": ["policy"],
    "properties": {
      "policy": {
        "type": "string",
        "minLength": 1
      }
    }
  }
}
```

## Node

`risk.check` accepts a node config with:

- `policy`: required policy key, for example `WINBACK_DAILY_CAP` or
  `OUTREACH_COMPLIANCE`.

The execution adapter trims and validates `policy`, then emits a deterministic
risk envelope containing plugin id, node type, policy, subject, input payload,
context data, stub check marker, allowed flag, decision, and status. Subject
defaults to the execution user id, then `anonymous` when no user id exists.

This is a deterministic local stub for execution, dry-run, and template trace
evidence. The stub returns `BLOCKED` with decision `blocked` when the trimmed,
uppercased policy contains `BLOCK` or `COMPLIANCE`; all other policies return
`MATCHED` with decision `allowed`.

It does not perform real risk scoring, consent workflow execution, provider
calls, database writes, approval creation, message sending, create a second
plugin registry, or take ownership of platform enablement checks.

## Ownership

- Handler binding and node execution: `canvas-context-execution`.
- Registry metadata, manifest validation, permissions, persistence, and
  enablement state: `canvas-platform`.
- Public catalog or node metadata exposure: `canvas-web`.

Rollback is limited to the risk plugin package, risk plugin tests, and this
document.
