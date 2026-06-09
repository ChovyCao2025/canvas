package org.chovy.canvas.flink.risk;

import java.util.List;

/**
 * 风险实时特征 Flink 作业定义。
 */
public final class RiskRealtimeFeatureJob {

    public static final String PIPELINE_KEY = "risk_realtime_features";

    /**
     * 作业定义类不允许实例化。
     */
    private RiskRealtimeFeatureJob() {
    }

    /**
     * 构建风险实时特征作业规格。
     *
     * @return 包含源端 topic、Redis key 模板、Doris 表和窗口特征清单的作业规格
     */
    public static JobSpec jobSpec() {
        return new JobSpec(
                PIPELINE_KEY,
                "canvas-risk-events",
                "risk:feature:{tenantId}:{featureKey}:{subjectHash}",
                "canvas_dws.risk_realtime_feature_snapshot",
                List.of(
                        "user.fail_count_1d",
                        "user.success_count_1d",
                        "device.change_user_1d",
                        "ip.change_user_1h",
                        "benefit.issue_amount_1d"));
    }

    /**
     * 风险实时特征作业规格记录。
     *
     * @param pipelineKey 作业管道标识
     * @param sourceTopic 风险事件源端 topic
     * @param redisSinkKeyPattern Redis 特征快照 key 模板
     * @param dorisSinkTable Doris 特征快照目标表
     * @param windowFeatures 需要计算和落库的窗口特征列表
     */
    public record JobSpec(
            String pipelineKey,
            String sourceTopic,
            String redisSinkKeyPattern,
            String dorisSinkTable,
            List<String> windowFeatures
    ) {
        /**
         * 规范化窗口特征列表，避免外部修改影响作业规格。
         */
        public JobSpec {
            windowFeatures = windowFeatures == null ? List.of() : List.copyOf(windowFeatures);
        }
    }
}
