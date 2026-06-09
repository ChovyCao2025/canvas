package org.chovy.canvas.common.enums;

/**
 * AudienceSnapshotMode 枚举 common.enums 场景中的固定业务取值。
 */
public enum AudienceSnapshotMode {
    STATIC_LOCKED,
    DYNAMIC_REFRESH;

    /**
     * normalize 校验或转换 common.enums 场景的数据。
     * @param raw raw 参数，用于 normalize 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public static AudienceSnapshotMode normalize(String raw) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (raw == null || raw.isBlank()) {
            return STATIC_LOCKED;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (AudienceSnapshotMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw.trim())) {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported audienceSnapshotMode: " + raw);
    }
}
