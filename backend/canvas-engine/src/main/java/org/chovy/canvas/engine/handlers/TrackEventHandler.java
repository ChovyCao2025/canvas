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
import reactor.core.scheduler.Schedulers;

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
    /** 事件日志访问器，用于写入当前节点追踪事件。 */
    private final EventLogMapper eventLogMapper;

    /** JSON 序列化器，用于保存事件属性。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造 TrackEventHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param eventLogMapper eventLogMapper 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     */
    public TrackEventHandler(EventLogMapper eventLogMapper, ObjectMapper objectMapper) {
        this.eventLogMapper = eventLogMapper;
        this.objectMapper = objectMapper;
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
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return Mono.just(NodeResult.fail("TRACK_EVENT 必须配置 eventCode"));
        }
        return Mono.fromCallable(() -> executeBlocking(config, ctx, eventCode))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(NodeResult.fail("TRACK_EVENT: " + e.getMessage())));
    }

    @SuppressWarnings("unchecked")
    private NodeResult executeBlocking(Map<String, Object> config, ExecutionContext ctx, String eventCode) {
        Map<String, Object> attributes = config.get("attributes") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        EventLogDO event = new EventLogDO();
        event.setEventCode(eventCode);
        event.setUserId(ctx.getUserId());
        event.setAttributes(toJson(attributes));
        event.setCanvasTriggered(0);
        event.setCanvasCount(0);
        // 事件追踪节点会落库一条 EventLog，供目标检测和后续分析复用。
        eventLogMapper.insert(event);
        return NodeResult.ok(
                string(config, "nextNodeId", null),
                Map.of(MapFieldKeys.EVENT_LOG_ID, event.getId()));
    }

    /**
     * 构建、解析或转换 to Json 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("事件属性序列化失败", e);
        }
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
