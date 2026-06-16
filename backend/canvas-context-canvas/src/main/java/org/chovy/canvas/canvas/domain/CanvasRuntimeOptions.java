package org.chovy.canvas.canvas.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 承载CanvasRuntimeOptions的数据快照。
 */
public record CanvasRuntimeOptions(
        /**
         * 记录triggerType。
         */
        String triggerType,
        /**
         * 记录cronExpression。
         */
        String cronExpression,
        /**
         * 记录validStart。
         */
        String validStart,
        /**
         * 记录validEnd。
         */
        String validEnd,
        /**
         * 记录maxTotalExecutions。
         */
        Integer maxTotalExecutions,
        /**
         * 记录perUserDailyLimit。
         */
        Integer perUserDailyLimit,
        /**
         * 记录perUserTotalLimit。
         */
        Integer perUserTotalLimit,
        /**
         * 记录cooldownSeconds。
         */
        Integer cooldownSeconds,
        /**
         * 记录controlGroupPercent。
         */
        Integer controlGroupPercent,
        /**
         * 记录controlGroupSalt。
         */
        String controlGroupSalt,
        /**
         * 记录conversionEventCode。
         */
        String conversionEventCode,
        /**
         * 记录attributionWindowDays。
         */
        Integer attributionWindowDays,
        /**
         * 记录attributionModel。
         */
        String attributionModel) {

    /**
     * 创建不携带运行时选项的空配置。
     */
    public static CanvasRuntimeOptions empty() {
        return new CanvasRuntimeOptions(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    /**
     * 保存内存实现使用的to execution options映射数据。
     */
    public Map<String, Object> toExecutionOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        putIfPresent(options, "triggerType", triggerType);
        putIfPresent(options, "cronExpression", cronExpression);
        putIfPresent(options, "validStart", validStart);
        putIfPresent(options, "validEnd", validEnd);
        putIfPresent(options, "maxTotalExecutions", maxTotalExecutions);
        putIfPresent(options, "perUserDailyLimit", perUserDailyLimit);
        putIfPresent(options, "perUserTotalLimit", perUserTotalLimit);
        putIfPresent(options, "cooldownSeconds", cooldownSeconds);
        putIfPresent(options, "controlGroupPercent", controlGroupPercent);
        putIfPresent(options, "controlGroupSalt", controlGroupSalt);
        putIfPresent(options, "conversionEventCode", conversionEventCode);
        putIfPresent(options, "attributionWindowDays", attributionWindowDays);
        putIfPresent(options, "attributionModel", attributionModel);
        return Map.copyOf(options);
    }

    /**
     * 在值存在时写入执行选项映射。
     */
    private static void putIfPresent(Map<String, Object> options, String key, Object value) {
        if (value != null) {
            options.put(key, value);
        }
    }
}
