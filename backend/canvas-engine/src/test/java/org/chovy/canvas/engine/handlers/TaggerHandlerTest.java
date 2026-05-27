package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler, bitmapStore);

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
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler, bitmapStore);

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
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler, bitmapStore);
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
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler, bitmapStore);
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
}
