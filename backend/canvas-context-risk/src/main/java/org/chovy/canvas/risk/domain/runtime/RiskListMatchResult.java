package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

/**
 * 风控名单匹配结果。
 *
 * @param matched 是否命中名单
 * @param subjectHash 查询使用的主体哈希
 * @param subjectMasked 命中主体脱敏展示值
 * @param signal 命中后产生的决策信号
 */
public final class RiskListMatchResult {

    /**
     * RiskListMatchResult 的 matched 字段。
     */
    private final boolean matched;


    /**
     * RiskListMatchResult 的 subjectHash 字段。
     */
    private final String subjectHash;


    /**
     * RiskListMatchResult 的 subjectMasked 字段。
     */
    private final String subjectMasked;


    /**
     * RiskListMatchResult 的 signal 字段。
     */
    private final RiskDecisionSignal signal;


    /**
     * 创建 RiskListMatchResult。
     *
     * @param matched RiskListMatchResult 的 matched 字段
     * @param subjectHash RiskListMatchResult 的 subjectHash 字段
     * @param subjectMasked RiskListMatchResult 的 subjectMasked 字段
     * @param signal RiskListMatchResult 的 signal 字段
     */
    public RiskListMatchResult(boolean matched, String subjectHash, String subjectMasked, RiskDecisionSignal signal) {
        this.matched = matched;
        this.subjectHash = subjectHash;
        this.subjectMasked = subjectMasked;
        this.signal = signal;
    }

    /**
     * 返回 RiskListMatchResult 的 matched 字段。
     *
     * @return matched 字段值
     */
    public boolean matched() {
        return matched;
    }

    /**
     * 返回 RiskListMatchResult 的 subjectHash 字段。
     *
     * @return subjectHash 字段值
     */
    public String subjectHash() {
        return subjectHash;
    }

    /**
     * 返回 RiskListMatchResult 的 subjectMasked 字段。
     *
     * @return subjectMasked 字段值
     */
    public String subjectMasked() {
        return subjectMasked;
    }

    /**
     * 返回 RiskListMatchResult 的 signal 字段。
     *
     * @return signal 字段值
     */
    public RiskDecisionSignal signal() {
        return signal;
    }

    /**
     * 比较当前 RiskListMatchResult 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskListMatchResult other)) {
            return false;
        }
        return matched == other.matched
                && Objects.equals(subjectHash, other.subjectHash)
                && Objects.equals(subjectMasked, other.subjectMasked)
                && Objects.equals(signal, other.signal);
    }

    /**
     * 计算 RiskListMatchResult 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(matched, subjectHash, subjectMasked, signal);
    }

    /**
         * 创建未命中结果。
         */
        public static RiskListMatchResult none(String subjectHash) {
            return new RiskListMatchResult(false, subjectHash, null, null);
        }

        /**
         * 创建命中结果。
         */
        public static RiskListMatchResult matched(String subjectHash, String subjectMasked, RiskDecisionSignal signal) {
            return new RiskListMatchResult(true, subjectHash, subjectMasked, signal);
        }

        /**
         * 返回避免泄露原始主体的调试字符串。
         */
        @Override
        public String toString() {
            return "RiskListMatchResult[matched=" + matched
                    + ", subjectHashPresent=" + (subjectHash != null)
                    + ", subjectMasked=" + subjectMasked
                    + ", signal=" + signal + "]";
        }
}
