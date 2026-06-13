# Official Message Plugin

The official message plugin contributes the `message.send` action node type
through the execution-owned `NodeHandler` extension point. Runtime discovery is
handled by `NodeHandlerRegistry`; registry metadata, manifest validation,
permissions, compatibility, persistence, and enablement remain platform-owned.

## Manifest

```json
{
  "id": "canvas-plugin-message",
  "name": "Message Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler"],
  "permissions": ["message:send"],
  "nodes": ["message.send"],
  "templates": [],
  "configSchema": {
    "type": "object",
    "required": ["template"],
    "properties": {
      "channel": {
        "type": "string",
        "enum": ["sms", "email", "push", "workchat"],
        "default": "sms"
      },
      "template": {
        "type": "string",
        "minLength": 1
      },
      "recipient": {
        "type": "string",
        "description": "Optional literal recipient or template reference such as ${payload.phone}."
      }
    }
  }
}
```

## Node

`message.send` accepts a node config with:

- `template`: required template key or copy variant identifier.
- `channel`: optional channel, defaulting to `sms`.
- `recipient`: optional literal recipient or payload/context reference,
  defaulting to the execution user id, then `anonymous` when no user id exists.

The current execution adapter validates the template and emits a deterministic
message envelope containing plugin id, node type, channel, template, recipient,
input payload, context data, stub delivery marker, and `SENT` status. It does
not send SMS, email, push, or workchat messages, create a second plugin
registry, or take ownership of platform enablement checks.

## Ownership

- Handler binding and node execution: `canvas-context-execution`.
- Registry metadata, manifest validation, permissions, persistence, and
  enablement state: `canvas-platform`.
- Public catalog or node metadata exposure: `canvas-web`.

Rollback is limited to the message plugin package, message plugin tests, and
this document.
