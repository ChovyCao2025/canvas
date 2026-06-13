package org.chovy.canvas.execution.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@NodeHandlerType("WAIT")
public class WaitNodeHandler implements NodeHandler {

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String resumeStatus = resumeStatus(context);
        if ("TIMEOUT".equals(resumeStatus) || "EXPIRED".equals(resumeStatus)) {
            String timeoutNodeId = NodeHandlerSupport.string(context.node().config().get("timeoutNodeId"), null);
            return NodeExecutionResult.routed(
                    Map.of("waitStatus", "TIMEOUT"),
                    timeoutNodeId == null ? Map.of() : Map.of("timeout", timeoutNodeId));
        }
        if ("COMPLETED".equals(resumeStatus)) {
            String successNodeId = successNodeId(context.node().config());
            return NodeExecutionResult.routed(
                    Map.of("waitStatus", "COMPLETED"),
                    successNodeId == null ? Map.of() : Map.of("success", successNodeId));
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("waitStatus", "PENDING");
        output.put("waitType", NodeHandlerSupport.upper(
                context.node().config().getOrDefault("waitType", context.node().config().get("wait_type")),
                "DURATION"));
        output.put("sourceNodeId", context.node().nodeId());
        putIfPresent(output, "successNodeId", successNodeId(context.node().config()));
        putIfPresent(output, "timeoutNodeId", NodeHandlerSupport.string(context.node().config().get("timeoutNodeId"), null));
        long delayMillis = durationMillis(context.node().config().getOrDefault(
                "duration",
                context.node().config().get("maxWait")));
        if (delayMillis > 0) {
            output.put("resumeDelayMillis", delayMillis);
        }
        return NodeExecutionResult.pending(output);
    }

    private String resumeStatus(NodeExecutionContext context) {
        Object configured = context.node().config().getOrDefault("waitResumeStatus", context.node().config().get("resumeStatus"));
        Object payload = context.payload().getOrDefault("waitResumeStatus", context.payload().get("resumeStatus"));
        Object contextValue = context.contextData().getOrDefault("waitResumeStatus", context.contextData().get("resumeStatus"));
        return NodeHandlerSupport.upper(configured != null ? configured : payload != null ? payload : contextValue, "");
    }

    private String successNodeId(Map<String, Object> config) {
        String next = NodeHandlerSupport.string(config.get("nextNodeId"), null);
        return next == null ? NodeHandlerSupport.string(config.get("successNodeId"), null) : next;
    }

    private long durationMillis(Object value) {
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue()).toMillis();
        }
        if (value instanceof Map<?, ?> map) {
            Object amountValue = map.containsKey("value") ? map.get("value") : map.get("durationValue");
            Number amount = NodeHandlerSupport.number(amountValue);
            Object unitValue = map.containsKey("unit") ? map.get("unit") : map.get("durationUnit");
            String unit = NodeHandlerSupport.upper(unitValue, "SECONDS");
            return amount == null ? 0 : duration(amount.longValue(), unit).toMillis();
        }
        return 0;
    }

    private Duration duration(long amount, String unit) {
        return switch (unit) {
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            case "DAY", "DAYS" -> Duration.ofDays(amount);
            default -> Duration.ofSeconds(amount);
        };
    }

    private void putIfPresent(Map<String, Object> output, String key, String value) {
        if (value != null) {
            output.put(key, value);
        }
    }
}
