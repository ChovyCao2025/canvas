package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.customer.CustomerTaskRecord;
import org.chovy.canvas.domain.customer.CustomerTaskRecordMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.CREATE_TASK)
public class CreateTaskHandler implements NodeHandler {
    private final CustomerTaskRecordMapper taskMapper;

    public CreateTaskHandler(CustomerTaskRecordMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        CustomerTaskRecord task = new CustomerTaskRecord();
        task.setUserId(ctx.getUserId());
        task.setTaskType(string(config, "taskType", "FOLLOW_UP"));
        task.setTitle(string(config, "title", "营销旅程跟进"));
        task.setDescription(string(config, "description", null));
        task.setPriority(string(config, "priority", "NORMAL"));
        task.setAssignee(string(config, "assignee", null));
        if (config.get("dueHours") instanceof Number hours) {
            task.setDueAt(LocalDateTime.now().plusHours(hours.longValue()));
        }
        task.setStatus(CustomerTaskRecord.STATUS_OPEN);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getCreatedAt());
        taskMapper.insert(task);
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of("taskId", task.getId())));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
