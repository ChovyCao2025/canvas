package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionControllerTest {

    @Test
    void dryRunUsesAuthenticatedSubjectInsteadOfRequestUserId() {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        ExecutionController controller = new ExecutionController(executionService, disruptorService,
                new MachineRequestAuthService("machine-secret-at-least-32-bytes-long"), new ObjectMapper());
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
}
