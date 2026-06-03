package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.security.PublicTriggerAuthService;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionControllerTest {

    private static final String PUBLIC_TRIGGER_SECRET = "0123456789abcdef0123456789abcdef";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void dryRunUsesAuthenticatedSubjectInsteadOfRequestUserId() {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = new ExecutionController(
                executionService,
                disruptorService,
                new PublicTriggerAuthService(PUBLIC_TRIGGER_SECRET, CLOCK),
                new ObjectMapper());
        ExecutionController.DirectCallReq req = new ExecutionController.DirectCallReq();
        req.setUserId("forged-user");
        req.setInputParams(Map.of("amount", 100));
        req.setGraphJson("{\"nodes\":[]}");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("auth-user-7");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());
        when(executionService.triggerDryRun(eq(10L), eq("auth-user-7"), eq(Map.of("amount", 100)), eq("{\"nodes\":[]}")))
                .thenReturn(Mono.just(Map.of("ok", true)));

        var response = controller.dryRun(10L, req)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData()).containsEntry("ok", true);
        verify(executionService).triggerDryRun(eq(10L), eq("auth-user-7"), eq(Map.of("amount", 100)), eq("{\"nodes\":[]}"));
    }

    @Test
    void directCallVerifiesHmacBeforeUsingSignedUserId() throws Exception {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = new ExecutionController(
                executionService,
                disruptorService,
                new PublicTriggerAuthService(PUBLIC_TRIGGER_SECRET, CLOCK),
                new ObjectMapper());
        String body = """
                {"userId":"signed-user","inputParams":{"amount":100},"idempotencyKey":"idem-1"}
                """;
        String timestamp = String.valueOf(CLOCK.millis());
        MockServerHttpRequest request = MockServerHttpRequest.post("/canvas/execute/direct/10")
                .header(EventReportAuthService.TIMESTAMP_HEADER, timestamp)
                .header(EventReportAuthService.SIGNATURE_HEADER, hmac(timestamp + "\n" + body))
                .body(body);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("auth-user-7");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());
        when(executionService.trigger(eq(10L), eq("signed-user"), eq(NodeType.DIRECT_CALL),
                eq(NodeType.DIRECT_CALL), eq(null), eq(Map.of("amount", 100)), eq("idem-1"), eq(false)))
                .thenReturn(Mono.just(Map.of("ok", true)));

        var response = controller.directCall(request, 10L, Mono.just(body))
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData()).containsEntry("ok", true);
        verify(executionService).trigger(eq(10L), eq("signed-user"), eq(NodeType.DIRECT_CALL),
                eq(NodeType.DIRECT_CALL), eq(null), eq(Map.of("amount", 100)), eq("idem-1"), eq(false));
    }

    @Test
    void behaviorTriggerVerifiesHmacBeforePublishingSignedUserId() throws Exception {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = new ExecutionController(
                executionService,
                disruptorService,
                new PublicTriggerAuthService(PUBLIC_TRIGGER_SECRET, CLOCK),
                new ObjectMapper());
        String body = """
                {"canvasId":20,"userId":"signed-user","eventCode":"LOGIN","eventId":"evt-1","behaviorData":{"channel":"web"}}
                """;
        String timestamp = String.valueOf(CLOCK.millis());
        MockServerHttpRequest request = MockServerHttpRequest.post("/canvas/trigger/behavior")
                .header(EventReportAuthService.TIMESTAMP_HEADER, timestamp)
                .header(EventReportAuthService.SIGNATURE_HEADER, hmac(timestamp + "\n" + body))
                .body(body);

        var response = controller.behaviorTrigger(request, Mono.just(body)).block();

        assertThat(response.getCode()).isZero();
        verify(disruptorService).publish(
                eq(20L), eq("signed-user"), eq("BEHAVIOR"),
                eq(NodeType.EVENT_TRIGGER), eq("LOGIN"),
                eq(Map.of("channel", "web")), eq("evt-1"));
    }

    private static String hmac(String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(PUBLIC_TRIGGER_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }
}
