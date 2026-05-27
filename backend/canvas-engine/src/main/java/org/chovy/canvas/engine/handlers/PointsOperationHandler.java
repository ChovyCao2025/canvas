package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO;
import org.chovy.canvas.dal.mapper.CustomerPointsLedgerMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 积分操作节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.POINTS_OPERATION)
public class PointsOperationHandler implements NodeHandler {
    private final CustomerPointsLedgerMapper ledgerMapper;

    public PointsOperationHandler(CustomerPointsLedgerMapper ledgerMapper) {
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String idempotencyKey = string(config, "idempotencyKey",
                ctx.getExecutionId() + ":" + string(config, "__nodeId", "points"));
        CustomerPointsLedgerDO existing = ledgerMapper.selectOne(new LambdaQueryWrapper<CustomerPointsLedgerDO>()
                .eq(CustomerPointsLedgerDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return Mono.just(NodeResult.ok(string(config, "nextNodeId", null),
                    Map.of(MapFieldKeys.POINTS_LEDGER_ID, existing.getId(), MapFieldKeys.DUPLICATE, true)));
        }

        CustomerPointsLedgerDO ledger = new CustomerPointsLedgerDO();
        ledger.setUserId(ctx.getUserId());
        ledger.setOperation(string(config, "operation", "GRANT"));
        ledger.setPoints(number(config.get("points"), 0));
        ledger.setPointsType(string(config, "pointsType", "MARKETING"));
        ledger.setReason(string(config, "reason", null));
        ledger.setIdempotencyKey(idempotencyKey);
        if (config.get("expireAfterDays") instanceof Number days) {
            ledger.setExpiresAt(LocalDateTime.now().plusDays(days.longValue()));
        }
        ledger.setCreatedAt(LocalDateTime.now());
        ledgerMapper.insert(ledger);
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null),
                Map.of(MapFieldKeys.POINTS_LEDGER_ID, ledger.getId(), MapFieldKeys.DUPLICATE, false)));
    }

    @Override
    public boolean isBenefitNode() {
        return true;
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
