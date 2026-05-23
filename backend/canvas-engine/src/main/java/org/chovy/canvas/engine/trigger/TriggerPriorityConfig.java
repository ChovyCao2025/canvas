package org.chovy.canvas.engine.trigger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 触发优先级配置。
 * HIGH：高优先级触发，后续执行路径不丢弃，仅按独立上限记录告警。
 * NORMAL：标准并发控制，溢出时进 MQ 延迟重试。
 * LOW：使用 maxConcurrency x lowRatio 的更严格限制，溢出时直接丢弃。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution.priority")
public class TriggerPriorityConfig {

    private List<String> high = List.of("DIRECT_CALL");
    private List<String> normal = List.of("MQ", "BEHAVIOR", "EVENT", "EVENT_TRIGGER", "API_CALL");
    private List<String> low = List.of("SCHEDULED");

    /** LOW 优先级并发系数，默认 0.5（即 maxConc x 0.5）。 */
    private double lowRatio = 0.5;

    /** HIGH 优先级告警并发系数，默认 2.0（即 maxConc x 2.0）。 */
    private double highMaxConcurrencyRatio = 2.0;

    /** 溢出延迟重试间隔（毫秒）。 */
    private long overflowRetryDelayMs = 5000;

    /** 溢出最大重试次数（超过后写 DLQ）。 */
    private int overflowMaxRetry = 3;

    public enum Priority { HIGH, NORMAL, LOW }

    public Priority of(String triggerType) {
        if (triggerType == null) {
            return Priority.NORMAL;
        }
        if (high.contains(triggerType)) {
            return Priority.HIGH;
        }
        if (normal.contains(triggerType)) {
            return Priority.NORMAL;
        }
        if (low.contains(triggerType)) {
            return Priority.LOW;
        }
        return Priority.NORMAL;
    }
}
