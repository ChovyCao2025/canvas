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

    /**
     * 创建 TaggerHandler 实例并注入 engine.handlers 场景依赖。
     * @param audienceBitmapStore audience bitmap store 参数，用于 TaggerHandler 流程中的校验、计算或对象转换。
     * @param audienceUserResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceSnapshotService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行 Tagger 节点：普通模式写出标签相关上下文，受众模式按命中结果或批处理场景决定路由。
     *
     * <p>非受众模式没有外部副作用，只沿 nextNodeId 输出标签字段。受众模式在普通用户执行中查询实时位图或快照，
     * 命中走 hitNextNodeId，未命中走 missNextNodeId；在调度批处理上下文中会 fan-out 触发受众内用户的画布执行，
     * 这是本节点的外部副作用。</p>
     *
     * @param config 节点配置，包含 mode、audienceId、受众快照 ID、命中/未命中下一跳和标签字段
     * @param ctx 执行上下文，用于读取用户、触发载荷和画布执行信息
     * @return 节点结果；普通受众判断返回命中输出，批处理返回 fanoutCount 输出
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String mode = String.valueOf(config.getOrDefault(MapFieldKeys.MODE, MapFieldKeys.OFFLINE));
        if (MapFieldKeys.AUDIENCE.equals(mode)) {
            return handleAudienceMode(config, ctx);
        }
        return Mono.just(NodeResult.ok((String) config.get(MapFieldKeys.NEXT_NODE_ID), tagOutput(config, mode)));
    }

    /**
     * 生成普通标签模式的输出，只写入后续节点需要消费的标签键和值。
     */
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

    /**
     * 处理受众模式：调度批处理触发 fan-out，普通执行根据受众成员关系选择命中或未命中下一跳。
     */
    private Mono<NodeResult> handleAudienceMode(Map<String, Object> config, ExecutionContext ctx) {
        // 准备本次处理所需的上下文和中间变量。
        Object audienceIdRaw = config.get(MapFieldKeys.AUDIENCE_ID);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Mono.just(NodeResult.ok(nextNodeId, output));
    }

    /**
     * 将调度批处理执行扩展为受众内每个用户的一次画布触发，并返回 fan-out 数量。
     */
    private Mono<NodeResult> fanOutAudienceUsers(Map<String, Object> config, ExecutionContext ctx, Long audienceId) {
        String nodeId = String.valueOf(config.getOrDefault(MapFieldKeys.NODE_ID_INTERNAL, ""));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (nodeId.isBlank()) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: 批处理缺少节点 ID"));
        }
        Long snapshotId = snapshotId(config, ctx);
        return Mono.fromCallable(() -> resolveFanOutUsers(audienceId, snapshotId))
                .subscribeOn(Schedulers.boundedElastic())
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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
                    // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 解析需要 fan-out 的用户集合；指定快照时使用快照成员，否则读取实时受众解析结果。
     */
    private List<String> resolveFanOutUsers(Long audienceId, Long snapshotId) {
        if (snapshotId != null) {
            return audienceSnapshotService.users(snapshotId);
        }
        return audienceUserResolver.resolve(audienceId);
    }

    /**
     * 构造受众节点输出，记录受众、快照和本次批处理触发的用户数量。
     */
    private Map<String, Object> audienceOutput(Long audienceId, Long snapshotId, int fanoutCount) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(MapFieldKeys.AUDIENCE_ID, audienceId);
        output.put("fanoutCount", fanoutCount);
        if (snapshotId != null) {
            output.put(AUDIENCE_SNAPSHOT_ID, snapshotId);
        }
        return output;
    }

    /**
     * 解析受众快照 ID，恢复或调度载荷中的快照优先于节点静态配置。
     */
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

    /**
     * 判断当前执行是否来自定时批量人群上下文。
     *
     * @param ctx 执行上下文
     * @return true 表示由定时批量调度触发
     */
    private boolean isScheduledBatchContext(ExecutionContext ctx) {
        return ctx != null
                && Boolean.TRUE.equals(ctx.getTriggerPayload().get(MapFieldKeys.SCHEDULED_BATCH))
                && CanvasSchedulerService.isScheduledBatchUser(ctx.getUserId());
    }
}
