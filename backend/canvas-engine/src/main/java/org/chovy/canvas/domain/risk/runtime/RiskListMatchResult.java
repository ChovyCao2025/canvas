package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控名单匹配结果。
 *
 * @param matched 是否命中名单
 * @param subjectHash 查询使用的主体哈希
 * @param subjectMasked 命中主体脱敏展示值
 * @param signal 命中后产生的决策信号
 */
public record RiskListMatchResult(
        boolean matched,
        String subjectHash,
        String subjectMasked,
        RiskDecisionSignal signal
) {

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
