package org.chovy.canvas.domain.bi.subscription;

import java.util.Map;

/**
 * BiAlertRuleCommand 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param alertKey alertKey 字段。
 * @param name name 字段。
 * @param datasetKey datasetKey 字段。
 * @param metricKey metricKey 字段。
 * @param condition condition 字段。
 * @param receivers receivers 字段。
 * @param enabled enabled 字段。
 */
public record BiAlertRuleCommand(
        String alertKey,
        String name,
        String datasetKey,
        String metricKey,
        Map<String, Object> condition,
        Map<String, Object> receivers,
        Boolean enabled
) {
    public BiAlertRuleCommand {
        condition = condition == null ? Map.of() : Map.copyOf(condition);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
    }
}
