package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
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

    private Mono<NodeResult> handleAudienceMode(Map<String, Object> config, ExecutionContext ctx) {
        // audience 模式要求配置 audienceId
        Object audienceIdRaw = config.get(MapFieldKeys.AUDIENCE_ID);
        if (audienceIdRaw == null) {
            return Mono.just(NodeResult.fail("TAGGER[audience]: audienceId 未配置"));
        }
        Long audienceId = Long.parseLong(String.valueOf(audienceIdRaw));
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
}
