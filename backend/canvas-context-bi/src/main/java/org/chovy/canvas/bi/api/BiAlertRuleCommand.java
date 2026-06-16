package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiAlertRuleCommand 命令。
 */
public record BiAlertRuleCommand(
        /**
         * alertKey 对应的业务键。
         */
        String alertKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 指标键。
         */
        String metricKey,
        /**
         * condition 字段值。
         */
        Map<String, Object> condition,
        /**
         * receivers 对应的数据集合。
         */
        Map<String, Object> receivers,
        Boolean enabled) {

    public BiAlertRuleCommand {
        condition = condition == null ? Map.of() : Map.copyOf(condition);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
    }
}
