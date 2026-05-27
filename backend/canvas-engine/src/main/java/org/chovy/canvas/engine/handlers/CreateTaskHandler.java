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
    /** 客户任务记录访问器，用于创建 CRM 跟进任务。 */
    private final CustomerTaskRecordMapper taskMapper;

    /**
     * 构造 CreateTaskHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param taskMapper taskMapper 方法执行所需的业务参数
     */
    public CreateTaskHandler(CustomerTaskRecordMapper taskMapper) {
        this.taskMapper = taskMapper;
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
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        CustomerTaskRecordDO task = new CustomerTaskRecordDO();
        task.setUserId(ctx.getUserId());
        task.setTaskType(string(config, "taskType", "FOLLOW_UP"));
        task.setTitle(string(config, "title", "营销旅程跟进"));
        task.setDescription(string(config, "description", null));
        task.setPriority(string(config, "priority", "NORMAL"));
        task.setAssignee(string(config, "assignee", null));
        if (config.get("dueHours") instanceof Number hours) {
            // dueHours 是相对当前时间的 SLA，用于后续任务提醒或逾期统计。
            task.setDueAt(LocalDateTime.now().plusHours(hours.longValue()));
        }
        task.setStatus(CustomerTaskRecordDO.STATUS_OPEN);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getCreatedAt());
        // 创建任务是 CRM 侧副作用，插入成功后把 taskId 写回上下文。
        taskMapper.insert(task);
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.TASK_ID, task.getId())));
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
