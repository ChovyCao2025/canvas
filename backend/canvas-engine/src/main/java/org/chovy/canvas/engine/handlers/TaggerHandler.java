package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.chovy.canvas.engine.audience.AudienceSnapshotService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Unified Tagger node. Historical realtime/offline node types are removed. */
@Component
@NodeHandlerType(NodeType.TAGGER)
public class TaggerHandler implements NodeHandler {

    private static final String AUDIENCE_SNAPSHOT_ID = "audienceSnapshotId";

    private final AudienceBitmapStore audienceBitmapStore;
    private final AudienceUserResolver audienceUserResolver;
    private final CanvasExecutionService executionService;
    private final AudienceSnapshotService audienceSnapshotService;

    @Autowired
    public TaggerHandler(AudienceBitmapStore audienceBitmapStore,
                         AudienceUserResolver audienceUserResolver,
                         @Lazy CanvasExecutionService executionService,
                         AudienceSnapshotService audienceSnapshotService) {
        this.audienceBitmapStore = audienceBitmapStore;
        this.audienceUserResolver = audienceUserResolver;
        this.executionService = executionService;
        this.audienceSnapshotService = audienceSnapshotService;
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
        Long snapshotId = snapshotId(config, ctx);
        boolean hit = snapshotId == null
                ? audienceBitmapStore.isMember(audienceId, ctx.getUserId())
                : audienceSnapshotService.contains(snapshotId, ctx.getUserId());
        String nextNodeId = hit
                ? (String) config.get(MapFieldKeys.HIT_NEXT_NODE_ID)
                : (String) config.get(MapFieldKeys.MISS_NEXT_NODE_ID);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(MapFieldKeys.AUDIENCE_HIT, hit);
        output.put(MapFieldKeys.AUDIENCE_ID, audienceId);
        if (snapshotId != null) {
            output.put(AUDIENCE_SNAPSHOT_ID, snapshotId);
        }
        return Mono.just(NodeResult.ok(nextNodeId, output));
    }

    private Mono<NodeResult> fanOutAudienceUsers(Map<String, Object> config, ExecutionContext ctx, Long audienceId) {
        String nodeId = String.valueOf(config.getOrDefault(MapFieldKeys.NODE_ID_INTERNAL, ""));
        if (nodeId.isBlank()) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: 批处理缺少节点 ID"));
        }
        Long snapshotId = snapshotId(config, ctx);
        return Mono.fromCallable(() -> resolveFanOutUsers(audienceId, snapshotId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userIds -> {
                    if (userIds.isEmpty()) {
                        return Mono.just(NodeResult.ok(null, audienceOutput(audienceId, snapshotId, 0)));
                    }
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put(MapFieldKeys.AUDIENCE_ID, audienceId);
                    payload.put("scheduledBatchExecutionId", ctx.getExecutionId());
                    if (snapshotId != null) {
                        payload.put(AUDIENCE_SNAPSHOT_ID, snapshotId);
                    }
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
                            .then(Mono.just(NodeResult.ok(null, audienceOutput(audienceId, snapshotId, userIds.size()))));
                });
    }

    private List<String> resolveFanOutUsers(Long audienceId, Long snapshotId) {
        if (snapshotId != null) {
            return audienceSnapshotService.users(snapshotId);
        }
        return audienceUserResolver.resolve(audienceId);
    }

    private Map<String, Object> audienceOutput(Long audienceId, Long snapshotId, int fanoutCount) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(MapFieldKeys.AUDIENCE_ID, audienceId);
        output.put("fanoutCount", fanoutCount);
        if (snapshotId != null) {
            output.put(AUDIENCE_SNAPSHOT_ID, snapshotId);
        }
        return output;
    }

    private Long snapshotId(Map<String, Object> config, ExecutionContext ctx) {
        Object fromPayload = ctx == null || ctx.getTriggerPayload() == null
                ? null
                : ctx.getTriggerPayload().get(AUDIENCE_SNAPSHOT_ID);
        Object raw = fromPayload != null ? fromPayload : config.get(AUDIENCE_SNAPSHOT_ID);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        return Long.parseLong(String.valueOf(raw));
    }

    private boolean isScheduledBatchContext(ExecutionContext ctx) {
        return ctx != null
                && Boolean.TRUE.equals(ctx.getTriggerPayload().get(MapFieldKeys.SCHEDULED_BATCH))
                && CanvasSchedulerService.isScheduledBatchUser(ctx.getUserId());
    }
}
