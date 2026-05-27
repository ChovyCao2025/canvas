package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CustomerTaskRecordDO;
import org.chovy.canvas.dal.mapper.CustomerTaskRecordMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建任务节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.CREATE_TASK)
public class CreateTaskHandler implements NodeHandler {
    private final CustomerTaskRecordMapper taskMapper;

    public CreateTaskHandler(CustomerTaskRecordMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        CustomerTaskRecordDO task = new CustomerTaskRecordDO();
        task.setUserId(ctx.getUserId());
        task.setTaskType(string(config, "taskType", "FOLLOW_UP"));
        task.setTitle(string(config, "title", "营销旅程跟进"));
        task.setDescription(string(config, "description", null));
        task.setPriority(string(config, "priority", "NORMAL"));
        task.setAssignee(string(config, "assignee", null));
        if (config.get("dueHours") instanceof Number hours) {
            task.setDueAt(LocalDateTime.now().plusHours(hours.longValue()));
        }
        task.setStatus(CustomerTaskRecordDO.STATUS_OPEN);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getCreatedAt());
        taskMapper.insert(task);
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.TASK_ID, task.getId())));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
