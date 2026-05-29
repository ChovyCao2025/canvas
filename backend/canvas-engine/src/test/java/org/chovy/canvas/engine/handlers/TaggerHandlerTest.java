package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.audience.AudienceUserResolver;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tagger 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TaggerHandlerTest {

    @Test
    void delegates_to_offline_handler_when_mode_is_offline() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        TaggerHandler handler = handler(offlineHandler, realtimeHandler, bitmapStore);

        NodeResult expected = NodeResult.ok("next_node", Map.of());
        when(offlineHandler.executeAsync(any(), any())).thenReturn(Mono.just(expected));

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");
        NodeResult result = handler.executeAsync(
            Map.of("mode", "offline", "tagCodeKey", "high_value"), ctx).block();

        assertThat(result).isEqualTo(expected);
        Mockito.verify(offlineHandler).executeAsync(any(), any());
        Mockito.verify(realtimeHandler, Mockito.never()).executeAsync(any(), any());
    }

    @Test
    void delegates_to_realtime_handler_when_mode_is_realtime() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        TaggerHandler handler = handler(offlineHandler, realtimeHandler, bitmapStore);

        NodeResult expected = NodeResult.ok("next_node", Map.of());
        when(realtimeHandler.executeAsync(any(), any())).thenReturn(Mono.just(expected));

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");
        handler.executeAsync(
            Map.of("mode", "realtime", "tagCodeKey", "realtime_active"), ctx).block();

        Mockito.verify(realtimeHandler).executeAsync(any(), any());
        Mockito.verify(offlineHandler, Mockito.never()).executeAsync(any(), any());
    }

    @Test
    void routes_to_hit_branch_when_user_is_in_audience() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        TaggerHandler handler = handler(offlineHandler, realtimeHandler, bitmapStore);
        when(bitmapStore.isMember(101L, "u1")).thenReturn(true);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "mode", "audience",
                "audienceId", 101,
                "hitNextNodeId", "send_coupon",
                "missNextNodeId", "end"
        ), ctx).block();

        assertThat(result.nextNodeId()).isEqualTo("send_coupon");
        assertThat(result.output()).containsEntry("audienceHit", true);
    }

    @Test
    void routes_to_miss_branch_when_user_is_not_in_audience() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        TaggerHandler handler = handler(offlineHandler, realtimeHandler, bitmapStore);
        when(bitmapStore.isMember(101L, "u1")).thenReturn(false);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "mode", "audience",
                "audienceId", 101,
                "hitNextNodeId", "send_coupon",
                "missNextNodeId", "end"
        ), ctx).block();

        assertThat(result.nextNodeId()).isEqualTo("end");
        assertThat(result.output()).containsEntry("audienceHit", false);
    }

    @Test
    void scheduled_batch_audience_tagger_fans_out_to_resolved_users() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        AudienceUserResolver audienceUserResolver = Mockito.mock(AudienceUserResolver.class);
        CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler, bitmapStore, audienceUserResolver, executionService);

        when(audienceUserResolver.resolve(101L)).thenReturn(List.of("u1", "u2"));
        when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(62L);
        ctx.setExecutionId("batch-exec");
        ctx.setUserId("__scheduled_batch__:62:schedule");
        ctx.setTriggerPayload(Map.of(MapFieldKeys.SCHEDULED_BATCH, true));

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "audience",
                "mode", "audience",
                "audienceId", 101,
                "hitNextNodeId", "send_coupon",
                "missNextNodeId", "end"
        ), ctx).block();

        assertThat(result.nextNodeId()).isNull();
        assertThat(result.output()).containsEntry("fanoutCount", 2);
        Mockito.verify(executionService).trigger(eq(62L), eq("u1"), eq(TriggerType.SCHEDULED),
                eq(NodeType.TAGGER), eq("audience"), anyMap(), anyString(), eq(false));
        Mockito.verify(executionService).trigger(eq(62L), eq("u2"), eq(TriggerType.SCHEDULED),
                eq(NodeType.TAGGER), eq("audience"), anyMap(), anyString(), eq(false));
        Mockito.verifyNoInteractions(bitmapStore);
    }

    private static TaggerHandler handler(TaggerOfflineHandler offlineHandler,
                                         TaggerRealtimeHandler realtimeHandler,
                                         AudienceBitmapStore bitmapStore) {
        return new TaggerHandler(
                offlineHandler,
                realtimeHandler,
                bitmapStore,
                Mockito.mock(AudienceUserResolver.class),
                Mockito.mock(CanvasExecutionService.class)
        );
    }
}
