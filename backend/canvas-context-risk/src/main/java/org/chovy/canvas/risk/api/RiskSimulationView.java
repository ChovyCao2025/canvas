package org.chovy.canvas.risk.api;

import java.util.Map;
import java.util.Objects;

/**
 * 定义 RiskSimulationView 的风控模块职责和数据契约。
 */
public final class RiskSimulationView {

    /**
     * RiskSimulationView 的 simulationId 字段。
     */
    private final String simulationId;


    /**
     * RiskSimulationView 的 status 字段。
     */
    private final String status;


    /**
     * RiskSimulationView 的 sampleSize 字段。
     */
    private final int sampleSize;


    /**
     * RiskSimulationView 的 actionDistribution 字段。
     */
    private final Map<String, Integer> actionDistribution;


    /**
     * RiskSimulationView 的 changedActionCount 字段。
     */
    private final int changedActionCount;


    /**
     * RiskSimulationView 的 actionChanges 字段。
     */
    private final Map<String, Integer> actionChanges;


    /**
     * 创建 RiskSimulationView。
     *
     * @param simulationId RiskSimulationView 的 simulationId 字段
     * @param status RiskSimulationView 的 status 字段
     * @param sampleSize RiskSimulationView 的 sampleSize 字段
     * @param actionDistribution RiskSimulationView 的 actionDistribution 字段
     * @param changedActionCount RiskSimulationView 的 changedActionCount 字段
     * @param actionChanges RiskSimulationView 的 actionChanges 字段
     */
    public RiskSimulationView(String simulationId, String status, int sampleSize, Map<String, Integer> actionDistribution, int changedActionCount, Map<String, Integer> actionChanges) {
        this.simulationId = simulationId;
        this.status = status;
        this.sampleSize = sampleSize;
        this.actionDistribution = actionDistribution;
        this.changedActionCount = changedActionCount;
        this.actionChanges = actionChanges;
    }

    /**
     * 返回 RiskSimulationView 的 simulationId 字段。
     *
     * @return simulationId 字段值
     */
    public String simulationId() {
        return simulationId;
    }

    /**
     * 返回 RiskSimulationView 的 status 字段。
     *
     * @return status 字段值
     */
    public String status() {
        return status;
    }

    /**
     * 返回 RiskSimulationView 的 sampleSize 字段。
     *
     * @return sampleSize 字段值
     */
    public int sampleSize() {
        return sampleSize;
    }

    /**
     * 返回 RiskSimulationView 的 actionDistribution 字段。
     *
     * @return actionDistribution 字段值
     */
    public Map<String, Integer> actionDistribution() {
        return actionDistribution;
    }

    /**
     * 返回 RiskSimulationView 的 changedActionCount 字段。
     *
     * @return changedActionCount 字段值
     */
    public int changedActionCount() {
        return changedActionCount;
    }

    /**
     * 返回 RiskSimulationView 的 actionChanges 字段。
     *
     * @return actionChanges 字段值
     */
    public Map<String, Integer> actionChanges() {
        return actionChanges;
    }

    /**
     * 比较当前 RiskSimulationView 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskSimulationView other)) {
            return false;
        }
        return Objects.equals(simulationId, other.simulationId)
                && Objects.equals(status, other.status)
                && sampleSize == other.sampleSize
                && Objects.equals(actionDistribution, other.actionDistribution)
                && changedActionCount == other.changedActionCount
                && Objects.equals(actionChanges, other.actionChanges);
    }

    /**
     * 计算 RiskSimulationView 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(simulationId, status, sampleSize, actionDistribution, changedActionCount, actionChanges);
    }

    /**
     * 返回 RiskSimulationView 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskSimulationView[simulationId=" + simulationId + ", status=" + status + ", sampleSize=" + sampleSize + ", actionDistribution=" + actionDistribution + ", changedActionCount=" + changedActionCount + ", actionChanges=" + actionChanges + "]";
    }
}
