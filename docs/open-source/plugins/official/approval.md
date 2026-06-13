# Official Approval Plugin

The official approval plugin contributes the `approval.request` action node type
through the execution-owned `NodeHandler` extension point. Runtime discovery is
handled by `NodeHandlerRegistry`; registry metadata, manifest validation,
permissions, compatibility, persistence, and enablement remain platform-owned.

## Manifest

```json
{
  "id": "canvas-plugin-approval",
  "name": "Approval Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler"],
  "permissions": ["approval:create"],
  "nodes": ["approval.request"],
  "templates": [],
  "configSchema": {
    "type": "object",
    "required": ["approvalCode"],
    "properties": {
      "approvalCode": {
        "type": "string",
        "minLength": 1
      }
    }
  }
}
```

## Node

`approval.request` accepts a node config with:

- `approvalCode`: required approval workflow key, for example
  `HIGH_VALUE_COUPON`.

The execution adapter trims and validates `approvalCode`, then emits a
deterministic approval envelope containing plugin id, node type, approval code,
requester, input payload, context data, stub request marker, and `APPROVED`
status. Requester defaults to the execution user id, then `anonymous` when no
user id exists.

This is a deterministic stub for local execution, dry-run, and template trace
evidence. It does not create real approval tasks or instances, call an approval
provider, create a second plugin registry, or take ownership of platform
enablement checks.

## Ownership

- Handler binding and node execution: `canvas-context-execution`.
- Registry metadata, manifest validation, permissions, persistence, and
  enablement state: `canvas-platform`.
- Public catalog or node metadata exposure: `canvas-web`.

Rollback is limited to the approval plugin package, approval plugin tests, and
this document.
