package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Unified Tagger handler — delegates to offline or realtime based on config.mode.
 */
@Component
@NodeHandlerType("TAGGER")
public class TaggerHandler implements NodeHandler {

    private final TaggerOfflineHandler  offlineHandler;
    private final TaggerRealtimeHandler realtimeHandler;

    @Autowired
    public TaggerHandler(TaggerOfflineHandler offlineHandler,
                         TaggerRealtimeHandler realtimeHandler) {
        this.offlineHandler  = offlineHandler;
        this.realtimeHandler = realtimeHandler;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String mode = (String) config.getOrDefault("mode", "offline");
        if ("realtime".equals(mode)) {
            return realtimeHandler.executeAsync(config, ctx);
        }
        return offlineHandler.executeAsync(config, ctx);
    }
}
