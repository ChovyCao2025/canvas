package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
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
 * 定时规则解析、下一次触发时间计算都不在本节点中实现。
 */
@Component
@NodeHandlerType("SCHEDULED_TRIGGER")
public class ScheduledTriggerHandler implements NodeHandler {

    /**
     * 定时触发节点执行。
     *
     * <p>本方法只做“上下文透传”，不做定时表达式解析与用户筛选。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // nextNodeId 为定时触发节点的唯一出口
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        // 触发载荷（userId、scheduleTime 等）已在 CanvasExecutionService 写入 triggerPayload，此处透传
        Map<String, Object> output = new HashMap<>(ctx.getTriggerPayload());
        // output 主要供下游节点读取调度时刻/用户信息
        //（不在本节点新增/删减字段，保持触发信息可追溯）
        return Mono.just(NodeResult.ok(nextNodeId, output));
    }
}
