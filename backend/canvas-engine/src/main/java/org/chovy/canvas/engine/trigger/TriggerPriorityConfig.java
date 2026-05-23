package org.chovy.canvas.engine.trigger;

import lombok.Data;
import org.chovy.canvas.domain.constant.TriggerType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution.priority")
public class TriggerPriorityConfig {

    private List<String> high = List.of(TriggerType.DIRECT_CALL);
    private List<String> normal = List.of(TriggerType.MQ, "BEHAVIOR", "EVENT_TRIGGER", "API_CALL");
    private List<String> low = List.of(TriggerType.SCHEDULED);
    private double lowRatio = 0.5;
    private double highMaxConcurrencyRatio = 2.0;
    private long overflowRetryDelayMs = 5000;
    private int overflowMaxRetry = 3;

    public enum Priority {
        HIGH,
        NORMAL,
        LOW
    }

    public Priority of(String triggerType) {
        if (triggerType == null) {
            return Priority.NORMAL;
        }
        if (contains(high, triggerType)) {
            return Priority.HIGH;
        }
        if (contains(low, triggerType)) {
            return Priority.LOW;
        }
        return Priority.NORMAL;
    }

    private boolean contains(List<String> values, String value) {
        return values != null && values.contains(value);
    }
}
