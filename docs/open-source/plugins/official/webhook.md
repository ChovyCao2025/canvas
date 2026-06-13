# Official Webhook Plugin

The official webhook plugin contributes the `webhook` trigger node type through
the execution-owned `NodeHandler` extension point. Runtime discovery is handled
by `NodeHandlerRegistry`; registry metadata, manifest validation, permissions,
compatibility, persistence, and enablement remain platform-owned.

## Manifest

```json
{
  "id": "canvas-plugin-webhook",
  "name": "Webhook Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler"],
  "permissions": ["webhook:register"],
  "nodes": ["webhook"],
  "templates": [],
  "configSchema": {
    "type": "object",
    "required": ["event"],
    "properties": {
      "event": {
        "type": "string",
        "minLength": 1
      },
      "source": {
        "type": "string",
        "default": "webhook"
      }
    }
  }
}
```

## Node

`webhook` accepts a node config with:

- `event`: required inbound event name, for example `user.registered`.
- `source`: optional source label copied into the execution envelope,
  defaulting to `webhook`.

The current execution adapter validates the event and emits a deterministic
trigger envelope containing plugin id, node type, event, source, input payload,
context data, and received status. It does not make outbound HTTP calls, create
a second plugin registry, or take ownership of platform enablement checks.

## Ownership

- Handler binding and node execution: `canvas-context-execution`.
- Registry metadata, manifest validation, permissions, persistence, and
  enablement state: `canvas-platform`.
- Public catalog or node metadata exposure: `canvas-web`.

Rollback is limited to the webhook plugin package, webhook plugin tests, and
this document.
