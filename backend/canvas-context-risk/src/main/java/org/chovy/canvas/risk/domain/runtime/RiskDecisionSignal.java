package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

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
public final class RiskDecisionSignal {

    /**
     * RiskDecisionSignal 的 source 字段。
     */
    private final String source;


    /**
     * RiskDecisionSignal 的 reason 字段。
     */
    private final String reason;


    /**
     * RiskDecisionSignal 的 action 字段。
     */
    private final RiskDecisionAction action;


    /**
     * RiskDecisionSignal 的 scoreDelta 字段。
     */
    private final int scoreDelta;


    /**
     * RiskDecisionSignal 的 shadowSignal 字段。
     */
    private final boolean shadowSignal;


    /**
     * RiskDecisionSignal 的 listType 字段。
     */
    private final RiskListType listType;


    /**
     * RiskDecisionSignal 的 order 字段。
     */
    private final int order;


    /**
     * RiskDecisionSignal 的 label 字段。
     */
    private final String label;


    /**
     * 创建 RiskDecisionSignal。
     *
     * @param source RiskDecisionSignal 的 source 字段
     * @param reason RiskDecisionSignal 的 reason 字段
     * @param action RiskDecisionSignal 的 action 字段
     * @param scoreDelta RiskDecisionSignal 的 scoreDelta 字段
     * @param shadowSignal RiskDecisionSignal 的 shadowSignal 字段
     * @param listType RiskDecisionSignal 的 listType 字段
     * @param order RiskDecisionSignal 的 order 字段
     * @param label RiskDecisionSignal 的 label 字段
     */
    public RiskDecisionSignal(String source, String reason, RiskDecisionAction action, int scoreDelta, boolean shadowSignal, RiskListType listType, int order, String label) {
        this.source = source;
        this.reason = reason;
        this.action = action;
        this.scoreDelta = scoreDelta;
        this.shadowSignal = shadowSignal;
        this.listType = listType;
        this.order = order;
        this.label = label;
    }

    /**
     * 返回 RiskDecisionSignal 的 source 字段。
     *
     * @return source 字段值
     */
    public String source() {
        return source;
    }

    /**
     * 返回 RiskDecisionSignal 的 reason 字段。
     *
     * @return reason 字段值
     */
    public String reason() {
        return reason;
    }

    /**
     * 返回 RiskDecisionSignal 的 action 字段。
     *
     * @return action 字段值
     */
    public RiskDecisionAction action() {
        return action;
    }

    /**
     * 返回 RiskDecisionSignal 的 scoreDelta 字段。
     *
     * @return scoreDelta 字段值
     */
    public int scoreDelta() {
        return scoreDelta;
    }

    /**
     * 返回 RiskDecisionSignal 的 shadowSignal 字段。
     *
     * @return shadowSignal 字段值
     */
    public boolean shadowSignal() {
        return shadowSignal;
    }

    /**
     * 返回 RiskDecisionSignal 的 listType 字段。
     *
     * @return listType 字段值
     */
    public RiskListType listType() {
        return listType;
    }

    /**
     * 返回 RiskDecisionSignal 的 order 字段。
     *
     * @return order 字段值
     */
    public int order() {
        return order;
    }

    /**
     * 返回 RiskDecisionSignal 的 label 字段。
     *
     * @return label 字段值
     */
    public String label() {
        return label;
    }

    /**
     * 比较当前 RiskDecisionSignal 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionSignal other)) {
            return false;
        }
        return Objects.equals(source, other.source)
                && Objects.equals(reason, other.reason)
                && Objects.equals(action, other.action)
                && scoreDelta == other.scoreDelta
                && shadowSignal == other.shadowSignal
                && Objects.equals(listType, other.listType)
                && order == other.order
                && Objects.equals(label, other.label);
    }

    /**
     * 计算 RiskDecisionSignal 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(source, reason, action, scoreDelta, shadowSignal, listType, order, label);
    }

    /**
     * 返回 RiskDecisionSignal 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionSignal[source=" + source + ", reason=" + reason + ", action=" + action + ", scoreDelta=" + scoreDelta + ", shadowSignal=" + shadowSignal + ", listType=" + listType + ", order=" + order + ", label=" + label + "]";
    }

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
