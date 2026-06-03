# Provider Backpressure, Fallback, And Dedupe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add provider rate-limit decisions, one-level fallback routing, and cross-canvas dedupe for connector-backed delivery.

**Architecture:** Evaluate backpressure and dedupe before provider calls, then let fallback choose one replacement channel/provider when the primary path is unavailable. Persist policy and decision records in MySQL and keep Redis counters behind a small service interface for tests.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Redis, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-008b-provider-backpressure-fallback-and-dedupe.md`
- Depends on P1-008 connector contract and registry.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V103__channel_provider_policies.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelProviderLimitDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelFallbackPolicyDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelFallbackDecisionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelDedupeRecordDO.java`
- Create matching mappers under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ProviderBackpressureService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelFallbackService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelDedupeService.java`
- Create tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CouponHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`

### Task 1: Policy Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelProviderPolicySchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V103__channel_provider_policies.sql`

- [ ] **Step 1: Write schema test**

Create `ChannelProviderPolicySchemaTest.java`:

```java
package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelProviderPolicySchemaTest {

    @Test
    void migrationCreatesProviderPolicyTables() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V103__channel_provider_policies.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS channel_provider_limit")
                .contains("per_second_limit")
                .contains("daily_limit")
                .contains("CREATE TABLE IF NOT EXISTS channel_fallback_policy")
                .contains("fallback_channel")
                .contains("fallback_provider")
                .contains("CREATE TABLE IF NOT EXISTS channel_fallback_decision")
                .contains("attempt_chain_json")
                .contains("CREATE TABLE IF NOT EXISTS channel_dedupe_record")
                .contains("dedupe_group")
                .contains("content_hash");
    }
}
```

- [ ] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelProviderPolicySchemaTest
```

Expected: FAIL because migration does not exist.

- [ ] **Step 3: Add policy migration**

Create `V103__channel_provider_policies.sql`:

```sql
CREATE TABLE IF NOT EXISTS channel_provider_limit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  channel VARCHAR(32) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  operation VARCHAR(64) NOT NULL DEFAULT 'SEND',
  per_second_limit INT NOT NULL DEFAULT 100,
  daily_limit BIGINT NULL,
  fail_closed TINYINT NOT NULL DEFAULT 1,
  updated_by VARCHAR(128) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_provider_limit (tenant_id, channel, provider, operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_fallback_policy (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  channel VARCHAR(32) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  fallback_channel VARCHAR(32) NOT NULL,
  fallback_provider VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  reason VARCHAR(128) NOT NULL DEFAULT 'PROVIDER_UNAVAILABLE',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_fallback_policy (tenant_id, channel, provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_fallback_decision (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  execution_id VARCHAR(128) NULL,
  node_id VARCHAR(128) NULL,
  original_channel VARCHAR(32) NOT NULL,
  original_provider VARCHAR(64) NOT NULL,
  final_channel VARCHAR(32) NULL,
  final_provider VARCHAR(64) NULL,
  decision_reason VARCHAR(128) NOT NULL,
  attempt_chain_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_channel_fallback_decision_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_dedupe_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dedupe_group VARCHAR(128) NOT NULL,
  content_hash VARCHAR(128) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_dedupe (tenant_id, dedupe_group, content_hash, channel, user_id),
  INDEX idx_channel_dedupe_expire (tenant_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelProviderPolicySchemaTest
```

Expected: PASS.

### Task 2: Backpressure And Fallback Services

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ProviderBackpressureServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelFallbackServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ProviderBackpressureService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelFallbackService.java`

- [ ] **Step 1: Write backpressure tests**

Create tests for isolated limits:

```java
@Test
void perSecondLimitIsIsolatedByTenantChannelProviderAndOperation() {
    ProviderBackpressureService.CounterStore counters = new ProviderBackpressureService.InMemoryCounterStore();
    ProviderBackpressureService service = new ProviderBackpressureService(counters, key ->
            new ProviderBackpressureService.ProviderLimit(1, 100L, true));

    assertThat(service.decide(0L, "SMS", "ALIYUN", "SEND", false).status()).isEqualTo("ALLOWED");
    assertThat(service.decide(0L, "SMS", "ALIYUN", "SEND", false).status()).isEqualTo("THROTTLED_RETRY");
    assertThat(service.decide(0L, "EMAIL", "ALIYUN", "SEND", false).status()).isEqualTo("ALLOWED");
}

@Test
void redisUnavailableFailsClosedForRealModeAndBypassesSandbox() {
    ProviderBackpressureService.CounterStore broken = key -> { throw new IllegalStateException("redis down"); };
    ProviderBackpressureService service = new ProviderBackpressureService(broken, key ->
            new ProviderBackpressureService.ProviderLimit(1, 100L, true));

    assertThat(service.decide(0L, "SMS", "ALIYUN", "SEND", false).status()).isEqualTo("REGISTRY_UNAVAILABLE");
    assertThat(service.decide(0L, "SMS", "SANDBOX", "SEND", true).status()).isEqualTo("ALLOWED");
}
```

- [ ] **Step 2: Write fallback tests**

Create tests for route selection:

```java
@Test
void fallbackSelectsOneLevelReplacementAndRecordsDecision() {
    ChannelFallbackService.PolicyRepository policies = mock(ChannelFallbackService.PolicyRepository.class);
    when(policies.find(0L, "PUSH", "JPUSH")).thenReturn(new ChannelFallbackService.FallbackPolicy("SMS", "ALIYUN", true, "PRIMARY_THROTTLED"));
    ChannelFallbackService.DecisionRepository decisions = mock(ChannelFallbackService.DecisionRepository.class);
    ChannelFallbackService service = new ChannelFallbackService(policies, decisions);

    ChannelFallbackService.FallbackDecision decision = service.resolve(0L, "exec-1", "node-1", "PUSH", "JPUSH");

    assertThat(decision.finalChannel()).isEqualTo("SMS");
    assertThat(decision.finalProvider()).isEqualTo("ALIYUN");
    assertThat(decision.reason()).isEqualTo("PRIMARY_THROTTLED");
    verify(decisions).insert(decision);
}

@Test
void fallbackCycleIsRejected() {
    ChannelFallbackService.PolicyRepository policies = mock(ChannelFallbackService.PolicyRepository.class);
    when(policies.find(0L, "PUSH", "JPUSH")).thenReturn(new ChannelFallbackService.FallbackPolicy("SMS", "ALIYUN", true, "x"));
    when(policies.find(0L, "SMS", "ALIYUN")).thenReturn(new ChannelFallbackService.FallbackPolicy("PUSH", "JPUSH", true, "x"));

    assertThatThrownBy(() -> new ChannelFallbackService(policies, mock(ChannelFallbackService.DecisionRepository.class))
            .validateNoCycle(0L, "PUSH", "JPUSH"))
            .hasMessageContaining("PUSH:JPUSH -> SMS:ALIYUN -> PUSH:JPUSH");
}
```

- [ ] **Step 3: Implement services**

Implement `ProviderBackpressureService.Decision` statuses: `ALLOWED`, `THROTTLED_RETRY`, `THROTTLED_SKIP`, `PROVIDER_DISABLED`, `REGISTRY_UNAVAILABLE`. Implement `ChannelFallbackService` with one-level `resolve` and cycle validation across saved policies.

- [ ] **Step 4: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProviderBackpressureServiceTest,ChannelFallbackServiceTest
```

Expected: PASS.

### Task 3: Dedupe And Handler Integration

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelDedupeServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelDedupeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CouponHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ChannelConnectorHandlerTest.java`

- [ ] **Step 1: Write dedupe tests**

Create `ChannelDedupeServiceTest.java`:

```java
@Test
void duplicateContentSuppressesProviderCallWithinWindow() {
    ChannelDedupeService.Repository repo = mock(ChannelDedupeService.Repository.class);
    when(repo.reserve(0L, "welcome", "hash-1", "SMS", "u1", Duration.ofHours(24))).thenReturn(true, false);
    ChannelDedupeService service = new ChannelDedupeService(repo);

    assertThat(service.reserve(0L, "welcome", "hash-1", "SMS", "u1", Duration.ofHours(24)).status()).isEqualTo("RESERVED");
    assertThat(service.reserve(0L, "welcome", "hash-1", "SMS", "u1", Duration.ofHours(24)).status()).isEqualTo("DUPLICATE");
}
```

- [ ] **Step 2: Implement dedupe service**

Use a repository method that inserts `channel_dedupe_record` and returns false on duplicate key. Hash payload with SHA-256 over channel, template id, content JSON, and normalized variables.

- [ ] **Step 3: Integrate policy sequence in handlers**

Use this order in `AbstractSendMessageHandler` and coupon connector path:

```text
resolve connector -> dedupe reserve -> provider backpressure decision -> fallback resolve if blocked -> connector send or ReachDeliveryService send
```

When dedupe returns duplicate, return `NodeResult.suppressed(skipNodeId, "CHANNEL_DEDUPE", "duplicate channel content")` and do not call provider.

- [ ] **Step 4: Run integration tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelDedupeServiceTest,ChannelConnectorHandlerTest,CouponHandlerTest,SendMessageHandlerTest
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p1-008b-provider-backpressure-fallback-and-dedupe.md`
- Modify: `docs/product-evolution/plans/p1-008b-provider-backpressure-fallback-and-dedupe-plan.md`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelProviderPolicySchemaTest,ProviderBackpressureServiceTest,ChannelFallbackServiceTest,ChannelDedupeServiceTest,ChannelConnectorHandlerTest
```

Expected: PASS.

- [ ] **Step 2: Run handler regression**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SendMessageHandlerTest,CouponHandlerTest,CommitActionHandlerTest,ApiCallHandlerRateLimitTest
```

Expected: PASS.

- [ ] **Step 3: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V103__channel_provider_policies.sql backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery backend/canvas-engine/src/main/java/org/chovy/canvas/dal backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers docs/product-evolution/specs/p1-008b-provider-backpressure-fallback-and-dedupe.md docs/product-evolution/plans/p1-008b-provider-backpressure-fallback-and-dedupe-plan.md
git commit -m "feat: add channel provider policy controls"
```

Expected: commit contains only provider policy schema, backpressure, fallback, dedupe, handler integration, tests, and related docs.
