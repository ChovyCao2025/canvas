package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class BehaviorTriggerHandlerTest {

    @Test
    void routes_inapp_to_behaviorInAppHandler() {
        BehaviorInAppHandler inappHandler  = Mockito.mock(BehaviorInAppHandler.class);
        DirectCallHandler    directHandler = Mockito.mock(DirectCallHandler.class);
        BehaviorTriggerHandler handler = new BehaviorTriggerHandler(inappHandler, directHandler);

        when(inappHandler.executeAsync(any(), any()))
            .thenReturn(Mono.just(NodeResult.ok("n2", Map.of())));

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");
        handler.executeAsync(Map.of("triggerType", "inapp", "eventCode", "add_cart"), ctx).block();

        verify(inappHandler).executeAsync(any(), any());
        verify(directHandler, never()).executeAsync(any(), any());
    }

    @Test
    void routes_direct_to_directCallHandler() {
        BehaviorInAppHandler inappHandler  = Mockito.mock(BehaviorInAppHandler.class);
        DirectCallHandler    directHandler = Mockito.mock(DirectCallHandler.class);
        BehaviorTriggerHandler handler = new BehaviorTriggerHandler(inappHandler, directHandler);

        when(directHandler.executeAsync(any(), any()))
            .thenReturn(Mono.just(NodeResult.ok("n3", Map.of())));

        ExecutionContext ctx2 = new ExecutionContext();
        ctx2.setUserId("u1");
        handler.executeAsync(Map.of("triggerType", "direct"), ctx2).block();

        verify(directHandler).executeAsync(any(), any());
        verify(inappHandler, never()).executeAsync(any(), any());
    }
}
