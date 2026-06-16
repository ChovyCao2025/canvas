package org.chovy.canvas.platform.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * 规范化后的平台工作流稳定键。
 */
public final class WorkstreamKey {

    /**
     * 已转为小写并通过格式校验的工作流键。
     */
    private final String value;

    /**
     * 创建工作流键并执行标准化与格式校验。
     *
     * @param value 原始工作流键
     */
    public WorkstreamKey(String value) {
        this.value = normalize(value);
    }

    /**
     * 返回已转为小写并通过格式校验的工作流键。
     *
     * @return 已转为小写并通过格式校验的工作流键
     */
    public String value() {
        return value;
    }

    /**
     * 将输入键转为平台内部统一格式。
     *
     * @param value 原始工作流键
     * @return 去除首尾空白并转为小写后的工作流键
     * @throws NullPointerException 当原始键为空时抛出
     * @throws IllegalArgumentException 当键不满足平台工作流命名规则时抛出
     */
    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "workstreamKey").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            // 只允许稳定、URL 友好的键，避免子规格路径和状态查询出现多种拼写。
            throw new IllegalArgumentException("invalid workstream key: " + value);
        }
        return normalized;
    }

    /**
     * 判断两个工作流键是否相同。
     *
     * @param object 待比较对象
     * @return 键值相同时返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof WorkstreamKey that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    /**
     * 计算工作流键哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * 返回与原 record 形态一致的字符串。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "WorkstreamKey[value=" + value + "]";
    }
}
