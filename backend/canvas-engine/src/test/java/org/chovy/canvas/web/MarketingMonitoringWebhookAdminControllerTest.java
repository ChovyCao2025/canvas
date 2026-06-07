package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookSecretView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitoringWebhookAdminControllerTest {

    @Test
    void rotateSecretPassesCurrentTenantAndOperator() {
        MarketingMonitorWebhookIngestionService service = mock(MarketingMonitorWebhookIngestionService.class);
        when(service.rotateSecret(7L, 10L, "operator-1"))
                .thenReturn(secretView());
        MarketingMonitoringWebhookAdminController controller =
                new MarketingMonitoringWebhookAdminController(service, resolver());

        StepVerifier.create(controller.rotateSecret(10L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().sourceKey()).isEqualTo("brandwatch");
                    assertThat(response.getData().signingSecret()).isEqualTo("monwhsec_raw");
                })
                .verifyComplete();

        verify(service).rotateSecret(7L, 10L, "operator-1");
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private MarketingMonitorWebhookSecretView secretView() {
        return new MarketingMonitorWebhookSecretView(
                10L,
                7L,
                "brandwatch",
                "monwhsec_raw".substring(0, 12),
                "monwhsec_raw",
                "/public/marketing-monitoring/webhooks/7/brandwatch",
                300,
                "operator-1",
                LocalDateTime.of(2026, 6, 6, 8, 0));
    }
}
