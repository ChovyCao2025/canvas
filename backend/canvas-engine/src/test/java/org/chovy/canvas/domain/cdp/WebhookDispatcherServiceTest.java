package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.WebhookSubscriptionDO;
import org.chovy.canvas.dal.mapper.WebhookDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.WebhookSubscriptionMapper;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebhookDispatcherServiceTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void dispatchSkipsSubscriptionsWithoutExactEventTypeMatch() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDeliveryLogMapper deliveryLogMapper = mock(WebhookDeliveryLogMapper.class);
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        WebhookDispatcherService dispatcher = service(subscriptionMapper, deliveryLogMapper, request);

        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription("[\"*\"]")));

        dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

        assertThat(request.get()).isNull();
        verify(deliveryLogMapper, never()).insert(any(WebhookDeliveryLogDO.class));
    }

    @Test
    void dispatchSignsCanonicalHeadersAndLogsSuccess() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDeliveryLogMapper deliveryLogMapper = mock(WebhookDeliveryLogMapper.class);
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        WebhookDispatcherService dispatcher = service(subscriptionMapper, deliveryLogMapper, request);

        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription("[\"cdp.event.ingested\"]")));

        dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

        ArgumentCaptor<WebhookDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(WebhookDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        WebhookDeliveryLogDO log = logCaptor.getValue();

        assertThat(request.get()).isNotNull();
        assertThat(request.get().headers().getFirst("X-Canvas-Event")).isEqualTo("cdp.event.ingested");
        assertThat(request.get().headers().getFirst("X-Canvas-Delivery")).isEqualTo(log.getDeliveryId());
        assertThat(request.get().headers().getFirst("X-Canvas-Timestamp")).isNotBlank();
        assertThat(request.get().headers().getFirst("X-Canvas-Signature")).startsWith("sha256=");
        assertThat(log.getStatus()).isEqualTo(WebhookDeliveryLogDO.SUCCESS);
        assertThat(log.getPayload()).contains("msg-1");
    }

    @Test
    void dispatchRetriesHttp429() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDeliveryLogMapper deliveryLogMapper = mock(WebhookDeliveryLogMapper.class);
        WebhookDispatcherService dispatcher = service(subscriptionMapper, deliveryLogMapper, new AtomicReference<>(), HttpStatus.TOO_MANY_REQUESTS);

        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription("[\"cdp.event.ingested\"]")));

        dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

        ArgumentCaptor<WebhookDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(WebhookDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(WebhookDeliveryLogDO.RETRYING);
        assertThat(logCaptor.getValue().getNextRetryAt()).isNotNull();
    }

    @Test
    void dispatchMarksNon429Http4xxAsFailed() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDeliveryLogMapper deliveryLogMapper = mock(WebhookDeliveryLogMapper.class);
        WebhookDispatcherService dispatcher = service(subscriptionMapper, deliveryLogMapper, new AtomicReference<>(), HttpStatus.BAD_REQUEST);

        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription("[\"cdp.event.ingested\"]")));

        dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

        ArgumentCaptor<WebhookDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(WebhookDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(WebhookDeliveryLogDO.FAILED);
        assertThat(logCaptor.getValue().getHttpStatus()).isEqualTo(400);
        assertThat(logCaptor.getValue().getNextRetryAt()).isNull();
    }

    @Test
    void dispatchSkipsInactiveSubscriptions() {
        WebhookSubscriptionMapper subscriptionMapper = mock(WebhookSubscriptionMapper.class);
        WebhookDeliveryLogMapper deliveryLogMapper = mock(WebhookDeliveryLogMapper.class);
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        WebhookDispatcherService dispatcher = service(subscriptionMapper, deliveryLogMapper, request);

        when(subscriptionMapper.selectList(any())).thenReturn(List.of());

        dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

        assertThat(request.get()).isNull();
        verifyNoInteractions(deliveryLogMapper);
    }

    private static WebhookDispatcherService service(WebhookSubscriptionMapper subscriptionMapper,
                                                    WebhookDeliveryLogMapper deliveryLogMapper,
                                                    AtomicReference<ClientRequest> request) {
        return service(subscriptionMapper, deliveryLogMapper, request, HttpStatus.ACCEPTED);
    }

    private static WebhookDispatcherService service(WebhookSubscriptionMapper subscriptionMapper,
                                                    WebhookDeliveryLogMapper deliveryLogMapper,
                                                    AtomicReference<ClientRequest> request,
                                                    HttpStatus status) {
        ExchangeFunction exchangeFunction = clientRequest -> {
            request.set(clientRequest);
            return Mono.just(ClientResponse.create(status).build());
        };
        return new WebhookDispatcherService(
                subscriptionMapper,
                deliveryLogMapper,
                new WebhookSignatureService(),
                new WebhookRetryPolicy(),
                new ObjectMapper(),
                WebClient.builder().exchangeFunction(exchangeFunction),
                SecretCipher.fromBase64Key(KEY));
    }

    private static WebhookSubscriptionDO subscription(String eventTypes) {
        SecretCipher cipher = SecretCipher.fromBase64Key(KEY);
        WebhookSubscriptionDO subscription = new WebhookSubscriptionDO();
        subscription.setId(7L);
        subscription.setTenantId(42L);
        subscription.setName("events");
        subscription.setCallbackUrl("https://hooks.example.test/canvas");
        subscription.setSecretCiphertext(cipher.encrypt("secret-123"));
        subscription.setEventTypes(eventTypes);
        subscription.setStatus(WebhookSubscriptionDO.ACTIVE);
        subscription.setMaxAttempts(3);
        return subscription;
    }
}
