package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TaggerHandlerTest {

    @Test
    void delegates_to_offline_handler_when_mode_is_offline() {
        TaggerOfflineHandler offlineHandler = Mockito.mock(TaggerOfflineHandler.class);
        TaggerRealtimeHandler realtimeHandler = Mockito.mock(TaggerRealtimeHandler.class);
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler);

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
        TaggerHandler handler = new TaggerHandler(offlineHandler, realtimeHandler);

        NodeResult expected = NodeResult.ok("next_node", Map.of());
        when(realtimeHandler.executeAsync(any(), any())).thenReturn(Mono.just(expected));

        ExecutionContext ctx2 = new ExecutionContext();
        ctx2.setUserId("u1");
        handler.executeAsync(
            Map.of("mode", "realtime", "tagCodeKey", "realtime_active"), ctx2).block();

        Mockito.verify(realtimeHandler).executeAsync(any(), any());
        Mockito.verify(offlineHandler, Mockito.never()).executeAsync(any(), any());
    }
}
