package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.UserInputFormDO;
import org.chovy.canvas.dal.dataobject.UserInputResponseDO;
import org.chovy.canvas.dal.dataobject.UserInputResumeAuditDO;
import org.chovy.canvas.dal.mapper.UserInputFormMapper;
import org.chovy.canvas.dal.mapper.UserInputResponseMapper;
import org.chovy.canvas.dal.mapper.UserInputResumeAuditMapper;
import org.chovy.canvas.dto.canvas.UserInputSubmitReq;
import org.chovy.canvas.dto.canvas.UserInputSubmitResp;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserInputServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-04T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void createPendingIsIdempotentByRuntimeKey() {
        UserInputFormMapper formMapper = mock(UserInputFormMapper.class);
        UserInputResponseMapper responseMapper = mock(UserInputResponseMapper.class);
        UserInputService service = service(formMapper, responseMapper, mock(UserInputResumeAuditMapper.class),
                mock(CanvasExecutionService.class));
        UserInputResponseDO existing = response(12L, UserInputService.STATUS_PENDING);
        existing.setFormId(11L);
        when(responseMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        UserInputService.PendingInput pending = service.createPending(ctx(), "input-1",
                List.of(Map.of("key", "email")), "done-1", "timeout-1", null);

        assertThat(pending.responseId()).isEqualTo(12L);
        assertThat(pending.status()).isEqualTo(UserInputService.STATUS_PENDING);
    }

    @Test
    void createPendingInsertsFormAndResponse() {
        UserInputFormMapper formMapper = mock(UserInputFormMapper.class);
        UserInputResponseMapper responseMapper = mock(UserInputResponseMapper.class);
        when(responseMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(formMapper.insert(any(UserInputFormDO.class))).thenAnswer(invocation -> {
            invocation.<UserInputFormDO>getArgument(0).setId(11L);
            return 1;
        });
        when(responseMapper.insert(any(UserInputResponseDO.class))).thenAnswer(invocation -> {
            invocation.<UserInputResponseDO>getArgument(0).setId(12L);
            return 1;
        });
        UserInputService service = service(formMapper, responseMapper, mock(UserInputResumeAuditMapper.class),
                mock(CanvasExecutionService.class));

        UserInputService.PendingInput pending = service.createPending(ctx(), "input-1",
                List.of(Map.of("key", "email")), "done-1", "timeout-1",
                LocalDateTime.of(2026, 6, 4, 10, 30));

        assertThat(pending.formId()).isEqualTo(11L);
        assertThat(pending.responseId()).isEqualTo(12L);
        ArgumentCaptor<UserInputResponseDO> captor = ArgumentCaptor.forClass(UserInputResponseDO.class);
        verify(responseMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("USER_INPUT:exec-1:input-1:user-1");
    }

    @Test
    void submitCompletesPendingResponseAuditsAndTriggersResume() {
        UserInputResponseMapper responseMapper = mock(UserInputResponseMapper.class);
        UserInputResumeAuditMapper auditMapper = mock(UserInputResumeAuditMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        UserInputResponseDO pending = response(12L, UserInputService.STATUS_PENDING);
        when(responseMapper.selectById(12L)).thenReturn(pending);
        when(responseMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(executionService.trigger(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(Mono.just(Map.of()));
        UserInputService service = service(mock(UserInputFormMapper.class), responseMapper, auditMapper, executionService);

        UserInputSubmitResp resp = service.submit(12L, new UserInputSubmitReq(Map.of("email", "a@example.com"), "alice"));

        assertThat(resp.duplicate()).isFalse();
        assertThat(resp.status()).isEqualTo(UserInputService.STATUS_COMPLETED);
        verify(auditMapper).insert(any(UserInputResumeAuditDO.class));
        verify(executionService).trigger(eq(10L), eq("user-1"), eq(TriggerType.WAIT_RESUME),
                eq(NodeType.USER_INPUT), eq("input-1"), any(), any(), eq(false));
    }

    @Test
    void submitIsDuplicateWhenAlreadyCompleted() {
        UserInputResponseMapper responseMapper = mock(UserInputResponseMapper.class);
        UserInputResponseDO completed = response(12L, UserInputService.STATUS_COMPLETED);
        when(responseMapper.selectById(12L)).thenReturn(completed);
        UserInputService service = service(mock(UserInputFormMapper.class), responseMapper,
                mock(UserInputResumeAuditMapper.class), mock(CanvasExecutionService.class));

        UserInputSubmitResp resp = service.submit(12L, new UserInputSubmitReq(Map.of("email", "a@example.com"), "alice"));

        assertThat(resp.duplicate()).isTrue();
        assertThat(resp.status()).isEqualTo(UserInputService.STATUS_COMPLETED);
    }

    private UserInputService service(UserInputFormMapper formMapper,
                                     UserInputResponseMapper responseMapper,
                                     UserInputResumeAuditMapper auditMapper,
                                     CanvasExecutionService executionService) {
        return new UserInputService(formMapper, responseMapper, auditMapper,
                new ObjectMapper(), executionService, CLOCK);
    }

    private static UserInputResponseDO response(Long id, String status) {
        UserInputResponseDO response = new UserInputResponseDO();
        response.setId(id);
        response.setTenantId(7L);
        response.setFormId(11L);
        response.setCanvasId(10L);
        response.setVersionId(20L);
        response.setExecutionId("exec-1");
        response.setNodeId("input-1");
        response.setUserId("user-1");
        response.setStatus(status);
        response.setCompletedNodeId("done-1");
        response.setTimeoutNodeId("timeout-1");
        return response;
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(7L);
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(20L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
