package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO;
import org.chovy.canvas.domain.cdp.CustomerPointsLedgerService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 积分操作节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component("pointsOperationHandler")
public class PointsOperationHandler implements NodeHandler {
    /** 积分流水服务，用于隔离 handler 与持久层细节。 */
    private final CustomerPointsLedgerService ledgerService;

    /**
     * 构造 PointsOperationHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param ledgerService 积分流水服务
     */
    public PointsOperationHandler(CustomerPointsLedgerService ledgerService) {
        this.ledgerService = ledgerService;
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
        return Mono.fromCallable(() -> executeBlocking(config, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(NodeResult.fail("POINTS_OPERATION: " + e.getMessage())));
    }

    /**
     * 同步执行积分流水写入。
     *
     * @param config 节点配置
     * @param ctx 执行上下文
     * @return 节点执行结果
     */
    private NodeResult executeBlocking(Map<String, Object> config, ExecutionContext ctx) {
        String idempotencyKey = string(config, "idempotencyKey",
                ctx.getExecutionId() + ":" + string(config, "__nodeId", "points"));
        CustomerPointsLedgerDO existing = ledgerService.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            // 命中幂等流水时直接返回原流水 ID，避免重复发放或扣减积分。
            return NodeResult.ok(string(config, "nextNodeId", null),
                    Map.of(MapFieldKeys.POINTS_LEDGER_ID, existing.getId(), MapFieldKeys.DUPLICATE, true));
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
        try {
            ledgerService.insert(ledger);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DuplicateKeyException e) {
            return NodeResult.ok(string(config, "nextNodeId", null),
                    Map.of(MapFieldKeys.DUPLICATE, true, "idempotent", true));
        }
        Map<String, Object> output = new HashMap<>();
        if (ledger.getId() != null) {
            output.put(MapFieldKeys.POINTS_LEDGER_ID, ledger.getId());
        }
        output.put(MapFieldKeys.DUPLICATE, false);
        return NodeResult.ok(string(config, "nextNodeId", null), output);
    }

    /**
     * isBenefitNode 校验或转换 engine.handlers 场景的数据。
     * @return 返回布尔判断结果。
     */
    @Override
    public boolean isBenefitNode() {
        return true;
    }

    /**
     * 声明积分节点会写入积分流水并产生权益副作用，需要节点级幂等保护。
     *
     * <p>重复执行时调度层可复用已完成输出，避免重复插入积分流水；节点内部也会按流水幂等键兜底查重。
     *
     * @param config 当前积分节点配置，包含操作类型、积分数和幂等键
     * @param ctx 画布执行上下文
     * @return 始终为 {@code true}
     */
    @Override
    public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
        return true;
    }

    /**
     * 构造积分操作的副作用操作键。
     *
     * <p>优先使用显式幂等键；未配置时按用户、操作类型和积分数量生成稳定键，决定重复执行时是否跳过积分流水写入。
     *
     * @param config 当前积分节点配置，读取 {@code idempotencyKey}、operation 和 points
     * @param ctx 画布执行上下文，读取用户 ID
     * @return 用于节点副作用幂等表的业务操作键
     */
    @Override
    public String sideEffectOperationKey(Map<String, Object> config, ExecutionContext ctx) {
        Object explicit = config.get("idempotencyKey");
        if (explicit != null && !explicit.toString().isBlank()) {
            return explicit.toString();
        }
        return ctx.getUserId()
                + ":points:"
                /**
                 * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
                 *
                 * @param config 配置对象，用于控制运行参数和策略开关。
                 * @return 返回 string 流程生成的业务结果。
                 */
                + string(config, "operation", "GRANT")
                + ":"
                /**
                 * 执行 number 流程，围绕 number 完成校验、计算或结果组装。
                 *
                 * @return 返回 number 流程生成的业务结果。
                 */
                + number(config.get("points"), 0);
    }

    /**
     * 将对象转换为整数。
     *
     * @param value 原始值
     * @param fallback 默认值
     * @return 整数值或默认值
     */
    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /**
     * 读取字符串配置。
     *
     * @param config 节点配置
     * @param key 配置 key
     * @param fallback 默认值
     * @return 字符串值或默认值
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
