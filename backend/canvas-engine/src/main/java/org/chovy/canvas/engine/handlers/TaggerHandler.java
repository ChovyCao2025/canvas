package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.audience.AudienceBitmapStore;
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
    private final AudienceBitmapStore   audienceBitmapStore;

    @Autowired
    public TaggerHandler(TaggerOfflineHandler offlineHandler,
                         TaggerRealtimeHandler realtimeHandler,
                         AudienceBitmapStore audienceBitmapStore) {
        this.offlineHandler = offlineHandler;
        this.realtimeHandler = realtimeHandler;
        this.audienceBitmapStore = audienceBitmapStore;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String mode = (String) config.getOrDefault("mode", "offline");
        if ("audience".equals(mode)) {
            return handleAudienceMode(config, ctx);
        }
        if ("realtime".equals(mode)) {
            return realtimeHandler.executeAsync(config, ctx);
        }
        return offlineHandler.executeAsync(config, ctx);
    }

    private Mono<NodeResult> handleAudienceMode(Map<String, Object> config, ExecutionContext ctx) {
        Object audienceIdRaw = config.get("audienceId");
        if (audienceIdRaw == null) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: audienceId 未配置"));
        }
        Long audienceId = Long.parseLong(String.valueOf(audienceIdRaw));
        boolean hit = audienceBitmapStore.isMember(audienceId, ctx.getUserId());
        String nextNodeId = hit
                ? (String) config.get("hitNextNodeId")
                : (String) config.get("missNextNodeId");
        return Mono.just(NodeResult.ok(nextNodeId, Map.of(
                "audienceHit", hit,
                "audienceId", audienceId
        )));
    }
}
