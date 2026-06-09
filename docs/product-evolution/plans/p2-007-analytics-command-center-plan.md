# Analytics Command Center Implementation Plan

Status: Open execution plan; implementation is not complete in this docs-only audit because the plan retains unchecked execution tasks.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first analytics command center workflow: metric dictionary, tenant-scoped dashboard summary, channel comparison, and export-state helpers.

**Architecture:** Store metric dictionary metadata in an additive table and compute command-center summaries through a domain service backed by a repository boundary. This slice does not implement scheduled reports or deep chart drill-down; it establishes the metric contract, comparison output, and frontend formatting required for later analytics specs.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-007-analytics-command-center.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#analytics-command-center`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V166__analytics_command_center.sql` - metric dictionary table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterService.java` - summary and comparison aggregation logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsCommandCenterController.java` - `/analytics-command-center` API.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AnalyticsCommandCenterControllerTest.java`

**Frontend**
- Create: `frontend/src/services/analyticsApi.ts`
- Create: `frontend/src/pages/analytics-command-center/analyticsCommandCenter.ts`
- Create: `frontend/src/pages/analytics-command-center/analyticsCommandCenter.test.ts`

### Task 1: Metric Dictionary And Summary Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V166__analytics_command_center.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterServiceTest.java`

- [ ] **Step 1: Write analytics service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterServiceTest.java`:

```java
package org.chovy.canvas.domain.analytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsCommandCenterServiceTest {

    @Test
    void migrationCreatesMetricDictionaryTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V166__analytics_command_center.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS analytics_metric_definition")
                .contains("metric_key VARCHAR(128) NOT NULL")
                .contains("metric_type VARCHAR(32) NOT NULL")
                .contains("source_query_key VARCHAR(128) NOT NULL")
                .contains("UNIQUE KEY uk_analytics_metric_key");
    }

    @Test
    void summaryReturnsMetricsAndChannelComparisonForTenantDateRange() {
        AnalyticsCommandCenterService.AnalyticsRepository repository = mock(AnalyticsCommandCenterService.AnalyticsRepository.class);
        AnalyticsCommandCenterService service = new AnalyticsCommandCenterService(repository);
        AnalyticsCommandCenterService.DateRange range = new AnalyticsCommandCenterService.DateRange(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-03"));
        when(repository.metricValues(8L, range)).thenReturn(Map.of("revenue", 12000L, "roi", 3.2));
        when(repository.channelComparison(8L, range)).thenReturn(List.of(
                new AnalyticsCommandCenterService.ChannelMetric("SMS", 1000L, 120L, 3000L),
                new AnalyticsCommandCenterService.ChannelMetric("IN_APP", 800L, 180L, 5000L)));

        AnalyticsCommandCenterService.Summary summary = service.summary(8L, range);

        assertThat(summary.metricValues()).containsEntry("revenue", 12000L);
        assertThat(summary.channelComparison()).extracting(AnalyticsCommandCenterService.ChannelMetric::channel)
                .containsExactly("IN_APP", "SMS");
    }

    @Test
    void summaryRejectsInvertedDateRange() {
        AnalyticsCommandCenterService.AnalyticsRepository repository = mock(AnalyticsCommandCenterService.AnalyticsRepository.class);
        AnalyticsCommandCenterService service = new AnalyticsCommandCenterService(repository);

        assertThatThrownBy(() -> service.summary(8L, new AnalyticsCommandCenterService.DateRange(
                LocalDate.parse("2026-06-03"), LocalDate.parse("2026-06-01"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start date must be before or equal to end date");
    }

    @Test
    void exportRequestRecordsTenantMetricAndRange() {
        AnalyticsCommandCenterService.AnalyticsRepository repository = mock(AnalyticsCommandCenterService.AnalyticsRepository.class);
        AnalyticsCommandCenterService service = new AnalyticsCommandCenterService(repository);
        AnalyticsCommandCenterService.DateRange range = new AnalyticsCommandCenterService.DateRange(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-03"));

        AnalyticsCommandCenterService.ExportRequest request = service.exportRequest(8L, "channel_comparison", range);

        assertThat(request.tenantId()).isEqualTo(8L);
        assertThat(request.metricKey()).isEqualTo("channel_comparison");
        assertThat(request.fileName()).isEqualTo("channel_comparison_2026-06-01_2026-06-03.csv");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsCommandCenterServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add metric dictionary migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V166__analytics_command_center.sql`:

```sql
CREATE TABLE IF NOT EXISTS analytics_metric_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  metric_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  metric_type VARCHAR(32) NOT NULL,
  source_query_key VARCHAR(128) NOT NULL,
  unit VARCHAR(32) NOT NULL DEFAULT 'count',
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analytics_metric_key (metric_key),
  INDEX idx_analytics_metric_enabled (enabled, metric_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO analytics_metric_definition(metric_key, display_name, metric_type, source_query_key, unit, enabled)
VALUES
  ('revenue', 'Revenue', 'BUSINESS', 'revenue_by_canvas', 'currency', 1),
  ('roi', 'ROI', 'BUSINESS', 'roi_by_canvas', 'ratio', 1),
  ('channel_comparison', 'Channel Comparison', 'COMPARISON', 'channel_delivery_comparison', 'count', 1),
  ('canvas_comparison', 'Canvas Comparison', 'COMPARISON', 'canvas_outcome_comparison', 'count', 1);
```

- [ ] **Step 4: Implement analytics service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterService.java`:

```java
package org.chovy.canvas.domain.analytics;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AnalyticsCommandCenterService {

    private final AnalyticsRepository repository;

    public AnalyticsCommandCenterService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    public Summary summary(Long tenantId, DateRange range) {
        validateRange(range);
        List<ChannelMetric> comparison = repository.channelComparison(tenantId, range).stream()
                .sorted(Comparator.comparing(ChannelMetric::revenue).reversed())
                .toList();
        return new Summary(repository.metricValues(tenantId, range), comparison);
    }

    public ExportRequest exportRequest(Long tenantId, String metricKey, DateRange range) {
        validateRange(range);
        return new ExportRequest(
                tenantId,
                metricKey,
                range.startDate(),
                range.endDate(),
                metricKey + "_" + range.startDate() + "_" + range.endDate() + ".csv");
    }

    private static void validateRange(DateRange range) {
        if (range.startDate().isAfter(range.endDate())) {
            throw new IllegalArgumentException("start date must be before or equal to end date");
        }
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {}

    public record ChannelMetric(String channel, long sentCount, long conversionCount, long revenue) {}

    public record Summary(Map<String, Object> metricValues, List<ChannelMetric> channelComparison) {}

    public record ExportRequest(Long tenantId, String metricKey, LocalDate startDate, LocalDate endDate, String fileName) {}

    public interface AnalyticsRepository {
        Map<String, Object> metricValues(Long tenantId, DateRange range);
        List<ChannelMetric> channelComparison(Long tenantId, DateRange range);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsCommandCenterServiceTest
```

Expected: PASS.

### Task 2: Analytics Command Center API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsCommandCenterController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AnalyticsCommandCenterControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AnalyticsCommandCenterControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AnalyticsCommandCenterService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsCommandCenterControllerTest {

    @Test
    void summaryUsesTenantAndDateRange() {
        AnalyticsCommandCenterService service = mock(AnalyticsCommandCenterService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        AnalyticsCommandCenterService.DateRange range = new AnalyticsCommandCenterService.DateRange(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-03"));
        when(service.summary(8L, range)).thenReturn(new AnalyticsCommandCenterService.Summary(
                Map.of("revenue", 12000L), List.of()));
        AnalyticsCommandCenterController controller = new AnalyticsCommandCenterController(service, resolver);

        StepVerifier.create(controller.summary("2026-06-01", "2026-06-03"))
                .assertNext(response -> assertThat(response.getData().metricValues()).containsEntry("revenue", 12000L))
                .verifyComplete();

        verify(service).summary(8L, range);
    }

    @Test
    void exportUsesTenantMetricAndRange() {
        AnalyticsCommandCenterService service = mock(AnalyticsCommandCenterService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        AnalyticsCommandCenterService.DateRange range = new AnalyticsCommandCenterService.DateRange(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-03"));
        when(service.exportRequest(8L, "channel_comparison", range)).thenReturn(new AnalyticsCommandCenterService.ExportRequest(
                8L, "channel_comparison", range.startDate(), range.endDate(), "channel_comparison_2026-06-01_2026-06-03.csv"));
        AnalyticsCommandCenterController controller = new AnalyticsCommandCenterController(service, resolver);

        StepVerifier.create(controller.export("channel_comparison", "2026-06-01", "2026-06-03"))
                .assertNext(response -> assertThat(response.getData().fileName()).endsWith(".csv"))
                .verifyComplete();
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsCommandCenterControllerTest
```

Expected: FAIL because `AnalyticsCommandCenterController` does not exist.

- [ ] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsCommandCenterController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AnalyticsCommandCenterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;

@RestController
@RequestMapping("/analytics-command-center")
@RequiredArgsConstructor
public class AnalyticsCommandCenterController {

    private final AnalyticsCommandCenterService service;
    private final TenantContextResolver tenantContextResolver;

    @GetMapping("/summary")
    public Mono<R<AnalyticsCommandCenterService.Summary>> summary(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        AnalyticsCommandCenterService.DateRange range = range(startDate, endDate);
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.summary(context.tenantId(), range))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @GetMapping("/exports")
    public Mono<R<AnalyticsCommandCenterService.ExportRequest>> export(
            @RequestParam String metricKey,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        AnalyticsCommandCenterService.DateRange range = range(startDate, endDate);
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.exportRequest(context.tenantId(), metricKey, range))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    private static AnalyticsCommandCenterService.DateRange range(String startDate, String endDate) {
        return new AnalyticsCommandCenterService.DateRange(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsCommandCenterControllerTest
```

Expected: PASS.

### Task 3: Frontend Analytics Helpers

**Files:**
- Create: `frontend/src/services/analyticsApi.ts`
- Create: `frontend/src/pages/analytics-command-center/analyticsCommandCenter.ts`
- Create: `frontend/src/pages/analytics-command-center/analyticsCommandCenter.test.ts`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/analytics-command-center/analyticsCommandCenter.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { exportStateText, formatMetricValue, rankChannelsByRevenue, type ChannelMetric } from './analyticsCommandCenter'

describe('analyticsCommandCenter helpers', () => {
  it('formats metric values by unit', () => {
    expect(formatMetricValue(12000, 'currency')).toBe('$12,000')
    expect(formatMetricValue(3.2, 'ratio')).toBe('3.20x')
    expect(formatMetricValue(42, 'count')).toBe('42')
  })

  it('ranks channels by revenue', () => {
    const rows: ChannelMetric[] = [
      { channel: 'SMS', sentCount: 1000, conversionCount: 120, revenue: 3000 },
      { channel: 'IN_APP', sentCount: 800, conversionCount: 180, revenue: 5000 },
    ]

    expect(rankChannelsByRevenue(rows).map(row => row.channel)).toEqual(['IN_APP', 'SMS'])
  })

  it('formats export states', () => {
    expect(exportStateText('IDLE')).toBe('Ready to export')
    expect(exportStateText('RUNNING')).toBe('Export running')
    expect(exportStateText('FAILED')).toBe('Export failed')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- analyticsCommandCenter.test.ts
```

Expected: FAIL because `analyticsCommandCenter.ts` does not exist.

- [ ] **Step 3: Add API wrapper**

Create `frontend/src/services/analyticsApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { AnalyticsSummary, ExportRequest } from '../pages/analytics-command-center/analyticsCommandCenter'

export const analyticsCommandCenterApi = {
  summary: (params: { startDate: string; endDate: string }) =>
    http.get<R<AnalyticsSummary>, R<AnalyticsSummary>>('/analytics-command-center/summary', { params }),
  exportRequest: (params: { metricKey: string; startDate: string; endDate: string }) =>
    http.get<R<ExportRequest>, R<ExportRequest>>('/analytics-command-center/exports', { params }),
}
```

- [ ] **Step 4: Add frontend helpers**

Create `frontend/src/pages/analytics-command-center/analyticsCommandCenter.ts`:

```ts
export interface ChannelMetric {
  channel: string
  sentCount: number
  conversionCount: number
  revenue: number
}

export interface AnalyticsSummary {
  metricValues: Record<string, unknown>
  channelComparison: ChannelMetric[]
}

export interface ExportRequest {
  tenantId: number
  metricKey: string
  startDate: string
  endDate: string
  fileName: string
}

export function formatMetricValue(value: number, unit: 'currency' | 'ratio' | 'count') {
  if (unit === 'currency') return `$${value.toLocaleString('en-US')}`
  if (unit === 'ratio') return `${value.toFixed(2)}x`
  return String(value)
}

export function rankChannelsByRevenue(rows: ChannelMetric[]) {
  return [...rows].sort((left, right) => right.revenue - left.revenue)
}

export function exportStateText(state: 'IDLE' | 'RUNNING' | 'FAILED') {
  if (state === 'RUNNING') return 'Export running'
  if (state === 'FAILED') return 'Export failed'
  return 'Ready to export'
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- analyticsCommandCenter.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-007-analytics-command-center.md`
- Modify: `docs/product-evolution/plans/p2-007-analytics-command-center-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V166__analytics_command_center.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsCommandCenterController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AnalyticsCommandCenterControllerTest.java`
- Create: `frontend/src/services/analyticsApi.ts`
- Create: `frontend/src/pages/analytics-command-center/analyticsCommandCenter.ts`
- Create: `frontend/src/pages/analytics-command-center/analyticsCommandCenter.test.ts`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AnalyticsCommandCenterServiceTest,AnalyticsCommandCenterControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- analyticsCommandCenter.test.ts
```

Expected: PASS.

- [ ] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V166__analytics_command_center.sql`, then expose summary and export-request endpoints to tenant operators. Rollback: hide the analytics command center route; metric dictionary rows are additive and do not change existing analytics queries.
```

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V166__analytics_command_center.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsCommandCenterController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsCommandCenterServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AnalyticsCommandCenterControllerTest.java \
  frontend/src/services/analyticsApi.ts \
  frontend/src/pages/analytics-command-center/analyticsCommandCenter.ts \
  frontend/src/pages/analytics-command-center/analyticsCommandCenter.test.ts \
  docs/product-evolution/specs/p2-007-analytics-command-center.md \
  docs/product-evolution/plans/p2-007-analytics-command-center-plan.md
git commit -m "feat: add analytics command center foundation"
```

Expected: commit contains only analytics command center schema, service/API, frontend helpers, tests, spec, and plan files.
