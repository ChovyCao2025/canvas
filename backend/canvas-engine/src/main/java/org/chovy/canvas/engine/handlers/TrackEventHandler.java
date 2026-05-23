package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.meta.EventLog;
import org.chovy.canvas.domain.meta.EventLogMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

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
        EventLog event = new EventLog();
        event.setEventCode(eventCode);
        event.setUserId(ctx.getUserId());
        event.setAttributes(toJson(attributes));
        event.setCanvasTriggered(0);
        event.setCanvasCount(0);
        eventLogMapper.insert(event);
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of("eventLogId", event.getId())));
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
