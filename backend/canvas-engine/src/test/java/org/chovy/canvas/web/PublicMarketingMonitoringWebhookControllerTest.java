package org.chovy.canvas.web;

import org.chovy.canvas.domain.monitoring.MarketingMonitorIngestResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicMarketingMonitoringWebhookControllerTest {

    @Test
    void ingestWebhookPassesTenantSourceHeadersAndRawBody() {
        MarketingMonitorWebhookIngestionService service = mock(MarketingMonitorWebhookIngestionService.class);
        String rawBody = "{\"id\":\"mention-1\",\"text\":\"bad support\"}";
        when(service.ingestWebhook(7L, "brandwatch", "1780704000", "sha256=abc", rawBody))
                .thenReturn(webhookResult());
        PublicMarketingMonitoringWebhookController controller =
                new PublicMarketingMonitoringWebhookController(service);

        StepVerifier.create(controller.ingestWebhook(
                        7L,
                        "brandwatch",
                        "1780704000",
                        "sha256=abc",
                        rawBody))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().sourceKey()).isEqualTo("brandwatch");
                    assertThat(response.getData().result().item().externalItemId()).isEqualTo("mention-1");
                })
                .verifyComplete();

        verify(service).ingestWebhook(7L, "brandwatch", "1780704000", "sha256=abc", rawBody);
    }

    private MarketingMonitorWebhookIngestView webhookResult() {
        MarketingMonitorItemView item = new MarketingMonitorItemView(
                100L,
                7L,
                10L,
                "mention-1",
                "GENERIC_SOCIAL",
                null,
                null,
                "our-brand",
                "bad support",
                null,
                now(),
                now(),
                Map.of("id", "mention-1"),
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                List.of());
        return new MarketingMonitorWebhookIngestView(
                7L,
                10L,
                "brandwatch",
                new MarketingMonitorIngestResult(item, null, List.of(), List.of()));
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }
}
