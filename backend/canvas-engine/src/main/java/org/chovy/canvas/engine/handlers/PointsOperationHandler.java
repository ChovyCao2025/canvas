package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.customer.CustomerPointsLedger;
import org.chovy.canvas.domain.customer.CustomerPointsLedgerMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

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
        CustomerPointsLedger existing = ledgerMapper.selectOne(new LambdaQueryWrapper<CustomerPointsLedger>()
                .eq(CustomerPointsLedger::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return Mono.just(NodeResult.ok(string(config, "nextNodeId", null),
                    Map.of("pointsLedgerId", existing.getId(), "duplicate", true)));
        }

        CustomerPointsLedger ledger = new CustomerPointsLedger();
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
                Map.of("pointsLedgerId", ledger.getId(), "duplicate", false)));
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
