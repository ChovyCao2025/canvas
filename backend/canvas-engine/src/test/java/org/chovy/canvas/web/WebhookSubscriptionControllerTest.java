package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.WebhookSubscriptionDO;
import org.chovy.canvas.dal.mapper.WebhookDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.WebhookSubscriptionMapper;
import org.chovy.canvas.domain.cdp.WebhookDispatcherService;
import org.chovy.canvas.domain.cdp.WebhookSubscriptionValidator;
import org.chovy.canvas.dto.webhook.WebhookDeliveryDTO;
import org.chovy.canvas.dto.webhook.WebhookRotateSecretResp;
import org.chovy.canvas.dto.webhook.WebhookSubscriptionDTO;
import org.chovy.canvas.dto.webhook.WebhookSubscriptionReq;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookSubscriptionControllerTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void createRejectsLocalhostCallbackUrl() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookSubscriptionController controller = controller(subscriptionMapper);
        WebhookSubscriptionReq req = new WebhookSubscriptionReq(
                "CRM", "http://localhost:8080/hook", List.of("cdp.event.ingested"), 3);

        assertThatThrownBy(() -> controller.create(req).block())
                .hasMessageContaining("callbackUrl is not allowed");
        verify(subscriptionMapper, never()).insert(any(WebhookSubscriptionDO.class));
    }

    @Test
    void createPersistsActiveSubscriptionWithEventTypesWithoutReturningRawSecret() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookSubscriptionController controller = controller(subscriptionMapper);
        WebhookSubscriptionReq req = new WebhookSubscriptionReq(
                " CRM ", "https://example.com/hook", List.of("cdp.event.ingested"), 3);

        WebhookSubscriptionDTO dto = controller.create(req).block().getData();

        ArgumentCaptor<WebhookSubscriptionDO> captor = ArgumentCaptor.forClass(WebhookSubscriptionDO.class);
        verify(subscriptionMapper).insert(captor.capture());
        WebhookSubscriptionDO row = captor.getValue();
        assertThat(row.getTenantId()).isEqualTo(42L);
        assertThat(row.getName()).isEqualTo("CRM");
        assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.ACTIVE);
        assertThat(row.getEventTypes()).contains("cdp.event.ingested");
        assertThat(row.getSecretPrefix()).startsWith("whsec_");
        assertThat(row.getSecretHash()).isNotBlank();
        assertThat(row.getSecretCiphertext()).isNotBlank();
        assertThat(dto.secretPrefix()).isEqualTo(row.getSecretPrefix());
    }

    @Test
    void pauseResumeAndDisableChangeStatus() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookSubscriptionDO row = row(9L, WebhookSubscriptionDO.ACTIVE);
        when(subscriptionMapper.selectById(9L)).thenReturn(row);
        WebhookSubscriptionController controller = controller(subscriptionMapper);

        controller.pause(9L).block();
        assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.PAUSED);

        controller.resume(9L).block();
        assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.ACTIVE);

        controller.disable(9L).block();
        assertThat(row.getStatus()).isEqualTo(WebhookSubscriptionDO.DISABLED);
        verify(subscriptionMapper, org.mockito.Mockito.times(3)).updateById(row);
    }

    @Test
    void rotateSecretReturnsRawSecretOnce() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookSubscriptionDO row = row(9L, WebhookSubscriptionDO.ACTIVE);
        when(subscriptionMapper.selectById(9L)).thenReturn(row);
        WebhookSubscriptionController controller = controller(subscriptionMapper);

        WebhookRotateSecretResp resp = controller.rotateSecret(9L).block().getData();

        assertThat(resp.subscriptionId()).isEqualTo(9L);
        assertThat(resp.secret()).startsWith("whsec_");
        assertThat(resp.secretPrefix()).isEqualTo(resp.secret().substring(0, 12));
        assertThat(row.getSecretPrefix()).isEqualTo(resp.secretPrefix());
        assertThat(row.getSecretCiphertext()).doesNotContain(resp.secret());
        verify(subscriptionMapper).updateById(row);
    }

    @Test
    void testDeliveryUsesDispatcher() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDispatcherService dispatcher = mock(WebhookDispatcherService.class);
        when(subscriptionMapper.selectById(9L)).thenReturn(row(9L, WebhookSubscriptionDO.ACTIVE));
        WebhookSubscriptionController controller = controller(subscriptionMapper, mock(WebhookDeliveryLogMapper.class), dispatcher);

        controller.testDelivery(9L).block();

        verify(dispatcher).sendOnce(any(WebhookSubscriptionDO.class), eq("webhook.test"), anyMap(), anyString(), eq(1));
    }

    @Test
    void deliveriesListMapsRowsWithoutPayloadOrResponseBody() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDeliveryLogMapper deliveryLogMapper = mock(WebhookDeliveryLogMapper.class);
        when(subscriptionMapper.selectById(9L)).thenReturn(row(9L, WebhookSubscriptionDO.ACTIVE));
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(delivery(91L)));
        WebhookSubscriptionController controller = controller(subscriptionMapper, deliveryLogMapper, mock(WebhookDispatcherService.class));

        List<WebhookDeliveryDTO> deliveries = controller.deliveries(9L).block().getData();

        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).deliveryId()).isEqualTo("delivery-91");
        assertThat(deliveries.get(0).status()).isEqualTo(WebhookDeliveryLogDO.DEAD);
    }

    private WebhookSubscriptionController controller(WebhookSubscriptionMapper subscriptionMapper) {
        return controller(subscriptionMapper, mock(WebhookDeliveryLogMapper.class), mock(WebhookDispatcherService.class));
    }

    private WebhookSubscriptionController controller(WebhookSubscriptionMapper subscriptionMapper,
                                                     WebhookDeliveryLogMapper deliveryLogMapper,
                                                     WebhookDispatcherService dispatcher) {
        return new WebhookSubscriptionController(
                tenantResolver(42L, "alice"),
                subscriptionMapper,
                deliveryLogMapper,
                new WebhookSubscriptionValidator(),
                dispatcher,
                new ObjectMapper(),
                new BCryptPasswordEncoder(),
                SecretCipher.fromBase64Key(KEY));
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }

    private WebhookSubscriptionDO row(Long id, String status) {
        WebhookSubscriptionDO row = new WebhookSubscriptionDO();
        row.setId(id);
        row.setTenantId(42L);
        row.setName("CRM");
        row.setCallbackUrl("https://example.com/hook");
        row.setSecretPrefix("whsec_old_1");
        row.setSecretHash("hash");
        row.setSecretCiphertext("ciphertext");
        row.setEventTypes("[\"cdp.event.ingested\"]");
        row.setStatus(status);
        row.setMaxAttempts(3);
        row.setCreatedAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        row.setUpdatedAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        return row;
    }

    private WebhookDeliveryLogDO delivery(Long id) {
        WebhookDeliveryLogDO row = new WebhookDeliveryLogDO();
        row.setId(id);
        row.setTenantId(42L);
        row.setSubscriptionId(9L);
        row.setDeliveryId("delivery-" + id);
        row.setEventType("cdp.event.ingested");
        row.setAttempt(3);
        row.setHttpStatus(500);
        row.setStatus(WebhookDeliveryLogDO.DEAD);
        row.setTerminalReason("MAX_ATTEMPTS_REACHED");
        row.setCreatedAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        return row;
    }
}
