package org.chovy.canvas.engine.trigger;

import lombok.Data;
import org.chovy.canvas.common.enums.TriggerType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 触发优先级配置。
 *
 * <p>HIGH：高优先级触发，后续执行路径不丢弃，仅按独立上限记录告警。
 * NORMAL：标准并发控制，溢出时进 MQ 延迟重试。
 * LOW：使用 maxConcurrency x lowRatio 的更严格限制，溢出时直接丢弃。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution.priority")
public class TriggerPriorityConfig {

    /** 高优先级触发类型集合。 */
    private List<String> high = List.of(TriggerType.DIRECT_CALL);
    /** 标准优先级触发类型集合。 */
    private List<String> normal = List.of(TriggerType.MQ, "BEHAVIOR", "EVENT", "EVENT_TRIGGER", "API_CALL");
    /** 低优先级触发类型集合。 */
    private List<String> low = List.of(TriggerType.SCHEDULED);

    /** LOW 优先级并发系数，默认 0.5（即 maxConc x 0.5）。 */
    private double lowRatio = 0.5;

    /** HIGH 优先级告警并发系数，默认 2.0（即 maxConc x 2.0）。 */
    private double highMaxConcurrencyRatio = 2.0;

    /** 溢出延迟重试间隔（毫秒）。 */
    private long overflowRetryDelayMs = 5000;

    /** 溢出最大重试次数（超过后写 DLQ）。 */
    private int overflowMaxRetry = 3;

    public enum Priority {
        /** 高优先级触发，超限只告警并继续尝试执行。 */
        HIGH,
        /** 标准优先级触发，超限进入延迟重试。 */
        NORMAL,
        /** 低优先级触发，超限直接丢弃。 */
        LOW
    }

    /**
     * 执行 of 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param triggerType triggerType 类型标识或分类条件
     * @return 方法执行后的业务结果
     */
    public Priority of(String triggerType) {
        if (triggerType == null) {
            return Priority.NORMAL;
        }
        // 优先级用于并发溢出路由：HIGH 告警放行，NORMAL 延迟重试，LOW 直接丢弃。
        if (contains(high, triggerType)) {
            return Priority.HIGH;
        }
        if (contains(normal, triggerType)) {
            return Priority.NORMAL;
        }
        if (contains(low, triggerType)) {
            return Priority.LOW;
        }
        return Priority.NORMAL;
    }

    /**
     * 执行 contains 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param values values 待写入、比较或转换的业务值
     * @param value value 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean contains(List<String> values, String value) {
        return values != null && values.contains(value);
    }
}
