# Channel Intelligence And Scheduling Implementation Plan

Status: Open execution plan; implementation is not complete in this docs-only audit because the plan retains unchecked execution tasks.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first channel intelligence workflow: rule-based channel routing, marketing calendar conflict checks, channel cost summaries, and send-time observation capture.

**Architecture:** Store channel rules, calendar entries, cost observations, and send-time observations in additive tables. Keep routing and conflict logic in `ChannelIntelligenceService`, add a small priority handler for graph output, then expose frontend API and presentation helpers for routing, conflicts, and cost metrics.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-012-channel-intelligence-and-scheduling.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#channel-intelligence-and-scheduling`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V171__channel_intelligence_scheduling.sql` - routing, calendar, cost, and send-time observation tables.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/channel/ChannelIntelligenceService.java` - route selection, conflict detection, cost summary, and send-time capture.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelIntelligenceController.java` - `/channel-intelligence` API.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PriorityHandler.java` - graph handler that emits selected priority and channel route.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/channel/ChannelIntelligenceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PriorityHandlerTest.java`

**Frontend**
- Create: `frontend/src/services/channelIntelligenceApi.ts`
- Create: `frontend/src/pages/channel-intelligence/channelIntelligence.ts`
- Create: `frontend/src/pages/channel-intelligence/channelIntelligence.test.tsx`
- Create: `frontend/src/pages/channel-intelligence/index.tsx`

### Task 1: Channel Intelligence Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V171__channel_intelligence_scheduling.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/channel/ChannelIntelligenceService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/channel/ChannelIntelligenceServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/channel/ChannelIntelligenceServiceTest.java`:

```java
package org.chovy.canvas.domain.channel;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelIntelligenceServiceTest {

    @Test
    void migrationCreatesRoutingCalendarCostAndSendTimeTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V171__channel_intelligence_scheduling.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS channel_route_rule")
                .contains("CREATE TABLE IF NOT EXISTS marketing_calendar_entry")
                .contains("CREATE TABLE IF NOT EXISTS channel_cost_observation")
                .contains("CREATE TABLE IF NOT EXISTS channel_send_time_observation")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("INDEX idx_marketing_calendar_window");
    }

    @Test
    void routeSelectsFirstEnabledRuleMatchingScenario() {
        ChannelIntelligenceService.ChannelIntelligenceRepository repository = mock(ChannelIntelligenceService.ChannelIntelligenceRepository.class);
        ChannelIntelligenceService service = new ChannelIntelligenceService(repository);
        when(repository.routeRules(8L)).thenReturn(List.of(
                new ChannelIntelligenceService.RouteRule(8L, "vip_wecom", "VIP", "WECOM", 1, true),
                new ChannelIntelligenceService.RouteRule(8L, "default_sms", "DEFAULT", "SMS", 99, true)));

        ChannelIntelligenceService.RouteDecision decision = service.route(8L, new ChannelIntelligenceService.RouteRequest(
                "VIP", Map.of("userId", "u1")));

        assertThat(decision.channel()).isEqualTo("WECOM");
        assertThat(decision.ruleKey()).isEqualTo("vip_wecom");
    }

    @Test
    void routeFallsBackToSmsWhenNoRuleMatches() {
        ChannelIntelligenceService.ChannelIntelligenceRepository repository = mock(ChannelIntelligenceService.ChannelIntelligenceRepository.class);
        ChannelIntelligenceService service = new ChannelIntelligenceService(repository);
        when(repository.routeRules(8L)).thenReturn(List.of());

        ChannelIntelligenceService.RouteDecision decision = service.route(8L, new ChannelIntelligenceService.RouteRequest(
                "WINBACK", Map.of("userId", "u1")));

        assertThat(decision.channel()).isEqualTo("SMS");
        assertThat(decision.ruleKey()).isEqualTo("fallback_sms");
    }

    @Test
    void conflictDetectionReturnsOverlappingCalendarEntries() {
        ChannelIntelligenceService.ChannelIntelligenceRepository repository = mock(ChannelIntelligenceService.ChannelIntelligenceRepository.class);
        ChannelIntelligenceService service = new ChannelIntelligenceService(repository);
        Instant start = Instant.parse("2026-06-03T10:00:00Z");
        Instant end = Instant.parse("2026-06-03T11:00:00Z");
        when(repository.calendarEntries(8L, start, end)).thenReturn(List.of(new ChannelIntelligenceService.CalendarEntry(
                8L, "summer_sale", "SMS", start.minusSeconds(600), end.minusSeconds(300))));

        List<ChannelIntelligenceService.CalendarConflict> conflicts = service.conflicts(8L, "SMS", start, end);

        assertThat(conflicts).extracting(ChannelIntelligenceService.CalendarConflict::campaignKey)
                .containsExactly("summer_sale");
    }

    @Test
    void costSummaryComputesCostPerConversion() {
        ChannelIntelligenceService.ChannelIntelligenceRepository repository = mock(ChannelIntelligenceService.ChannelIntelligenceRepository.class);
        ChannelIntelligenceService service = new ChannelIntelligenceService(repository);
        when(repository.costs(8L)).thenReturn(List.of(
                new ChannelIntelligenceService.ChannelCost("SMS", new BigDecimal("200.00"), 50),
                new ChannelIntelligenceService.ChannelCost("WECOM", new BigDecimal("20.00"), 10)));

        List<ChannelIntelligenceService.ChannelCostSummary> summaries = service.costSummary(8L);

        assertThat(summaries).extracting(ChannelIntelligenceService.ChannelCostSummary::costPerConversion)
                .containsExactly(new BigDecimal("4.00"), new BigDecimal("2.00"));
    }

    @Test
    void recordSendTimeObservationPersistsTenantScopedObservation() {
        ChannelIntelligenceService.ChannelIntelligenceRepository repository = mock(ChannelIntelligenceService.ChannelIntelligenceRepository.class);
        ChannelIntelligenceService service = new ChannelIntelligenceService(repository);
        ChannelIntelligenceService.SendTimeObservation observation = new ChannelIntelligenceService.SendTimeObservation(
                8L, "SMS", "u1", Instant.parse("2026-06-03T10:00:00Z"), true);

        service.recordSendTimeObservation(observation);

        verify(repository).insertSendTimeObservation(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.channel().equals("SMS")
                        && saved.converted()));
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelIntelligenceServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add channel intelligence migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V171__channel_intelligence_scheduling.sql`:

```sql
CREATE TABLE IF NOT EXISTS channel_route_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  rule_key VARCHAR(128) NOT NULL,
  scenario_key VARCHAR(128) NOT NULL,
  channel VARCHAR(64) NOT NULL,
  priority INT NOT NULL DEFAULT 100,
  enabled TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_route_rule (tenant_id, rule_key),
  INDEX idx_channel_route_rule_lookup (tenant_id, enabled, scenario_key, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS marketing_calendar_entry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  campaign_key VARCHAR(128) NOT NULL,
  channel VARCHAR(64) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_marketing_calendar_window (tenant_id, channel, start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_cost_observation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  channel VARCHAR(64) NOT NULL,
  cost_amount DECIMAL(18, 4) NOT NULL,
  conversion_count BIGINT NOT NULL DEFAULT 0,
  observed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_channel_cost_tenant_channel (tenant_id, channel, observed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS channel_send_time_observation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  channel VARCHAR(64) NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  sent_at DATETIME NOT NULL,
  converted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_channel_send_time_tenant_channel (tenant_id, channel, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement channel intelligence service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/channel/ChannelIntelligenceService.java`:

```java
package org.chovy.canvas.domain.channel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ChannelIntelligenceService {

    private final ChannelIntelligenceRepository repository;

    public ChannelIntelligenceService(ChannelIntelligenceRepository repository) {
        this.repository = repository;
    }

    public RouteDecision route(Long tenantId, RouteRequest request) {
        return repository.routeRules(tenantId).stream()
                .filter(RouteRule::enabled)
                .filter(rule -> rule.scenarioKey().equals(request.scenarioKey()) || rule.scenarioKey().equals("DEFAULT"))
                .sorted(Comparator.comparingInt(RouteRule::priority))
                .findFirst()
                .map(rule -> new RouteDecision(rule.ruleKey(), rule.channel()))
                .orElse(new RouteDecision("fallback_sms", "SMS"));
    }

    public List<CalendarConflict> conflicts(Long tenantId, String channel, Instant startAt, Instant endAt) {
        return repository.calendarEntries(tenantId, startAt, endAt).stream()
                .filter(entry -> entry.channel().equals(channel))
                .map(entry -> new CalendarConflict(entry.campaignKey(), entry.channel(), entry.startAt(), entry.endAt()))
                .toList();
    }

    public List<ChannelCostSummary> costSummary(Long tenantId) {
        return repository.costs(tenantId).stream()
                .map(cost -> new ChannelCostSummary(
                        cost.channel(),
                        cost.costAmount(),
                        cost.conversionCount(),
                        cost.conversionCount() == 0
                                ? BigDecimal.ZERO
                                : cost.costAmount().divide(BigDecimal.valueOf(cost.conversionCount()), 2, RoundingMode.HALF_UP)))
                .toList();
    }

    public void recordSendTimeObservation(SendTimeObservation observation) {
        repository.insertSendTimeObservation(observation);
    }

    public record RouteRequest(String scenarioKey, Map<String, Object> userContext) {}

    public record RouteDecision(String ruleKey, String channel) {}

    public record RouteRule(Long tenantId, String ruleKey, String scenarioKey, String channel, int priority, boolean enabled) {}

    public record CalendarEntry(Long tenantId, String campaignKey, String channel, Instant startAt, Instant endAt) {}

    public record CalendarConflict(String campaignKey, String channel, Instant startAt, Instant endAt) {}

    public record ChannelCost(String channel, BigDecimal costAmount, long conversionCount) {}

    public record ChannelCostSummary(String channel, BigDecimal costAmount, long conversionCount, BigDecimal costPerConversion) {}

    public record SendTimeObservation(Long tenantId, String channel, String userId, Instant sentAt, boolean converted) {}

    public interface ChannelIntelligenceRepository {
        List<RouteRule> routeRules(Long tenantId);
        List<CalendarEntry> calendarEntries(Long tenantId, Instant startAt, Instant endAt);
        List<ChannelCost> costs(Long tenantId);
        void insertSendTimeObservation(SendTimeObservation observation);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelIntelligenceServiceTest
```

Expected: PASS.

### Task 2: Priority Handler And API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PriorityHandler.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PriorityHandlerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelIntelligenceController.java`

- [ ] **Step 1: Write priority handler tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PriorityHandlerTest.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PriorityHandlerTest {

    @Test
    void emitsPriorityAndChannelRoute() {
        PriorityHandler handler = new PriorityHandler();

        var result = handler.executeAsync(Map.of(
                "priority", "HIGH",
                "channel", "WECOM",
                "successNodeId", "next"), mock(ExecutionContext.class)).block();

        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("priority", "HIGH");
        assertThat(result.output()).containsEntry("channel", "WECOM");
    }
}
```

- [ ] **Step 2: Run handler tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PriorityHandlerTest
```

Expected: FAIL because `PriorityHandler` does not exist.

- [ ] **Step 3: Implement priority handler**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PriorityHandler.java`:

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@NodeHandlerType("PRIORITY")
public class PriorityHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String priority = String.valueOf(config.getOrDefault("priority", "NORMAL"));
        String channel = String.valueOf(config.getOrDefault("channel", "SMS"));
        String successNodeId = (String) config.get("successNodeId");
        return Mono.just(NodeResult.ok(successNodeId, Map.of(
                "priority", priority,
                "channel", channel)));
    }
}
```

- [ ] **Step 4: Implement channel intelligence controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelIntelligenceController.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.channel.ChannelIntelligenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/channel-intelligence")
public class ChannelIntelligenceController {

    private final ChannelIntelligenceService service;
    private final TenantContextResolver tenantContextResolver;

    public ChannelIntelligenceController(ChannelIntelligenceService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/route")
    public Mono<R<ChannelIntelligenceService.RouteDecision>> route(@RequestBody ChannelIntelligenceService.RouteRequest request) {
        return currentTenant().map(ctx -> R.ok(service.route(ctx.tenantId(), request)));
    }

    @GetMapping("/conflicts")
    public Mono<R<List<ChannelIntelligenceService.CalendarConflict>>> conflicts(
            @RequestParam String channel,
            @RequestParam String startAt,
            @RequestParam String endAt) {
        return currentTenant().map(ctx -> R.ok(service.conflicts(ctx.tenantId(), channel, Instant.parse(startAt), Instant.parse(endAt))));
    }

    @GetMapping("/cost-summary")
    public Mono<R<List<ChannelIntelligenceService.ChannelCostSummary>>> costSummary() {
        return currentTenant().map(ctx -> R.ok(service.costSummary(ctx.tenantId())));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new IllegalStateException("tenant context required")));
    }
}
```

- [ ] **Step 5: Run handler tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PriorityHandlerTest
```

Expected: PASS.

### Task 3: Frontend API And Presentation Helpers

**Files:**
- Create: `frontend/src/services/channelIntelligenceApi.ts`
- Create: `frontend/src/pages/channel-intelligence/channelIntelligence.ts`
- Create: `frontend/src/pages/channel-intelligence/channelIntelligence.test.tsx`
- Create: `frontend/src/pages/channel-intelligence/index.tsx`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/channel-intelligence/channelIntelligence.test.tsx`:

```ts
import { describe, expect, it, vi, type Mock } from 'vitest'
import http from '../../services/api'
import { channelIntelligenceApi } from '../../services/channelIntelligenceApi'
import {
  buildConflictLabel,
  buildRouteLabel,
  getCostPerConversionLabel,
} from './channelIntelligence'

vi.mock('../../services/api', () => ({
  default: {
    post: vi.fn((url: string, body?: unknown) => Promise.resolve({ code: 0, message: 'success', data: { url, body } })),
    get: vi.fn((url: string, config?: unknown) => Promise.resolve({ code: 0, message: 'success', data: { url, config } })),
  },
}))

describe('channelIntelligenceApi', () => {
  it('posts routing requests', async () => {
    await channelIntelligenceApi.route({ scenarioKey: 'VIP', userContext: { userId: 'u1' } })

    expect(http.post as unknown as Mock).toHaveBeenCalledWith('/channel-intelligence/route', {
      scenarioKey: 'VIP',
      userContext: { userId: 'u1' },
    })
  })

  it('loads calendar conflicts with date parameters', async () => {
    await channelIntelligenceApi.conflicts('SMS', '2026-06-03T10:00:00Z', '2026-06-03T11:00:00Z')

    expect(http.get as unknown as Mock).toHaveBeenCalledWith('/channel-intelligence/conflicts', {
      params: {
        channel: 'SMS',
        startAt: '2026-06-03T10:00:00Z',
        endAt: '2026-06-03T11:00:00Z',
      },
    })
  })
})

describe('channel intelligence presentation', () => {
  it('formats routing decisions', () => {
    expect(buildRouteLabel({ ruleKey: 'vip_wecom', channel: 'WECOM' })).toBe('WECOM via vip_wecom')
  })

  it('formats calendar conflicts', () => {
    expect(buildConflictLabel({ campaignKey: 'summer_sale', channel: 'SMS' })).toBe('SMS conflict with summer_sale')
  })

  it('formats cost per conversion labels', () => {
    expect(getCostPerConversionLabel({ channel: 'SMS', costPerConversion: 4 })).toBe('SMS: 4.00 per conversion')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- channelIntelligence.test.tsx
```

Expected: FAIL because the API wrapper and helper do not exist.

- [ ] **Step 3: Implement frontend API wrapper**

Create `frontend/src/services/channelIntelligenceApi.ts`:

```ts
import type { R } from '../types'
import http from './api'

export interface RouteRequest {
  scenarioKey: string
  userContext: Record<string, unknown>
}

export interface RouteDecision {
  ruleKey: string
  channel: string
}

export interface CalendarConflict {
  campaignKey: string
  channel: string
  startAt?: string
  endAt?: string
}

export interface ChannelCostSummary {
  channel: string
  costAmount?: number
  conversionCount?: number
  costPerConversion: number
}

export const channelIntelligenceApi = {
  route: (body: RouteRequest) =>
    http.post<R<RouteDecision>, R<RouteDecision>>('/channel-intelligence/route', body),
  conflicts: (channel: string, startAt: string, endAt: string) =>
    http.get<R<CalendarConflict[]>, R<CalendarConflict[]>>('/channel-intelligence/conflicts', {
      params: { channel, startAt, endAt },
    }),
  costSummary: () =>
    http.get<R<ChannelCostSummary[]>, R<ChannelCostSummary[]>>('/channel-intelligence/cost-summary'),
}
```

- [ ] **Step 4: Implement presentation helpers and page shell**

Create `frontend/src/pages/channel-intelligence/channelIntelligence.ts`:

```ts
import type { CalendarConflict, ChannelCostSummary, RouteDecision } from '../../services/channelIntelligenceApi'

export function buildRouteLabel(decision: RouteDecision): string {
  return `${decision.channel} via ${decision.ruleKey}`
}

export function buildConflictLabel(conflict: Pick<CalendarConflict, 'campaignKey' | 'channel'>): string {
  return `${conflict.channel} conflict with ${conflict.campaignKey}`
}

export function getCostPerConversionLabel(summary: Pick<ChannelCostSummary, 'channel' | 'costPerConversion'>): string {
  return `${summary.channel}: ${summary.costPerConversion.toFixed(2)} per conversion`
}
```

Create `frontend/src/pages/channel-intelligence/index.tsx`:

```tsx
import { Card, Space, Typography } from 'antd'
import { buildConflictLabel, buildRouteLabel, getCostPerConversionLabel } from './channelIntelligence'

export default function ChannelIntelligencePage() {
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Typography.Title level={3}>Channel Intelligence</Typography.Title>
      <Card size="small">{buildRouteLabel({ ruleKey: 'fallback_sms', channel: 'SMS' })}</Card>
      <Card size="small">{buildConflictLabel({ campaignKey: 'none', channel: 'SMS' })}</Card>
      <Card size="small">{getCostPerConversionLabel({ channel: 'SMS', costPerConversion: 0 })}</Card>
    </Space>
  )
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- channelIntelligence.test.tsx
```

Expected: PASS.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-012-channel-intelligence-and-scheduling.md`
- Modify: `docs/product-evolution/plans/p2-012-channel-intelligence-and-scheduling-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChannelIntelligenceServiceTest,PriorityHandlerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- channelIntelligence.test.tsx
```

Expected: PASS.

- [ ] **Step 3: Run broad regression gates**

Run:

```bash
(cd backend && mvn -pl canvas-engine test)
(cd frontend && npm test -- --run)
(cd frontend && npm run build)
```

Expected: PASS for the backend module tests, PASS for Vitest, and PASS for the Vite build.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Use this text in the PR:

```markdown
Rollout notes:
- Feature flag: keep `/channel-intelligence` hidden until `V171__channel_intelligence_scheduling.sql` is applied and route/conflict/cost smoke tests pass.
- Migration: apply `V171__channel_intelligence_scheduling.sql` before enabling routing decisions or calendar conflict checks.
- Tenant and role impact: channel rules, conflicts, costs, and send-time observations are tenant scoped through JWT tenant context.
- Manual verification: create one WECOM route rule, request a VIP route, add an overlapping SMS calendar entry, and confirm the conflict and cost labels render.
- Rollback: hide the page and stop writing send-time observations; all schema changes are additive.
```

- [ ] **Step 5: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V171__channel_intelligence_scheduling.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/channel/ChannelIntelligenceService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelIntelligenceController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PriorityHandler.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/channel/ChannelIntelligenceServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PriorityHandlerTest.java \
  frontend/src/services/channelIntelligenceApi.ts \
  frontend/src/pages/channel-intelligence/channelIntelligence.ts \
  frontend/src/pages/channel-intelligence/channelIntelligence.test.tsx \
  frontend/src/pages/channel-intelligence/index.tsx \
  docs/product-evolution/specs/p2-012-channel-intelligence-and-scheduling.md \
  docs/product-evolution/plans/p2-012-channel-intelligence-and-scheduling-plan.md
git commit -m "feat: add channel intelligence scheduling plan"
```

Expected: commit contains only the P2-012 implementation files and the matching spec/plan documentation.
