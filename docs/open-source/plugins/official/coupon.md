# Official Coupon Plugin

The official coupon plugin contributes the `coupon.grant` action node type
through the execution-owned `NodeHandler` extension point. Runtime discovery is
handled by `NodeHandlerRegistry`; registry metadata, manifest validation,
permissions, compatibility, persistence, and enablement remain platform-owned.

## Manifest

```json
{
  "id": "canvas-plugin-coupon",
  "name": "Coupon Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler"],
  "permissions": ["coupon:grant"],
  "nodes": ["coupon.grant"],
  "templates": [],
  "configSchema": {
    "type": "object",
    "required": ["couponKey"],
    "properties": {
      "couponKey": {
        "type": "string",
        "minLength": 1
      }
    }
  }
}
```

## Node

`coupon.grant` accepts a node config with:

- `couponKey`: required coupon or benefit key, for example `WELCOME_10`.

The execution adapter trims and validates `couponKey`, then emits a
deterministic coupon envelope containing plugin id, node type, coupon key,
recipient, input payload, context data, stub grant marker, and `SENT` status.
Recipient defaults to the execution user id, then `anonymous` when no user id
exists.

This is a deterministic stub for local execution, dry-run, and template trace
evidence. It does not grant real coupons, call a coupon provider, create a
second plugin registry, or take ownership of platform enablement checks.

## Ownership

- Handler binding and node execution: `canvas-context-execution`.
- Registry metadata, manifest validation, permissions, persistence, and
  enablement state: `canvas-platform`.
- Public catalog or node metadata exposure: `canvas-web`.

Rollback is limited to the coupon plugin package, coupon plugin tests, and this
document.
