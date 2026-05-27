package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CustomerTagDO;
import org.chovy.canvas.dal.mapper.CustomerTagMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 标签操作节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.TAG_OPERATION)
public class TagOperationHandler implements NodeHandler {
    /** 客户标签访问器，用于增删用户标签记录。 */
    private final CustomerTagMapper tagMapper;

    /**
     * 构造 TagOperationHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param tagMapper tagMapper 方法执行所需的业务参数
     */
    public TagOperationHandler(CustomerTagMapper tagMapper) {
        this.tagMapper = tagMapper;
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
        List<Map<String, Object>> operations = (List<Map<String, Object>>) config.getOrDefault("operations", List.of());
        int changed = 0;
        for (Map<String, Object> op : operations) {
            String operation = string(op, "operation", "ADD");
            List<Object> tags = (List<Object>) op.getOrDefault("tags", List.of());
            for (Object tag : tags) {
                if (tag == null || tag.toString().isBlank()) continue;
                if ("REMOVE".equalsIgnoreCase(operation)) {
                    // REMOVE 直接删除用户标签记录，是不可回滚的数据库副作用。
                    changed += tagMapper.delete(new LambdaQueryWrapper<CustomerTagDO>()
                            .eq(CustomerTagDO::getUserId, ctx.getUserId())
                            .eq(CustomerTagDO::getTag, tag.toString()));
                } else {
                    upsertTag(ctx, tag.toString(), op);
                    changed++;
                }
            }
        }
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.TAGS_CHANGED, changed)));
    }

    /**
     * 执行 upsert Tag 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param tag tag 方法执行所需的业务参数
     * @param op op 方法执行所需的业务参数
     */
    private void upsertTag(ExecutionContext ctx, String tag, Map<String, Object> op) {
        CustomerTagDO entity = new CustomerTagDO();
        entity.setUserId(ctx.getUserId());
        entity.setTag(tag);
        entity.setSource(ctx.getExecutionId());
        entity.setUpdatedAt(LocalDateTime.now());
        if (op.get("expireAfterDays") instanceof Number days) {
            // 可选过期天数用于临时标签，到期后由清理任务或查询侧过滤。
            entity.setExpiresAt(LocalDateTime.now().plusDays(days.longValue()));
        }
        int updated = tagMapper.update(entity, new LambdaUpdateWrapper<CustomerTagDO>()
                .eq(CustomerTagDO::getUserId, ctx.getUserId())
                .eq(CustomerTagDO::getTag, tag));
        if (updated == 0) {
            // 先更新后插入，避免重复执行时产生同一用户同一标签的多条记录。
            entity.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(entity);
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
