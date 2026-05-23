package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.customer.CustomerTag;
import org.chovy.canvas.domain.customer.CustomerTagMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.TAG_OPERATION)
public class TagOperationHandler implements NodeHandler {
    private final CustomerTagMapper tagMapper;

    public TagOperationHandler(CustomerTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

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
                    changed += tagMapper.delete(new LambdaQueryWrapper<CustomerTag>()
                            .eq(CustomerTag::getUserId, ctx.getUserId())
                            .eq(CustomerTag::getTag, tag.toString()));
                } else {
                    upsertTag(ctx, tag.toString(), op);
                    changed++;
                }
            }
        }
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of(MapFieldKeys.TAGS_CHANGED, changed)));
    }

    private void upsertTag(ExecutionContext ctx, String tag, Map<String, Object> op) {
        CustomerTag entity = new CustomerTag();
        entity.setUserId(ctx.getUserId());
        entity.setTag(tag);
        entity.setSource(ctx.getExecutionId());
        entity.setUpdatedAt(LocalDateTime.now());
        if (op.get("expireAfterDays") instanceof Number days) {
            entity.setExpiresAt(LocalDateTime.now().plusDays(days.longValue()));
        }
        int updated = tagMapper.update(entity, new LambdaUpdateWrapper<CustomerTag>()
                .eq(CustomerTag::getUserId, ctx.getUserId())
                .eq(CustomerTag::getTag, tag));
        if (updated == 0) {
            entity.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(entity);
        }
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
