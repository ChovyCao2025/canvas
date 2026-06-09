# Review and Escalation Gates

This document defines required review gates for DDD rewrite work.

Every code-writing worker delivery must pass these gates before integration.

---

## Gate 1: Scope Review

Reviewer checks:

```text
changed files are inside allowed write scope
forbidden write scope was not touched
shared files were changed only by coordinator
worker did not edit another worker's context
```

Failure action:

```text
reject out-of-scope edits
ask worker to re-submit only allowed changes
coordinator makes shared changes separately
```

---

## Gate 2: Placement Review

Reviewer checks:

```text
api types are in api
application services are in application
entities, value objects, policies, repository interfaces are in domain
DO/Mapper/repository implementations are in adapter.persistence
MQ classes are in adapter.messaging
external clients are in adapter.external
controllers are in canvas-web
runtime assembly is in canvas-boot
```

Failure action:

```text
move class to correct package or split it by responsibility
```

---

## Gate 3: Failure Mode Review

Reviewer checks every item in:

```text
docs/ddd-rewrite/guardrails/llm-drift-and-failure-modes.md
```

Worker cannot pass if any failure mode remains.

Failure action:

```text
return to worker with exact failure mode ID
require corrected implementation and re-review
```

Example:

```text
Rejected: LLM-003 Mapper Penetration.
MarketingCampaignApplicationService imports MarketingCampaignMasterMapper.
Move mapper access to MybatisMarketingCampaignRepository.
```

---

## Gate 4: Automated Guardrail Review

Reviewer runs:

```bash
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Reviewer records:

```text
command
result
failures
manual follow-up decisions
```

Failure action:

```text
fix structural failure or document coordinator-approved temporary bridge
```

---

## Gate 5: Rich Domain Review

Reviewer checks:

```text
central aggregate has meaningful behavior
value objects validate/normalize themselves
state transitions are not only if/else blocks in application service
readiness/eligibility policies are domain policies
application service is orchestration, not the entire domain
```

Failure action:

```text
extract value objects, policies, or aggregate methods before approval
```

---

## Gate 6: Compatibility Review

Reviewer checks:

```text
old API route behavior is preserved
response envelope is preserved
tenant scoping is preserved
status/default/normalization behavior is preserved
existing tests were moved or replaced
contract test evidence matches the changed scope
```

Failure action:

```text
restore old behavior or require a separate behavior-change spec
```

---

## Gate 7: Temporary Bridge Review

Reviewer checks every class with:

```text
Legacy
Compatibility
Bridge
```

Required bridge metadata:

```text
owner
reason
removal phase
replacement path
cutover blocker statement
```

Failure action:

```text
reject bridge or require metadata before merge
```

No bridge can remain in the Phase 10 cutover path without explicit coordinator
approval.

---

## Gate 8: Integration Review

Coordinator checks:

```text
module dependencies are acyclic
no context imports another context's adapter package
canvas-web imports only api/application facades
canvas-boot owns assembly only
worker concerns are resolved
contract tests pass for affected routes
```

Failure action:

```text
do not integrate worker branch until violations are fixed
```

---

## Escalation Rules

Worker must return `NEEDS_CONTEXT` when:

```text
ownership is ambiguous
cross-context API is missing
old behavior is unclear
required test cannot be ported without shared changes
task pack conflicts with spec or child spec
```

Worker must return `BLOCKED` when:

```text
assigned scope cannot satisfy the task
required dependency is not available
contract is contradictory
tests prove old behavior but spec requires incompatible behavior
```

Worker must not:

```text
guess cross-context contracts
edit shared modules to unblock itself
mark DONE with unresolved failure modes
silently drop compatibility behavior
```
