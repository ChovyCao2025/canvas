package org.chovy.canvas.domain.risk.runtime;

/**
 * 候选决策信号，由命中规则、名单命中或影子探测器产生。
 *
 * @param source 信号来源
 * @param reason 信号原因
 * @param action 建议动作
 * @param scoreDelta 分数增量
 * @param shadowSignal 是否仅作为影子信号
 * @param listType 名单类型，非名单信号可为空
 * @param order 稳定排序序号
 * @param label 信号标签
 */
public record RiskDecisionSignal(
        String source,
        String reason,
        RiskDecisionAction action,
        int scoreDelta,
        boolean shadowSignal,
        RiskListType listType,
        int order,
        String label
) {

    /**
     * 创建参与强制决策的有效信号。
     */
    public static RiskDecisionSignal effective(String source,
                                               String reason,
                                               RiskDecisionAction action,
                                               int scoreDelta) {
        return new RiskDecisionSignal(source, reason, action, scoreDelta, false, null, 0, null);
    }

    /**
     * 返回影子信号副本。
     */
    public RiskDecisionSignal shadowOnly() {
        return new RiskDecisionSignal(source, reason, action, scoreDelta, true, listType, order, label);
    }

    /**
     * 返回标记名单类型后的信号副本。
     */
    public RiskDecisionSignal fromList(RiskListType type) {
        return new RiskDecisionSignal(source, reason, action, scoreDelta, shadowSignal, type, order, label);
    }

    /**
     * 返回替换排序序号后的信号副本。
     */
    public RiskDecisionSignal withOrder(int newOrder) {
        return new RiskDecisionSignal(source, reason, action, scoreDelta, shadowSignal, listType, newOrder, label);
    }

    /**
     * 返回替换标签后的信号副本。
     */
    public RiskDecisionSignal withLabel(String newLabel) {
        return new RiskDecisionSignal(source, reason, action, scoreDelta, shadowSignal, listType, order, newLabel);
    }
}
