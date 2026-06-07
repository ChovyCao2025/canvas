package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlTest {

    @Test
    void gracefulShutdownIsConfiguredWithBoundedDrainTimeout() {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
        assertThat(properties.getProperty("canvas.execution.shutdown-drain-timeout-ms")).isEqualTo("10000");
        assertThat(properties.getProperty("canvas.background-tasks.shutdown-timeout-ms")).isEqualTo("5000");
    }

    @Test
    void dorisEnterpriseOlapMetricsUrlsAreConfigurable() {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("canvas.doris.fe-metrics-urls"))
                .isEqualTo("${CANVAS_DORIS_FE_METRICS_URLS:}");
        assertThat(properties.getProperty("canvas.doris.be-metrics-urls"))
                .isEqualTo("${CANVAS_DORIS_BE_METRICS_URLS:}");
        assertThat(properties.getProperty("canvas.doris.query-slo-sql"))
                .isEqualTo("${CANVAS_DORIS_QUERY_SLO_SQL:}");
    }

    @Test
    void enterpriseOlapEvidenceSchedulerIsDisabledByDefault() {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("canvas.warehouse.enterprise-olap-evidence-scheduler.enabled"))
                .isEqualTo("${CANVAS_ENTERPRISE_OLAP_EVIDENCE_SCHEDULER_ENABLED:false}");
        assertThat(properties.getProperty("canvas.warehouse.enterprise-olap-evidence-scheduler.fixed-delay-ms"))
                .isEqualTo("${CANVAS_ENTERPRISE_OLAP_EVIDENCE_SCHEDULER_FIXED_DELAY_MS:300000}");
        assertThat(properties.getProperty("canvas.warehouse.enterprise-olap-evidence-scheduler.actor"))
                .isEqualTo("${CANVAS_ENTERPRISE_OLAP_EVIDENCE_SCHEDULER_ACTOR:enterprise-olap-evidence-scheduler}");
    }

    @Test
    void marketingAssetUploadS3HandoffIsDisabledByDefaultAndConfigurable() {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("canvas.marketing.content.asset-upload.s3.enabled"))
                .isEqualTo("${CANVAS_MARKETING_ASSET_UPLOAD_S3_ENABLED:false}");
        assertThat(properties.getProperty("canvas.marketing.content.asset-upload.s3.endpoint"))
                .isEqualTo("${CANVAS_MARKETING_ASSET_UPLOAD_S3_ENDPOINT:}");
        assertThat(properties.getProperty("canvas.marketing.content.asset-upload.s3.public-base-url"))
                .isEqualTo("${CANVAS_MARKETING_ASSET_UPLOAD_S3_PUBLIC_BASE_URL:}");
    }

    private Properties applicationProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
