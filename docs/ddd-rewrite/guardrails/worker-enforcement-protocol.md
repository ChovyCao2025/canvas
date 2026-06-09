# Worker Enforcement Protocol

This protocol defines how coordinator, workers, and reviewers enforce DDD
rewrite guardrails during parallel or long-running execution.

---

## 1. Worker Start Protocol

Before writing code, each worker must report:

```text
assigned task pack:
allowed write scope:
forbidden write scope:
old classes to migrate:
target packages:
tests expected:
guardrails read:
open questions:
```

The worker may proceed only when:

```text
allowed scope is explicit
old classes are identified
target packages are clear
cross-context APIs are defined or not needed
```

---

## 2. Worker During-Task Protocol

Worker must stop and ask for coordinator input when:

```text
it needs to edit canvas-common
it needs to edit backend/pom.xml
it needs to edit canvas-web or canvas-boot
it needs a new cross-context API
it finds ambiguous table ownership
it finds old behavior that conflicts with the spec
```

Worker must not:

```text
create a duplicate API in its own context to avoid waiting
move ambiguous classes into common
reach into another context's adapter
change frontend API behavior while migrating backend structure
```

---

## 3. Worker Final Response Protocol

Every worker final response must use this format:

```text
status:
task id:
dispatch id:
branch:
worktree:
base commit:
head commit:
assigned task pack:
files changed:
contracts changed:
old classes migrated:
new public api:
domain model changes:
persistence ownership changes:
tests run:
verification result:
verification output summary/path:
evidence artifact paths:
guardrail checks:
failure modes reviewed:
compatibility evidence:
temporary bridges:
open risks:
coordinator actions needed:
ledger update:
rollback path:
```

Rules:

- `status: DONE` is allowed only when all failure modes are clear.
- `DONE_WITH_CONCERNS` is required when code compiles but a risk remains.
- `NEEDS_CONTEXT` is required for unresolved ownership or contract questions.
- `BLOCKED` is required when the task cannot be completed inside scope.
- This response is a DDD extension of the canonical return contract in
  `docs/program-coordination/subagent-worker-packets.md`; task packs must not
  use a shorter response format.

---

## 4. Reviewer Response Protocol

Reviewer must respond with:

```text
status:
scope review:
placement review:
failure mode review:
automated checks:
rich domain review:
compatibility review:
temporary bridge review:
required fixes:
approval:
```

Approval values:

```text
APPROVED
APPROVED_WITH_NOTES
REJECTED
NEEDS_COORDINATOR_DECISION
```

`APPROVED_WITH_NOTES` cannot be used for unresolved failure modes. It is only
for non-blocking observations.

---

## 5. Coordinator Integration Protocol

Coordinator integrates a worker only after:

```text
worker returned DONE or concerns were resolved
spec reviewer approved
code quality reviewer approved
guardrail checks passed or exceptions are documented
changed files are inside assigned scope
module tests pass
```

Coordinator must reject integration when:

```text
worker changed shared files without approval
worker invented cross-context API without child spec update
worker left a temporary bridge without removal gate
worker substituted narrow tests for broad compatibility evidence
```

---

## 6. Parallel Execution Protocol

Allowed parallel workers:

```text
platform + risk + marketing
cdp + bi + conversation
```

Restricted workers:

```text
canvas + execution
```

Canvas and execution may explore in parallel, but implementation must follow
the shared contract in:

```text
docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
```

Coordinator-owned files:

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
docs/ddd-rewrite/inventory/**
cross-context API contracts
```

---

## 7. Retrying a Failed Worker

When a worker fails, do not ask it to "try again" without changes.

The retry prompt must include:

```text
specific failure mode IDs
exact files that violated the guardrail
required correction
unchanged write scope
tests to rerun
```

Example:

```text
Retry because of LLM-002 and LLM-003.
Files:
  MarketingCampaignApplicationService.java imports MarketingCampaignMasterDO and MarketingCampaignMasterMapper.
Required correction:
  Move mapper access to MybatisMarketingCampaignRepository.
  Return MarketingCampaignView from application API.
Rerun:
  mvn test -pl canvas-context-marketing
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

---

## 8. Stop-the-Line Conditions

The coordinator must pause implementation and update the spec or child spec
when:

```text
two workers need the same class or table
two contexts define incompatible API contracts
architecture tests need an exclusion
compatibility tests contradict the target design
open-source growth work depends on DDD module placement
canvas/execution publication contract is insufficient
```

No worker should work around these conditions locally.
