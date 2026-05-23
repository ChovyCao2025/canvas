package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Tagger 实时标签触发节点（TAGGER_REALTIME）。
 *
 * <p>实时标签变更事件触发时，事件字段已在 triggerPayload 中，
 * 此节点只负责透传给后续节点使用。
 */
@Component
@NodeHandlerType("TAGGER_REALTIME")
public class TaggerRealtimeHandler implements NodeHandler {

    /**
     * 直接透传实时标签事件载荷到下游。
     *
     * <p>下游节点可读取 triggerPayload 中的标签字段做二次判断。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 实时模式也依赖 nextNodeId 显式指定后继节点
        String nextNodeId = (String) config.get("nextNodeId");
        // 复制 triggerPayload，避免下游误改原始上下文 map
        //（原始 payload 可能在追踪/审计链路中继续使用）
        // 该节点不额外补字段，保持上游事件原貌
        return Mono.just(NodeResult.ok(nextNodeId, new HashMap<>(ctx.getTriggerPayload())));
    }
}
