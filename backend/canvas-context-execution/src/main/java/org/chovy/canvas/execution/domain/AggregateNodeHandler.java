package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 定义 AggregateNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType("AGGREGATE")
public class AggregateNodeHandler implements NodeHandler {

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        List<String> upstreamIds = NodeHandlerSupport.stringList(context.node().config().get("upstreamIds"));
        long totalCount = upstreamIds.size();
        long successCount = upstreamIds.stream()
                .filter(upstreamId -> isSuccess(upstreamStatus(context, upstreamId)))
                .count();
        long failCount = totalCount - successCount;
        double successRate = totalCount == 0 ? 100.0 : successCount * 100.0 / totalCount;
        boolean passed = passed(context.node().config(), successCount, successRate);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("successCount", successCount);
        output.put("failCount", failCount);
        output.put("totalCount", totalCount);
        output.put("successRate", Math.round(successRate * 10.0) / 10.0);
        output.put("passed", passed);

        String target = passed
                ? NodeHandlerSupport.string(context.node().config().get("successNodeId"), null)
                : NodeHandlerSupport.string(context.node().config().get("failNodeId"), null);
        if (target == null) {
            return NodeExecutionResult.success(output);
        }
        return NodeExecutionResult.routed(output, Map.of(passed ? "success" : "fail", target));
    }

    /**
     * 执行 passed 对应的业务处理。
     * @param config config 参数
     * @param successCount successCount 参数
     * @param successRate successRate 参数
     * @return 处理后的结果
     */
    private boolean passed(Map<String, Object> config, long successCount, double successRate) {
        String mode = NodeHandlerSupport.upper(config.get("evaluateMode"), "COUNT");
        return switch (mode) {
            case "RATE" -> {
                Number minRate = NodeHandlerSupport.number(config.get("minRate"));
                yield successRate >= (minRate == null ? 100.0 : minRate.doubleValue());
            }
            case "COUNT" -> {
                Number minCount = NodeHandlerSupport.number(config.get("minCount"));
                yield successCount >= (minCount == null ? 1L : minCount.longValue());
            }
            default -> false;
        };
    }

    /**
     * 执行 upstreamStatus 对应的业务处理。
     * @param context context 参数
     * @param upstreamId upstreamId 参数
     */
    private Object upstreamStatus(NodeExecutionContext context, String upstreamId) {
        Object status = fromMap(context.contextData().get("nodeStatuses"), upstreamId);
        if (status != null) {
            return status;
        }
        status = context.contextData().get("nodeStatus." + upstreamId);
        if (status != null) {
            return status;
        }
        Object output = fromMap(context.contextData().get("nodeOutputs"), upstreamId);
        if (output instanceof Map<?, ?> map) {
            Object outputStatus = map.get("status");
            if (outputStatus != null) {
                return outputStatus;
            }
            if (map.containsKey("success")) {
                return map.get("success");
            }
            return "SUCCESS";
        }
        if (output != null) {
            return "SUCCESS";
        }
        return null;
    }

    /**
     * 执行 fromMap 对应的业务处理。
     * @param value value 参数
     * @param key key 参数
     */
    private Object fromMap(Object value, String key) {
        if (value instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    /**
     * 执行 isSuccess 对应的业务处理。
     * @param status status 参数
     */
    private boolean isSuccess(Object status) {
        if (status instanceof Boolean value) {
            return value;
        }
        String normalized = NodeHandlerSupport.upper(status, "");
        return "SUCCESS".equals(normalized) || "COMPLETED".equals(normalized) || "PASSED".equals(normalized);
    }
}
