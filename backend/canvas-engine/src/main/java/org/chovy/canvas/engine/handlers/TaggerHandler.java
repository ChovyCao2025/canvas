package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.audience.AudienceUserResolver;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
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

import java.util.Map;
import java.util.UUID;

/**
 * 统一 Tagger 节点入口（TAGGER）。
 *
 * <p>按 mode 分发到不同策略：
 * - audience：基于 audience bitmap 判断命中/未命中分支；
 * - realtime：实时标签触发透传；
 * - offline（默认）：查询离线标签值并判定。
 * 新增模式时建议在此处集中分发，保持调用入口一致。
 */
@Component
@NodeHandlerType("TAGGER")
public class TaggerHandler implements NodeHandler {

    /** 离线标签策略处理器。 */
    private final TaggerOfflineHandler  offlineHandler;

    /** 实时标签策略处理器。 */
    private final TaggerRealtimeHandler realtimeHandler;

    /** 人群 bitmap 查询能力（audience 模式）。 */
    private final AudienceBitmapStore   audienceBitmapStore;
    private final AudienceUserResolver audienceUserResolver;
    private final CanvasExecutionService executionService;

    /**
     * 构造 TaggerHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param offlineHandler offlineHandler 方法执行所需的业务参数
     * @param realtimeHandler realtimeHandler 时间、过期时间或持续时长参数
     * @param audienceBitmapStore audienceBitmapStore 方法执行所需的业务参数
     */
    @Autowired
    public TaggerHandler(TaggerOfflineHandler offlineHandler,
                         TaggerRealtimeHandler realtimeHandler,
                         AudienceBitmapStore audienceBitmapStore,
                         AudienceUserResolver audienceUserResolver,
                         @Lazy CanvasExecutionService executionService) {
        this.offlineHandler = offlineHandler;
        this.realtimeHandler = realtimeHandler;
        this.audienceBitmapStore = audienceBitmapStore;
        this.audienceUserResolver = audienceUserResolver;
        this.executionService = executionService;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // mode 默认为 offline，保持历史节点配置兼容
        String mode = (String) config.getOrDefault(MapFieldKeys.MODE, MapFieldKeys.OFFLINE);
        if (MapFieldKeys.AUDIENCE.equals(mode)) {
            return handleAudienceMode(config, ctx);
        }
        if (MapFieldKeys.REALTIME.equals(mode)) {
            return realtimeHandler.executeAsync(config, ctx);
        }
        // 未识别模式统一按 offline 处理，避免因配置遗漏导致流程中断
        return offlineHandler.executeAsync(config, ctx);
    }

    /**
     * 执行 handle Audience Mode 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    private Mono<NodeResult> handleAudienceMode(Map<String, Object> config, ExecutionContext ctx) {
        // audience 模式要求配置 audienceId
        Object audienceIdRaw = config.get(MapFieldKeys.AUDIENCE_ID);
        if (audienceIdRaw == null) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: audienceId 未配置"));
        }
        Long audienceId = Long.parseLong(String.valueOf(audienceIdRaw));
        if (isScheduledBatchContext(ctx)) {
            return fanOutAudienceUsers(config, ctx, audienceId);
        }
        // 判断当前 userId 是否在离线计算好的人群 bitmap 里
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
