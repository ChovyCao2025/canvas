package org.chovy.canvas.flink.risk;

import java.util.List;
import java.util.Objects;

/**
 * 风险实时特征 Flink 作业定义。
 *
 * <p>该类描述首批实时风险特征的源端、Redis key 模板、Doris 快照表和窗口特征清单，供注册表和测试共同校验部署契约。
 */
public final class RiskRealtimeFeatureJob {

    /**
     * 风险实时特征管道标识。
     */
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
     * 风险实时特征作业规格。
     */
    public static final class JobSpec {

        /**
         * 作业管道标识。
         */
        private final String pipelineKey;

        /**
         * 风险事件源端 topic。
         */
        private final String sourceTopic;

        /**
         * Redis 特征快照 key 模板。
         */
        private final String redisSinkKeyPattern;

        /**
         * Doris 特征快照目标表。
         */
        private final String dorisSinkTable;

        /**
         * 需要计算和落库的窗口特征列表。
         */
        private final List<String> windowFeatures;

        /**
         * 创建风险实时特征作业规格。
         *
         * @param pipelineKey 作业管道标识
         * @param sourceTopic 风险事件源端 topic
         * @param redisSinkKeyPattern Redis 特征快照 key 模板
         * @param dorisSinkTable Doris 特征快照目标表
         * @param windowFeatures 需要计算和落库的窗口特征列表
         */
        public JobSpec(String pipelineKey,
                       String sourceTopic,
                       String redisSinkKeyPattern,
                       String dorisSinkTable,
                       List<String> windowFeatures) {
            this.pipelineKey = pipelineKey;
            this.sourceTopic = sourceTopic;
            this.redisSinkKeyPattern = redisSinkKeyPattern;
            this.dorisSinkTable = dorisSinkTable;
            // 对外部传入列表做不可变拷贝，避免部署契约被后续修改影响。
            this.windowFeatures = windowFeatures == null ? List.of() : List.copyOf(windowFeatures);
        }

        /**
         * 返回作业管道标识。
         *
         * @return 作业管道标识
         */
        public String pipelineKey() {
            return pipelineKey;
        }

        /**
         * 返回风险事件源端 topic。
         *
         * @return 风险事件源端 topic
         */
        public String sourceTopic() {
            return sourceTopic;
        }

        /**
         * 返回 Redis 特征快照 key 模板。
         *
         * @return Redis 特征快照 key 模板
         */
        public String redisSinkKeyPattern() {
            return redisSinkKeyPattern;
        }

        /**
         * 返回 Doris 特征快照目标表。
         *
         * @return Doris 特征快照目标表
         */
        public String dorisSinkTable() {
            return dorisSinkTable;
        }

        /**
         * 返回需要计算和落库的窗口特征列表。
         *
         * @return 不可变窗口特征列表
         */
        public List<String> windowFeatures() {
            return windowFeatures;
        }

        /**
         * 按字段值判断两个作业规格是否相同。
         *
         * @param o 待比较对象
         * @return true 表示所有字段相同
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof JobSpec jobSpec)) {
                return false;
            }
            return Objects.equals(pipelineKey, jobSpec.pipelineKey)
                    && Objects.equals(sourceTopic, jobSpec.sourceTopic)
                    && Objects.equals(redisSinkKeyPattern, jobSpec.redisSinkKeyPattern)
                    && Objects.equals(dorisSinkTable, jobSpec.dorisSinkTable)
                    && Objects.equals(windowFeatures, jobSpec.windowFeatures);
        }

        /**
         * 基于所有字段生成 hashCode。
         *
         * @return 字段组合哈希值
         */
        @Override
        public int hashCode() {
            int result = Objects.hashCode(pipelineKey);
            result = 31 * result + Objects.hashCode(sourceTopic);
            result = 31 * result + Objects.hashCode(redisSinkKeyPattern);
            result = 31 * result + Objects.hashCode(dorisSinkTable);
            result = 31 * result + Objects.hashCode(windowFeatures);
            return result;
        }

        /**
         * 返回与原 record 形式一致的调试字符串。
         *
         * @return 字段名和值组成的字符串
         */
        @Override
        public String toString() {
            return "JobSpec["
                    + "pipelineKey=" + pipelineKey
                    + ", sourceTopic=" + sourceTopic
                    + ", redisSinkKeyPattern=" + redisSinkKeyPattern
                    + ", dorisSinkTable=" + dorisSinkTable
                    + ", windowFeatures=" + windowFeatures
                    + ']';
        }
    }
}
