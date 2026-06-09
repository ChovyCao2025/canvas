package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 通用提交动作节点。
 *
 * <p>用于承载“一旦成功就产生关键外部副作用”的动作。调度层通过 {@link #isBenefitNode()}
 * 识别提交点，后续节点失败时按防资损规则收敛为整体成功。
 */
@Component
@NodeHandlerType(NodeType.COMMIT_ACTION)
public class CommitActionHandler implements NodeHandler {
    static final String ACTION_ISSUE_COUPON = "ISSUE_COUPON";
    static final String ACTION_POINTS = "POINTS";

    private final NodeHandler couponHandler;
    private final NodeHandler pointsHandler;

    /**
     * 创建 CommitActionHandler 实例并注入 engine.handlers 场景依赖。
     * @param couponHandler coupon handler 参数，用于 CommitActionHandler 流程中的校验、计算或对象转换。
     * @param pointsHandler points handler 参数，用于 CommitActionHandler 流程中的校验、计算或对象转换。
     */
    public CommitActionHandler(@Qualifier("couponHandler") NodeHandler couponHandler,
                               @Qualifier("pointsOperationHandler") NodeHandler pointsHandler) {
        this.couponHandler = couponHandler;
        this.pointsHandler = pointsHandler;
    }

    /**
     * 执行提交动作节点并委派到具体权益处理器。
     *
     * <p>根据 {@code actionType} 路由到发券或积分处理器；被委派处理器决定成功、失败和下一跳。提交动作代表关键外部
     * 副作用，后续由调度层结合 {@link #requiresSideEffectIdempotency(Map, ExecutionContext)} 防止重复提交。
     *
     * @param config 当前节点配置，必须包含 {@code actionType}，并透传给被委派的权益处理器
     * @param ctx 画布执行上下文，提供租户、用户、执行实例和上游输出
     * @return 被委派处理器产生的节点结果，包含下一跳路由和上下文输出
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String actionType = string(config.get(MapFieldKeys.ACTION_TYPE));
        if (actionType == null || actionType.isBlank()) {
            return Mono.just(NodeResult.fail("COMMIT_ACTION: actionType 未配置"));
        }
        return switch (actionType) {
            case ACTION_ISSUE_COUPON -> couponHandler.executeAsync(config, ctx);
            case ACTION_POINTS -> pointsHandler.executeAsync(config, ctx);
            default -> Mono.just(NodeResult.fail("COMMIT_ACTION: 未知提交动作类型 " + actionType));
        };
    }

    /**
     * 标记提交动作节点属于权益副作用节点。
     *
     * <p>调度层使用该标记识别已发生关键外部副作用的节点，便于在后续节点失败时按防资损策略处理整体执行结果。
     *
     * @return 始终为 {@code true}
     */
    @Override
    public boolean isBenefitNode() {
        return true;
    }

    /**
     * 声明提交动作需要节点级副作用幂等。
     *
     * <p>提交动作会委派到发券或积分等外部系统；返回 {@code true} 后，调度层会先占用幂等记录，重复执行时复用已完成输出。
     *
     * @param config 当前节点配置，包含动作类型和可能的显式幂等键
     * @param ctx 画布执行上下文
     * @return 始终为 {@code true}
     */
    @Override
    public boolean requiresSideEffectIdempotency(Map<String, Object> config, ExecutionContext ctx) {
        return true;
    }

    /**
     * 构造提交动作副作用的业务操作键。
     *
     * <p>优先使用显式幂等键；否则按用户和动作类型生成稳定键。该键用于区分同一节点内不同提交动作，影响重复执行时
     * 是否跳过外部权益处理器调用。
     *
     * @param config 当前节点配置，读取 {@code idempotencyKey} 和 {@code actionType}
     * @param ctx 画布执行上下文，读取用户 ID
     * @return 用于节点副作用幂等表的业务操作键
     */
    @Override
    public String sideEffectOperationKey(Map<String, Object> config, ExecutionContext ctx) {
        Object explicit = config.get(MapFieldKeys.IDEMPOTENCY_KEY);
        if (explicit != null && !explicit.toString().isBlank()) {
            return explicit.toString();
        }
        return ctx.getUserId() + ":commit:" + string(config.get(MapFieldKeys.ACTION_TYPE));
    }

    /**
     * 将对象转换为字符串。
     *
     * @param value 原始值
     * @return 字符串值，null 保持为 null
     */
    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
