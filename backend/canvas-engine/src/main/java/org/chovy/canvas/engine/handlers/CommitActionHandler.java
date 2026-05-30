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
    private final NodeHandler couponHandler;
    private final NodeHandler pointsHandler;

    public CommitActionHandler(@Qualifier("couponHandler") NodeHandler couponHandler,
                               @Qualifier("pointsOperationHandler") NodeHandler pointsHandler) {
        this.couponHandler = couponHandler;
        this.pointsHandler = pointsHandler;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String actionType = string(config.get(MapFieldKeys.ACTION_TYPE));
        if (actionType == null || actionType.isBlank()) {
            return Mono.just(NodeResult.fail("COMMIT_ACTION: actionType 未配置"));
        }
        return switch (actionType) {
            case NodeType.COUPON -> couponHandler.executeAsync(config, ctx);
            case NodeType.POINTS_OPERATION -> pointsHandler.executeAsync(config, ctx);
            default -> Mono.just(NodeResult.fail("COMMIT_ACTION: 未知提交动作类型 " + actionType));
        };
    }

    @Override
    public boolean isBenefitNode() {
        return true;
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
