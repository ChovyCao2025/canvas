# Official AI Plugin

The official AI plugin contributes the `ai.generate-copy` action node type
through the execution-owned `NodeHandler` extension point. Runtime discovery is
handled by `NodeHandlerRegistry`; registry metadata, manifest validation,
permissions, compatibility, persistence, and enablement remain platform-owned.

## Manifest

```json
{
  "id": "canvas-plugin-ai",
  "name": "AI Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler"],
  "permissions": ["ai:generate"],
  "nodes": ["ai.generate-copy"],
  "templates": ["ai-copy-review-publish"],
  "configSchema": {
    "type": "object",
    "required": ["promptKey"],
    "properties": {
      "promptKey": {
        "type": "string",
        "minLength": 1
      }
    }
  }
}
```

## Node

`ai.generate-copy` accepts a node config with:

- `promptKey`: required prompt or copy strategy key, for example
  `seasonal_offer`.

The execution adapter trims and validates `promptKey`, then emits a
deterministic AI copy envelope containing plugin id, node type, prompt key,
operator, input payload, context data, stub generation marker, generated copy,
and `GENERATED` status. Operator defaults to the execution user id, then
`anonymous` when no user id exists.

This is a deterministic local stub for execution, dry-run, and template trace
evidence. It does not call OpenAI, LLM, or other AI providers, generate
production-ready campaign copy, create real review workflows, create a second
plugin registry, or take ownership of platform enablement checks.

## Ownership

- Handler binding and node execution: `canvas-context-execution`.
- Registry metadata, manifest validation, permissions, persistence, and
  enablement state: `canvas-platform`.
- Public catalog or node metadata exposure: `canvas-web`.

Rollback is limited to the AI plugin package, AI plugin tests, and this
document.
