package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionManagementControllerTest {

    @Test
    void approveDoesNotResumeWhenPendingApprovalWasAlreadyCompletedConcurrently() {
        CanvasManualApprovalMapper approvalMapper = mock(CanvasManualApprovalMapper.class);
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        NotificationEventService notificationEventService = mock(NotificationEventService.class);
        CanvasExecutionManagementController controller = new CanvasExecutionManagementController(
                approvalMapper, ctxStore, executionService, new ObjectMapper(), notificationEventService);
        CanvasManualApprovalDO approval = CanvasManualApprovalDO.builder()
                .id("approval-1")
                .executionId("exec-1")
                .tenantId(0L)
                .canvasId(10L)
                .userId("user-1")
                .nodeId("approval-node")
                .status(ApprovalStatus.PENDING)
                .approvers("[\"alice\"]")
                .build();
        when(approvalMapper.selectList(any())).thenReturn(List.of(approval));
        when(approvalMapper.update(any(), any())).thenReturn(0);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        controller.approve("exec-1")
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        verify(ctxStore, never()).load(any(), any());
        verify(notificationEventService, never()).approvalResult(any(), any(), any());
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void approveRejectsLegacyManualApprovalWhenExecutionContextBelongsToAnotherTenant() {
        CanvasManualApprovalMapper approvalMapper = mock(CanvasManualApprovalMapper.class);
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        NotificationEventService notificationEventService = mock(NotificationEventService.class);
        CanvasExecutionManagementController controller = new CanvasExecutionManagementController(
                approvalMapper, ctxStore, executionService, new ObjectMapper(), notificationEventService);
        CanvasManualApprovalDO approval = CanvasManualApprovalDO.builder()
                .id("approval-1")
                .executionId("exec-1")
                .canvasId(10L)
                .userId("user-1")
                .nodeId("approval-node")
                .status(ApprovalStatus.PENDING)
                .approvers("[\"alice\"]")
                .build();
        ExecutionContext context = new ExecutionContext();
        context.setCanvasId(10L);
        context.setUserId("user-1");
        context.setTenantId(1L);
        when(approvalMapper.selectList(any())).thenReturn(List.of(approval));
        when(ctxStore.load(10L, "user-1")).thenReturn(context);
        Claims claims = mock(Claims.class);
        when(claims.get("tenantId")).thenReturn(2L);
        when(claims.get("username", String.class)).thenReturn("alice");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        assertThatThrownBy(() -> controller.approve("exec-1")
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("current tenant");

        verify(approvalMapper, never()).update(any(), any());
        verify(notificationEventService, never()).approvalResult(any(), any(), any());
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    }
}
