package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDeliveryDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertDeliveryMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.domain.cdp.WebhookRetryPolicy;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MarketingMonitorAlertFanoutServiceTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertChannelStoresEncryptedSecretAndReturnsMaskedView() {
        Fixture fixture = fixture(HttpStatus.ACCEPTED);
        when(fixture.channelMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAlertChannelDO>getArgument(0).setId(11L);
            return 1;
        }).when(fixture.channelMapper).insert(any(MarketingMonitorAlertChannelDO.class));

        MarketingMonitorAlertChannelView view = fixture.service.upsertChannel(7L,
                new MarketingMonitorAlertChannelCommand(
                        "brand-duty",
                        "WEBHOOK",
                        "Brand Duty",
                        "https://hooks.example.test/brand-duty",
                        true,
                        "HIGH",
                        List.of("NEGATIVE_SENTIMENT"),
                        "CANVAS_HMAC",
                        "fanout-secret",
                        Map.of("owner", "brand-team"),
                        4),
                "operator-1");

        ArgumentCaptor<MarketingMonitorAlertChannelDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertChannelDO.class);
        verify(fixture.channelMapper).insert(captor.capture());
        MarketingMonitorAlertChannelDO inserted = captor.getValue();
        assertThat(inserted.getTenantId()).isEqualTo(7L);
        assertThat(inserted.getChannelKey()).isEqualTo("brand-duty");
        assertThat(inserted.getChannelType()).isEqualTo("WEBHOOK");
        assertThat(inserted.getSecretCiphertext()).doesNotContain("fanout-secret");
        assertThat(fixture.secretCipher.decrypt(inserted.getSecretCiphertext())).isEqualTo("fanout-secret");
        assertThat(fixture.passwordEncoder.matches("fanout-secret", inserted.getSecretHash())).isTrue();
        assertThat(view.id()).isEqualTo(11L);
        assertThat(view.secretPrefix()).isEqualTo("fanout-secre");
        assertThat(view.metadata()).containsEntry("owner", "brand-team");
    }

    @Test
    void dispatchAlertSendsGenericWebhookWithCanvasHmacHeadersAndLogsSuccess() {
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        Fixture fixture = fixture(HttpStatus.ACCEPTED, request);
        when(fixture.alertMapper.selectById(401L)).thenReturn(alert(7L, 401L, "OPEN", "HIGH"));
        when(fixture.channelMapper.selectList(any())).thenReturn(List.of(channel("WEBHOOK", "CANVAS_HMAC")));
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAlertDeliveryDO>getArgument(0).setId(900L);
            return 1;
        }).when(fixture.deliveryMapper).insert(any(MarketingMonitorAlertDeliveryDO.class));

        MarketingMonitorAlertDispatchView result = fixture.service.dispatchAlert(7L, 401L, "operator-1");

        ArgumentCaptor<MarketingMonitorAlertDeliveryDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDeliveryDO.class);
        verify(fixture.deliveryMapper).insert(captor.capture());
        MarketingMonitorAlertDeliveryDO delivery = captor.getValue();
        assertThat(result.attempted()).isEqualTo(1);
        assertThat(result.delivered()).isEqualTo(1);
        assertThat(request.get().headers().getFirst("X-Canvas-Event"))
                .isEqualTo("marketing.monitor.alert.opened");
        assertThat(request.get().headers().getFirst("X-Canvas-Delivery"))
                .isEqualTo(delivery.getDeliveryId());
        assertThat(request.get().headers().getFirst("X-Canvas-Signature")).startsWith("sha256=");
        assertThat(delivery.getStatus()).isEqualTo("SUCCESS");
        assertThat(delivery.getRequestPayload()).contains("Negative sentiment detected", "operator-1");
    }

    @Test
    void dispatchAlertFormatsFeishuSignedTextPayload() {
        Fixture fixture = fixture(HttpStatus.OK);
        when(fixture.alertMapper.selectById(401L)).thenReturn(alert(7L, 401L, "OPEN", "HIGH"));
        when(fixture.channelMapper.selectList(any())).thenReturn(List.of(channel("FEISHU", "FEISHU_BOT")));

        MarketingMonitorAlertDispatchView result = fixture.service.dispatchAlert(7L, 401L, "operator-1");

        ArgumentCaptor<MarketingMonitorAlertDeliveryDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDeliveryDO.class);
        verify(fixture.deliveryMapper).insert(captor.capture());
        assertThat(result.delivered()).isEqualTo(1);
        assertThat(captor.getValue().getRequestPayload())
                .contains("\"timestamp\":\"1780704000\"")
                .contains("\"sign\"")
                .contains("\"msg_type\":\"text\"")
                .contains("Negative sentiment detected");
    }

    @Test
    void dispatchAlertSkipsResolvedAlertsAndNonMatchingChannels() {
        Fixture fixture = fixture(HttpStatus.ACCEPTED);
        when(fixture.alertMapper.selectById(401L)).thenReturn(alert(7L, 401L, "RESOLVED", "HIGH"));

        MarketingMonitorAlertDispatchView resolved = fixture.service.dispatchAlert(7L, 401L, "operator-1");

        assertThat(resolved.attempted()).isZero();
        verifyNoInteractions(fixture.deliveryMapper);

        when(fixture.alertMapper.selectById(402L)).thenReturn(alert(7L, 402L, "OPEN", "LOW"));
        when(fixture.channelMapper.selectList(any())).thenReturn(List.of(channel("WEBHOOK", "CANVAS_HMAC")));

        MarketingMonitorAlertDispatchView filtered = fixture.service.dispatchAlert(7L, 402L, "operator-1");

        assertThat(filtered.attempted()).isZero();
        verify(fixture.deliveryMapper, never()).insert(any(MarketingMonitorAlertDeliveryDO.class));
    }

    @Test
    void dispatchAlertLogsHardFailureForNonRetryableHttpStatus() {
        Fixture fixture = fixture(HttpStatus.BAD_REQUEST);
        when(fixture.alertMapper.selectById(401L)).thenReturn(alert(7L, 401L, "OPEN", "HIGH"));
        when(fixture.channelMapper.selectList(any())).thenReturn(List.of(channel("WEBHOOK", "NONE")));

        MarketingMonitorAlertDispatchView result = fixture.service.dispatchAlert(7L, 401L, "operator-1");

        ArgumentCaptor<MarketingMonitorAlertDeliveryDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDeliveryDO.class);
        verify(fixture.deliveryMapper).insert(captor.capture());
        assertThat(result.delivered()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(captor.getValue().getHttpStatus()).isEqualTo(400);
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getTerminalReason()).isEqualTo("HTTP_400");
    }

    @Test
    void deliveriesAreTenantScopedFilteredAndBounded() {
        Fixture fixture = fixture(HttpStatus.ACCEPTED);
        when(fixture.deliveryMapper.selectList(any())).thenReturn(List.of(
                delivery(7L, 900L, 401L, "SUCCESS"),
                delivery(99L, 901L, 401L, "SUCCESS"),
                delivery(7L, 902L, 402L, "FAILED")));

        List<MarketingMonitorAlertDeliveryView> views =
                fixture.service.deliveries(7L, 401L, "SUCCESS", 500);

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.tenantId()).isEqualTo(7L);
            assertThat(view.alertId()).isEqualTo(401L);
            assertThat(view.status()).isEqualTo("SUCCESS");
        });
    }

    private Fixture fixture(HttpStatus status) {
        return fixture(status, new AtomicReference<>());
    }

    private Fixture fixture(HttpStatus status, AtomicReference<ClientRequest> request) {
        MarketingMonitorAlertChannelMapper channelMapper = mock(MarketingMonitorAlertChannelMapper.class);
        MarketingMonitorAlertDeliveryMapper deliveryMapper = mock(MarketingMonitorAlertDeliveryMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        SecretCipher secretCipher = SecretCipher.fromBase64Key(KEY);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        ExchangeFunction exchangeFunction = clientRequest -> {
            request.set(clientRequest);
            return Mono.just(ClientResponse.create(status).build());
        };
        return new Fixture(
                channelMapper,
                deliveryMapper,
                alertMapper,
                secretCipher,
                passwordEncoder,
                new MarketingMonitorAlertFanoutService(
                        channelMapper,
                        deliveryMapper,
                        alertMapper,
                        new WebhookRetryPolicy(),
                        new ObjectMapper(),
                        WebClient.builder().exchangeFunction(exchangeFunction),
                        secretCipher,
                        passwordEncoder,
                        CLOCK));
    }

    private MarketingMonitorAlertChannelDO channel(String channelType, String signingMode) {
        SecretCipher cipher = SecretCipher.fromBase64Key(KEY);
        MarketingMonitorAlertChannelDO row = new MarketingMonitorAlertChannelDO();
        row.setId(11L);
        row.setTenantId(7L);
        row.setChannelKey("brand-duty");
        row.setChannelType(channelType);
        row.setDisplayName("Brand Duty");
        row.setEndpointUrl("https://hooks.example.test/brand-duty");
        row.setEnabled(1);
        row.setMinSeverity("HIGH");
        row.setAlertTypesJson("[\"NEGATIVE_SENTIMENT\"]");
        row.setSigningMode(signingMode);
        row.setSecretPrefix("fanout-secre");
        row.setSecretHash(new BCryptPasswordEncoder().encode("fanout-secret"));
        row.setSecretCiphertext(cipher.encrypt("fanout-secret"));
        row.setMetadataJson("{\"owner\":\"brand-team\"}");
        row.setMaxAttempts(3);
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private MarketingMonitorAlertDO alert(Long tenantId, Long id, String status, String severity) {
        MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAlertType("NEGATIVE_SENTIMENT");
        row.setSeverity(severity);
        row.setStatus(status);
        row.setScopeKey("our-brand");
        row.setTitle("Negative sentiment detected");
        row.setReason("Detected negative sentiment");
        row.setItemCount(1);
        row.setWindowStart(now());
        row.setWindowEnd(now());
        row.setMetadataJson("{\"itemId\":100}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private MarketingMonitorAlertDeliveryDO delivery(Long tenantId, Long id, Long alertId, String status) {
        MarketingMonitorAlertDeliveryDO row = new MarketingMonitorAlertDeliveryDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAlertId(alertId);
        row.setChannelId(11L);
        row.setDeliveryId("delivery-" + id);
        row.setAttempt(1);
        row.setHttpStatus(202);
        row.setStatus(status);
        row.setRequestPayload("{\"event\":\"marketing.monitor.alert.opened\"}");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }

    private record Fixture(MarketingMonitorAlertChannelMapper channelMapper,
                           MarketingMonitorAlertDeliveryMapper deliveryMapper,
                           MarketingMonitorAlertMapper alertMapper,
                           SecretCipher secretCipher,
                           BCryptPasswordEncoder passwordEncoder,
                           MarketingMonitorAlertFanoutService service) {
    }
}
