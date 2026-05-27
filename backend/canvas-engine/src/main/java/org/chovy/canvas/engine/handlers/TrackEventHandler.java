package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 事件追踪节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.TRACK_EVENT)
public class TrackEventHandler implements NodeHandler {
    private final EventLogMapper eventLogMapper;
    private final ObjectMapper objectMapper;

    public TrackEventHandler(EventLogMapper eventLogMapper, ObjectMapper objectMapper) {
        this.eventLogMapper = eventLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return Mono.just(NodeResult.fail("TRACK_EVENT 必须配置 eventCode"));
        }
        Map<String, Object> attributes = config.get("attributes") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        EventLogDO event = new EventLogDO();
        event.setEventCode(eventCode);
        event.setUserId(ctx.getUserId());
        event.setAttributes(toJson(attributes));
        event.setCanvasTriggered(0);
        event.setCanvasCount(0);
        eventLogMapper.insert(event);
        return Mono.just(NodeResult.ok(
                string(config, "nextNodeId", null),
                Map.of(MapFieldKeys.EVENT_LOG_ID, event.getId())));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("事件属性序列化失败", e);
        }
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
