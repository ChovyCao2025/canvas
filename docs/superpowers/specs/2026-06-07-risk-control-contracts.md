# Risk Control Rule Engine Production Contracts

## 1. Purpose

This document defines the contracts that implementation, tests, Strategy Studio, Canvas nodes, external callers, and operations must share for the enterprise risk-control rule engine.

It is the implementation-facing companion to:

- `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md`
- `docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md`
- `docs/runbooks/risk-control-rule-engine.md`

The contracts below are Canvas-owned. Public references are used for product and architecture direction, not for vendor-compatible API behavior.

## 2. Canonical Enums

### 2.1 Decision Actions

| Value | Meaning | Can Block Business Flow |
| --- | --- | --- |
| `ALLOW` | Continue normally | No |
| `REVIEW` | Create manual review case and wait or continue according to caller policy | Maybe |
| `VERIFY` | Require stronger verification before continuing | Yes |
| `BLOCK` | Reject or stop the business action | Yes |
| `DELAY` | Delay execution or delivery | Yes |
| `LIMIT` | Reduce amount, count, rate, channel, or entitlement | Yes |
| `SHADOW_ONLY` | Record suggested action without affecting caller | No |

Default priority:

```text
BLOCK > VERIFY > REVIEW > LIMIT > DELAY > ALLOW
```

### 2.2 Risk Bands

| Value | Score Range | Default Meaning |
| --- | --- | --- |
| `LOW` | `0 <= score < 50` | Low risk |
| `MEDIUM` | `50 <= score < 85` | Needs review, verification, or limit depending on scene |
| `HIGH` | `85 <= score <= 100` | Strong intervention candidate |

### 2.3 Runtime Modes

| Value | Runtime Behavior |
| --- | --- |
| `SIMULATION` | Evaluates historical samples only and never affects live traffic |
| `MARK` | Evaluates candidate rules online, records hits, and returns baseline action |
| `SHADOW` | Evaluates candidate strategy online, records suggested decision, and returns baseline action |
| `DUAL_RUN` | Evaluates baseline and candidate online, records both results, and returns baseline action |
| `CANARY` | Deterministically routes a configured traffic percentage to candidate enforcement |
| `ENFORCE` | Candidate result becomes returned decision |
| `PAUSED` | Strategy, rule group, rule, list, factor, or model gate is skipped |

### 2.4 Fail Policies

| Value | Runtime Failure Result |
| --- | --- |
| `FAIL_OPEN` | Return `ALLOW` with failure reason |
| `FAIL_REVIEW` | Return `REVIEW` with failure reason |
| `FAIL_CLOSED` | Return `BLOCK` with failure reason |

### 2.5 Resource Status

Shared status values:

```text
DRAFT
VALIDATED
SIMULATED
APPROVAL_PENDING
MARK
SHADOW
DUAL_RUN
CANARY
ACTIVE
PAUSED
ROLLED_BACK
ARCHIVED
REJECTED
FAILED
```

## 3. Online Decision API Contract

### 3.1 Endpoint

```http
POST /canvas/risk/decisions/evaluate
Content-Type: application/json
Authorization: Bearer <jwt>
Idempotency-Key: <optional-idempotency-key>
```

Tenant identity is resolved from authentication context. If the request body contains `tenantId`, the server ignores it and records an audit warning.

### 3.2 Request

```json
{
  "requestId": "risk-req-20260607-000001",
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "subject": {
    "userId": "u-123",
    "deviceId": "d-456",
    "ip": "203.0.113.9",
    "email": "user@example.com",
    "phone": "+15551234567"
  },
  "eventTime": "2026-06-07T12:00:00Z",
  "event": {
    "amount": 100,
    "currency": "USD",
    "couponCode": "WELCOME50",
    "channel": "APP"
  },
  "context": {
    "canvasId": 42,
    "nodeId": "coupon_1",
    "executionId": "exec-9",
    "businessLine": "LOYALTY",
    "caller": "CANVAS_NODE"
  },
  "features": {
    "risk.score": 67,
    "buyer.fail_count_1d": 2
  },
  "options": {
    "modeOverride": "ENFORCE",
    "includeTrace": false,
    "deadlineMs": 50
  }
}
```

Required fields:

- `requestId`
- `sceneKey`
- at least one subject identifier
- `eventTime`
- `event`
- `context.caller`

Validation rules:

- `requestId` must be unique per tenant unless the same payload is retried.
- `sceneKey` must exist and be active.
- `eventTime` must be ISO-8601 and not more than 24 hours in the future.
- `options.deadlineMs` must be between `10` and the scene latency budget.
- `features` keys must exist in the factor catalog if supplied by caller.
- Raw PII fields are accepted for decision evaluation but must be masked or hashed before persistence.

### 3.3 Response

```json
{
  "requestId": "risk-req-20260607-000001",
  "decisionRunId": "rd_01JY1RISK000000000001",
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "strategyKey": "benefit_issue_default",
  "strategyVersion": 12,
  "mode": "ENFORCE",
  "decision": "REVIEW",
  "score": 78,
  "riskBand": "MEDIUM",
  "reasons": [
    {
      "code": "PAYMENT_FAIL_COUNT_HIGH",
      "message": "Recent failed payment count is high",
      "severity": "MEDIUM"
    }
  ],
  "matchedRules": [
    {
      "groupKey": "payment_velocity",
      "ruleKey": "fail_count_1d_gte_3",
      "mode": "ENFORCE",
      "action": "REVIEW",
      "scoreDelta": 30,
      "reasonCode": "PAYMENT_FAIL_COUNT_HIGH"
    }
  ],
  "labels": [
    "PAYMENT_VELOCITY_RISK"
  ],
  "missingFeatures": [],
  "traceAvailable": true,
  "latencyMs": 24
}
```

### 3.4 Idempotency

Idempotency key:

```text
tenantId + requestId
```

Behavior:

- Same `requestId` and same canonical request hash returns the first decision.
- Same `requestId` with a different canonical request hash returns `409 RISK_REQUEST_REPLAY_MISMATCH`.
- Idempotency records keep final decision, strategy version, request hash, decision run ID, and creation time.

### 3.5 Error Response

```json
{
  "code": "RISK_SCENE_NOT_FOUND",
  "message": "Risk scene is not configured",
  "requestId": "risk-req-20260607-000001",
  "details": {
    "sceneKey": "UNKNOWN_SCENE"
  }
}
```

Error codes:

| Code | HTTP | Meaning |
| --- | --- | --- |
| `RISK_BAD_REQUEST` | 400 | Request schema or value is invalid |
| `RISK_SCENE_NOT_FOUND` | 404 | Scene does not exist for tenant |
| `RISK_SCENE_DISABLED` | 409 | Scene exists but is not active |
| `RISK_STRATEGY_NOT_ACTIVE` | 409 | No active strategy version is available |
| `RISK_REQUEST_REPLAY_MISMATCH` | 409 | Same request ID used with different payload |
| `RISK_DEADLINE_EXCEEDED` | 504 | Decision exceeded caller deadline |
| `RISK_DEPENDENCY_FAILED` | 503 | Required feature, model, Redis, or audit dependency failed |
| `RISK_FORBIDDEN` | 403 | Caller lacks risk decision permission |

For runtime dependency failures, the service returns a normal decision shaped by scene fail policy when possible. It uses error responses only when the platform cannot produce a governed decision.

## 4. Rule DSL Contract

### 4.1 Rule Group Node

```json
{
  "logic": "AND",
  "conditions": [],
  "groups": []
}
```

Fields:

- `logic`: `AND` or `OR`.
- `conditions`: zero or more condition nodes.
- `groups`: zero or more nested rule group nodes.

Limits:

- Maximum nesting depth: `5`.
- Maximum total conditions per rule: `100`.
- Maximum serialized JSON size per rule: `64 KiB`.

### 4.2 Condition Node

```json
{
  "left": {
    "type": "FEATURE",
    "key": "buyer.fail_count_1d"
  },
  "op": ">=",
  "right": {
    "type": "LITERAL",
    "value": 3
  }
}
```

Supported operators:

| Category | Operators |
| --- | --- |
| Comparison | `==`, `!=`, `>`, `>=`, `<`, `<=` |
| String | `LIKE`, `STARTS_WITH`, `ENDS_WITH`, `CONTAINS` |
| Collection | `IN`, `NOT_IN`, `INTERSECTS` |
| Null/empty | `EXISTS`, `IS_EMPTY`, `IS_NULL` |
| Time | `BEFORE`, `AFTER`, `BETWEEN_TIME` |

### 4.3 Operand Types

| Type | Fields | Example |
| --- | --- | --- |
| `FEATURE` | `key` | `{ "type": "FEATURE", "key": "risk.score" }` |
| `LITERAL` | `value` | `{ "type": "LITERAL", "value": 85 }` |
| `LIST` | `key` | `{ "type": "LIST", "key": "blacklist.device" }` |
| `CONTEXT` | `path` | `{ "type": "CONTEXT", "path": "canvasId" }` |
| `EVENT` | `path` | `{ "type": "EVENT", "path": "amount" }` |
| `SUBJECT` | `path` | `{ "type": "SUBJECT", "path": "userId" }` |

Unsafe operands are not allowed in online rules:

- Raw script body.
- Java class name.
- Reflection target.
- File path.
- URL.
- Network endpoint.

### 4.4 Safe Expression Governance

Safe expressions are a future extension for approved computed factors and non-primary rule fragments. They are not accepted by the Phase 2 structured rule parser and are never a general script execution surface.

Any `SCRIPTED_SAFE_FUNCTION` implementation must satisfy all of these entry criteria before it can be activated in `MARK`, `SHADOW`, `DUAL_RUN`, `CANARY`, or `ENFORCE`:

- Function whitelist is explicit per tenant and strategy version.
- Reflection, class loading, process execution, file access, network access, and environment access are disabled.
- Expression length and compiled bytecode budget are enforced before compilation.
- Compile timeout and evaluation timeout are enforced.
- Compile cache is bounded by tenant, strategy version, and total entries.
- Cache entries use TTL after access and explicit invalidation on version deactivation.
- Runtime records compile count, cache hit count, cache eviction count, compile failure count, timeout count, evaluation latency, CodeCache pressure, and class-loading pressure.
- Validation rejects the expression if any referenced factor, function, or operator is not registered.
- Emergency pause can disable expression-backed rules without disabling the entire scene.

### 4.5 Validation Errors

Validation returns deterministic field-path errors:

```json
{
  "valid": false,
  "errors": [
    {
      "path": "$.conditions[0].left.key",
      "code": "UNKNOWN_FEATURE",
      "message": "Feature buyer.fail_count_1d is not registered"
    }
  ]
}
```

Validation error codes:

```text
INVALID_JSON
UNKNOWN_OPERATOR
UNKNOWN_OPERAND_TYPE
UNKNOWN_FEATURE
FEATURE_OFFLINE_ONLY
UNKNOWN_LIST
LIST_SUBJECT_TYPE_MISMATCH
MAX_DEPTH_EXCEEDED
MAX_CONDITIONS_EXCEEDED
TYPE_MISMATCH
UNSAFE_EXPRESSION
```

## 5. Strategy Snapshot Contract

Runtime consumes only immutable compiled snapshots.

```json
{
  "tenantId": 7,
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "strategyKey": "benefit_issue_default",
  "version": 12,
  "mode": "ENFORCE",
  "trafficPercent": 100,
  "failPolicy": "FAIL_REVIEW",
  "latencyBudgetMs": 50,
  "ruleGroups": [
    {
      "groupKey": "guardrail_lists",
      "groupType": "LIST_GATE",
      "executionOrder": 10,
      "matchPolicy": "ANY_MATCHED",
      "enabled": true,
      "rules": []
    },
    {
      "groupKey": "payment_velocity",
      "groupType": "SCORING",
      "executionOrder": 20,
      "matchPolicy": "WEIGHTED_SCORE",
      "enabled": true,
      "rules": [
        {
          "ruleKey": "fail_count_1d_gte_3",
          "priority": 100,
          "mode": "ENFORCE",
          "dsl": {
            "logic": "AND",
            "conditions": [
              {
                "left": { "type": "FEATURE", "key": "buyer.fail_count_1d" },
                "op": ">=",
                "right": { "type": "LITERAL", "value": 3 }
              }
            ],
            "groups": []
          },
          "action": "REVIEW",
          "scoreDelta": 30,
          "reasonCode": "PAYMENT_FAIL_COUNT_HIGH",
          "labels": ["PAYMENT_VELOCITY_RISK"]
        }
      ]
    }
  ],
  "actionPolicy": {
    "priority": ["BLOCK", "VERIFY", "REVIEW", "LIMIT", "DELAY", "ALLOW"],
    "scoreBands": [
      { "band": "LOW", "minInclusive": 0, "maxExclusive": 50 },
      { "band": "MEDIUM", "minInclusive": 50, "maxExclusive": 85 },
      { "band": "HIGH", "minInclusive": 85, "maxInclusive": 100 }
    ]
  },
  "compiledHash": "sha256:6cf3d9b4f5c32e8c0f4c4a4f0f0c6a1a",
  "createdAt": "2026-06-07T12:00:00Z"
}
```

Compiled hash input:

```text
canonicalJson(strategy snapshot without compiledHash)
```

## 6. Decision Table Contract

Decision table:

```json
{
  "tableKey": "benefit_amount_policy",
  "version": 3,
  "hitPolicy": "FIRST",
  "inputs": [
    {
      "name": "riskBand",
      "type": "STRING",
      "source": { "type": "FEATURE", "key": "risk.band" }
    },
    {
      "name": "benefitAmount",
      "type": "DECIMAL",
      "source": { "type": "EVENT", "path": "amount" }
    }
  ],
  "outputs": [
    { "name": "action", "type": "ACTION" },
    { "name": "scoreDelta", "type": "INTEGER" },
    { "name": "reasonCode", "type": "STRING" }
  ],
  "rows": [
    {
      "priority": 10,
      "when": {
        "riskBand": ["HIGH"],
        "benefitAmount": { "op": ">=", "value": 500 }
      },
      "then": {
        "action": "BLOCK",
        "scoreDelta": 40,
        "reasonCode": "HIGH_RISK_HIGH_AMOUNT"
      }
    }
  ]
}
```

Supported hit policies:

| Hit Policy | Semantics |
| --- | --- |
| `FIRST` | Return the first matched row by `priority` ascending |
| `UNIQUE` | Exactly one row may match; multiple matches fail validation |
| `COLLECT` | Return all matched row outputs and let group policy merge them |
| `PRIORITY` | Return the matched row with highest configured output priority |

Import validation must reject:

- duplicate row priority in one table version.
- row with no output.
- unsupported output action.
- overlapping rows under `UNIQUE`.
- empty condition columns.
- unsupported data type.

## 7. Database Contract

All risk tables are tenant-scoped. All timestamps are UTC. JSON columns store canonical JSON. Applied migrations are immutable.

### 7.1 Foundation DDL

```sql
CREATE TABLE risk_scene (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  event_schema_key VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  default_mode VARCHAR(32) NOT NULL,
  fail_policy VARCHAR(32) NOT NULL,
  latency_budget_ms INT NOT NULL,
  owner VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_scene_tenant_key (tenant_id, scene_key),
  KEY idx_risk_scene_tenant_status (tenant_id, status)
);

CREATE TABLE risk_strategy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  active_version INT NULL,
  draft_version INT NULL,
  risk_level VARCHAR(32) NOT NULL,
  owner VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_strategy_tenant_key (tenant_id, strategy_key),
  KEY idx_risk_strategy_tenant_scene (tenant_id, scene_key)
);

CREATE TABLE risk_strategy_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  mode VARCHAR(32) NOT NULL,
  traffic_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
  compiled_hash VARCHAR(128) NOT NULL,
  definition_json JSON NOT NULL,
  validation_json JSON NULL,
  created_by VARCHAR(128) NOT NULL,
  approved_by VARCHAR(128) NULL,
  approved_at DATETIME(3) NULL,
  effective_from DATETIME(3) NULL,
  effective_to DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_strategy_version (tenant_id, strategy_key, version),
  KEY idx_risk_strategy_version_mode (tenant_id, strategy_key, mode)
);

CREATE TABLE risk_list (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  list_key VARCHAR(128) NOT NULL,
  list_type VARCHAR(32) NOT NULL,
  subject_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
  owner VARCHAR(128) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_list_tenant_key (tenant_id, list_key)
);

CREATE TABLE risk_list_entry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  list_key VARCHAR(128) NOT NULL,
  subject_hash VARCHAR(128) NOT NULL,
  subject_masked VARCHAR(255) NOT NULL,
  reason VARCHAR(512) NOT NULL,
  source VARCHAR(128) NOT NULL,
  effective_from DATETIME(3) NOT NULL,
  expires_at DATETIME(3) NULL,
  created_by VARCHAR(128) NOT NULL,
  approval_id BIGINT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_list_entry_subject (tenant_id, list_key, subject_hash),
  KEY idx_risk_list_entry_expiry (tenant_id, list_key, expires_at)
);

CREATE TABLE risk_decision_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  request_id VARCHAR(128) NOT NULL,
  request_hash VARCHAR(128) NOT NULL,
  scene_key VARCHAR(128) NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  strategy_version INT NOT NULL,
  subject_hash VARCHAR(128) NOT NULL,
  decision VARCHAR(32) NOT NULL,
  score INT NOT NULL,
  risk_band VARCHAR(32) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  latency_ms INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  input_snapshot_json JSON NOT NULL,
  output_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_risk_decision_request (tenant_id, request_id),
  KEY idx_risk_decision_scene_time (tenant_id, scene_key, created_at),
  KEY idx_risk_decision_subject_time (tenant_id, subject_hash, created_at)
);

CREATE TABLE risk_rule_hit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  decision_run_id BIGINT NOT NULL,
  strategy_key VARCHAR(128) NOT NULL,
  strategy_version INT NOT NULL,
  group_key VARCHAR(128) NOT NULL,
  rule_key VARCHAR(128) NOT NULL,
  mode VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  score_delta INT NOT NULL,
  reason_code VARCHAR(128) NOT NULL,
  evidence_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_risk_rule_hit_run (tenant_id, decision_run_id),
  KEY idx_risk_rule_hit_rule_time (tenant_id, rule_key, created_at)
);
```

### 7.2 Extension Tables

Later migrations add:

- `risk_rule_group`
- `risk_rule`
- `risk_factor_definition`
- `risk_decision_table`
- `risk_decision_trace`
- `risk_simulation_run`
- `risk_approval`
- `risk_review_case`
- `risk_model_registry`
- `risk_graph_case`
- `risk_audit_event`

The foundation migration must not depend on these later tables.

## 8. MQ Event Contract

### 8.1 Decision Event

Topic:

```text
risk.decision.events
```

Key:

```text
tenantId + ":" + sceneKey + ":" + requestId
```

Payload:

```json
{
  "eventType": "RISK_DECISION_COMPLETED",
  "eventVersion": 1,
  "tenantId": 7,
  "requestId": "risk-req-20260607-000001",
  "decisionRunId": "rd_01JY1RISK000000000001",
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "strategyKey": "benefit_issue_default",
  "strategyVersion": 12,
  "decision": "REVIEW",
  "score": 78,
  "riskBand": "MEDIUM",
  "mode": "ENFORCE",
  "labels": ["PAYMENT_VELOCITY_RISK"],
  "createdAt": "2026-06-07T12:00:00Z"
}
```

### 8.2 Strategy Event

Topic:

```text
risk.strategy.events
```

Event types:

```text
RISK_STRATEGY_VALIDATED
RISK_STRATEGY_SUBMITTED
RISK_STRATEGY_APPROVED
RISK_STRATEGY_ACTIVATED
RISK_STRATEGY_PAUSED
RISK_STRATEGY_ROLLED_BACK
RISK_STRATEGY_REJECTED
```

### 8.3 Feature Event

Topic:

```text
risk.feature.events
```

Event types:

```text
RISK_FEATURE_UPDATED
RISK_FEATURE_EXPIRED
RISK_FEATURE_QUALITY_DEGRADED
RISK_FEATURE_BACKFILL_COMPLETED
```

## 9. Metrics Contract

Metric names follow Prometheus naming conventions.

| Metric | Type | Labels |
| --- | --- | --- |
| `risk_decision_requests_total` | counter | `tenant_id`, `scene_key`, `mode`, `caller` |
| `risk_decision_latency_ms` | histogram | `tenant_id`, `scene_key`, `mode` |
| `risk_decision_failures_total` | counter | `tenant_id`, `scene_key`, `fail_policy`, `failure_type` |
| `risk_rule_hits_total` | counter | `tenant_id`, `scene_key`, `strategy_key`, `version`, `group_key`, `rule_key`, `mode`, `action` |
| `risk_feature_missing_total` | counter | `tenant_id`, `scene_key`, `feature_key`, `source` |
| `risk_feature_latency_ms` | histogram | `tenant_id`, `feature_key`, `source` |
| `risk_list_hits_total` | counter | `tenant_id`, `list_key`, `list_type`, `subject_type` |
| `risk_strategy_activations_total` | counter | `tenant_id`, `scene_key`, `strategy_key`, `mode` |
| `risk_strategy_rollbacks_total` | counter | `tenant_id`, `scene_key`, `strategy_key` |
| `risk_simulation_runs_total` | counter | `tenant_id`, `scene_key`, `strategy_key`, `status` |
| `risk_model_gateway_latency_ms` | histogram | `tenant_id`, `model_key`, `model_version` |
| `risk_model_gateway_failures_total` | counter | `tenant_id`, `model_key`, `failure_type` |
| `risk_safe_expression_compiles_total` | counter | `tenant_id`, `strategy_key`, `status` |
| `risk_safe_expression_cache_hits_total` | counter | `tenant_id`, `strategy_key` |
| `risk_safe_expression_cache_evictions_total` | counter | `tenant_id`, `reason` |
| `risk_safe_expression_eval_latency_ms` | histogram | `tenant_id`, `strategy_key` |
| `risk_safe_expression_timeouts_total` | counter | `tenant_id`, `strategy_key`, `phase` |
| `risk_jvm_code_cache_usage_bytes` | gauge | `tenant_id`, `service` |
| `risk_jvm_class_loading_total` | counter | `tenant_id`, `service`, `source` |
| `risk_review_cases_total` | counter | `tenant_id`, `scene_key`, `status` |

Trace spans:

```text
risk.decision.evaluate
risk.scene.route
risk.strategy.compile
risk.feature.resolve
risk.rule_group.execute
risk.rule.evaluate
risk.list.lookup
risk.model.score
risk.decision.merge
risk.trace.persist
```

## 10. Audit Event Contract

Audit event:

```json
{
  "auditEventId": "rae_01JY1RISK000000000002",
  "tenantId": 7,
  "eventType": "RISK_STRATEGY_ACTIVATED",
  "resourceType": "STRATEGY_VERSION",
  "resourceKey": "benefit_issue_default",
  "resourceVersion": "12",
  "operatorId": "user-9",
  "approvalId": "approval-88",
  "reason": "Approved rollout after dual-run",
  "beforeJson": {
    "activeVersion": 11
  },
  "afterJson": {
    "activeVersion": 12
  },
  "createdAt": "2026-06-07T12:00:00Z"
}
```

Required audit event types:

```text
RISK_SCENE_CREATED
RISK_SCENE_UPDATED
RISK_STRATEGY_DRAFT_CREATED
RISK_STRATEGY_VALIDATED
RISK_STRATEGY_SUBMITTED
RISK_STRATEGY_APPROVED
RISK_STRATEGY_REJECTED
RISK_STRATEGY_ACTIVATED
RISK_STRATEGY_PAUSED
RISK_STRATEGY_ROLLED_BACK
RISK_LIST_CREATED
RISK_LIST_ENTRY_IMPORTED
RISK_LIST_ENTRY_DELETED
RISK_DECISION_TABLE_IMPORTED
RISK_MODEL_REGISTERED
RISK_MODEL_THRESHOLD_CHANGED
RISK_AI_RULE_DRAFT_CREATED
RISK_EMERGENCY_ACTION
RISK_AUDIT_EXPORTED
```

## 11. Canvas Node Contract

Node type:

```text
RISK_DECISION
```

Node config:

```json
{
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "subjectMapping": {
    "userId": "$.user.id",
    "deviceId": "$.device.id",
    "ip": "$.request.ip"
  },
  "eventMapping": {
    "amount": "$.coupon.amount",
    "couponCode": "$.coupon.code",
    "channel": "$.channel"
  },
  "contextMapping": {
    "businessLine": "LOYALTY"
  },
  "failPolicy": "FAIL_REVIEW",
  "routes": {
    "ALLOW": "next_allow",
    "REVIEW": "next_review",
    "VERIFY": "next_verify",
    "BLOCK": "next_block",
    "DELAY": "next_delay",
    "LIMIT": "next_limit"
  }
}
```

Canvas handler behavior:

- Build `RiskDecisionRequest` from execution context.
- Use Canvas tenant from authenticated or execution context.
- Store decision result under `context.riskDecision`.
- Route by final action.
- Apply node fail policy if risk service call fails before a governed decision is returned.

## 12. Readiness Gates

Implementation is contract-ready when these checks pass:

- API tests assert tenant context overrides request body tenant.
- DSL parser rejects unsafe operands and unknown operators.
- DSL validator rejects unknown features, offline-only factors, list subject mismatch, and excessive depth.
- Strategy compiler computes stable hash and records required features.
- Decision service records decision run and rule hits.
- Idempotency returns stable decisions for repeated requests.
- List matcher hashes subject values before lookup and logging.
- Metrics exist with the names in this document.
- Audit events exist for all required governance actions.
- Runbook verification commands pass.
