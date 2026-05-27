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
    /** 积分流水访问器，用于幂等记录积分发放或扣减。 */
    private final CustomerPointsLedgerMapper ledgerMapper;

    /**
     * 构造 PointsOperationHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param ledgerMapper ledgerMapper 方法执行所需的业务参数
     */
    public PointsOperationHandler(CustomerPointsLedgerMapper ledgerMapper) {
        this.ledgerMapper = ledgerMapper;
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
        String idempotencyKey = string(config, "idempotencyKey",
                ctx.getExecutionId() + ":" + string(config, "__nodeId", "points"));
        CustomerPointsLedgerDO existing = ledgerMapper.selectOne(new LambdaQueryWrapper<CustomerPointsLedgerDO>()
                .eq(CustomerPointsLedgerDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null) {
            // 命中幂等流水时直接返回原流水 ID，避免重复发放或扣减积分。
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
            // 发放类积分可配置过期时间，流水本身记录过期点供账户侧结算。
            ledger.setExpiresAt(LocalDateTime.now().plusDays(days.longValue()));
        }
        ledger.setCreatedAt(LocalDateTime.now());
        ledgerMapper.insert(ledger);
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null),
                Map.of(MapFieldKeys.POINTS_LEDGER_ID, ledger.getId(), MapFieldKeys.DUPLICATE, false)));
    }

    /**
     * 判断 is Benefit Node 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    @Override
    public boolean isBenefitNode() {
        return true;
    }

    /**
     * 执行 number 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
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
