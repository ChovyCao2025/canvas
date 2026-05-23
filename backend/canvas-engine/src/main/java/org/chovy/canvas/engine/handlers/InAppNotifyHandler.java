package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 端内通知节点（IN_APP_NOTIFY）。
 *
 * <p>当前为 mock 实现：记录日志并直接返回成功。
 * 后续接入真实推送客户端后，建议把 messageId 等回执写入 output 方便追踪。
 * 失败重试策略建议放在客户端 SDK 或消息中间层，不在节点里手写循环重试。
 */
@Component
@Slf4j
@NodeHandlerType("IN_APP_NOTIFY")
public class InAppNotifyHandler implements NodeHandler {

    /**
     * 端内通知执行。
     *
     * <p>当前版本只做日志记录与路由放行，后续接入真实通知服务后可把回执写入 output。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // messageCodeKey 表示通知模板编码；bizData 为模板变量
        String messageCodeKey = (String) config.get("messageCodeKey");
        String nextNodeId     = (String) config.get("nextNodeId");
        List<Map<String, Object>> bizData = (List<Map<String, Object>>) config.getOrDefault("bizData", List.of());

        // 当前阶段不真正调用推送服务，仅作为流程占位节点
        log.info("[IN_APP_NOTIFY] 推送端内通知 messageCode={} userId={} bizDataSize={}",
                messageCodeKey, ctx.getUserId(), bizData.size());
        // TODO: 接入 MQTT 推送客户端
        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
    }

    /** 标记为触达类节点，用于后续统计聚合。 */
    @Override public boolean isReachNode() { return true; }
}
