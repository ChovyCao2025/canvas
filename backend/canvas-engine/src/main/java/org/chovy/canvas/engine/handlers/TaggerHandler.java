package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.chovy.canvas.engine.audience.AudienceUserResolver;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Unified Tagger node. Historical realtime/offline node types are removed. */
@Component
@NodeHandlerType(NodeType.TAGGER)
public class TaggerHandler implements NodeHandler {

    private final AudienceBitmapStore audienceBitmapStore;
    private final AudienceUserResolver audienceUserResolver;
    private final CanvasExecutionService executionService;

    @Autowired
    public TaggerHandler(AudienceBitmapStore audienceBitmapStore,
                         AudienceUserResolver audienceUserResolver,
                         @Lazy CanvasExecutionService executionService) {
        this.audienceBitmapStore = audienceBitmapStore;
        this.audienceUserResolver = audienceUserResolver;
        this.executionService = executionService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String mode = String.valueOf(config.getOrDefault(MapFieldKeys.MODE, MapFieldKeys.OFFLINE));
        if (MapFieldKeys.AUDIENCE.equals(mode)) {
            return handleAudienceMode(config, ctx);
        }
        return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID), tagOutput(config, mode)));
    }

    private Map<String, Object> tagOutput(Map<String, Object> config, String mode) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(MapFieldKeys.MODE, mode);
        Object tagCodeKey = config.get("tagCodeKey");
        if (tagCodeKey != null) {
            output.put("tagCodeKey", tagCodeKey);
        }
        Object tagValue = config.get(MapFieldKeys.TAG_VALUE);
        if (tagValue != null) {
            output.put(MapFieldKeys.TAG_VALUE, tagValue);
        }
        return output;
    }

    private Mono<NodeResult> handleAudienceMode(Map<String, Object> config, ExecutionContext ctx) {
        Object audienceIdRaw = config.get(MapFieldKeys.AUDIENCE_ID);
        if (audienceIdRaw == null) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: audienceId 未配置"));
        }
        Long audienceId = Long.parseLong(String.valueOf(audienceIdRaw));
        if (isScheduledBatchContext(ctx)) {
            return fanOutAudienceUsers(config, ctx, audienceId);
        }
        boolean hit = audienceBitmapStore.isMember(audienceId, ctx.getUserId());
        String nextNodeId = hit
                ? (String) config.get(MapFieldKeys.HIT_NEXT_NODE_ID)
                : (String) config.get(MapFieldKeys.MISS_NEXT_NODE_ID);
        return Mono.just(NodeResult.ok(nextNodeId, Map.of(
                MapFieldKeys.AUDIENCE_HIT, hit,
                MapFieldKeys.AUDIENCE_ID, audienceId
        )));
    }

    private Mono<NodeResult> fanOutAudienceUsers(Map<String, Object> config, ExecutionContext ctx, Long audienceId) {
        String nodeId = String.valueOf(config.getOrDefault(MapFieldKeys.NODE_ID_INTERNAL, ""));
        if (nodeId.isBlank()) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: 批处理缺少节点 ID"));
        }
        return Mono.fromCallable(() -> audienceUserResolver.resolve(audienceId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userIds -> {
                    if (userIds.isEmpty()) {
                        return Mono.just(NodeResult.ok(null, Map.of(
                                MapFieldKeys.AUDIENCE_ID, audienceId,
                                "fanoutCount", 0
                        )));
                    }
                    Map<String, Object> payload = Map.of(
                            MapFieldKeys.AUDIENCE_ID, audienceId,
                            "scheduledBatchExecutionId", ctx.getExecutionId()
                    );
                    return Flux.fromIterable(userIds)
                            .flatMap(userId -> executionService.trigger(
                                    ctx.getCanvasId(),
                                    userId,
                                    TriggerType.SCHEDULED,
                                    NodeType.TAGGER,
                                    nodeId,
                                    payload,
                                    UUID.randomUUID().toString(),
                                    false
                            ), 32)
                            .then(Mono.just(NodeResult.ok(null, Map.of(
                                    MapFieldKeys.AUDIENCE_ID, audienceId,
                                    "fanoutCount", userIds.size()
                            ))));
                });
    }

    private boolean isScheduledBatchContext(ExecutionContext ctx) {
        return ctx != null
                && Boolean.TRUE.equals(ctx.getTriggerPayload().get(MapFieldKeys.SCHEDULED_BATCH))
                && CanvasSchedulerService.isScheduledBatchUser(ctx.getUserId());
    }
}
