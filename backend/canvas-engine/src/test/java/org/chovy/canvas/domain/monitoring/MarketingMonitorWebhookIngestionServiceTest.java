package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorWebhookIngestionServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneOffset.UTC);
    private static final String CIPHER_KEY = Base64.getEncoder()
            .encodeToString("12345678901234567890123456789012".getBytes());

    @Test
    void rotateSecretStoresEncryptedSecretAndReturnsRawValueOnce() {
        Fixture fixture = fixture();
        MarketingMonitorSourceDO source = source("brandwatch", "{}");
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source);
        ArgumentCaptor<MarketingMonitorSourceDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorSourceDO.class);

        MarketingMonitorWebhookSecretView view = fixture.service.rotateSecret(7L, 10L, "operator-1");

        verify(fixture.sourceMapper).updateById(captor.capture());
        MarketingMonitorSourceDO updated = captor.getValue();
        assertThat(view.sourceId()).isEqualTo(10L);
        assertThat(view.sourceKey()).isEqualTo("brandwatch");
        assertThat(view.signingSecret()).startsWith("monwhsec_");
        assertThat(view.secretPrefix()).isEqualTo(view.signingSecret().substring(0, 12));
        assertThat(view.endpointPath()).isEqualTo("/public/marketing-monitoring/webhooks/7/brandwatch");
        assertThat(updated.getWebhookEnabled()).isEqualTo(1);
        assertThat(updated.getWebhookSecretPrefix()).isEqualTo(view.secretPrefix());
        assertThat(updated.getWebhookSecretHash()).doesNotContain(view.signingSecret());
        assertThat(updated.getWebhookSecretCiphertext()).doesNotContain(view.signingSecret());
        assertThat(fixture.secretCipher.decrypt(updated.getWebhookSecretCiphertext()))
                .isEqualTo(view.signingSecret());
        assertThat(updated.getWebhookSignatureToleranceSeconds()).isEqualTo(300);
    }

    @Test
    void validSignedWebhookMapsPayloadAndDelegatesToMonitoringIngest() {
        Fixture fixture = fixture();
        String rawSecret = "monwhsec_test-secret";
        MarketingMonitorSourceDO source = source("brandwatch",
                "{\"defaultBrandKey\":\"our-brand\",\"competitors\":{\"competitorx\":[\"CompetitorX\"]}}");
        source.setWebhookEnabled(1);
        source.setWebhookSecretCiphertext(fixture.secretCipher.encrypt(rawSecret));
        source.setWebhookSignatureToleranceSeconds(300);
        when(fixture.sourceMapper.selectOne(any())).thenReturn(source);
        when(fixture.monitoringService.ingestItem(eq(7L), any(), eq("monitor-webhook:brandwatch")))
                .thenReturn(ingestResult());
        String rawBody = "{\"id\":\"mention-1\",\"message\":\"CompetitorX has bad support\"}";
        String timestamp = String.valueOf(CLOCK.instant().getEpochSecond());
        String signature = fixture.signatureService.sign(rawSecret, timestamp, rawBody);
        ArgumentCaptor<MarketingMonitorItemIngestCommand> commandCaptor =
                ArgumentCaptor.forClass(MarketingMonitorItemIngestCommand.class);

        MarketingMonitorWebhookIngestView view = fixture.service.ingestWebhook(
                7L,
                "brandwatch",
                timestamp,
                signature,
                rawBody);

        assertThat(view.tenantId()).isEqualTo(7L);
        assertThat(view.sourceKey()).isEqualTo("brandwatch");
        assertThat(view.result().item().externalItemId()).isEqualTo("mention-1");
        verify(fixture.monitoringService).ingestItem(eq(7L), commandCaptor.capture(),
                eq("monitor-webhook:brandwatch"));
        assertThat(commandCaptor.getValue().brandKey()).isEqualTo("our-brand");
        assertThat(commandCaptor.getValue().competitors())
                .containsEntry("competitorx", List.of("CompetitorX"));
    }

    @Test
    void rejectsStaleTimestampAndInvalidSignatureBeforeIngest() {
        Fixture fixture = fixture();
        String rawSecret = "monwhsec_test-secret";
        MarketingMonitorSourceDO source = source("brandwatch", "{}");
        source.setWebhookEnabled(1);
        source.setWebhookSecretCiphertext(fixture.secretCipher.encrypt(rawSecret));
        source.setWebhookSignatureToleranceSeconds(300);
        when(fixture.sourceMapper.selectOne(any())).thenReturn(source);
        String rawBody = "{\"id\":\"mention-1\",\"text\":\"bad support\"}";
        String staleTimestamp = String.valueOf(CLOCK.instant().minusSeconds(301).getEpochSecond());
        String staleSignature = fixture.signatureService.sign(rawSecret, staleTimestamp, rawBody);

        assertThatThrownBy(() -> fixture.service.ingestWebhook(
                7L,
                "brandwatch",
                staleTimestamp,
                staleSignature,
                rawBody))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        String timestamp = String.valueOf(CLOCK.instant().getEpochSecond());
        assertThatThrownBy(() -> fixture.service.ingestWebhook(
                7L,
                "brandwatch",
                timestamp,
                "sha256=invalid",
                rawBody))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(fixture.monitoringService, never()).ingestItem(any(), any(), any());
    }

    private Fixture fixture() {
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitoringService monitoringService = mock(MarketingMonitoringService.class);
        SecretCipher secretCipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        MarketingMonitorWebhookSignatureService signatureService =
                new MarketingMonitorWebhookSignatureService(CLOCK);
        return new Fixture(
                sourceMapper,
                monitoringService,
                secretCipher,
                signatureService,
                new MarketingMonitorWebhookIngestionService(
                        sourceMapper,
                        monitoringService,
                        new MarketingMonitorWebhookPayloadMapper(new ObjectMapper()),
                        signatureService,
                        secretCipher,
                        new BCryptPasswordEncoder(),
                        new ObjectMapper(),
                        CLOCK));
    }

    private MarketingMonitorSourceDO source(String sourceKey, String metadataJson) {
        MarketingMonitorSourceDO source = new MarketingMonitorSourceDO();
        source.setId(10L);
        source.setTenantId(7L);
        source.setSourceKey(sourceKey);
        source.setSourceType("GENERIC_SOCIAL");
        source.setDisplayName("Brandwatch");
        source.setEnabled(1);
        source.setMetadataJson(metadataJson);
        source.setCreatedBy("operator-1");
        source.setCreatedAt(now());
        source.setUpdatedAt(now());
        return source;
    }

    private MarketingMonitorIngestResult ingestResult() {
        MarketingMonitorItemView item = new MarketingMonitorItemView(
                100L,
                7L,
                10L,
                "mention-1",
                "GENERIC_SOCIAL",
                null,
                null,
                "our-brand",
                "CompetitorX has bad support",
                null,
                now(),
                now(),
                Map.of("id", "mention-1"),
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                List.of("competitorx"));
        MarketingSentimentAnalysisView sentiment = new MarketingSentimentAnalysisView(
                200L,
                7L,
                100L,
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                MarketingMonitoringService.SENTIMENT_MODEL_KEY,
                "lexicon_v1",
                Map.of("negative", List.of("bad"), "positive", List.of()),
                now());
        return new MarketingMonitorIngestResult(item, sentiment, List.of(), List.of());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }

    private record Fixture(MarketingMonitorSourceMapper sourceMapper,
                           MarketingMonitoringService monitoringService,
                           SecretCipher secretCipher,
                           MarketingMonitorWebhookSignatureService signatureService,
                           MarketingMonitorWebhookIngestionService service) {
    }
}
