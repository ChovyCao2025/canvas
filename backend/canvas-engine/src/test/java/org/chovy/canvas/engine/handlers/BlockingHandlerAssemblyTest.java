package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO;
import org.chovy.canvas.dal.dataobject.CustomerTagDO;
import org.chovy.canvas.dal.dataobject.CustomerTaskRecordDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.mapper.CustomerPointsLedgerMapper;
import org.chovy.canvas.dal.mapper.CustomerProfileMapper;
import org.chovy.canvas.dal.mapper.CustomerTagMapper;
import org.chovy.canvas.dal.mapper.CustomerTaskRecordMapper;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BlockingHandlerAssemblyTest {

    @Test
    void pointsOperationDoesNotTouchMapperBeforeSubscription() {
        CustomerPointsLedgerMapper mapper = mock(CustomerPointsLedgerMapper.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, CustomerPointsLedgerDO.class).setId(1L);
            return 1;
        }).when(mapper).insert(any(CustomerPointsLedgerDO.class));
        PointsOperationHandler handler = new PointsOperationHandler(mapper);

        Mono<NodeResult> result = handler.executeAsync(Map.of("points", 10), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void pointsOperationDuplicateInsertIsIdempotent() {
        CustomerPointsLedgerMapper mapper = mock(CustomerPointsLedgerMapper.class);
        doThrow(new DuplicateKeyException("duplicate"))
                .when(mapper).insert(any(CustomerPointsLedgerDO.class));
        PointsOperationHandler handler = new PointsOperationHandler(mapper);

        NodeResult result = handler.executeAsync(Map.of("points", 10, MapFieldKeys.NEXT_NODE_ID, "next"), ctx()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("idempotent", true);
    }

    @Test
    void tagOperationDoesNotTouchMapperBeforeSubscription() {
        CustomerTagMapper mapper = mock(CustomerTagMapper.class);
        TagOperationHandler handler = new TagOperationHandler(mapper);

        Mono<NodeResult> result = handler.executeAsync(Map.of(
                "operations", List.of(Map.of("operation", "ADD", "tags", List.of("vip")))), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void tagOperationDuplicateInsertIsIdempotent() {
        CustomerTagMapper mapper = mock(CustomerTagMapper.class);
        when(mapper.update(any(), any())).thenReturn(0);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insert(any(CustomerTagDO.class));
        TagOperationHandler handler = new TagOperationHandler(mapper);

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NEXT_NODE_ID, "next",
                "operations", List.of(Map.of("operation", "ADD", "tags", List.of("vip")))), ctx()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("idempotent", true);
    }

    @Test
    void updateProfileDoesNotTouchMapperBeforeSubscription() {
        CustomerProfileMapper mapper = mock(CustomerProfileMapper.class);
        UpdateProfileHandler handler = new UpdateProfileHandler(mapper, new ObjectMapper());

        Mono<NodeResult> result = handler.executeAsync(Map.of(
                "operations", List.of(Map.of("field", "region", "value", "EU"))), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void createTaskDoesNotTouchMapperBeforeSubscription() {
        CustomerTaskRecordMapper mapper = mock(CustomerTaskRecordMapper.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, CustomerTaskRecordDO.class).setId(1L);
            return 1;
        }).when(mapper).insert(any(CustomerTaskRecordDO.class));
        CreateTaskHandler handler = new CreateTaskHandler(mapper);

        Mono<NodeResult> result = handler.executeAsync(Map.of("title", "follow up"), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void manualApprovalDoesNotCreateApprovalBeforeSubscription() {
        CanvasManualApprovalMapper mapper = mock(CanvasManualApprovalMapper.class);
        NotificationEventService notificationService = mock(NotificationEventService.class);
        ManualApprovalHandler handler = new ManualApprovalHandler(mapper, new ObjectMapper(), notificationService);

        Mono<NodeResult> result = handler.executeAsync(Map.of(MapFieldKeys.NODE_ID_INTERNAL, "approval-1"), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper, notificationService);
    }

    @Test
    void trackEventDoesNotTouchMapperBeforeSubscription() {
        EventLogMapper mapper = mock(EventLogMapper.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, EventLogDO.class).setId(1L);
            return 1;
        }).when(mapper).insert(any(EventLogDO.class));
        TrackEventHandler handler = new TrackEventHandler(mapper, new ObjectMapper());

        Mono<NodeResult> result = handler.executeAsync(Map.of("eventCode", "ORDER_PAID"), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void cdpTagWriteDoesNotCallServiceBeforeSubscription() {
        CdpTagService tagService = mock(CdpTagService.class);
        CdpTagWriteHandler handler = new CdpTagWriteHandler(tagService);

        Mono<NodeResult> result = handler.executeAsync(Map.of(
                "tagCode", "vip", "valueMode", "fixed", "tagValue", "true"), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(tagService);
    }

    @Test
    void frequencyCapDoesNotCallPolicyServiceBeforeSubscription() {
        MarketingPolicyService policyService = mock(MarketingPolicyService.class);
        when(policyService.consumeFrequency(anyString(), anyLong(), anyString(), anyString(), anyString(),
                anyInt(), any(Duration.class))).thenReturn(MarketingPolicyService.PolicyDecision.allow());
        FrequencyCapHandler handler = new FrequencyCapHandler(policyService);

        Mono<NodeResult> result = handler.executeAsync(Map.of("maxCount", 1), ctx());

        assertThat(result).isNotNull();
        verify(policyService, never()).consumeFrequency(anyString(), anyLong(), anyString(), anyString(),
                anyString(), anyInt(), any(Duration.class));
    }

    @Test
    void suppressionCheckDoesNotCallPolicyServiceBeforeSubscription() {
        MarketingPolicyService policyService = mock(MarketingPolicyService.class);
        when(policyService.consentAllowed(anyString(), anyString(), eq(true)))
                .thenReturn(MarketingPolicyService.PolicyDecision.allow());
        when(policyService.suppressionAllowed(anyString(), anyString()))
                .thenReturn(MarketingPolicyService.PolicyDecision.allow());
        SuppressionCheckHandler handler = new SuppressionCheckHandler(policyService);

        Mono<NodeResult> result = handler.executeAsync(Map.of("channel", "EMAIL"), ctx());

        assertThat(result).isNotNull();
        verify(policyService, never()).consentAllowed(anyString(), anyString(), eq(true));
        verify(policyService, never()).suppressionAllowed(anyString(), anyString());
    }

    @Test
    void goalCheckDoesNotQueryEventsBeforeSubscription() {
        EventLogMapper mapper = mock(EventLogMapper.class);
        WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
        GoalCheckHandler handler = new GoalCheckHandler(mapper, waitService, new ObjectMapper(), java.time.Clock.systemUTC());

        Mono<NodeResult> result = handler.executeAsync(Map.of("eventCode", "ORDER_PAID"), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(mapper, waitService);
    }

    @Test
    void canvasTriggerDoesNotLoadCanvasBeforeSubscription() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasTriggerHandler handler = new CanvasTriggerHandler(
                canvasMapper, mock(CanvasConfigCache.class), mock(DagEngine.class));

        Mono<NodeResult> result = handler.executeAsync(Map.of("targetCanvasId", 20L), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(canvasMapper);
    }

    @Test
    void subFlowRefDoesNotLoadCanvasBeforeSubscription() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        SubFlowRefHandler handler = new SubFlowRefHandler(
                canvasMapper,
                mock(CanvasVersionMapper.class),
                mock(CanvasConfigCache.class),
                mock(DagEngine.class),
                new ObjectMapper());

        Mono<NodeResult> result = handler.executeAsync(Map.of("subFlowId", 20L), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(canvasMapper);
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(100L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
