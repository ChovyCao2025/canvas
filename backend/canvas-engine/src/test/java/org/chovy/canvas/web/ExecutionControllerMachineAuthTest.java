package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.security.PublicTriggerAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExecutionControllerMachineAuthTest {

    private static final String SECRET = "machine-secret-at-least-32-bytes-long";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-03T06:00:00Z"), ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void directCallRejectsUnsignedMachineRequest() {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = newController(executionService, disruptorService);

        assertThatThrownBy(() -> controller.directCall(request(new HttpHeaders()), 10L,
                        Mono.just("{\"userId\":\"u1\",\"idempotencyKey\":\"idem-1\"}")).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
        verifyNoInteractions(executionService, disruptorService);
    }

    @Test
    void directCallAcceptsSignedMachineRequestAndUsesBodyUserIdWhenNoLoginContextExists() {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = newController(executionService, disruptorService);
        String body = "{\"userId\":\"u1\",\"inputParams\":{\"amount\":100},\"idempotencyKey\":\"idem-1\"}";
        when(executionService.trigger(eq(10L), eq("u1"), eq(NodeType.DIRECT_CALL), eq(NodeType.DIRECT_CALL),
                eq(null), eq(Map.of("amount", 100)), eq("idem-1"), eq(false)))
                .thenReturn(Mono.just(Map.of("ok", true)));

        var response = controller.directCall(request(signedHeaders(body)), 10L, Mono.just(body)).block();

        assertThat(response.getData()).containsEntry("ok", true);
        verify(executionService).trigger(eq(10L), eq("u1"), eq(NodeType.DIRECT_CALL), eq(NodeType.DIRECT_CALL),
                eq(null), eq(Map.of("amount", 100)), eq("idem-1"), eq(false));
    }

    @Test
    void behaviorTriggerRequiresSignedMachineRequest() {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = newController(executionService, disruptorService);

        assertThatThrownBy(() -> controller.behaviorTrigger(request(new HttpHeaders()), Mono.just("{}")).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
        verifyNoInteractions(executionService, disruptorService);
    }

    private ExecutionController newController(CanvasExecutionService executionService,
                                              CanvasDisruptorService disruptorService) {
        return new ExecutionController(executionService, disruptorService,
                new PublicTriggerAuthService(SECRET, CLOCK), objectMapper);
    }

    private org.springframework.http.server.reactive.ServerHttpRequest request(HttpHeaders headers) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.post("/canvas/execute/direct/10");
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        return builder.build();
    }

    private HttpHeaders signedHeaders(String body) {
        String timestamp = String.valueOf(CLOCK.millis());
        HttpHeaders headers = new HttpHeaders();
        headers.add(EventReportAuthService.TIMESTAMP_HEADER, timestamp);
        headers.add(EventReportAuthService.SIGNATURE_HEADER, "sha256=" + hmac(timestamp + "\n" + body));
        return headers;
    }

    private String hmac(String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
