# Product Usage Analytics And Feedback Loop Implementation Plan

Status: Open execution plan; implementation is not complete in this docs-only audit because the plan retains unchecked execution tasks.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first product analytics feedback loop: tenant-scoped usage events, feedback/NPS capture, runtime feature flag lookup, and alert-rule evaluation.

**Architecture:** Store product analytics inputs in additive Flyway tables and keep validation, flag lookup, and alert evaluation in a small domain service behind repository interfaces. Expose a thin WebFlux-style controller that resolves tenant context before every write or read, then add a typed frontend API wrapper and a feedback button state helper.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-009-product-usage-analytics-feedback-loop.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#product-usage-analytics-and-feedback-loop`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V168__product_usage_analytics_feedback.sql` - event, feedback, flag, and alert-rule tables.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsService.java` - validation, event recording, feedback capture, flag lookup, and alert evaluation.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProductAnalyticsController.java` - `/product-analytics` API.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ProductAnalyticsControllerTest.java`

**Frontend**
- Create: `frontend/src/services/productAnalytics.ts`
- Create: `frontend/src/components/feedback/FeedbackButton.tsx`
- Create: `frontend/src/services/productAnalytics.test.ts`

### Task 1: Schema And Domain Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V168__product_usage_analytics_feedback.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsServiceTest.java`:

```java
package org.chovy.canvas.domain.productanalytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductAnalyticsServiceTest {

    @Test
    void migrationCreatesTenantScopedAnalyticsTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V168__product_usage_analytics_feedback.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS product_usage_event")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("event_type VARCHAR(32) NOT NULL")
                .contains("CREATE TABLE IF NOT EXISTS product_feedback")
                .contains("nps_score INT NULL")
                .contains("CREATE TABLE IF NOT EXISTS product_feature_flag")
                .contains("CREATE TABLE IF NOT EXISTS product_alert_rule")
                .contains("INDEX idx_product_usage_event_tenant_time");
    }

    @Test
    void recordEventPersistsTenantScopedPageFeatureAndActionEvents() {
        ProductAnalyticsService.AnalyticsRepository repository = mock(ProductAnalyticsService.AnalyticsRepository.class);
        ProductAnalyticsService service = new ProductAnalyticsService(repository);

        ProductAnalyticsService.UsageEvent event = service.recordEvent(8L, new ProductAnalyticsService.UsageEventDraft(
                "PAGE", "canvas-editor", "open", "operator-1", Map.of("canvasId", 42)));

        assertThat(event.tenantId()).isEqualTo(8L);
        assertThat(event.surface()).isEqualTo("canvas-editor");
        verify(repository).insertEvent(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.eventType().equals("PAGE")
                        && saved.actor().equals("operator-1")));
    }

    @Test
    void recordEventRejectsUnknownEventType() {
        ProductAnalyticsService service = new ProductAnalyticsService(mock(ProductAnalyticsService.AnalyticsRepository.class));

        assertThatThrownBy(() -> service.recordEvent(8L, new ProductAnalyticsService.UsageEventDraft(
                "BATCH", "canvas-editor", "open", "operator-1", Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported product event type BATCH");
    }

    @Test
    void submitFeedbackStoresNpsAndComment() {
        ProductAnalyticsService.AnalyticsRepository repository = mock(ProductAnalyticsService.AnalyticsRepository.class);
        ProductAnalyticsService service = new ProductAnalyticsService(repository);

        ProductAnalyticsService.Feedback feedback = service.submitFeedback(8L, new ProductAnalyticsService.FeedbackDraft(
                "canvas-editor", "auto-save", 9, "autosave feels reliable", "operator-1"));

        assertThat(feedback.npsScore()).isEqualTo(9);
        verify(repository).insertFeedback(argThat(saved ->
                saved.tenantId().equals(8L)
                        && saved.featureKey().equals("auto-save")
                        && saved.comment().equals("autosave feels reliable")));
    }

    @Test
    void submitFeedbackRejectsOutOfRangeNps() {
        ProductAnalyticsService service = new ProductAnalyticsService(mock(ProductAnalyticsService.AnalyticsRepository.class));

        assertThatThrownBy(() -> service.submitFeedback(8L, new ProductAnalyticsService.FeedbackDraft(
                "canvas-editor", "auto-save", 11, "too high", "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NPS score must be between 0 and 10");
    }

    @Test
    void featureFlagDefaultsToDisabledWhenRepositoryHasNoMatch() {
        ProductAnalyticsService.AnalyticsRepository repository = mock(ProductAnalyticsService.AnalyticsRepository.class);
        ProductAnalyticsService service = new ProductAnalyticsService(repository);
        when(repository.findFlag(8L, "feedback_button")).thenReturn(null);

        ProductAnalyticsService.FeatureFlag flag = service.resolveFlag(8L, "feedback_button");

        assertThat(flag.enabled()).isFalse();
        assertThat(flag.rolloutPercent()).isZero();
    }

    @Test
    void alertEvaluationReturnsTriggeredRulesOnly() {
        ProductAnalyticsService.AnalyticsRepository repository = mock(ProductAnalyticsService.AnalyticsRepository.class);
        ProductAnalyticsService service = new ProductAnalyticsService(repository);
        when(repository.alertRules(8L)).thenReturn(List.of(
                new ProductAnalyticsService.AlertRule(8L, "low_feedback", "nps_avg", "LT", 6.0, true),
                new ProductAnalyticsService.AlertRule(8L, "healthy_usage", "daily_active_users", "GT", 100.0, true)));

        List<ProductAnalyticsService.AlertEvaluation> result = service.evaluateAlerts(8L, Map.of(
                "nps_avg", 5.5,
                "daily_active_users", 88));

        assertThat(result).extracting(ProductAnalyticsService.AlertEvaluation::ruleKey)
                .containsExactly("low_feedback");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductAnalyticsServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add analytics migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V168__product_usage_analytics_feedback.sql`:

```sql
CREATE TABLE IF NOT EXISTS product_usage_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  surface VARCHAR(128) NOT NULL,
  action_key VARCHAR(128) NOT NULL,
  actor VARCHAR(128) NULL,
  attributes_json JSON NOT NULL,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_usage_event_tenant_time (tenant_id, occurred_at),
  INDEX idx_product_usage_event_surface (tenant_id, surface, action_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_feedback (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  surface VARCHAR(128) NOT NULL,
  feature_key VARCHAR(128) NOT NULL,
  nps_score INT NULL,
  comment TEXT NULL,
  actor VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_feedback_tenant_feature (tenant_id, feature_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_feature_flag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  flag_key VARCHAR(128) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  rollout_percent INT NOT NULL DEFAULT 0,
  description VARCHAR(255) NULL,
  updated_by VARCHAR(128) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_feature_flag (tenant_id, flag_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  rule_key VARCHAR(128) NOT NULL,
  metric_key VARCHAR(128) NOT NULL,
  comparator VARCHAR(16) NOT NULL,
  threshold_value DECIMAL(18, 4) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_alert_rule (tenant_id, rule_key),
  INDEX idx_product_alert_rule_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement product analytics service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsService.java`:

```java
package org.chovy.canvas.domain.productanalytics;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductAnalyticsService {

    private static final Set<String> EVENT_TYPES = Set.of("PAGE", "FEATURE", "ACTION");

    private final AnalyticsRepository repository;

    public ProductAnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    public UsageEvent recordEvent(Long tenantId, UsageEventDraft draft) {
        if (!EVENT_TYPES.contains(draft.eventType())) {
            throw new IllegalArgumentException("unsupported product event type " + draft.eventType());
        }
        UsageEvent event = new UsageEvent(
                tenantId,
                draft.eventType(),
                draft.surface(),
                draft.actionKey(),
                draft.actor(),
                draft.attributes(),
                Instant.now());
        repository.insertEvent(event);
        return event;
    }

    public Feedback submitFeedback(Long tenantId, FeedbackDraft draft) {
        if (draft.npsScore() != null && (draft.npsScore() < 0 || draft.npsScore() > 10)) {
            throw new IllegalArgumentException("NPS score must be between 0 and 10");
        }
        Feedback feedback = new Feedback(
                tenantId,
                draft.surface(),
                draft.featureKey(),
                draft.npsScore(),
                draft.comment(),
                draft.actor(),
                Instant.now());
        repository.insertFeedback(feedback);
        return feedback;
    }

    public FeatureFlag resolveFlag(Long tenantId, String flagKey) {
        FeatureFlag flag = repository.findFlag(tenantId, flagKey);
        return flag == null ? new FeatureFlag(tenantId, flagKey, false, 0) : flag;
    }

    public List<AlertEvaluation> evaluateAlerts(Long tenantId, Map<String, Number> metrics) {
        return repository.alertRules(tenantId).stream()
                .filter(AlertRule::enabled)
                .filter(rule -> isTriggered(rule, metrics.get(rule.metricKey())))
                .map(rule -> new AlertEvaluation(rule.ruleKey(), rule.metricKey(), metrics.get(rule.metricKey()).doubleValue(), rule.thresholdValue()))
                .toList();
    }

    private boolean isTriggered(AlertRule rule, Number currentValue) {
        if (currentValue == null) {
            return false;
        }
        double current = currentValue.doubleValue();
        return switch (rule.comparator()) {
            case "GT" -> current > rule.thresholdValue();
            case "GTE" -> current >= rule.thresholdValue();
            case "LT" -> current < rule.thresholdValue();
            case "LTE" -> current <= rule.thresholdValue();
            default -> throw new IllegalArgumentException("unsupported alert comparator " + rule.comparator());
        };
    }

    public record UsageEventDraft(String eventType, String surface, String actionKey, String actor, Map<String, Object> attributes) {}

    public record UsageEvent(Long tenantId, String eventType, String surface, String actionKey, String actor, Map<String, Object> attributes, Instant occurredAt) {}

    public record FeedbackDraft(String surface, String featureKey, Integer npsScore, String comment, String actor) {}

    public record Feedback(Long tenantId, String surface, String featureKey, Integer npsScore, String comment, String actor, Instant createdAt) {}

    public record FeatureFlag(Long tenantId, String flagKey, boolean enabled, int rolloutPercent) {}

    public record AlertRule(Long tenantId, String ruleKey, String metricKey, String comparator, double thresholdValue, boolean enabled) {}

    public record AlertEvaluation(String ruleKey, String metricKey, double currentValue, double thresholdValue) {}

    public interface AnalyticsRepository {
        void insertEvent(UsageEvent event);
        void insertFeedback(Feedback feedback);
        FeatureFlag findFlag(Long tenantId, String flagKey);
        List<AlertRule> alertRules(Long tenantId);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductAnalyticsServiceTest
```

Expected: PASS.

### Task 2: Tenant-Scoped Controller

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProductAnalyticsController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ProductAnalyticsControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ProductAnalyticsControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.productanalytics.ProductAnalyticsService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductAnalyticsControllerTest {

    @Test
    void recordEventUsesTenantFromSecurityContext() {
        ProductAnalyticsService service = mock(ProductAnalyticsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        ProductAnalyticsController controller = new ProductAnalyticsController(service, resolver);
        ProductAnalyticsService.UsageEvent event = new ProductAnalyticsService.UsageEvent(
                8L, "PAGE", "canvas-editor", "open", "operator-1", Map.of("canvasId", 42), Instant.parse("2026-06-03T00:00:00Z"));
        when(service.recordEvent(8L, new ProductAnalyticsService.UsageEventDraft(
                "PAGE", "canvas-editor", "open", "operator-1", Map.of("canvasId", 42)))).thenReturn(event);

        StepVerifier.create(controller.recordEvent(new ProductAnalyticsService.UsageEventDraft(
                        "PAGE", "canvas-editor", "open", "operator-1", Map.of("canvasId", 42))))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().tenantId()).isEqualTo(8L);
                })
                .verifyComplete();

        verify(service).recordEvent(8L, new ProductAnalyticsService.UsageEventDraft(
                "PAGE", "canvas-editor", "open", "operator-1", Map.of("canvasId", 42)));
    }

    @Test
    void submitFeedbackRejectsMissingTenantContext() {
        ProductAnalyticsService service = mock(ProductAnalyticsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.empty());
        ProductAnalyticsController controller = new ProductAnalyticsController(service, resolver);

        StepVerifier.create(controller.submitFeedback(new ProductAnalyticsService.FeedbackDraft(
                        "canvas-editor", "auto-save", 8, "useful", "operator-1")))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("tenant context required"))
                .verify();
    }

    @Test
    void flagReadAllowsOperatorRoleAndUsesTenant() {
        ProductAnalyticsService service = mock(ProductAnalyticsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        when(service.resolveFlag(8L, "feedback_button")).thenReturn(new ProductAnalyticsService.FeatureFlag(8L, "feedback_button", false, 0));
        ProductAnalyticsController controller = new ProductAnalyticsController(service, resolver);

        StepVerifier.create(controller.flag("feedback_button"))
                .assertNext(response -> assertThat(response.getData().enabled()).isFalse())
                .verifyComplete();

        verify(service).resolveFlag(8L, "feedback_button");
    }

    @Test
    void recordEventRejectsUnknownRole() {
        ProductAnalyticsService service = mock(ProductAnalyticsService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, "VIEWER", "viewer-1")));
        ProductAnalyticsController controller = new ProductAnalyticsController(service, resolver);

        StepVerifier.create(controller.recordEvent(new ProductAnalyticsService.UsageEventDraft(
                        "PAGE", "canvas-editor", "open", "viewer-1", Map.of())))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("operator role required"))
                .verify();
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductAnalyticsControllerTest
```

Expected: FAIL because `ProductAnalyticsController` does not exist.

- [ ] **Step 3: Implement controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProductAnalyticsController.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.productanalytics.ProductAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/product-analytics")
public class ProductAnalyticsController {

    private static final java.util.Set<String> PRODUCT_ANALYTICS_ROLES = java.util.Set.of(
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN,
            RoleNames.OPERATOR);

    private final ProductAnalyticsService service;
    private final TenantContextResolver tenantContextResolver;

    public ProductAnalyticsController(ProductAnalyticsService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/events")
    public Mono<R<ProductAnalyticsService.UsageEvent>> recordEvent(@RequestBody ProductAnalyticsService.UsageEventDraft draft) {
        return currentTenant().map(ctx -> R.ok(service.recordEvent(ctx.tenantId(), draft)));
    }

    @PostMapping("/feedback")
    public Mono<R<ProductAnalyticsService.Feedback>> submitFeedback(@RequestBody ProductAnalyticsService.FeedbackDraft draft) {
        return currentTenant().map(ctx -> R.ok(service.submitFeedback(ctx.tenantId(), draft)));
    }

    @GetMapping("/flags/{flagKey}")
    public Mono<R<ProductAnalyticsService.FeatureFlag>> flag(@PathVariable String flagKey) {
        return currentTenant().map(ctx -> R.ok(service.resolveFlag(ctx.tenantId(), flagKey)));
    }

    @PostMapping("/alerts/evaluate")
    public Mono<R<java.util.List<ProductAnalyticsService.AlertEvaluation>>> evaluateAlerts(@RequestBody Map<String, Number> metrics) {
        return currentTenant().map(ctx -> R.ok(service.evaluateAlerts(ctx.tenantId(), metrics)));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new IllegalStateException("tenant context required")))
                .filter(ctx -> PRODUCT_ANALYTICS_ROLES.contains(ctx.role()))
                .switchIfEmpty(Mono.error(new IllegalStateException("operator role required")));
    }
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductAnalyticsControllerTest
```

Expected: PASS.

### Task 3: Frontend API And Feedback State

**Files:**
- Create: `frontend/src/services/productAnalytics.ts`
- Create: `frontend/src/components/feedback/FeedbackButton.tsx`
- Create: `frontend/src/services/productAnalytics.test.ts`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/services/productAnalytics.test.ts`:

```ts
import { describe, expect, it, vi, type Mock } from 'vitest'
import http from './api'
import { productAnalyticsApi, type FeedbackDraft } from './productAnalytics'
import { buildFeedbackButtonLabel, canSubmitFeedback } from '../components/feedback/FeedbackButton'

vi.mock('./api', () => ({
  default: {
    post: vi.fn((url: string, body: unknown) => Promise.resolve({ code: 0, message: 'success', data: { url, body } })),
    get: vi.fn((url: string) => Promise.resolve({ code: 0, message: 'success', data: { flagKey: url.split('/').pop(), enabled: false, rolloutPercent: 0 } })),
  },
}))

describe('productAnalyticsApi', () => {
  it('posts usage events with surface and action attributes', async () => {
    await productAnalyticsApi.recordEvent({
      eventType: 'PAGE',
      surface: 'canvas-editor',
      actionKey: 'open',
      actor: 'operator-1',
      attributes: { canvasId: 42 },
    })

    expect(http.post as unknown as Mock).toHaveBeenCalledWith('/product-analytics/events', {
      eventType: 'PAGE',
      surface: 'canvas-editor',
      actionKey: 'open',
      actor: 'operator-1',
      attributes: { canvasId: 42 },
    })
  })

  it('posts feedback drafts with NPS score and comment', async () => {
    const draft: FeedbackDraft = {
      surface: 'canvas-editor',
      featureKey: 'auto-save',
      npsScore: 9,
      comment: 'reliable',
      actor: 'operator-1',
    }

    await productAnalyticsApi.submitFeedback(draft)

    expect(http.post as unknown as Mock).toHaveBeenCalledWith('/product-analytics/feedback', draft)
  })

  it('returns disabled flag shape when backend flag is disabled', async () => {
    await productAnalyticsApi.flag('feedback_button')

    expect(http.get as unknown as Mock).toHaveBeenCalledWith('/product-analytics/flags/feedback_button')
  })
})

describe('FeedbackButton helpers', () => {
  it('labels loading, disabled, success, and ready states', () => {
    expect(buildFeedbackButtonLabel({ loading: true, disabled: false, submitted: false })).toBe('Submitting...')
    expect(buildFeedbackButtonLabel({ loading: false, disabled: true, submitted: false })).toBe('Feedback unavailable')
    expect(buildFeedbackButtonLabel({ loading: false, disabled: false, submitted: true })).toBe('Feedback sent')
    expect(buildFeedbackButtonLabel({ loading: false, disabled: false, submitted: false })).toBe('Send feedback')
  })

  it('allows only non-empty comments with NPS score from zero to ten', () => {
    expect(canSubmitFeedback({ comment: 'useful', npsScore: 0 })).toBe(true)
    expect(canSubmitFeedback({ comment: 'useful', npsScore: 10 })).toBe(true)
    expect(canSubmitFeedback({ comment: '', npsScore: 8 })).toBe(false)
    expect(canSubmitFeedback({ comment: 'useful', npsScore: 12 })).toBe(false)
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- productAnalytics.test.ts
```

Expected: FAIL because `productAnalytics.ts` and `FeedbackButton.tsx` do not exist.

- [ ] **Step 3: Implement product analytics API wrapper**

Create `frontend/src/services/productAnalytics.ts`:

```ts
import type { R } from '../types'
import http from './api'

export type ProductEventType = 'PAGE' | 'FEATURE' | 'ACTION'

export interface UsageEventDraft {
  eventType: ProductEventType
  surface: string
  actionKey: string
  actor?: string
  attributes: Record<string, unknown>
}

export interface FeedbackDraft {
  surface: string
  featureKey: string
  npsScore?: number
  comment?: string
  actor?: string
}

export interface FeatureFlag {
  tenantId?: number
  flagKey: string
  enabled: boolean
  rolloutPercent: number
}

export const productAnalyticsApi = {
  recordEvent: (body: UsageEventDraft) =>
    http.post<R<unknown>, R<unknown>>('/product-analytics/events', body),
  submitFeedback: (body: FeedbackDraft) =>
    http.post<R<unknown>, R<unknown>>('/product-analytics/feedback', body),
  flag: (flagKey: string) =>
    http.get<R<FeatureFlag>, R<FeatureFlag>>(`/product-analytics/flags/${flagKey}`),
}
```

- [ ] **Step 4: Implement feedback button state helper and component**

Create `frontend/src/components/feedback/FeedbackButton.tsx`:

```tsx
import { useState } from 'react'
import { Button } from 'antd'
import { productAnalyticsApi } from '../../services/productAnalytics'

export interface FeedbackButtonState {
  loading: boolean
  disabled: boolean
  submitted: boolean
}

export interface FeedbackDraftState {
  comment: string
  npsScore: number
}

export function buildFeedbackButtonLabel(state: FeedbackButtonState): string {
  if (state.loading) return 'Submitting...'
  if (state.disabled) return 'Feedback unavailable'
  if (state.submitted) return 'Feedback sent'
  return 'Send feedback'
}

export function canSubmitFeedback(draft: FeedbackDraftState): boolean {
  return draft.comment.trim().length > 0 && draft.npsScore >= 0 && draft.npsScore <= 10
}

export function FeedbackButton({ surface, featureKey, actor }: { surface: string; featureKey: string; actor?: string }) {
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  async function submit() {
    setLoading(true)
    try {
      await productAnalyticsApi.submitFeedback({ surface, featureKey, npsScore: 8, comment: 'quick feedback', actor })
      setSubmitted(true)
    } finally {
      setLoading(false)
    }
  }

  const disabled = submitted

  return (
    <Button size="small" loading={loading} disabled={disabled} onClick={submit}>
      {buildFeedbackButtonLabel({ loading, disabled, submitted })}
    </Button>
  )
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- productAnalytics.test.ts
```

Expected: PASS.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-009-product-usage-analytics-feedback-loop.md`
- Modify: `docs/product-evolution/plans/p2-009-product-usage-analytics-feedback-loop-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductAnalyticsServiceTest,ProductAnalyticsControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- productAnalytics.test.ts
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
- Feature flag: `feedback_button` controls the visible feedback entry point; leave disabled until the migration is applied and smoke tests pass.
- Migration: apply `V168__product_usage_analytics_feedback.sql` before enabling event writes.
- Tenant and role impact: every API path resolves tenant context from JWT claims; product events, feedback, flags, and alert rules are tenant scoped.
- Manual verification: open the canvas editor, confirm `GET /product-analytics/flags/feedback_button`, submit one feedback entry, and verify one row in `product_feedback` for the active tenant.
- Rollback: disable `feedback_button`; no destructive migration rollback is required because all schema changes are additive.
```

- [ ] **Step 5: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V168__product_usage_analytics_feedback.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProductAnalyticsController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/productanalytics/ProductAnalyticsServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ProductAnalyticsControllerTest.java \
  frontend/src/services/productAnalytics.ts \
  frontend/src/components/feedback/FeedbackButton.tsx \
  frontend/src/services/productAnalytics.test.ts \
  docs/product-evolution/specs/p2-009-product-usage-analytics-feedback-loop.md \
  docs/product-evolution/plans/p2-009-product-usage-analytics-feedback-loop-plan.md
git commit -m "feat: add product analytics feedback loop plan"
```

Expected: commit contains only the P2-009 implementation files and the matching spec/plan documentation.
