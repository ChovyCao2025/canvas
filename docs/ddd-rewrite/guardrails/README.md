# DDD Rewrite Guardrails

This directory defines hard guardrails for the DDD modular rewrite.

The goal is to prevent long-running model or multi-agent execution from
drifting into a new version of the old architecture. These guardrails convert
common mistakes into explicit failure conditions.

---

## Files

- [LLM Drift and Failure Modes](./llm-drift-and-failure-modes.md)
- [Automated Guardrail Checks](./automated-guardrail-checks.md)
- [Review and Escalation Gates](./review-and-escalation-gates.md)
- [Worker Enforcement Protocol](./worker-enforcement-protocol.md)
- [Guardrail Check Script](./checks/ddd-guardrail-checks.sh)

---

## Mandatory Use

Before a worker starts:

```text
read this directory
read the assigned task pack
confirm allowed write scope
confirm forbidden write scope
```

Before a reviewer approves:

```text
run or inspect the automated checks
check all failure modes
check changed-file scope
check contract compatibility evidence
```

Before cutover:

```text
all temporary bridges are removed or explicitly blocked
all architecture checks pass
all compatibility tests pass
all worker concerns are resolved
```

---

## Guardrail Priority

When documents conflict, use this priority:

```text
1. User instruction
2. docs/program-coordination/subagent-worker-packets.md for dispatch and write scope
3. docs/program-coordination/gate-verification-matrix.md for gate evidence
4. Child spec for the affected area
5. Task pack allowed/forbidden scope
6. Guardrails in this directory
7. DDD rewrite spec
8. Examples and references
```

The broad DDD rewrite spec describes target architecture and source inventory.
It does not override dispatch-time write scopes. In particular, "Moves from
current packages" means "source classes to inventory and migrate later"; it is
not permission for a context worker to edit `backend/canvas-web/**`,
`backend/canvas-boot/**`, root Maven, or another context module.

If a worker believes a guardrail must be violated, the worker returns
`NEEDS_CONTEXT` or `BLOCKED`. It must not silently violate the guardrail.
