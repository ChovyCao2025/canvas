# Risk Control Rule Engine Production Runbook

## Scope

This runbook covers production operation for the Canvas Risk Control Platform:

- Online risk decisions for API, MQ, batch, and Canvas `RISK_DECISION` nodes.
- Strategy lifecycle from draft through simulation, mark, shadow, dual-run, canary, enforce, pause, and rollback.
- List management, realtime features, model gateway, graph intelligence, decision trace, audit ledger, metrics, and alerts.
- Incident response for false positives, false negatives, dependency outages, latency regressions, rule-runtime faults, and compliance export.

The runbook assumes the product contracts described in:

- `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md`
- `docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md`
- `docs/superpowers/specs/2026-06-07-risk-control-contracts.md`
- `docs/superpowers/specs/2026-06-07-risk-control-traceability-matrix.md`
- `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md`

## Production Invariants

These invariants must hold in every production environment:

- No active strategy can be mutated in place. Runtime uses immutable compiled strategy snapshots.
- Every decision run has a tenant-scoped trace ID, request ID, strategy version, feature snapshot, matched rules, final action, and fail policy evidence.
- Every strategy activation, rollback, pause, list import, list deletion, model threshold change, and AI-generated rule draft has audit evidence.
- High-risk strategies must pass simulation, mark, dual-run, canary, approval, and rollback readiness gates before enforcement.
- Whitelist entries cannot bypass compliance blacklists.
- Raw PII is never stored in list entries, logs, traces, or exported incident evidence.
- Online runtime must not execute arbitrary scripts. Safe expressions require function whitelist, timeout, compile-cache budget, and explicit invalidation.
- Runtime failure uses the configured scene fail policy: `FAIL_OPEN`, `FAIL_REVIEW`, or `FAIL_CLOSED`.
- Emergency pause and rollback must be available without deploying code.

## Required Roles

- `RISK_ADMIN`: manage platform configuration, global fail policies, emergency actions, and observability.
- `RISK_STRATEGIST`: create and edit scenes, strategies, rules, factors, lists, decision tables, and simulations.
- `RISK_APPROVER`: approve high-risk strategy activation, rollback, model threshold changes, and bulk list imports.
- `RISK_REVIEWER`: process review cases and feedback labels.
- `AUDITOR`: export audit evidence and decision traces with PII masking.
- `SUPER_ADMIN`: break-glass emergency access, audited separately from normal risk roles.

Emergency actions require `RISK_ADMIN` or `SUPER_ADMIN`. Strategy activation requires `RISK_APPROVER` plus a different author identity for high-risk changes.

## Required Configuration

Production must configure:

```bash
CANVAS_RISK_ENABLED=true
CANVAS_RISK_DECISION_DEFAULT_FAIL_POLICY=FAIL_REVIEW
CANVAS_RISK_DECISION_TIMEOUT_MS=50
CANVAS_RISK_COMPLEX_DECISION_TIMEOUT_MS=100
CANVAS_RISK_TRACE_RETENTION_DAYS=180
CANVAS_RISK_AUDIT_RETENTION_DAYS=730
CANVAS_RISK_PII_HASH_SECRET=<32-or-more-byte-secret>
CANVAS_RISK_SAFE_EXPRESSION_ENABLED=true
CANVAS_RISK_SAFE_EXPRESSION_TIMEOUT_MS=20
CANVAS_RISK_SAFE_EXPRESSION_CACHE_MAX_PER_TENANT=5000
CANVAS_RISK_SAFE_EXPRESSION_CACHE_TTL_SECONDS=3600
CANVAS_RISK_PEAK_CHANGE_FREEZE_ENABLED=true
CANVAS_RISK_PEAK_CHANGE_FREEZE_WINDOWS=11:00-14:00,18:00-22:00
CANVAS_RISK_MODEL_GATEWAY_TIMEOUT_MS=30
CANVAS_RISK_REDIS_FEATURE_TIMEOUT_MS=15
CANVAS_RISK_EMERGENCY_PAUSE_REQUIRE_REASON=true
```

Production startup must fail if:

- `CANVAS_RISK_PII_HASH_SECRET` is missing or shorter than 32 bytes.
- Risk is enabled and audit persistence is unavailable.
- Risk is enabled and no emergency pause storage is available.
- Safe expressions are enabled without timeout and cache limits.
- Model gateway is enabled without timeout and circuit-breaker settings.

## Core Dashboards

Risk operations must maintain these dashboard panels:

- Decision QPS by tenant, scene, strategy version, and caller type.
- Decision latency P50, P95, P99, and timeout count.
- Decision failure rate by fail policy.
- Action distribution: `ALLOW`, `REVIEW`, `VERIFY`, `BLOCK`, `DELAY`, `LIMIT`, `SHADOW_ONLY`.
- Rule-hit rate by rule group, rule, strategy version, and mode.
- Feature missing rate by feature key, source, and scene.
- List hit count and list import activity.
- Model gateway latency, timeout, error rate, and fallback rate.
- Redis feature latency and miss rate.
- Flink realtime feature lag and checkpoint health.
- Safe expression compile count, cache hit rate, eviction count, timeout count, and CodeCache pressure.
- Strategy activation, rollback, pause, and approval activity.
- Review case backlog and SLA breach count.

## Alert Rules

Minimum alert set:

| Alert | Severity | Trigger | First Action |
| --- | --- | --- | --- |
| `RiskDecisionLatencySloBreach` | P1 | P95 above scene budget for 10 minutes | Check dependency latency and disable expensive candidate strategies |
| `RiskDecisionFailureSpike` | P0/P1 | Failure rate exceeds scene threshold | Apply fail policy, pause newest strategy if correlated |
| `RiskBlockRateSpike` | P0 | `BLOCK` rate rises above baseline plus threshold | Enter false-positive playbook |
| `RiskAllowRateSpike` | P0 | `ALLOW` rate rises after strategy/model/list change | Enter false-negative playbook |
| `RiskFeatureMissingSpike` | P1 | Feature missing rate exceeds threshold | Check Redis, Flink, schema drift, and resolver config |
| `RiskModelGatewayFailure` | P1 | Model timeout/error/fallback spike | Switch model gate to fallback or disable model contribution |
| `RiskRedisFeatureOutage` | P0/P1 | Redis feature reads unavailable | Use scene fail policy and disable Redis-only candidate rules |
| `RiskFlinkFeatureLag` | P1 | Realtime feature lag exceeds allowed window | Freeze activations that rely on lagging features |
| `RiskSafeExpressionPressure` | P1 | Compile eviction/timeout/CodeCache pressure | Pause expression-heavy candidate strategies |
| `RiskAuditWriteFailure` | P0 | Audit ledger write unavailable | Stop activation and use configured runtime fail policy |
| `RiskReviewBacklogSlaBreach` | P1 | Review queue exceeds SLA | Apply staffing escalation and reduce new REVIEW-producing strategy rollout |

## Normal Strategy Rollout

Use this sequence for every production strategy change.

### 1. Draft and Validate

1. Confirm author is not the final approver for high-risk scenes.
2. Validate DSL syntax, factor availability, list ownership, decision-table hit policy, and action policy.
3. Confirm no rule references online-disallowed factors in `ENFORCE`, `MARK`, `SHADOW`, `DUAL_RUN`, or `CANARY`.
4. Confirm expression-backed rules use approved functions only.
5. Confirm rule count, group count, decision-table row count, and expression count stay below scene limits.

### 2. Simulate

1. Select historical sample windows covering normal traffic, known attack windows, campaign peaks, and high-value users.
2. Run baseline and candidate strategies against the same samples.
3. Compare action distribution, block rate, review rate, high-value user impact, feature missing rate, and latency estimate.
4. Export simulation report and attach it to approval evidence.
5. Reject activation if simulation shows unexplained block-rate, allow-rate, or missing-feature drift.

### 3. Mark

1. Enable `MARK` mode for candidate rules.
2. Confirm marked rules write rule-hit details but do not alter upstream action.
3. Monitor marked hit rate for at least one normal traffic window.
4. Compare marked hits with simulation expectations.
5. Require validation-result backfill before moving to dual-run.

### 4. Dual-Run

1. Enable `DUAL_RUN` for baseline and candidate versions.
2. Confirm trace contains both baseline and candidate results.
3. Compare action diffs by segment, channel, geography, account age, device class, and high-value user status.
4. Investigate all candidate `BLOCK` outcomes where baseline was `ALLOW`.
5. Investigate all candidate `ALLOW` outcomes where baseline was `BLOCK`, `VERIFY`, or `REVIEW`.
6. Reject activation if unexplained diffs exceed the approved threshold.

### 5. Canary

1. Enable deterministic canary by `tenantId + sceneKey + strategyKey + subjectKey`.
2. Start at 1 percent for high-risk scenes, 5 percent for ordinary scenes.
3. Watch decision latency, block rate, review rate, allow rate, feature missing, model fallback, and customer-support signals.
4. Increase canary only after the previous step is stable for the approved observation window.
5. Stop canary immediately if P0 or P1 alerts fire.

### 6. Enforce

1. Activate candidate version as `ENFORCE`.
2. Keep previous version available for immediate rollback.
3. Keep elevated monitoring for one full business cycle.
4. Close rollout only after dashboards are stable and audit evidence is complete.

## Peak-Hour Change Freeze

When `CANVAS_RISK_PEAK_CHANGE_FREEZE_ENABLED=true`, block high-risk activation during freeze windows unless both are true:

- Active P0/P1 incident requires the change to reduce user or financial harm.
- `SUPER_ADMIN` approves break-glass activation with incident ID and rollback plan.

Allowed during freeze:

- Emergency pause.
- Rollback to a previously safe version.
- Adding a compliance blacklist entry with approval.
- Disabling a failing model gate or non-critical expression-backed rule.

Not allowed during freeze:

- New high-risk block rules.
- New model threshold enforcement.
- Bulk list import without compliance incident.
- Decision table activation that increases `BLOCK`, `VERIFY`, or `REVIEW`.

## Emergency Pause

Use emergency pause when a strategy version, rule group, list, model, or feature dependency may be causing active harm.

1. Identify affected tenant, scene, strategy version, and latest activation or import event.
2. Pause the smallest harmful scope first: rule, rule group, strategy version, list, model gate, or full scene.
3. Enter reason, incident ID, operator, and expected customer impact.
4. Confirm new decision traces no longer include the paused component.
5. Keep audit export and trace sampling enabled.
6. Move to rollback only after the previous safe version is identified.

Expected control-plane APIs:

```bash
curl -sS -X POST -H "Authorization: Bearer $RISK_ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d "{\"reason\":\"$INCIDENT_ID emergency pause\", \"scope\":\"STRATEGY_VERSION\"}" \
  "$CANVAS_BASE_URL/canvas/risk/strategies/$STRATEGY_KEY/versions/$VERSION/pause"
```

```bash
curl -sS -X POST -H "Authorization: Bearer $RISK_ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d "{\"reason\":\"$INCIDENT_ID pause high-risk list\"}" \
  "$CANVAS_BASE_URL/canvas/risk/lists/$LIST_KEY/pause"
```

## Rollback

Rollback restores the last approved safe version and keeps the failed version available for investigation.

1. Confirm the target previous version is not `PAUSED`, `ARCHIVED`, or already marked unsafe.
2. Export active version trace sample before rollback.
3. Roll back strategy version.
4. Verify `risk_strategy.active_version` points to the previous safe version.
5. Verify new decision traces use the rollback target.
6. Watch block rate, allow rate, review rate, latency, and feature missing for 30 minutes.
7. Keep the failed version in `ROLLED_BACK` state with incident reference.

Expected API:

```bash
curl -sS -X POST -H "Authorization: Bearer $RISK_ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d "{\"reason\":\"$INCIDENT_ID rollback to previous safe version\", \"targetVersion\":\"$TARGET_VERSION\"}" \
  "$CANVAS_BASE_URL/canvas/risk/strategies/$STRATEGY_KEY/versions/$VERSION/rollback"
```

Rollback is complete only when:

- New traces no longer use the failed version.
- Alert that triggered rollback is clear or trending down.
- Audit ledger contains rollback evidence.
- Incident evidence includes before/after action distribution.

## False Positive Response

Use when legitimate users, merchants, messages, transactions, or Canvas executions are blocked, delayed, limited, or forced into review incorrectly.

1. Set severity:
   - P0: broad user-facing block, payment impact, campaign outage, or regulatory impact.
   - P1: one tenant, channel, campaign, or important segment affected.
   - P2: isolated rule or small sample.
2. Identify affected scene, strategy version, rule group, rule, list, model, or feature.
3. Pause the smallest harmful component.
4. If harm continues, roll back the full strategy version.
5. Export trace samples for affected and unaffected events.
6. Compare candidate vs baseline reasons and feature values.
7. Check recent changes:
   - Strategy activation.
   - Decision table import.
   - List import.
   - Model version or threshold.
   - Feature schema or Flink job.
   - Safe expression change.
8. Compensate business workflow:
   - Requeue eligible Canvas actions.
   - Reissue wrongly withheld benefits after approval.
   - Release wrongly held messages or transactions only after compliance review.
9. Add a regression simulation sample set before reactivation.

Exit criteria:

- False-positive rate returns to approved baseline.
- Affected users or business objects are remediated or explicitly waived.
- Root cause has an owner and follow-up due date.
- Regression sample is attached to the strategy.

## False Negative Response

Use when fraud, abuse, suspicious activity, or policy violations pass through the engine.

1. Set severity by financial, compliance, and reputational impact.
2. Preserve traces and raw event references for suspected events.
3. Identify why the decision allowed the event:
   - Missing feature.
   - Stale realtime aggregate.
   - List miss.
   - Model fallback.
   - Rule threshold too loose.
   - Canary not covering the segment.
   - Fail-open scene policy.
4. Add temporary mitigation:
   - Compliance blacklist.
   - Step-up verification.
   - Lower limit or delay.
   - Review queue rule.
   - Model gate fallback threshold.
5. Run simulation on attack-window samples and normal samples.
6. Deploy candidate mitigation through mark, dual-run, and canary unless P0 emergency approval permits direct enforcement.
7. Create post-incident feature or rule improvement task.

Exit criteria:

- Attack pattern is blocked, verified, reviewed, limited, or delayed according to incident decision.
- Normal traffic impact is measured and accepted.
- Temporary mitigation has an owner and expiry date.
- Permanent rule, feature, model, or graph improvement is planned.

## Redis Feature Outage

1. Confirm Redis health and `risk_feature_redis_latency_ms`.
2. Identify scenes and factors depending on Redis features.
3. Check fail policy for affected scenes.
4. Disable candidate rules that rely only on unavailable Redis features.
5. For strong-risk scenes, move to `FAIL_REVIEW` or `FAIL_CLOSED` according to pre-approved policy.
6. For low-risk marketing scenes, use `FAIL_OPEN` only if business owner approves and compliance blacklist still works.
7. Restore Redis service and warm high-cardinality feature keys.
8. Run dual-run comparison after recovery before re-enabling disabled candidate rules.

## Flink Realtime Feature Lag

1. Confirm lag, checkpoint status, backpressure, and failed operators.
2. Identify feature keys with stale event windows.
3. Freeze activation for strategies depending on lagging features.
4. If stale features can cause false positives, pause affected rules or switch them to `MARK`.
5. If stale features can cause false negatives, route affected decisions to `REVIEW`, `VERIFY`, or conservative limits based on scene policy.
6. Backfill feature state if the incident window invalidated counters.
7. Run simulation on recovered feature snapshots before returning to normal enforcement.

## Model Gateway Outage

1. Confirm model gateway timeout, error, and fallback metrics.
2. Identify model gates and score factors using the failing model.
3. Use configured fallback:
   - Last known safe score only if within TTL.
   - Rule-only strategy if model is optional.
   - `REVIEW` or `VERIFY` if model is required for high-risk decisions.
4. Disable model contribution in candidate versions before disabling in active versions.
5. Escalate to model owner with model version, input schema, response samples, and timeout traces.
6. Reactivate model gate only after canary dual-run confirms stable score distribution.

## Safe Expression or CodeCache Pressure

1. Check expression compile count, cache hit rate, eviction count, timeout count, and JVM CodeCache pressure.
2. Identify top expression-heavy strategy versions and tenants.
3. Pause new activations containing expression-backed rules.
4. Switch high-volume expression-backed candidate rules to `MARK` or pause them.
5. Increase cache capacity only when heap, CodeCache, and eviction metrics support the change.
6. Prefer converting recurring expressions into registered structured factors or AST rules.
7. Keep incident open until compile storm stops and latency returns below SLO.

## Decision API Latency

1. Break latency down by routing, feature resolution, list lookup, model gateway, rule evaluation, decision merge, trace persistence, and audit write.
2. Disable or mark candidate strategy versions that exceed latency budget.
3. Check remote model and Redis feature timeouts.
4. Reduce trace detail only if the incident is P0 and audit policy allows temporary sampling.
5. If latency continues, pause non-critical Canvas callers or lower canary percentage.
6. Restore traffic gradually and compare P50/P95/P99 against scene budgets.

## Audit Write Failure

Audit write failure is a P0 condition for governance operations.

1. Stop strategy activation, rollback, list import, and model threshold changes.
2. Continue online decision serving only if decision traces remain durable or approved degraded mode is active.
3. Confirm database, queue, and audit sink health.
4. Backfill missing audit evidence from decision trace and control-plane logs if any gap occurred.
5. Resume governance writes only after audit persistence is healthy and backfill evidence is attached.

## Bulk List Import Error

1. Pause the imported list if it is active and harm is possible.
2. Export import batch metadata: operator, source, row count, accepted count, rejected count, hash salt version, and approval ID.
3. Compare sample masked values with expected subjects.
4. Confirm expired entries and delete requests were processed correctly.
5. Roll back the import batch if wrong subjects, wrong list type, or wrong tenant is detected.
6. Re-run simulation or mark mode before reactivating list-dependent rules.

## Review Queue Overload

1. Check queue size, age distribution, SLA breach count, and decision sources.
2. Identify rules and strategy versions causing review growth.
3. Reduce candidate canary or pause review-heavy rules.
4. Temporarily adjust low-risk REVIEW actions to `VERIFY`, `LIMIT`, `DELAY`, or `ALLOW` only with business and compliance approval.
5. Add reviewer capacity for high-risk queues.
6. Close incident after queue age stays below SLA for one observation window.

## AI-Assisted Rule Incident

AI-generated or AI-assisted rules must never auto-enforce.

1. Identify rule drafts or versions with `AI_ASSISTED` source.
2. Confirm prompt summary, model version, generated diff, reviewer, and approval record.
3. Pause the rule or strategy version if AI-assisted logic contributed to harm.
4. Add the incident samples to simulation regression data.
5. Disable AI rule suggestions for the affected scene until review completes.

## Audit Export and PII Handling

Audit exports must include:

- Tenant ID.
- Scene key.
- Strategy key and version.
- Decision run ID and request ID.
- Final action and score.
- Rule hits and reason codes.
- Feature keys and masked feature values.
- List keys and masked subjects.
- Operator and approval IDs for governance actions.
- Incident IDs for emergency actions.

Audit exports must not include:

- Raw phone numbers.
- Raw emails.
- Raw payment card data.
- Raw device secrets.
- Raw access tokens.
- Full request payloads containing PII.
- Unmasked model input payloads.

Use tenant-scoped hash and masking for subject evidence. Store export reason, exporter identity, and recipient in the audit ledger.

## Post-Incident Evidence

Create incident evidence under:

```text
docs/architecture/evidence/incidents/<yyyy-mm-dd>-risk-control-<incident-id>.md
```

Evidence must include:

- Incident ID, severity, tenant, scenes, strategy versions, and time window.
- Alert names and dashboard links.
- Latest strategy activation, rollback, list import, feature deployment, and model deployment events.
- Sample decision run IDs.
- Before and after action distribution.
- Mitigation actions and audit IDs.
- Customer or business remediation status.
- Root cause.
- Follow-up tasks with owners and due dates.

## Recovery Checklist

Do not close a risk-control incident until all items are true:

- Triggering alert is clear or accepted by incident commander.
- New decision traces show the intended strategy version and action behavior.
- False-positive or false-negative sample set has been re-evaluated.
- Audit ledger contains every emergency action.
- Review queue impact is understood.
- Business remediation is complete or explicitly waived.
- Permanent fix owner is assigned.
- Regression simulation sample is attached to the strategy or feature.
- Post-incident evidence file is created.

## Verification Commands

After implementation, the following focused verification commands must pass before production enablement:

```bash
cd backend
mvn test -pl canvas-engine \
  -Dtest=RiskRuleParserTest,RiskRuleValidatorTest,RiskRuleEvaluatorTest,RiskStrategyCompilerTest,RiskDecisionMergerTest,RiskDecisionServiceTest,RiskListMatcherTest,RiskDecisionControllerTest,RiskDecisionHandlerTest,RiskMetricsTest \
  -DfailIfNoTests=false
```

```bash
cd frontend
npm run test -- riskApi.test.ts riskWorkbench.test.ts canvasSchemas.test.ts
npm run build
```

Production readiness audit must also confirm:

- Emergency pause and rollback APIs are present.
- Metrics and alert rules are deployed.
- Trace and audit retention are configured.
- PII masking is verified.
- Safe-expression cache and timeout metrics exist.
- Simulation, mark, dual-run, and canary gates are enforced for high-risk changes.
