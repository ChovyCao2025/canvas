package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直调返回节点：从上下文 / 自定义值构建同步返回给业务方的数据结构。
 *
 * <p>常见于 DIRECT_CALL 流程尾部，用于把流程内字段映射成业务 API 需要的响应结构。
 * 映射规则通常由前端配置面板维护。
 */
@Component
@NodeHandlerType("DIRECT_RETURN")
public class DirectReturnHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // data 每一项描述一个“返回字段映射规则”
        List<Map<String, Object>> data = (List<Map<String, Object>>) config.get("data");
        Map<String, Object> result = new HashMap<>();

        if (data != null) {
            for (Map<String, Object> item : data) {
                String name      = (String) item.get("name");
                String valueType = (String) item.get("valueType");
                String value     = (String) item.get("value");
                // name 作为响应字段名，value 作为字段来源或常量值
                // CONTEXT: 从上下文读取；其他类型按字面值回传
                if ("CONTEXT".equals(valueType)) {
                    // 从上下文取值时，value 表示字段 key
                    result.put(name, ctx.getContextValue(value));
                } else {
                    // 非 CONTEXT 视作常量值直接透传
                    result.put(name, value);
                }
            }
        }

        // 终止节点，output 即为返回给调用方的数据
        return Mono.just(NodeResult.terminal(result));
    }

    @Override
    public boolean isBenefitNode() { return false; }
}
