# Enterprise Risk Control Rule Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade risk control rule engine for Marketing Canvas with online decisions, governed strategy authoring, list management, simulation, versioning, audit, and Canvas integration.

**Architecture:** The platform is a separate `domain/risk` and `web/risk` subsystem. Online runtime evaluates only compiled, tenant-scoped, structured strategy snapshots; Strategy Studio and Risk Lab operate through versioned metadata and never mutate active runtime state in place.

**Execution Guard:** Current repository state shows a normal `main` checkout with many unrelated modified and untracked files. Code implementation must start in an isolated worktree or with explicit user consent to modify `main`. The current working tree also contains untracked migrations through `V350`, so the risk-control foundation migration uses `V351`.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, MySQL metadata, Redis/Caffeine runtime caches, RocketMQ event ingestion, Flink realtime feature jobs, Doris simulation store, React, Ant Design, Vitest, JUnit 5, AssertJ.

---

## Source Spec

- `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md`

## File Structure

### Documentation

- Create: `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md` — product and architecture design.
- Create: `docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md` — source evidence, trust level, and design-decision matrix.
- Create: `docs/superpowers/specs/2026-06-07-risk-control-contracts.md` — production API, DSL, database, event, metric, audit, and Canvas node contracts.
- Create: `docs/superpowers/specs/2026-06-07-risk-control-traceability-matrix.md` — requirements-to-artifacts-to-verification traceability matrix.
- Create: `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md` — implementation plan.
- Create: `docs/runbooks/risk-control-rule-engine.md` — production operations and incident response runbook.

### Backend Metadata and Domain

- Create: `backend/canvas-engine/src/main/resources/db/migration/V351__risk_control_rule_engine_foundation.sql` — additive metadata schema.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskDecision.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskDecisionAction.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskBand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskScene.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskStrategySnapshot.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskRuleGroup.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskRule.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskReason.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskDecisionTrace.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/model/RiskFeatureValue.java`

### Backend Rule DSL and Runtime

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleNode.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleGroupNode.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleConditionNode.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleOperand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleOperator.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleParser.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidator.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluator.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompiler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyRuntimeCache.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskFeatureResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMerger.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListMatcher.java`

### Backend Persistence

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskSceneDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskStrategyDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskStrategyVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskListDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskListEntryDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskDecisionRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/RiskRuleHitDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskSceneMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskStrategyMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskStrategyVersionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskListMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskListEntryMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskDecisionRunMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/RiskRuleHitMapper.java`

### Backend Web APIs

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskListController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskLabController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskReviewCaseController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionResponse.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskStrategyDraftRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskListEntryRequest.java`

### Canvas Integration

- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java` — add `RISK_DECISION`.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RiskDecisionHandler.java`
- Modify: `frontend/src/types/canvas.ts` and `frontend/src/types/canvasSchemas.ts` — add node schema.
- Modify: `frontend/src/components/config-panel` — add Risk Decision node config panel.

### Frontend Strategy Studio

- Create: `frontend/src/services/riskApi.ts`
- Create: `frontend/src/pages/risk/index.tsx`
- Create: `frontend/src/pages/risk/riskWorkbench.ts`
- Create: `frontend/src/pages/risk/ruleDsl.ts`
- Create: `frontend/src/pages/risk/riskWorkbench.test.ts`
- Create: `frontend/src/services/riskApi.test.ts`

### Tests

- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/dsl/RiskRuleParserTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidatorTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluatorTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMergerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskListMatcherTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskDecisionControllerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/RiskDecisionHandlerTest.java`

## Phase 0: Product and Architecture Baseline

### Task 0.1: Capture product design

**Files:**
- Create: `docs/superpowers/specs/2026-06-06-risk-control-rule-engine-design.md`

- [x] **Step 1: Record references**

Capture Antom Shield, old Ant risk-score API, Zeus, Youzan, Drools DMN, and OpenL references in the spec.

- [x] **Step 1a: Capture Zeus applicability**

Record that Zeus is a strong architecture and governance reference, but not an API or schema implementation source. Preserve the concrete implications: common-node access plus independent service API, scene/rule-group/rule/factor layering, extension/cumulative/decision-table/list/tool factors, mark/dual-run/replay validation, anti-misoperation governance, and Aviator compile-cache/JVM pressure.

- [x] **Step 1b: Create source evidence matrix**

Write `docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md` with source tiers, supported design decisions, insufficiencies, and final sufficiency assessment.

- [x] **Step 1c: Create production contracts**

Write `docs/superpowers/specs/2026-06-07-risk-control-contracts.md` with canonical enums, online decision API, rule DSL, strategy snapshot, decision table, database, MQ event, metrics, audit, and Canvas node contracts.

- [x] **Step 1d: Create traceability matrix**

Write `docs/superpowers/specs/2026-06-07-risk-control-traceability-matrix.md` mapping source learnings, success criteria, contract gates, implementation phases, and completion criteria to current evidence.

- [x] **Step 2: Define production-grade scope**

Define online decision, rule studio, feature platform, simulation, governance, model integration, list management, Canvas node integration, security, and SLOs.

- [x] **Step 3: Select runtime architecture**

Select structured DSL + Java evaluator for hot-path execution, with AviatorScript only as a governed expression sub-layer and DMN/OpenL style decision tables as importable internal models.

### Task 0.2: Write implementation plan

**Files:**
- Create: `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md`

- [x] **Step 1: Map file structure**

Map backend, frontend, persistence, tests, and runbook files.

- [x] **Step 2: Break work into production phases**

Define phase order: foundation, runtime, governance APIs, Canvas integration, Studio UI, Risk Lab, feature platform, model/graph intelligence, observability.

## Phase 1: Metadata Foundation

### Task 1.0: Prepare isolated implementation workspace

**Files:**
- Inspect only: current git branch and worktree status.
- No production files are modified in this task.

- [ ] **Step 1: Verify workspace isolation**

Run:

```bash
pwd
git rev-parse --show-toplevel
git rev-parse --git-dir
git rev-parse --git-common-dir
git branch --show-current
git status --short
```

Expected:

- If `git rev-parse --git-dir` and `git rev-parse --git-common-dir` differ and the repository is not a submodule, continue in the isolated worktree.
- If the branch is `main` and the directory is not an isolated worktree, stop before code changes and get explicit approval to create/use an isolated worktree or to work in place.

- [ ] **Step 2: Verify migration version availability**

Run:

```bash
find backend/canvas-engine/src/main/resources/db/migration -maxdepth 1 -type f -name 'V*__*.sql' -print \
  | sed -E 's#.*/V([0-9]+)__.*#\1#' \
  | sort -n \
  | tail -10
```

Expected: no migration newer than `V350` exists before creating `V351__risk_control_rule_engine_foundation.sql`. If a newer migration exists, choose the next available version and update this plan before implementation.

- [ ] **Step 3: Verify unrelated changes are not touched**

Run:

```bash
git status --short -- backend/canvas-engine/src/main/resources/db/migration docs/superpowers docs/runbooks
```

Expected: record unrelated modified and untracked files. Do not delete, rename, reorder, or rewrite any migration or document that is not part of this risk-control task.

### Task 1.1: Add additive Flyway schema

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V351__risk_control_rule_engine_foundation.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskControlSchemaTest.java`

- [ ] **Step 1: Write schema test**

Create `RiskControlSchemaTest` that reads the migration resource and asserts it contains the foundation schema contract:

```java
package org.chovy.canvas.domain.risk;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RiskControlSchemaTest {
    @Test
    void migrationDefinesRiskControlFoundationTables() throws Exception {
        InputStream resource = getClass()
                .getResourceAsStream("/db/migration/V351__risk_control_rule_engine_foundation.sql");
        assertThat(resource).isNotNull();

        String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("risk_scene");
        assertThat(sql).contains("risk_strategy");
        assertThat(sql).contains("risk_strategy_version");
        assertThat(sql).contains("risk_list");
        assertThat(sql).contains("risk_list_entry");
        assertThat(sql).contains("risk_decision_run");
        assertThat(sql).contains("risk_rule_hit");
        assertThat(sql).contains("tenant_id");
        assertThat(sql).contains("uk_risk_scene_tenant_key");
        assertThat(sql).contains("uk_risk_strategy_tenant_key");
        assertThat(sql).contains("uk_risk_strategy_version");
        assertThat(sql).contains("uk_risk_list_entry_subject");
        assertThat(sql).contains("idx_risk_decision_scene_time");
        assertThat(sql).contains("idx_risk_decision_subject_time");
    }
}
```

- [ ] **Step 2: Run schema test and verify RED**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskControlSchemaTest
```

Expected: fails because the migration file does not exist.

- [ ] **Step 3: Create migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V351__risk_control_rule_engine_foundation.sql` with:

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

This migration is additive only. Do not alter, delete, or renumber existing migrations.

- [ ] **Step 4: Run schema test and Flyway policy test**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskControlSchemaTest
```

Expected: pass.

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest='*Migration*Test'
```

Expected: risk migration has unique version and is additive. Existing unrelated migration failures must be recorded separately and not hidden.

### Task 1.2: Add persistence objects and mappers

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/risk/*.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/risk/*.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskPersistenceMappingTest.java`

- [ ] **Step 1: Write mapping test**

Use MyBatis-Plus metadata inspection to verify each DO exposes `tenantId`, key fields, timestamps, and JSON payload fields.

- [ ] **Step 2: Add DO classes**

Each DO uses existing repository conventions: `@TableName`, `@TableId`, camelCase properties, `LocalDateTime`, `Long tenantId`, `String createdBy`, `String updatedBy`.

- [ ] **Step 3: Add mapper interfaces**

Each mapper extends `BaseMapper<...DO>`.

- [ ] **Step 4: Run mapping tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskPersistenceMappingTest
```

Expected: pass.

## Phase 2: Rule DSL and Evaluator

Phase 2 implements the online hot-path rule core. It intentionally follows the Zeus lesson that expression engines are useful but risky at high rule volume: the first production slice uses a structured JSON AST plus a Java evaluator. AviatorScript is reserved for a later governed expression sub-layer and must not be accepted as arbitrary script text by this parser or evaluator.

### Phase 2 implementation contracts

Use these contracts before writing the tests. If the existing project naming convention requires a different suffix, update the tests and this plan together before implementation.

```java
package org.chovy.canvas.domain.risk.dsl;

public sealed interface RiskRuleNode permits RiskRuleGroupNode, RiskRuleConditionNode {
}

public record RiskRuleGroupNode(
        RiskRuleLogic logic,
        java.util.List<RiskRuleConditionNode> conditions,
        java.util.List<RiskRuleGroupNode> groups
) implements RiskRuleNode {
}

public record RiskRuleConditionNode(
        RiskRuleOperand left,
        RiskRuleOperator op,
        RiskRuleOperand right
) implements RiskRuleNode {
}

public enum RiskRuleLogic {
    AND,
    OR
}

public sealed interface RiskRuleOperand
        permits RiskRuleOperand.FeatureOperand,
                RiskRuleOperand.LiteralOperand,
                RiskRuleOperand.ListOperand,
                RiskRuleOperand.ContextOperand,
                RiskRuleOperand.EventOperand,
                RiskRuleOperand.SubjectOperand {

    RiskOperandType type();

    static FeatureOperand feature(String key) {
        return new FeatureOperand(key);
    }

    static LiteralOperand literal(Object value) {
        return new LiteralOperand(value);
    }

    static ListOperand list(String key) {
        return new ListOperand(key);
    }

    static ContextOperand context(String path) {
        return new ContextOperand(path);
    }

    static EventOperand event(String path) {
        return new EventOperand(path);
    }

    static SubjectOperand subject(String path) {
        return new SubjectOperand(path);
    }

    record FeatureOperand(String key) implements RiskRuleOperand {
        @Override
        public RiskOperandType type() {
            return RiskOperandType.FEATURE;
        }
    }

    record LiteralOperand(Object value) implements RiskRuleOperand {
        @Override
        public RiskOperandType type() {
            return RiskOperandType.LITERAL;
        }
    }

    record ListOperand(String key) implements RiskRuleOperand {
        @Override
        public RiskOperandType type() {
            return RiskOperandType.LIST;
        }
    }

    record ContextOperand(String path) implements RiskRuleOperand {
        @Override
        public RiskOperandType type() {
            return RiskOperandType.CONTEXT;
        }
    }

    record EventOperand(String path) implements RiskRuleOperand {
        @Override
        public RiskOperandType type() {
            return RiskOperandType.EVENT;
        }
    }

    record SubjectOperand(String path) implements RiskRuleOperand {
        @Override
        public RiskOperandType type() {
            return RiskOperandType.SUBJECT;
        }
    }
}

public enum RiskOperandType {
    FEATURE,
    LITERAL,
    LIST,
    CONTEXT,
    EVENT,
    SUBJECT
}

public enum RiskRuleOperator {
    EQ("=="),
    NE("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    LIKE("LIKE"),
    STARTS_WITH("STARTS_WITH"),
    ENDS_WITH("ENDS_WITH"),
    CONTAINS("CONTAINS"),
    IN("IN"),
    NOT_IN("NOT_IN"),
    INTERSECTS("INTERSECTS"),
    EXISTS("EXISTS"),
    IS_EMPTY("IS_EMPTY"),
    IS_NULL("IS_NULL"),
    BEFORE("BEFORE"),
    AFTER("AFTER"),
    BETWEEN_TIME("BETWEEN_TIME");

    private final String wireValue;

    RiskRuleOperator(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
```

Validation contracts:

```java
package org.chovy.canvas.domain.risk.dsl;

public enum RiskValidationErrorCode {
    INVALID_JSON,
    UNKNOWN_OPERATOR,
    UNKNOWN_OPERAND_TYPE,
    UNKNOWN_FEATURE,
    FEATURE_OFFLINE_ONLY,
    UNKNOWN_LIST,
    LIST_SUBJECT_TYPE_MISMATCH,
    MAX_DEPTH_EXCEEDED,
    MAX_CONDITIONS_EXCEEDED,
    TYPE_MISMATCH,
    UNSAFE_EXPRESSION
}

public final class RiskRuleParseException extends RuntimeException {
    private final RiskValidationErrorCode code;
    private final String path;

    public RiskRuleParseException(RiskValidationErrorCode code, String path, String message) {
        super(code + " at " + path + ": " + message);
        this.code = code;
        this.path = path;
    }

    public RiskValidationErrorCode code() {
        return code;
    }

    public String path() {
        return path;
    }
}

public record RiskValidationError(
        String path,
        RiskValidationErrorCode code,
        String message
) {
}

public record RiskRuleValidationResult(
        boolean valid,
        java.util.List<RiskValidationError> errors
) {
}

public enum RiskValueType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATETIME,
    STRING_SET,
    NUMBER_SET
}

public enum RiskFeatureAvailability {
    ONLINE,
    OFFLINE_ONLY
}

public enum RiskSubjectType {
    USER_ID,
    DEVICE_ID,
    IP,
    EMAIL,
    PHONE,
    CARD,
    GENERIC
}

public enum RiskRuntimeMode {
    SIMULATION,
    MARK,
    SHADOW,
    DUAL_RUN,
    CANARY,
    ENFORCE,
    PAUSED
}

public record RiskFactorDefinition(
        String key,
        RiskValueType valueType,
        RiskFeatureAvailability availability,
        RiskSubjectType subjectType
) {
}

public record RiskListDefinition(
        String key,
        RiskSubjectType subjectType,
        RiskValueType valueType
) {
}

public interface RiskFactorCatalog {
    java.util.Optional<RiskFactorDefinition> findByKey(String key);
}

public interface RiskListCatalog {
    java.util.Optional<RiskListDefinition> findByKey(String key);
}
```

Runtime contracts:

```java
package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;

public record RiskResolvedValue(
        boolean present,
        Object value
) {
    public static RiskResolvedValue present(Object value) {
        return new RiskResolvedValue(true, value);
    }

    public static RiskResolvedValue missing() {
        return new RiskResolvedValue(false, null);
    }
}

public interface RiskFeatureResolver {
    RiskResolvedValue resolve(RiskRuleOperand operand);
}

public record RiskRuleEvidence(
        String path,
        String operator,
        Object leftValue,
        Object rightValue,
        boolean matched
) {
}

public record RiskRuleEvaluationResult(
        boolean matched,
        java.util.List<RiskRuleEvidence> evidence,
        java.util.List<String> missingFeatures
) {
}
```

### Task 2.1: Implement rule DSL parser

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/*.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/dsl/RiskRuleParserTest.java`

- [ ] **Step 1: Write parser tests**

Create this test before production code:

```java
package org.chovy.canvas.domain.risk.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RiskRuleParserTest {

    private final RiskRuleParser parser = new RiskRuleParser();

    @Test
    void parsesNestedAndOrRuleGroup() {
        RiskRuleGroupNode node = parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "risk.score" },
                      "op": ">=",
                      "right": { "type": "LITERAL", "value": 85 }
                    },
                    {
                      "left": { "type": "EVENT", "path": "amount" },
                      "op": "<",
                      "right": { "type": "LITERAL", "value": 500 }
                    }
                  ],
                  "groups": [
                    {
                      "logic": "OR",
                      "conditions": [
                        {
                          "left": { "type": "FEATURE", "key": "device.change_card_1d" },
                          "op": ">",
                          "right": { "type": "LITERAL", "value": 2 }
                        }
                      ],
                      "groups": []
                    }
                  ]
                }
                """);

        assertThat(node.logic()).isEqualTo(RiskRuleLogic.AND);
        assertThat(node.conditions()).hasSize(2);
        assertThat(node.groups()).hasSize(1);
        assertThat(node.groups().getFirst().logic()).isEqualTo(RiskRuleLogic.OR);
        assertThat(node.conditions().getFirst().left())
                .isEqualTo(RiskRuleOperand.feature("risk.score"));
        assertThat(node.conditions().getFirst().op()).isEqualTo(RiskRuleOperator.GTE);
        assertThat(node.conditions().get(1).left())
                .isEqualTo(RiskRuleOperand.event("amount"));
    }

    @Test
    void parsesAllSupportedOperandTypes() {
        RiskRuleGroupNode node = parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "SUBJECT", "path": "userId" },
                      "op": "IN",
                      "right": { "type": "LIST", "key": "blacklist.user" }
                    },
                    {
                      "left": { "type": "CONTEXT", "path": "caller" },
                      "op": "==",
                      "right": { "type": "LITERAL", "value": "CANVAS_NODE" }
                    }
                  ],
                  "groups": []
                }
                """);

        assertThat(node.conditions().getFirst().left())
                .isEqualTo(RiskRuleOperand.subject("userId"));
        assertThat(node.conditions().getFirst().right())
                .isEqualTo(RiskRuleOperand.list("blacklist.user"));
        assertThat(node.conditions().get(1).left())
                .isEqualTo(RiskRuleOperand.context("caller"));
        assertThat(node.conditions().get(1).right())
                .isEqualTo(RiskRuleOperand.literal("CANVAS_NODE"));
    }

    @Test
    void rejectsInvalidJson() {
        assertThatExceptionOfType(RiskRuleParseException.class)
                .isThrownBy(() -> parser.parse("{not-json"))
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(RiskValidationErrorCode.INVALID_JSON);
                    assertThat(error.path()).isEqualTo("$");
                });
    }

    @Test
    void rejectsUnknownOperator() {
        assertThatExceptionOfType(RiskRuleParseException.class)
                .isThrownBy(() -> parser.parse("""
                        {
                          "logic": "AND",
                          "conditions": [
                            {
                              "left": { "type": "FEATURE", "key": "risk.score" },
                              "op": "EVAL",
                              "right": { "type": "LITERAL", "value": 85 }
                            }
                          ],
                          "groups": []
                        }
                        """))
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(RiskValidationErrorCode.UNKNOWN_OPERATOR);
                    assertThat(error.path()).isEqualTo("$.conditions[0].op");
                });
    }

    @Test
    void rejectsUnsafeScriptOperand() {
        assertThatThrownBy(() -> parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "SCRIPT", "body": "java.lang.Runtime.getRuntime().exec('x')" },
                      "op": "==",
                      "right": { "type": "LITERAL", "value": true }
                    }
                  ],
                  "groups": []
                }
                """))
                .isInstanceOf(RiskRuleParseException.class)
                .hasMessageContaining("UNKNOWN_OPERAND_TYPE");
    }
}
```

- [ ] **Step 2: Implement parser**

Parser maps JSON to the sealed node model above. It never evaluates values, never accepts raw script bodies, never loads classes, and never interprets URLs, file paths, reflection targets, or network endpoints as operands. The parser is responsible for deterministic structural errors only; business validation remains in Task 2.2.

- [ ] **Step 3: Run parser tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskRuleParserTest
```

Expected: pass.

### Task 2.2: Implement DSL validator

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidator.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidatorTest.java`

- [ ] **Step 1: Write validator tests**

Create this test before production code:

```java
package org.chovy.canvas.domain.risk.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RiskRuleValidatorTest {

    private final RiskRuleParser parser = new RiskRuleParser();
    private final RiskFactorCatalog factorCatalog = key -> switch (key) {
        case "risk.score" -> Optional.of(new RiskFactorDefinition(
                key, RiskValueType.DECIMAL, RiskFeatureAvailability.ONLINE, RiskSubjectType.USER_ID));
        case "buyer.fail_count_1d" -> Optional.of(new RiskFactorDefinition(
                key, RiskValueType.INTEGER, RiskFeatureAvailability.ONLINE, RiskSubjectType.USER_ID));
        case "offline.graph_cluster_score" -> Optional.of(new RiskFactorDefinition(
                key, RiskValueType.DECIMAL, RiskFeatureAvailability.OFFLINE_ONLY, RiskSubjectType.USER_ID));
        default -> Optional.empty();
    };
    private final RiskListCatalog listCatalog = key -> switch (key) {
        case "blacklist.user" -> Optional.of(new RiskListDefinition(
                key, RiskSubjectType.USER_ID, RiskValueType.STRING_SET));
        case "blacklist.device" -> Optional.of(new RiskListDefinition(
                key, RiskSubjectType.DEVICE_ID, RiskValueType.STRING_SET));
        default -> Optional.empty();
    };
    private final RiskRuleValidator validator = new RiskRuleValidator(factorCatalog, listCatalog);

    @Test
    void rejectsUnknownFeature() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "unknown.feature" },
                      "op": ">=",
                      "right": { "type": "LITERAL", "value": 1 }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].left.key");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.UNKNOWN_FEATURE);
        });
    }

    @Test
    void rejectsOfflineOnlyFeatureForEnforceMode() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "offline.graph_cluster_score" },
                      "op": ">",
                      "right": { "type": "LITERAL", "value": 70 }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].left.key");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.FEATURE_OFFLINE_ONLY);
        });
    }

    @Test
    void allowsOfflineOnlyFeatureForSimulationMode() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "offline.graph_cluster_score" },
                      "op": ">",
                      "right": { "type": "LITERAL", "value": 70 }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.SIMULATION);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsListSubjectMismatch() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "SUBJECT", "path": "userId" },
                      "op": "IN",
                      "right": { "type": "LIST", "key": "blacklist.device" }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].right.key");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.LIST_SUBJECT_TYPE_MISMATCH);
        });
    }

    @Test
    void rejectsNestingDepthGreaterThanFive() {
        RiskRuleGroupNode root = new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(), List.of());
        for (int depth = 0; depth < 6; depth++) {
            root = new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(), List.of(root));
        }

        RiskRuleValidationResult result = validator.validate(root, RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error ->
                assertThat(error.code()).isEqualTo(RiskValidationErrorCode.MAX_DEPTH_EXCEEDED));
    }

    @Test
    void rejectsMoreThanOneHundredConditions() {
        String conditions = IntStream.range(0, 101)
                .mapToObj(index -> """
                        {
                          "left": { "type": "FEATURE", "key": "buyer.fail_count_1d" },
                          "op": ">=",
                          "right": { "type": "LITERAL", "value": 1 }
                        }
                        """)
                .collect(Collectors.joining(","));

        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [%s],
                  "groups": []
                }
                """.formatted(conditions)), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error ->
                assertThat(error.code()).isEqualTo(RiskValidationErrorCode.MAX_CONDITIONS_EXCEEDED));
    }

    @Test
    void rejectsNumericOperatorWithStringLiteral() {
        RiskRuleValidationResult result = validator.validate(parser.parse("""
                {
                  "logic": "AND",
                  "conditions": [
                    {
                      "left": { "type": "FEATURE", "key": "risk.score" },
                      "op": ">=",
                      "right": { "type": "LITERAL", "value": "HIGH" }
                    }
                  ],
                  "groups": []
                }
                """), RiskRuntimeMode.ENFORCE);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(error -> {
            assertThat(error.path()).isEqualTo("$.conditions[0].right.value");
            assertThat(error.code()).isEqualTo(RiskValidationErrorCode.TYPE_MISMATCH);
        });
    }
}
```

- [ ] **Step 2: Implement validator**

Validator receives `RiskFactorCatalog` and `RiskListCatalog` interfaces and returns deterministic validation errors with JSON field paths. It must validate maximum depth, total condition count, operator and operand type compatibility, online availability for `ENFORCE`, list subject compatibility, and unsafe-expression rejection. It must not call external services or evaluate rule truth.

- [ ] **Step 3: Run validator tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskRuleValidatorTest
```

Expected: pass.

### Task 2.3: Implement evaluator

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluator.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluatorTest.java`

- [ ] **Step 1: Write evaluator tests**

Create this test before production code:

```java
package org.chovy.canvas.domain.risk.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.chovy.canvas.domain.risk.dsl.RiskRuleConditionNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleLogic;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RiskRuleEvaluatorTest {

    private final RiskRuleEvaluator evaluator = new RiskRuleEvaluator();

    @ParameterizedTest
    @MethodSource("matchingOperators")
    void evaluatesSupportedOperators(RiskRuleOperator operator, Object left, Object right) {
        RiskRuleEvaluationResult result = evaluator.evaluate(
                group(condition(RiskRuleOperand.literal(left), operator, RiskRuleOperand.literal(right))),
                operand -> RiskResolvedValue.present(((RiskRuleOperand.LiteralOperand) operand).value()));

        assertThat(result.matched()).isTrue();
        assertThat(result.missingFeatures()).isEmpty();
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().matched()).isTrue();
    }

    static Stream<Arguments> matchingOperators() {
        return Stream.of(
                Arguments.of(RiskRuleOperator.EQ, "A", "A"),
                Arguments.of(RiskRuleOperator.NE, "A", "B"),
                Arguments.of(RiskRuleOperator.GT, 91, 90),
                Arguments.of(RiskRuleOperator.GTE, 90, 90),
                Arguments.of(RiskRuleOperator.LT, 89, 90),
                Arguments.of(RiskRuleOperator.LTE, 90, 90),
                Arguments.of(RiskRuleOperator.LIKE, "promo-risk-high", "risk"),
                Arguments.of(RiskRuleOperator.STARTS_WITH, "device-123", "device"),
                Arguments.of(RiskRuleOperator.ENDS_WITH, "user@example.com", "example.com"),
                Arguments.of(RiskRuleOperator.CONTAINS, List.of("vip", "fraud"), "fraud"),
                Arguments.of(RiskRuleOperator.IN, "u-1", Set.of("u-1", "u-2")),
                Arguments.of(RiskRuleOperator.NOT_IN, "u-3", Set.of("u-1", "u-2")),
                Arguments.of(RiskRuleOperator.INTERSECTS, Set.of("ip-1", "ip-2"), Set.of("ip-2", "ip-3")),
                Arguments.of(RiskRuleOperator.EXISTS, "present-value", null),
                Arguments.of(RiskRuleOperator.IS_EMPTY, List.of(), null),
                Arguments.of(RiskRuleOperator.IS_NULL, null, null)
        );
    }

    @Test
    void evaluatesNestedAndOrGroups() {
        RiskRuleGroupNode root = new RiskRuleGroupNode(
                RiskRuleLogic.AND,
                List.of(condition(RiskRuleOperand.feature("risk.score"), RiskRuleOperator.GTE, RiskRuleOperand.literal(85))),
                List.of(new RiskRuleGroupNode(
                        RiskRuleLogic.OR,
                        List.of(
                                condition(RiskRuleOperand.feature("device.change_card_1d"), RiskRuleOperator.GT, RiskRuleOperand.literal(2)),
                                condition(RiskRuleOperand.feature("buyer.fail_count_1d"), RiskRuleOperator.GTE, RiskRuleOperand.literal(3))
                        ),
                        List.of())));

        Map<String, Object> values = Map.of(
                "risk.score", 90,
                "device.change_card_1d", 1,
                "buyer.fail_count_1d", 3);

        RiskRuleEvaluationResult result = evaluator.evaluate(root, operand -> {
            if (operand instanceof RiskRuleOperand.FeatureOperand featureOperand) {
                return RiskResolvedValue.present(values.get(featureOperand.key()));
            }
            if (operand instanceof RiskRuleOperand.LiteralOperand literalOperand) {
                return RiskResolvedValue.present(literalOperand.value());
            }
            return RiskResolvedValue.missing();
        });

        assertThat(result.matched()).isTrue();
        assertThat(result.evidence()).hasSize(3);
    }

    @Test
    void recordsMissingFeaturesWithoutThrowing() {
        RiskRuleGroupNode root = group(condition(
                RiskRuleOperand.feature("risk.score"),
                RiskRuleOperator.GTE,
                RiskRuleOperand.literal(85)));

        RiskRuleEvaluationResult result = evaluator.evaluate(root, operand -> {
            if (operand instanceof RiskRuleOperand.LiteralOperand literalOperand) {
                return RiskResolvedValue.present(literalOperand.value());
            }
            return RiskResolvedValue.missing();
        });

        assertThat(result.matched()).isFalse();
        assertThat(result.missingFeatures()).containsExactly("risk.score");
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().matched()).isFalse();
    }

    @Test
    void resolvesListOperandThroughResolver() {
        RiskRuleGroupNode root = group(condition(
                RiskRuleOperand.subject("userId"),
                RiskRuleOperator.IN,
                RiskRuleOperand.list("blacklist.user")));

        RiskRuleEvaluationResult result = evaluator.evaluate(root, operand -> {
            if (operand instanceof RiskRuleOperand.SubjectOperand subjectOperand
                    && subjectOperand.path().equals("userId")) {
                return RiskResolvedValue.present("u-1");
            }
            if (operand instanceof RiskRuleOperand.ListOperand listOperand
                    && listOperand.key().equals("blacklist.user")) {
                return RiskResolvedValue.present(Set.of("u-1", "u-2"));
            }
            return RiskResolvedValue.missing();
        });

        assertThat(result.matched()).isTrue();
        assertThat(result.evidence().getFirst().leftValue()).isEqualTo("u-1");
    }

    @Test
    void treatsExplicitNullAsResolvedForIsNull() {
        Map<String, Object> values = new HashMap<>();
        values.put("nullableFeature", null);

        RiskRuleEvaluationResult result = evaluator.evaluate(
                group(condition(RiskRuleOperand.feature("nullableFeature"), RiskRuleOperator.IS_NULL, RiskRuleOperand.literal(null))),
                operand -> {
                    if (operand instanceof RiskRuleOperand.FeatureOperand featureOperand
                            && values.containsKey(featureOperand.key())) {
                        return RiskResolvedValue.present(values.get(featureOperand.key()));
                    }
                    if (operand instanceof RiskRuleOperand.LiteralOperand literalOperand) {
                        return RiskResolvedValue.present(literalOperand.value());
                    }
                    return RiskResolvedValue.missing();
                });

        assertThat(result.matched()).isTrue();
        assertThat(result.missingFeatures()).isEmpty();
    }

    private static RiskRuleGroupNode group(RiskRuleConditionNode condition) {
        return new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(condition), List.of());
    }

    private static RiskRuleConditionNode condition(
            RiskRuleOperand left,
            RiskRuleOperator operator,
            RiskRuleOperand right
    ) {
        return new RiskRuleConditionNode(left, operator, right);
    }
}
```

- [ ] **Step 2: Implement evaluator**

Evaluator receives a parsed node and `RiskFeatureResolver`. It returns `RiskRuleEvaluationResult` with `matched`, `evidence`, and `missingFeatures`. Runtime semantics:

- `AND` short-circuit is allowed only after evidence for the failing condition is recorded.
- `OR` short-circuit is allowed only after evidence for the matching condition is recorded.
- Missing feature operands never throw; they make the current condition false and append the feature key to `missingFeatures`.
- Explicit null from `RiskResolvedValue.present(null)` is different from `RiskResolvedValue.missing()`.
- Numeric comparisons use `BigDecimal` normalization.
- String operators are case-sensitive in the first slice.
- `LIKE` means substring match, not SQL wildcard expansion.
- `IN` and `NOT_IN` require the right operand to resolve to a collection.
- `INTERSECTS` requires both operands to resolve to collections.
- `EXISTS` is true when the left operand is present and non-null.
- `IS_EMPTY` is true for empty string, empty collection, or empty map.
- `IS_NULL` is true only for a present null value.

- [ ] **Step 3: Run evaluator tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskRuleEvaluatorTest
```

Expected: pass.

### Task 2.4: Lock Phase 2 Zeus and Aviator guardrails

**Files:**
- Update: `docs/superpowers/specs/2026-06-07-risk-control-contracts.md`
- Update: `docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md`
- Test evidence: documentation scan only until code implementation starts.

- [ ] **Step 1: Record hot-path prohibition**

Confirm contracts state that online rule JSON does not accept arbitrary script text, Java class names, reflection targets, file paths, URLs, or network endpoints.

- [ ] **Step 2: Record future safe-expression entry criteria**

Any later Aviator-backed `SCRIPTED_SAFE_FUNCTION` must be implemented through a separate compiler stage with:

- function whitelist.
- disabled reflection, file, network, class loading, and process access.
- expression length limit.
- compile timeout and evaluation timeout.
- compile cache max entries per tenant and per strategy version.
- TTL after access.
- explicit invalidation on version deactivation.
- metrics for compile count, hit rate, eviction count, failure count, and evaluation latency.
- CodeCache and class-loading alerts in the runbook.

- [ ] **Step 3: Run Phase 2 documentation scan**

Run:

```bash
rg -n "SCRIPT|Aviator|UNSAFE_EXPRESSION|RiskRuleEvaluator|RiskRuleValidator|RiskRuleParser" \
  docs/superpowers/plans/2026-06-06-risk-control-rule-engine.md \
  docs/superpowers/specs/2026-06-07-risk-control-contracts.md \
  docs/superpowers/specs/2026-06-07-risk-control-reference-matrix.md \
  docs/runbooks/risk-control-rule-engine.md
```

Expected: references show structured DSL as the online hot path and Aviator only as a governed expression sub-layer.

## Phase 3: Runtime Decision Engine

Phase 3 turns validated DSL into immutable runtime snapshots and governed online decisions. The runtime must be deterministic, replayable, tenant-scoped, idempotent by request ID, and safe under partial dependency failure.

### Phase 3 runtime contracts

Use these contracts as the implementation baseline. They intentionally keep persistence and external features behind ports so the online core can be unit-tested without MySQL, Redis, RocketMQ, or HTTP.

```java
package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.model.RiskBand;
import org.chovy.canvas.domain.risk.model.RiskDecisionAction;

public record RiskStrategyIdentity(
        long tenantId,
        String sceneKey,
        String strategyKey,
        int version
) {
    public String cacheKey() {
        return tenantId + ":" + sceneKey + ":" + strategyKey + ":" + version;
    }
}

public enum RiskRuleGroupType {
    LIST_GATE,
    SCORING,
    DECISION_TABLE,
    MODEL_GATE,
    HARD_RULE
}

public enum RiskRuleGroupMatchPolicy {
    ANY_MATCHED,
    ALL_MATCHED,
    FIRST_MATCH,
    WEIGHTED_SCORE
}

public enum RiskFailPolicy {
    FAIL_OPEN,
    FAIL_REVIEW,
    FAIL_CLOSED
}

public record RiskScoreBandRule(
        RiskBand band,
        int minInclusive,
        Integer maxExclusive,
        Integer maxInclusive
) {
}

public record RiskActionPolicy(
        java.util.List<RiskDecisionAction> priority,
        java.util.List<RiskScoreBandRule> scoreBands
) {
}

public record RiskRuntimeRule(
        String groupKey,
        String ruleKey,
        int priority,
        RiskRuntimeMode mode,
        RiskRuleGroupNode dsl,
        RiskDecisionAction action,
        int scoreDelta,
        String reasonCode,
        java.util.List<String> labels
) {
}

public record RiskRuntimeRuleGroup(
        String groupKey,
        RiskRuleGroupType groupType,
        int executionOrder,
        RiskRuleGroupMatchPolicy matchPolicy,
        boolean enabled,
        java.util.List<RiskRuntimeRule> rules
) {
}

public record CompiledRiskStrategy(
        RiskStrategyIdentity identity,
        RiskRuntimeMode mode,
        int trafficPercent,
        RiskFailPolicy failPolicy,
        int latencyBudgetMs,
        java.util.List<RiskRuntimeRuleGroup> ruleGroups,
        RiskActionPolicy actionPolicy,
        java.util.Set<String> requiredFeatures,
        String compiledHash
) {
}

public record RiskStrategyCompileLimits(
        int maxGroups,
        int maxRules,
        int maxRequiredFeatures,
        int maxSafeExpressions,
        int maxCompiledExpressionBytes
) {
}

public enum RiskStrategyCompileErrorCode {
    UNKNOWN_ACTION,
    UNKNOWN_GROUP_POLICY,
    RULE_LIMIT_EXCEEDED,
    GROUP_LIMIT_EXCEEDED,
    FEATURE_LIMIT_EXCEEDED,
    SAFE_EXPRESSION_LIMIT_EXCEEDED,
    COMPILED_EXPRESSION_BUDGET_EXCEEDED,
    INVALID_DSL
}

public final class RiskStrategyCompileException extends RuntimeException {
    private final RiskStrategyCompileErrorCode code;
    private final String path;

    public RiskStrategyCompileException(RiskStrategyCompileErrorCode code, String path, String message) {
        super(code + " at " + path + ": " + message);
        this.code = code;
        this.path = path;
    }

    public RiskStrategyCompileErrorCode code() {
        return code;
    }

    public String path() {
        return path;
    }
}
```

List and merge contracts:

```java
package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.model.RiskDecisionAction;

public enum RiskListType {
    BLACK,
    WHITE,
    GRAY,
    OBSERVE,
    COMPLIANCE_BLACK
}

public record RiskListLookupRequest(
        long tenantId,
        String listKey,
        RiskListType listType,
        String subjectType,
        String rawSubjectValue,
        java.time.Instant now
) {
}

public record RiskListMatchResult(
        boolean matched,
        String listKey,
        RiskListType listType,
        String subjectHash,
        String maskedSubject,
        RiskDecisionAction action,
        String reasonCode
) {
}

public interface RiskSubjectHasher {
    String hash(String rawSubjectValue);
    String mask(String rawSubjectValue);
}

public interface RiskListEntryReader {
    java.util.Optional<RiskListEntryView> findActive(
            long tenantId,
            String listKey,
            String subjectHash,
            java.time.Instant now);
}

public record RiskListEntryView(
        String listKey,
        String subjectHash,
        String subjectMasked,
        java.time.Instant effectiveFrom,
        java.time.Instant expiresAt,
        String reasonCode
) {
}

public record RiskDecisionSignal(
        String source,
        String groupKey,
        String ruleKey,
        RiskDecisionAction action,
        int scoreDelta,
        String reasonCode,
        java.util.List<String> labels,
        RiskRuntimeMode mode
) {
}

public record RiskMergedDecision(
        RiskDecisionAction decision,
        int score,
        org.chovy.canvas.domain.risk.model.RiskBand riskBand,
        java.util.List<RiskDecisionSignal> effectiveSignals,
        java.util.List<RiskDecisionSignal> shadowSignals,
        java.util.List<String> missingFeatures,
        java.util.List<String> failureReasons
) {
}
```

Online decision service contracts:

```java
package org.chovy.canvas.domain.risk.runtime;

public record RiskDecisionRequestContext(
        long tenantId,
        String requestId,
        String sceneKey,
        java.time.Instant eventTime,
        java.util.Map<String, Object> subject,
        java.util.Map<String, Object> event,
        java.util.Map<String, Object> context,
        java.util.Map<String, Object> suppliedFeatures,
        int deadlineMs
) {
}

public interface RiskActiveStrategyReader {
    CompiledRiskStrategy activeStrategy(long tenantId, String sceneKey);
}

public interface RiskDecisionLedger {
    java.util.Optional<RiskPersistedDecision> findByRequestId(long tenantId, String requestId);
    RiskPersistedDecision insertDecisionRun(RiskDecisionRunDraft draft);
    void insertRuleHits(long decisionRunId, java.util.List<RiskDecisionSignal> signals);
}

public record RiskPersistedDecision(
        long decisionRunId,
        String requestId,
        String requestHash,
        RiskMergedDecision decision
) {
}

public record RiskDecisionRunDraft(
        long tenantId,
        String requestId,
        String requestHash,
        RiskStrategyIdentity strategyIdentity,
        RiskMergedDecision decision,
        String inputSnapshotJson,
        String outputJson,
        int latencyMs
) {
}
```

### Task 3.1: Implement strategy compiler and runtime cache

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompiler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyRuntimeCache.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompilerTest.java`

- [ ] **Step 1: Write compiler tests**

Create `RiskStrategyCompilerTest` with these tests before production code:

```java
@Test
void compilesStrategyIntoStableOrderedSnapshot()

@Test
void recordsOnlyDeclaredRequiredFeatures()

@Test
void compiledHashIsStableForCanonicalEquivalentSnapshots()

@Test
void rejectsUnknownActionWithJsonPath()

@Test
void rejectsGroupLimitExceeded()

@Test
void rejectsRuleLimitExceeded()

@Test
void rejectsFeatureLimitExceeded()

@Test
void rejectsSafeExpressionUntilGovernedCompilerExists()

@Test
void cacheReturnsSameCompiledSnapshotUntilInvalidated()

@Test
void cacheInvalidationRemovesOneStrategyVersionOnly()
```

The tests must assert:

- rule groups are sorted by `executionOrder` ascending, then `groupKey`.
- rules are sorted by `priority` descending, then `ruleKey`.
- disabled groups are preserved in the compiled snapshot but skipped by runtime execution.
- unknown action fails with `RiskStrategyCompileErrorCode.UNKNOWN_ACTION`.
- unknown group policy fails with `RiskStrategyCompileErrorCode.UNKNOWN_GROUP_POLICY`.
- required features include only `FEATURE` operands from enabled rules.
- `CONTEXT`, `EVENT`, `SUBJECT`, `LIST`, and `LITERAL` operands are not counted as feature-store dependencies.
- canonical JSON hashing ignores key order and excludes `compiledHash`.
- safe expression rules are rejected until the governed expression compiler from Task 2.4 and contract section 4.4 exists.
- cache key is `tenantId:sceneKey:strategyKey:version`.
- invalidating version 12 does not evict version 11.

- [ ] **Step 2: Implement compiler**

Compiler accepts `RiskStrategySnapshot` and outputs immutable `CompiledRiskStrategy`. It parses each rule DSL exactly once, validates action, group policy, runtime mode, and compile limits, extracts required features, computes canonical `sha256:` hash, and never mutates the source snapshot.

- [ ] **Step 3: Implement cache**

Cache key is `tenantId + sceneKey + strategyKey + version`. It uses Caffeine with explicit invalidation on activation.

Expression-backed rules use a separate bounded compiled-expression cache with:

- max entries per tenant.
- max entries per strategy version.
- TTL after access.
- explicit invalidation on version deactivation.
- metrics for compile count, cache hit count, eviction count, timeout count, and compile failure.

- [ ] **Step 4: Run compiler tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskStrategyCompilerTest
```

Expected: pass.

### Task 3.2: Implement list matcher

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListMatcher.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskListMatcherTest.java`

- [ ] **Step 1: Write matcher tests**

Create `RiskListMatcherTest` with these tests before production code:

```java
@Test
void blackListMatchReturnsBlockSignal()

@Test
void whiteListMatchReturnsAllowSignal()

@Test
void complianceBlackListOverridesWhiteList()

@Test
void grayListReturnsReviewSignal()

@Test
void observeListReturnsShadowOnlySignal()

@Test
void expiredEntryIsIgnored()

@Test
void notYetEffectiveEntryIsIgnored()

@Test
void rawSubjectIsHashedBeforeLookup()

@Test
void resultMasksSubjectAndNeverExposesRawValue()
```

The tests must use a fake `RiskSubjectHasher` that records raw input and returns deterministic hashes. Repository assertions must verify lookup receives only `subjectHash`, never the raw subject value.

- [ ] **Step 2: Implement matcher**

Matcher reads active entries by `tenantId + listKey + subjectHash`. It never logs raw PII, never persists raw PII, and returns only `subjectHash` and `maskedSubject` as evidence. Decision mapping:

- `COMPLIANCE_BLACK` -> `BLOCK`, highest priority.
- `BLACK` -> `BLOCK`.
- `WHITE` -> `ALLOW` and suppresses ordinary `BLACK`, `GRAY`, scoring, and hard-rule signals unless the competing signal is compliance.
- `GRAY` -> `REVIEW`.
- `OBSERVE` -> `SHADOW_ONLY`.

- [ ] **Step 3: Run matcher tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskListMatcherTest
```

Expected: pass.

### Task 3.3: Implement decision merger

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMerger.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMergerTest.java`

- [ ] **Step 1: Write merger tests**

Create `RiskDecisionMergerTest` with these tests before production code:

```java
@Test
void actionPriorityUsesConfiguredOrder()

@Test
void scoreIsClampedToZeroAndOneHundred()

@Test
void scoreMapsToConfiguredRiskBand()

@Test
void missingFeatureUsesFailOpen()

@Test
void missingFeatureUsesFailReview()

@Test
void missingFeatureUsesFailClosed()

@Test
void shadowOnlySignalsDoNotAffectFinalDecision()

@Test
void whiteListSuppressesOrdinaryRejectSignals()

@Test
void complianceBlockOverridesWhiteList()

@Test
void reasonsAndLabelsAreStableBySourceGroupRuleOrder()
```

The tests must assert the default action priority:

```text
BLOCK > VERIFY > REVIEW > LIMIT > DELAY > ALLOW
```

The tests must also assert default score bands:

```text
LOW: 0 <= score < 50
MEDIUM: 50 <= score < 85
HIGH: 85 <= score <= 100
```

- [ ] **Step 2: Implement merger**

Merger combines rule outputs, list outputs, model outputs, and scoring outputs into one `RiskMergedDecision`. It keeps effective and shadow signals separate, clamps score to `[0, 100]`, applies fail policy only when required features or mandatory dependencies are missing, and preserves stable reason ordering for replayability.

- [ ] **Step 3: Run merger tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskDecisionMergerTest
```

Expected: pass.

### Task 3.4: Implement online decision service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskFeatureResolver.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `RiskDecisionServiceTest` with these tests before production code:

```java
@Test
void evaluatesActiveStrategyForScene()

@Test
void repeatedRequestIdWithSameCanonicalPayloadReturnsPersistedDecision()

@Test
void repeatedRequestIdWithDifferentPayloadThrowsReplayMismatch()

@Test
void featureResolverReceivesRequiredFeaturesOnly()

@Test
void persistsDecisionRunBeforeReturning()

@Test
void persistsRuleHitsForEffectiveAndShadowSignals()

@Test
void appliesFailOpenWhenRuntimeDependencyFails()

@Test
void appliesFailReviewWhenRuntimeDependencyFails()

@Test
void appliesFailClosedWhenRuntimeDependencyFails()

@Test
void deadlineExceededUsesSceneFailPolicy()

@Test
void inputSnapshotMasksRawPiiBeforePersistence()
```

The tests must use in-memory fakes for `RiskActiveStrategyReader`, `RiskDecisionLedger`, `RiskFeatureResolver`, and clock. They must assert that `tenantId + requestId` is the idempotency key and that canonical request hash includes scene, subject, event, context, supplied features, and event time.

- [ ] **Step 2: Implement feature resolver**

First implementation resolves:

- event field.
- subject field.
- context field.
- registered static feature from request payload.
- list lookup through `RiskListMatcher`.

Redis/Flink feature reads are added in Phase 7.

- [ ] **Step 3: Implement decision service**

Service flow:

1. Resolve active strategy by `tenantId + sceneKey`.
2. Canonicalize request and compute request hash.
3. Return persisted decision for same request hash, or reject mismatched replay.
4. Resolve only `CompiledRiskStrategy.requiredFeatures`.
5. Evaluate enabled rule groups in compiled order.
6. Match list gates through `RiskListMatcher`.
7. Merge signals through `RiskDecisionMerger`.
8. Persist masked input snapshot, output JSON, and rule hits.
9. Return decision response with latency, score, band, reasons, matched rules, missing features, and trace flag.

Dependency failures that happen after request validation must prefer governed decisions over HTTP errors. The service returns `ALLOW`, `REVIEW`, or `BLOCK` according to `RiskFailPolicy` and records failure reasons.

- [ ] **Step 4: Run service tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskDecisionServiceTest
```

Expected: pass.

## Phase 4: Backend APIs and Governance

Phase 4 exposes the runtime through governed APIs. These APIs are not simple CRUD: they enforce tenant context, RBAC, immutable versions, approval gates, audit ledgers, and rollout safety controls.

### Phase 4 governance contracts

```java
package org.chovy.canvas.domain.risk.governance;

public enum RiskPermission {
    RISK_SCENE_READ,
    RISK_DECISION_EVALUATE,
    RISK_STRATEGY_READ,
    RISK_STRATEGY_WRITE,
    RISK_STRATEGY_VALIDATE,
    RISK_STRATEGY_SUBMIT,
    RISK_STRATEGY_APPROVE,
    RISK_STRATEGY_ACTIVATE,
    RISK_STRATEGY_ROLLBACK,
    RISK_LIST_READ,
    RISK_LIST_WRITE,
    RISK_LIST_IMPORT,
    RISK_AUDIT_EXPORT
}

public enum RiskStrategyLifecycleStatus {
    DRAFT,
    VALIDATED,
    SIMULATED,
    APPROVAL_PENDING,
    ACTIVE,
    PAUSED,
    ROLLED_BACK,
    ARCHIVED,
    REJECTED,
    FAILED
}

public enum RiskAuditEventType {
    RISK_DECISION_EVALUATED,
    RISK_STRATEGY_DRAFT_CREATED,
    RISK_STRATEGY_VALIDATED,
    RISK_STRATEGY_SUBMITTED,
    RISK_STRATEGY_APPROVED,
    RISK_STRATEGY_ACTIVATED,
    RISK_STRATEGY_PAUSED,
    RISK_STRATEGY_ROLLED_BACK,
    RISK_LIST_CREATED,
    RISK_LIST_ENTRY_ADDED,
    RISK_LIST_ENTRY_IMPORTED,
    RISK_LIST_ENTRY_REMOVED,
    RISK_AUDIT_EXPORTED
}

public record RiskAuditCommand(
        long tenantId,
        RiskAuditEventType eventType,
        String actor,
        String resourceType,
        String resourceKey,
        String beforeHash,
        String afterHash,
        java.util.Map<String, Object> metadata
) {
}
```

Governance invariants:

- Tenant ID is always taken from authenticated context; body `tenantId` is ignored and recorded as an audit warning.
- Draft versions are mutable; validated, submitted, active, paused, rolled-back, archived, and rejected versions are immutable.
- High-risk strategies require approval before activation.
- Submitter cannot approve the same strategy version.
- Activation must atomically set the active version and invalidate runtime cache for the affected strategy key.
- Rollback activates a previous immutable version and records both old and new version IDs.
- List entry APIs hash raw subject values before persistence and response bodies never return raw PII.
- Every write operation records `risk_audit_event` with before/after hashes.

### Task 4.1: Add decision API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionResponse.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskDecisionControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `RiskDecisionControllerTest` with these tests before production code:

```java
@Test
void usesAuthenticatedTenantContext()

@Test
void ignoresBodyTenantIdAndRecordsAuditWarning()

@Test
void rejectsBlankSceneKey()

@Test
void rejectsMissingSubjectIdentifier()

@Test
void rejectsEventTimeMoreThanTwentyFourHoursInFuture()

@Test
void rejectsDeadlineAboveSceneBudget()

@Test
void returnsDecisionScoreBandReasonsAndMatchedRules()

@Test
void mapsReplayMismatchToConflict()

@Test
void requiresRiskDecisionEvaluatePermission()
```

Response tests must assert the contract fields from `docs/superpowers/specs/2026-06-07-risk-control-contracts.md`: `requestId`, `decisionRunId`, `sceneKey`, `strategyKey`, `strategyVersion`, `mode`, `decision`, `score`, `riskBand`, `reasons`, `matchedRules`, `labels`, `missingFeatures`, `traceAvailable`, and `latencyMs`.

- [ ] **Step 2: Implement controller**

Endpoint: `POST /canvas/risk/decisions/evaluate`. Controller validates request shape, resolves tenant and actor from security context, delegates to `RiskDecisionService`, and maps governed runtime failures to successful decision responses when a fail policy applies.

- [ ] **Step 3: Run controller tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskDecisionControllerTest
```

Expected: pass.

### Task 4.2: Add strategy APIs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/RiskStrategyService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskStrategyControllerTest.java`

- [ ] **Step 1: Write tests**

Create `RiskStrategyControllerTest` with these tests before production code:

```java
@Test
void createsDraftVersion()

@Test
void validatesDraftAndStoresValidationResult()

@Test
void rejectsSubmitWhenValidationFails()

@Test
void submitsHighRiskStrategyForApproval()

@Test
void preventsSubmitterSelfApproval()

@Test
void activatesApprovedVersionAndInvalidatesRuntimeCache()

@Test
void rejectsActivationWithoutSimulationForHighRiskStrategy()

@Test
void rollsBackToPreviousImmutableVersion()

@Test
void returnsVersionDiffWithRuleAndActionChanges()

@Test
void recordsAuditEventForEveryStateTransition()

@Test
void requiresExpectedPermissionForEachCommand()
```

Lifecycle tests must prove allowed transitions:

```text
DRAFT -> VALIDATED -> SIMULATED -> APPROVAL_PENDING -> ACTIVE
ACTIVE -> PAUSED
ACTIVE -> ROLLED_BACK
APPROVAL_PENDING -> REJECTED
```

They must reject:

```text
DRAFT -> ACTIVE
VALIDATED -> ACTIVE for high-risk strategy
ACTIVE -> DRAFT
ARCHIVED -> ACTIVE
```

- [ ] **Step 2: Implement service**

Service stores draft versions, validates DSL, computes compiled hash, enforces lifecycle transitions, writes audit events, and invalidates `RiskStrategyRuntimeCache` on activation, pause, and rollback.

- [ ] **Step 3: Implement controller**

Endpoints under `/canvas/risk/strategies`:

```text
GET    /canvas/risk/strategies
POST   /canvas/risk/strategies
GET    /canvas/risk/strategies/{strategyKey}/versions
POST   /canvas/risk/strategies/{strategyKey}/versions/{version}/validate
POST   /canvas/risk/strategies/{strategyKey}/versions/{version}/submit
POST   /canvas/risk/strategies/{strategyKey}/versions/{version}/approve
POST   /canvas/risk/strategies/{strategyKey}/versions/{version}/reject
POST   /canvas/risk/strategies/{strategyKey}/versions/{version}/activate
POST   /canvas/risk/strategies/{strategyKey}/versions/{version}/pause
POST   /canvas/risk/strategies/{strategyKey}/rollback
GET    /canvas/risk/strategies/{strategyKey}/versions/{left}/diff/{right}
```

- [ ] **Step 4: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskStrategyControllerTest
```

Expected: pass.

### Task 4.3: Add list APIs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskListController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/RiskListService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskListControllerTest.java`

- [ ] **Step 1: Write tests**

Create `RiskListControllerTest` and `RiskListServiceTest` with these tests before production code:

```java
@Test
void createsTenantScopedList()

@Test
void addsEntryWithHashedSubjectAndMaskedValue()

@Test
void rejectsEntryWhenSubjectTypeDoesNotMatchList()

@Test
void importsEntriesAtomicallyWithRowLevelErrors()

@Test
void preventsRawPiiInResponseBody()

@Test
void ignoresExpiredEntriesInLookup()

@Test
void removesEntryWithAuditEvent()

@Test
void recordsListHitWithoutRawSubject()

@Test
void requiresListImportPermissionForBulkImport()
```

Import response must include total rows, accepted rows, rejected rows, deterministic row errors, and an import audit ID.

- [ ] **Step 2: Implement service**

Service hashes PII before persistence, stores masked value only, validates subject type, handles effective and expiry timestamps, writes audit events, and emits list import metrics.

- [ ] **Step 3: Implement controller**

Endpoints under `/canvas/risk/lists`:

```text
GET    /canvas/risk/lists
POST   /canvas/risk/lists
GET    /canvas/risk/lists/{listKey}/entries
POST   /canvas/risk/lists/{listKey}/entries
POST   /canvas/risk/lists/{listKey}/entries/import
DELETE /canvas/risk/lists/{listKey}/entries/{entryId}
```

- [ ] **Step 4: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskListControllerTest
```

Expected: pass.

## Phase 5: Canvas Integration

Phase 5 turns risk control into a native Canvas capability. The node must behave like other engine nodes while preserving risk-specific audit, idempotency, fail policy, and action routing semantics.

### Phase 5 Canvas node contract

Risk Decision node config:

```json
{
  "sceneKey": "MARKETING_BENEFIT_ISSUE",
  "subjectMapping": {
    "userId": "$.profile.userId",
    "deviceId": "$.event.deviceId",
    "ip": "$.event.ip"
  },
  "eventMapping": {
    "amount": "$.event.amount",
    "currency": "$.event.currency",
    "couponCode": "$.event.couponCode",
    "channel": "$.event.channel"
  },
  "contextMapping": {
    "businessLine": "$.canvas.businessLine",
    "caller": "CANVAS_NODE"
  },
  "actionRoutes": {
    "ALLOW": "node_allow",
    "REVIEW": "node_review",
    "VERIFY": "node_verify",
    "BLOCK": "node_block",
    "LIMIT": "node_limit",
    "DELAY": "node_delay"
  },
  "failPolicy": "FAIL_REVIEW",
  "timeoutMs": 50,
  "includeTrace": false
}
```

Node invariants:

- Request ID is `canvasExecutionId + ":" + nodeId + ":" + attempt`.
- Canvas tenant and actor are passed from execution context, not from node config.
- Handler maps only configured subject and event fields; missing required mappings apply node fail policy.
- `ALLOW`, `REVIEW`, `VERIFY`, `BLOCK`, `LIMIT`, and `DELAY` route to configured edges.
- `SHADOW_ONLY` is recorded in node output but routes as `ALLOW`.
- Risk response is persisted into execution context under `riskDecision`.
- Raw PII from subject mapping is not written to node logs.

### Task 5.1: Add `RISK_DECISION` node type

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RiskDecisionHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/RiskDecisionHandlerTest.java`

- [ ] **Step 1: Write handler tests**

Create `RiskDecisionHandlerTest` with these tests before production code:

```java
@Test
void allowRoutesToAllowEdge()

@Test
void reviewRoutesToReviewEdge()

@Test
void verifyRoutesToVerifyEdge()

@Test
void blockRoutesToBlockEdge()

@Test
void limitRoutesToLimitEdge()

@Test
void delayRoutesToDelayEdge()

@Test
void shadowOnlyRoutesToAllowAndStoresSuggestedDecision()

@Test
void buildsStableRiskRequestIdFromExecutionNodeAndAttempt()

@Test
void mapsConfiguredSubjectEventAndContextFields()

@Test
void missingRequiredMappingAppliesNodeFailPolicy()

@Test
void handlerDoesNotLogRawPii()
```

- [ ] **Step 2: Add node type**

Add `RISK_DECISION` with handler registration, node catalog metadata, allowed outgoing action routes, and config schema key.

- [ ] **Step 3: Implement handler**

Handler maps Canvas context into `RiskDecisionRequestContext`, calls `RiskDecisionService`, stores response in execution context, and returns `NodeResult` with the action route. Handler-level failures are converted through node fail policy unless the Canvas execution itself is already cancelled.

- [ ] **Step 4: Run handler tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskDecisionHandlerTest
```

Expected: pass.

### Task 5.2: Add frontend config support

**Files:**
- Modify: `frontend/src/types/canvas.ts`
- Modify: `frontend/src/types/canvasSchemas.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/types/canvasSchemas.test.ts`

- [ ] **Step 1: Write schema tests**

Create or extend `canvasSchemas.test.ts` with these assertions:

```text
RISK_DECISION config requires sceneKey.
RISK_DECISION config requires at least one subject mapping.
RISK_DECISION config requires eventMapping.
RISK_DECISION config requires actionRoutes.ALLOW.
RISK_DECISION config accepts REVIEW, VERIFY, BLOCK, LIMIT, and DELAY routes.
RISK_DECISION config rejects timeoutMs below 10.
RISK_DECISION config rejects timeoutMs above 500.
RISK_DECISION config accepts FAIL_OPEN, FAIL_REVIEW, and FAIL_CLOSED.
```

- [ ] **Step 2: Add types and schema**

Add Risk Decision node config with typed route map, fail policy enum, mapping expressions, timeout, and trace flag.

- [ ] **Step 3: Add config panel**

Add compact controls for scene key, subject mapping, event mapping, action routes, fail policy, timeout, and trace flag. Use existing config-panel patterns and avoid a separate landing or explanatory page.

- [ ] **Step 4: Run frontend tests**

Run:

```bash
cd frontend
npm run test -- canvasSchemas.test.ts
```

Expected: pass.

## Phase 6: Strategy Studio UI

### Task 6.1: Add risk API client

**Files:**
- Create: `frontend/src/services/riskApi.ts`
- Create: `frontend/src/services/riskApi.test.ts`

- [ ] **Step 1: Write API tests**

Assert client calls:

- `/risk/scenes`
- `/risk/strategies`
- `/risk/decisions/evaluate`
- `/risk/lists`
- `/risk/lab/simulations`

- [ ] **Step 2: Implement client**

Use existing `http` wrapper and `R<T>` response style.

- [ ] **Step 3: Run tests**

Run:

```bash
cd frontend
npm run test -- riskApi.test.ts
```

Expected: pass.

### Task 6.2: Add Strategy Studio page

**Files:**
- Create: `frontend/src/pages/risk/index.tsx`
- Create: `frontend/src/pages/risk/riskWorkbench.ts`
- Create: `frontend/src/pages/risk/riskWorkbench.test.ts`

- [ ] **Step 1: Write state tests**

Cover selecting scene, editing draft rule, validating rule, submitting approval, activating version, rolling back version.

- [ ] **Step 2: Implement workbench state helpers**

Keep rule DSL transformations in pure functions.

- [ ] **Step 3: Implement page**

Page layout:

- scene list.
- strategy version list.
- rule group table.
- rule editor drawer.
- list management tab.
- simulation tab.
- decision trace tab.

- [ ] **Step 4: Run tests**

Run:

```bash
cd frontend
npm run test -- riskWorkbench.test.ts
```

Expected: pass.

## Phase 7: Risk Lab and Simulation

### Task 7.1: Add simulation backend

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskLabController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/lab/RiskSimulationServiceTest.java`

- [ ] **Step 1: Write simulation tests**

Cover:

- historical sample run produces action distribution.
- baseline/candidate diff counts action changes.
- missing sample source fails validation.
- simulation never activates strategy.

- [ ] **Step 2: Implement service**

First slice reads sample events from persisted `risk_decision_run` payload snapshots. Doris replay source is added in Phase 8.

- [ ] **Step 3: Implement controller**

Endpoints under `/canvas/risk/lab`.

- [ ] **Step 4: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskSimulationServiceTest
```

Expected: pass.

### Task 7.2: Add shadow and dual-run modes

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionShadowModeTest.java`

- [ ] **Step 1: Write mode tests**

Assert:

- `SHADOW` records suggested decision but returns baseline decision.
- `DUAL_RUN` records both baseline and candidate result.
- `CANARY` chooses candidate by deterministic hash.
- `MARK` executes candidate logic online, writes rule-hit details, and never changes the returned action.
- trace replay can reconstruct both baseline and candidate execution from persisted snapshots.

- [ ] **Step 2: Implement modes**

Use `subjectKey + strategyKey + version` deterministic hash for canary routing.

Implement mode semantics:

- `MARK`: Zeus-style marked rule mode for online observation without affecting upstream decisions.
- `SHADOW`: strategy-level observation mode for full candidate strategy.
- `DUAL_RUN`: mandatory for production strategy changes that affect active rules, factors, decision tables, or action policy.
- `CANARY`: deterministic traffic split after simulation and dual-run pass.

- [ ] **Step 3: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskDecisionShadowModeTest
```

Expected: pass.

## Phase 8: Feature Platform

### Task 8.1: Add feature dictionary and Redis feature resolver

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureCatalogService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RedisRiskFeatureStore.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/feature/RedisRiskFeatureStoreTest.java`

- [ ] **Step 1: Write feature store tests**

Cover set/get, TTL, missing value, tenant prefix, value type parsing.

- [ ] **Step 2: Implement Redis feature store**

Key pattern: `risk:feature:{tenantId}:{featureKey}:{subjectHash}`.

- [ ] **Step 3: Integrate resolver**

`RiskFeatureResolver` reads request payload first, then Caffeine, then Redis.

- [ ] **Step 4: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RedisRiskFeatureStoreTest
```

Expected: pass.

### Task 8.2: Add Flink realtime feature job

**Files:**
- Create: `backend/canvas-flink-jobs/src/main/java/org/chovy/canvas/flink/risk/RiskRealtimeFeatureJob.java`
- Create: `backend/canvas-flink-jobs/src/main/resources/sql/risk_realtime_features.sql`
- Test: `backend/canvas-flink-jobs/src/test/java/org/chovy/canvas/flink/risk/RiskRealtimeFeatureJobTest.java`

- [ ] **Step 1: Write job config tests**

Assert source topic, sink Redis/Doris settings, and window definitions load from config.

- [ ] **Step 2: Implement first feature windows**

Produce:

- `user.fail_count_1d`
- `user.success_count_1d`
- `device.change_user_1d`
- `ip.change_user_1h`
- `benefit.issue_amount_1d`

- [ ] **Step 3: Run Flink job tests**

Run:

```bash
cd backend
mvn test -pl canvas-flink-jobs -Dtest=RiskRealtimeFeatureJobTest
```

Expected: pass.

## Phase 9: Model and Graph Intelligence

### Task 9.1: Add model gateway

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelRegistryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelGateway.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/modeling/RiskModelGatewayTest.java`

- [ ] **Step 1: Write gateway tests**

Cover timeout, fallback, score parsing, explanation parsing, version selection.

- [ ] **Step 2: Implement registry service**

Store endpoint, input schema, output schema, timeout, fallback policy.

- [ ] **Step 3: Implement gateway**

Use existing HTTP client conventions. Never send raw PII fields unless registry explicitly marks them approved.

- [ ] **Step 4: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskModelGatewayTest
```

Expected: pass.

### Task 9.2: Add graph analysis foundation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/graph/RiskGraphServiceTest.java`

- [ ] **Step 1: Write graph tests**

Cover graph built from shared device, IP, phone, email, address, and card fingerprint.

- [ ] **Step 2: Implement graph summary**

First slice computes association counts from recent decision runs and list entries.

- [ ] **Step 3: Run tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskGraphServiceTest
```

Expected: pass.

## Phase 10: Observability and Runbook

### Task 10.1: Add metrics and alerts

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/CanvasRuntimeMetrics.java`
- Create: `ops/alerts/risk-control-rules.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskMetricsTest.java`

- [ ] **Step 1: Write metrics test**

Assert meters exist:

- `risk_decision_requests_total`
- `risk_decision_latency_ms`
- `risk_decision_failures_total`
- `risk_rule_hits_total`
- `risk_feature_missing_total`
- `risk_strategy_activations_total`

- [ ] **Step 2: Implement metrics**

Record metrics in `RiskDecisionService`, strategy activation, list import, and simulation run.

- [ ] **Step 3: Add alert rules**

Alert on latency SLO breach, decision failure spike, block-rate spike, feature-missing spike, and model gateway failure.

- [ ] **Step 4: Run metrics tests**

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest=RiskMetricsTest
```

Expected: pass.

### Task 10.2: Add production runbook

**Files:**
- Create: `docs/runbooks/risk-control-rule-engine.md`

- [x] **Step 1: Write runbook**

Include:

- rollout process.
- emergency pause.
- rollback.
- high false-positive response.
- high false-negative response.
- peak-hour change freeze.
- mandatory simulation, mark, and dual-run gates before high-risk activation.
- operator checklist for validation-result backfill.
- Redis feature outage.
- model outage.
- expression compile-cache or CodeCache pressure.
- audit export.
- PII handling.

- [x] **Step 2: Link runbook from spec**

Add runbook reference to the design spec once created.

## Phase 11: Verification Gate

### Task 11.1: Run targeted backend tests

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest='Risk*Test'
```

Expected: all risk tests pass.

### Task 11.2: Run targeted frontend tests

Run:

```bash
cd frontend
npm run test -- riskApi.test.ts riskWorkbench.test.ts canvasSchemas.test.ts
```

Expected: all targeted frontend tests pass.

### Task 11.3: Run build checks

Run:

```bash
cd frontend
npm run build
```

Expected: build passes.

Run:

```bash
cd backend
mvn test -pl canvas-engine -Dtest='Risk*Test,Canvas*Risk*Test'
```

Expected: pass.

### Task 11.4: Production readiness audit

Verify:

- every risk table is tenant-scoped.
- no raw PII is persisted in list entries or decision traces.
- every active strategy version has compiled hash.
- high-risk activation requires approval.
- online decision has idempotency by request id.
- metrics exist for latency, failures, rule hits, feature misses.
- runbook documents rollback and emergency pause.

## Completion Criteria

The product is production-ready only when:

1. Online decision API is implemented and tested.
2. Strategy versions can be drafted, validated, approved, activated, paused, and rolled back.
3. Rule DSL, decision table, scoring, and list matching are supported.
4. Canvas can call `RISK_DECISION` and route by action.
5. Decision run, trace, and hit ledgers are persisted.
6. Risk Studio can manage scenes, strategies, rules, lists, simulations, and traces.
7. Simulation, shadow, dual-run, and canary modes work.
8. Realtime feature store has Redis-backed resolver and first Flink feature job.
9. Metrics, alerts, and runbook are present.
10. Targeted backend/frontend tests pass.
