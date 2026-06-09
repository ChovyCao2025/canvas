package org.chovy.canvas.flink.risk;

import org.chovy.canvas.flink.CanvasFlinkPipelineRegistry;
import org.chovy.canvas.flink.CanvasFlinkSqlTemplateLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRealtimeFeatureJobTest {

    @Test
    void registryExposesRiskRealtimeFeaturePipeline() {
        assertThat(CanvasFlinkPipelineRegistry.isKnown(RiskRealtimeFeatureJob.PIPELINE_KEY)).isTrue();
        assertThat(CanvasFlinkPipelineRegistry.sqlAssetFor(RiskRealtimeFeatureJob.PIPELINE_KEY))
                .isEqualTo("sql/risk_realtime_features.sql");
    }

    @Test
    void jobConfigDeclaresSourceAndSinkSettings() {
        RiskRealtimeFeatureJob.JobSpec spec = RiskRealtimeFeatureJob.jobSpec();

        assertThat(spec.sourceTopic()).isEqualTo("canvas-risk-events");
        assertThat(spec.redisSinkKeyPattern()).isEqualTo("risk:feature:{tenantId}:{featureKey}:{subjectHash}");
        assertThat(spec.dorisSinkTable()).isEqualTo("canvas_dws.risk_realtime_feature_snapshot");
        assertThat(spec.windowFeatures()).containsExactly(
                "user.fail_count_1d",
                "user.success_count_1d",
                "device.change_user_1d",
                "ip.change_user_1h",
                "benefit.issue_amount_1d");
    }

    @Test
    void sqlDefinesFirstRealtimeFeatureWindows() {
        String sql = CanvasFlinkSqlTemplateLoader.load("sql/risk_realtime_features.sql");

        assertThat(sql)
                .contains("canvas-risk-events")
                .contains("risk:feature:{tenantId}:{featureKey}:{subjectHash}")
                .contains("canvas_dws.risk_realtime_feature_snapshot")
                .contains("user.fail_count_1d")
                .contains("user.success_count_1d")
                .contains("device.change_user_1d")
                .contains("ip.change_user_1h")
                .contains("benefit.issue_amount_1d")
                .contains("TUMBLE")
                .contains("INTERVAL '1' DAY")
                .contains("INTERVAL '1' HOUR");
    }
}
