package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorAnomalyControllerTest {

    @Test
    void writeEndpointsPassCurrentTenantAndOperator() {
        MarketingMonitorAnomalyDetectionService service = mock(MarketingMonitorAnomalyDetectionService.class);
        MarketingMonitorAnomalyRuleCommand ruleCommand = ruleCommand();
        MarketingMonitorAnomalyDetectionCommand detectionCommand = detectionCommand();
        when(service.upsertRule(7L, ruleCommand, "operator-1")).thenReturn(ruleView());
        when(service.detect(7L, detectionCommand, "operator-1")).thenReturn(detectionView());
        when(service.resolveEvent(7L, 70L, "operator-1")).thenReturn(resolvedEventView());
        MarketingMonitorAnomalyController controller = new MarketingMonitorAnomalyController(service, resolver());

        StepVerifier.create(controller.upsertRule(ruleCommand))
                .assertNext(response -> assertThat(response.getData().metricKey()).isEqualTo("NEGATIVE_COUNT"))
                .verifyComplete();
        StepVerifier.create(controller.detect(detectionCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ANOMALY_DETECTED"))
                .verifyComplete();
        StepVerifier.create(controller.resolveEvent(70L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("RESOLVED"))
                .verifyComplete();

        verify(service).upsertRule(7L, ruleCommand, "operator-1");
        verify(service).detect(7L, detectionCommand, "operator-1");
        verify(service).resolveEvent(7L, 70L, "operator-1");
    }

    @Test
    void eventsEndpointPassesFiltersAndBoundsLimit() {
        MarketingMonitorAnomalyDetectionService service = mock(MarketingMonitorAnomalyDetectionService.class);
        MarketingMonitorAnomalyEventQuery query = new MarketingMonitorAnomalyEventQuery(60L, "OPEN", 100);
        when(service.events(7L, query)).thenReturn(List.of(eventView()));
        MarketingMonitorAnomalyController controller = new MarketingMonitorAnomalyController(service, resolver());

        StepVerifier.create(controller.events(60L, "OPEN", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(event -> assertThat(event.id()).isEqualTo(70L)))
                .verifyComplete();

        verify(service).events(7L, query);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private MarketingMonitorAnomalyRuleCommand ruleCommand() {
        return new MarketingMonitorAnomalyRuleCommand(
                "negative-spike",
                "Negative mention spike",
                10L,
                "NEGATIVE_COUNT",
                "DAY",
                "our-brand",
                "",
                "SPIKE",
                14,
                5,
                new BigDecimal("3.0000"),
                new BigDecimal("5.000000"),
                true,
                Map.of());
    }

    private MarketingMonitorAnomalyDetectionCommand detectionCommand() {
        return new MarketingMonitorAnomalyDetectionCommand(60L, now(), now().plusDays(1));
    }

    private MarketingMonitorAnomalyRuleView ruleView() {
        return new MarketingMonitorAnomalyRuleView(60L, 7L, "negative-spike", "Negative mention spike",
                10L, "NEGATIVE_COUNT", "DAY", "our-brand", "", "SPIKE",
                14, 5, new BigDecimal("3.0000"), new BigDecimal("5.000000"), true,
                Map.of(), "operator-1", now(), now());
    }

    private MarketingMonitorAnomalyDetectionView detectionView() {
        return new MarketingMonitorAnomalyDetectionView(7L, 60L, "negative-spike", "NEGATIVE_COUNT",
                "ANOMALY_DETECTED", 5, new BigDecimal("50.000000"),
                new BigDecimal("11.000000"), new BigDecimal("1.000000"), new BigDecimal("26.305500"),
                new BigDecimal("39.000000"), eventView());
    }

    private MarketingMonitorAnomalyEventView eventView() {
        return new MarketingMonitorAnomalyEventView(70L, 7L, 60L, "negative-spike", 10L,
                "brandwatch", "NEGATIVE_COUNT", "DAY", now(), now().plusDays(1),
                "our-brand", "", new BigDecimal("50.000000"), new BigDecimal("11.000000"),
                new BigDecimal("1.000000"), new BigDecimal("26.305500"), new BigDecimal("39.000000"),
                "SPIKE", "CRITICAL", "OPEN", Map.of(), "operator-1", null, null, now(), now());
    }

    private MarketingMonitorAnomalyEventView resolvedEventView() {
        return new MarketingMonitorAnomalyEventView(70L, 7L, 60L, "negative-spike", 10L,
                "brandwatch", "NEGATIVE_COUNT", "DAY", now(), now().plusDays(1),
                "our-brand", "", new BigDecimal("50.000000"), new BigDecimal("11.000000"),
                new BigDecimal("1.000000"), new BigDecimal("26.305500"), new BigDecimal("39.000000"),
                "SPIKE", "CRITICAL", "RESOLVED", Map.of(), "operator-1", "operator-1", now(), now(), now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }
}
