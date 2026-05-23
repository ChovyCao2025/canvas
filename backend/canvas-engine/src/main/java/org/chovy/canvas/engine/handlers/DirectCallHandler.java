package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 业务直调触发节点：校验必填入参，将 inputParams 写入上下文。
 *
 * <p>适用于“业务系统同步调用画布”的入口场景。
 * 该节点本身不写输出，只做入参完整性校验。
 * `inputParams` 常见字段：
 * - name: 参数名
 * - required: 是否必填
 * - desc: 参数说明（仅用于前端展示）
 */
@Component
@NodeHandlerType("DIRECT_CALL")
public class DirectCallHandler implements NodeHandler {

    /**
     * 校验必填参数是否已在上下文中准备完成，校验通过后放行到下游。
     *
     * <p>注意：这里只校验“存在性”，不校验复杂格式（如手机号/金额）。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) config.get(MapFieldKeys.INPUT_PARAMS);
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        // 对每个参数仅做“是否存在”校验，类型校验交由业务节点自行处理
        if (inputParams != null) {
            for (Map<String, Object> param : inputParams) {
                Boolean required = (Boolean) param.get("required");
                String name      = (String) param.get("name");
                // required=false 时即使缺失也允许继续
                // 必填字段在上下文中缺失时直接失败，避免后续节点拿到空值
                if (Boolean.TRUE.equals(required) && ctx.getContextValue(name) == null) {
                    // 缺参直接 fail，避免后续节点在空值上继续执行
                    return Mono.just(NodeResult.fail("业务直调必填参数缺失: " + name));
                }
            }
        }

        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
    }
}
