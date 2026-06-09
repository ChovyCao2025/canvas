# Channel Connector Contract And Disabled State Implementation Plan

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add explicit channel connector contracts and fail-closed disabled connector behavior for send-like handlers.

**Architecture:** Define a small `ChannelConnector` interface and registry, persist connector mode/capability metadata in MySQL, and route `SendMessageHandler` delivery through registry resolution before calling the current `ReachDeliveryService` path.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, WebClient, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-008-channel-connector-contract-and-disabled-state.md`

## Current Code Facts

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java` reads `channel` from config and delegates to `ReachDeliveryService`.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java` writes `message_send_record` and calls the reach platform.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerTest.java` uses a capturing `ReachDeliveryService`.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V102__channel_connector_contract.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelConnectorDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelConnectorMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnector.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistry.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DisabledChannelConnector.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistryTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ChannelConnectorHandlerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java`

### Task 1: Schema And Connector Contract

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V102__channel_connector_contract.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnector.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelConnectorDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelConnectorMapper.java`

- [x] **Step 1: Write schema test**

Create `ChannelConnectorSchemaTest.java`:

```java
package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelConnectorSchemaTest {

    @Test
    void migrationCreatesConnectorTable() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V102__channel_connector_contract.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS channel_connector")
                .contains("connector_key")
                .contains("channel")
                .contains("provider")
                .contains("mode")
                .contains("capabilities_json")
                .contains("health_status")
                .contains("disabled_reason")
                .contains("UNIQUE KEY uk_channel_connector");
    }
}
```

- [x] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorSchemaTest
```

Expected: FAIL because migration does not exist.

Actual: schema/registry/migration files were already present in the worktree. The migration uses `V120__channel_connector_contract.sql` because `V102` is already occupied by `V102__event_attribute_discovery_internal_event.sql`.

- [x] **Step 3: Add migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V102__channel_connector_contract.sql`:

```sql
CREATE TABLE IF NOT EXISTS channel_connector (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  connector_key VARCHAR(128) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  mode VARCHAR(20) NOT NULL DEFAULT 'DISABLED',
  capabilities_json JSON NULL,
  health_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
  health_message VARCHAR(500) NULL,
  disabled_reason VARCHAR(500) NULL,
  last_checked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_connector (tenant_id, connector_key),
  INDEX idx_channel_connector_lookup (tenant_id, channel, provider, mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 4: Add connector contract**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnector.java`:

```java
package org.chovy.canvas.engine.channel;

import java.util.Map;

public interface ChannelConnector {
    ConnectorMode mode();
    ConnectorHealth health();
    ConnectorCapabilities capabilities();
    ConnectorSendResult send(ConnectorSendRequest request);
    ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload);

    enum ConnectorMode { REAL, SANDBOX, DISABLED }
    record ConnectorHealth(String status, String message) {}
    record ConnectorCapabilities(boolean send, boolean receipt, Map<String, Object> attributes) {}
    record ConnectorSendRequest(Long tenantId, String channel, String provider, String userId, Map<String, Object> payload) {}
    record ConnectorSendResult(boolean accepted, String externalMessageId, String status, String reason) {}
    record ConnectorReceiptResult(String externalMessageId, String status, Map<String, Object> attributes) {}
}
```

- [x] **Step 5: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorSchemaTest
```

Expected: PASS.

Actual: covered by isolated P1-008 suite; `ChannelConnectorSchemaTest` passed against `V120__channel_connector_contract.sql`.

### Task 2: Registry And Disabled/Sandbox Connectors

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistryTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistry.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DisabledChannelConnector.java`

- [x] **Step 1: Write registry tests**

Create `ChannelConnectorRegistryTest.java`:

```java
package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelConnectorRegistryTest {

    @Test
    void missingConnectorResolvesDisabledConnector() {
        ChannelConnectorRegistry.Repository repository = mock(ChannelConnectorRegistry.Repository.class);
        when(repository.find(0L, "SMS", "ALIYUN")).thenReturn(null);

        ChannelConnector connector = new ChannelConnectorRegistry(repository, Map.of()).resolve(0L, "SMS", "ALIYUN");

        assertThat(connector.mode()).isEqualTo(ChannelConnector.ConnectorMode.DISABLED);
        assertThat(connector.health().message()).contains("not configured");
    }

    @Test
    void sandboxConnectorReturnsDeterministicFakeMessageId() {
        ChannelConnectorRegistry.Repository repository = mock(ChannelConnectorRegistry.Repository.class);
        when(repository.find(0L, "SMS", "SANDBOX")).thenReturn(new ChannelConnectorRegistry.ConnectorConfig(
                "sms-sandbox", "SMS", "SANDBOX", ChannelConnector.ConnectorMode.SANDBOX, null));

        ChannelConnector connector = new ChannelConnectorRegistry(repository, Map.of()).resolve(0L, "SMS", "SANDBOX");

        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                0L, "SMS", "SANDBOX", "u1", Map.of("templateId", "tpl-1")));

        assertThat(result.accepted()).isTrue();
        assertThat(result.externalMessageId()).isEqualTo("sandbox-SMS-u1");
    }
}
```

- [x] **Step 2: Run registry tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorRegistryTest
```

Expected: FAIL because registry classes do not exist.

Actual: registry tests/classes were already present in the worktree.

- [x] **Step 3: Implement disabled and sandbox behavior**

Create `DisabledChannelConnector`:

```java
public class DisabledChannelConnector implements ChannelConnector {
    private final String reason;

    public DisabledChannelConnector(String reason) {
        this.reason = reason;
    }

    @Override public ConnectorMode mode() { return ConnectorMode.DISABLED; }
    @Override public ConnectorHealth health() { return new ConnectorHealth("DISABLED", reason); }
    @Override public ConnectorCapabilities capabilities() { return new ConnectorCapabilities(false, false, Map.of()); }
    @Override public ConnectorSendResult send(ConnectorSendRequest request) {
        return new ConnectorSendResult(false, null, "DISABLED", reason);
    }
    @Override public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
        return new ConnectorReceiptResult(null, "UNSUPPORTED", Map.of());
    }
}
```

Create `ChannelConnectorRegistry` with `Repository.find(tenantId, channel, provider)`, registered real connectors by connector key, disabled fallback for missing config, and sandbox connector for `mode=SANDBOX`.

- [x] **Step 4: Run registry tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorRegistryTest
```

Expected: PASS.

Actual: covered by isolated P1-008 suite; `ChannelConnectorRegistryTest` passed.

### Task 3: Send Handler Integration

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ChannelConnectorHandlerTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java`

- [x] **Step 1: Write handler tests**

Create `ChannelConnectorHandlerTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelConnectorHandlerTest {

    @Test
    void disabledConnectorFailsClosedWithVisibleOutput() {
        ReachDeliveryService delivery = mock(ReachDeliveryService.class);
        ChannelConnectorRegistry registry = mock(ChannelConnectorRegistry.class);
        when(registry.resolve(0L, "SMS", "default")).thenReturn(new org.chovy.canvas.engine.channel.DisabledChannelConnector("SMS provider not configured"));
        SendMessageHandler handler = new SendMessageHandler(delivery, registry);

        NodeResult result = handler.executeAsync(Map.of("channel", "SMS"), context()).block();

        assertThat(result.success()).isFalse();
        assertThat(result.output()).containsEntry("connectorMode", "DISABLED")
                .containsEntry("connectorReason", "SMS provider not configured");
        verify(delivery, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sandboxConnectorReturnsSuccessWithoutRealDeliveryCall() {
        ReachDeliveryService delivery = mock(ReachDeliveryService.class);
        ChannelConnector connector = mock(ChannelConnector.class);
        when(connector.mode()).thenReturn(ChannelConnector.ConnectorMode.SANDBOX);
        when(connector.send(org.mockito.ArgumentMatchers.any())).thenReturn(new ChannelConnector.ConnectorSendResult(true, "sandbox-SMS-u1", "ACCEPTED", null));
        ChannelConnectorRegistry registry = mock(ChannelConnectorRegistry.class);
        when(registry.resolve(0L, "SMS", "default")).thenReturn(connector);
        SendMessageHandler handler = new SendMessageHandler(delivery, registry);

        NodeResult result = handler.executeAsync(Map.of("channel", "SMS", "successNodeId", "next"), context()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("connectorMode", "SANDBOX")
                .containsEntry("externalMessageId", "sandbox-SMS-u1");
        verify(delivery, never()).send(org.mockito.ArgumentMatchers.any());
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(20L);
        ctx.setUserId("u1");
        return ctx;
    }
}
```

- [x] **Step 2: Run handler tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorHandlerTest
```

Expected: FAIL because handler constructors and connector output are not implemented.

Actual: regular Maven targeted test was blocked by unrelated global `testCompile` drift, but the new handler test covered the missing constructor/output behavior before implementation.

- [x] **Step 3: Add failure result with output**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java`:

```java
public static NodeResult fail(String errorMessage, Map<String, Object> output) {
    return new NodeResult(null, null, null, null, null, output == null ? Map.of() : output,
            false, errorMessage, false, NodeOutcome.FAIL, Map.of(), "NODE_FAILED", errorMessage, null);
}
```

- [x] **Step 4: Integrate registry in send handler path**

Update `SendMessageHandler` constructor to accept `ReachDeliveryService` and `ChannelConnectorRegistry`. In `AbstractSendMessageHandler`, resolve provider from config:

```java
String provider = string(config, "provider", "default");
ChannelConnector connector = connectorRegistry.resolve(0L, channel, provider);
if (connector.mode() == ChannelConnector.ConnectorMode.DISABLED) {
    return Mono.just(NodeResult.fail("connector disabled: " + connector.health().message(),
            Map.of("connectorMode", "DISABLED", "connectorReason", connector.health().message())));
}
```

For `SANDBOX`, call `connector.send(...)` and route success/fail without calling `ReachDeliveryService`. For `REAL`, continue through `ReachDeliveryService` while adding connector mode/provider fields to the result output.

Actual: `AbstractSendMessageHandler` now resolves connector provider/mode when a registry is injected. Disabled mode returns `NodeResult.fail(..., output)` without delivery; sandbox mode calls `connector.send(...)` without delivery; real mode continues through `ReachDeliveryService` and adds connector output.

- [x] **Step 5: Run handler and existing send tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorHandlerTest,SendMessageHandlerTest
```

Expected: PASS.

Actual: isolated P1-008 suite passed for `ChannelConnectorHandlerTest` and `SendMessageHandlerTest`.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p1-008-channel-connector-contract-and-disabled-state.md`
- Modify: `docs/product-evolution/plans/p1-008-channel-connector-contract-and-disabled-state-plan.md`

- [x] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelConnectorSchemaTest,ChannelConnectorRegistryTest,ChannelConnectorHandlerTest,SendMessageHandlerTest
```

Expected: PASS.

Actual: `mvn -pl canvas-engine -DskipTests compile` passed, and isolated P1-008 runner passed 8 tests: `ChannelConnectorSchemaTest`, `ChannelConnectorRegistryTest`, `SendMessageHandlerTest`, `ChannelConnectorHandlerTest`.

- [x] **Step 2: Run affected handler regression**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CouponHandlerTest,CommitActionHandlerTest,ApiCallHandlerRateLimitTest
```

Expected: PASS.

Actual: isolated regression runner passed 25 tests: `CouponHandlerTest`, `CommitActionHandlerTest`, `ApiCallHandlerRateLimitTest`.

Commit boundary: no commit was created in this docs-only audit; commit and merge status remains unverified.

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java backend/canvas-engine/src/main/resources/db/migration/V102__channel_connector_contract.sql backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers docs/product-evolution/specs/p1-008-channel-connector-contract-and-disabled-state.md docs/product-evolution/plans/p1-008-channel-connector-contract-and-disabled-state-plan.md
git commit -m "feat: add channel connector contract"
```

Expected: commit contains only connector contract, disabled/sandbox behavior, handler integration, tests, migration, and related docs.

Actual: skipped in this session because the user requested implementation without committing.
