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
 * 定时触发节点（设计文档 18.1 节）。
 *
 * 此 Handler 是触发器节点，被 CanvasSchedulerService 调度后调用。
 * userSource 配置决定触发给哪些用户：
 *   TAGGER_GROUP → 从 Tagger 分页拉取标签用户
 *   USER_LIST    → 静态用户列表
 *   USER_API     → 调用自定义接口获取用户列表
 *
 * 每个用户触发一次独立的画布执行（在 CanvasSchedulerService 中分页并发触发）。
 * Handler 本身只负责将本次调度的用户信息写入上下文。
 */
@Component
@NodeHandlerType("SCHEDULED_TRIGGER")
public class ScheduledTriggerHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nextNodeId = (String) config.get("nextNodeId");
        // 触发载荷已在 CanvasExecutionService 写入 triggerPayload，此处透传
        Map<String, Object> output = new HashMap<>(ctx.getTriggerPayload());
        return Mono.just(NodeResult.ok(nextNodeId, output));
    }
}
