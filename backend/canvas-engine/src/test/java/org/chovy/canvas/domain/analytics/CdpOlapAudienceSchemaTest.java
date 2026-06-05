package org.chovy.canvas.domain.analytics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpOlapAudienceSchemaTest {

    @Test
    void mysqlMigrationCreatesMaterializationMetadata() throws Exception {
        String sql = readMigration("cdp_olap_audience_materialization");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_user_index")
                .contains("UNIQUE KEY uk_cdp_user_index_user")
                .contains("UNIQUE KEY uk_cdp_user_index_value")
                .contains("CREATE TABLE IF NOT EXISTS audience_bitmap_version")
                .contains("CREATE TABLE IF NOT EXISTS audience_materialization_run")
                .contains("CREATE TABLE IF NOT EXISTS audience_quality_check");
    }

    @Test
    void dorisDdlCreatesBehaviorFactAndAggregateTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/infrastructure/doris/cdp-audience-ddl.sql"));

        assertThat(sql)
                .contains("CREATE DATABASE IF NOT EXISTS canvas_ods")
                .contains("CREATE DATABASE IF NOT EXISTS canvas_dwd")
                .contains("CREATE DATABASE IF NOT EXISTS canvas_dws")
                .contains("canvas_ods.cdp_event_log")
                .contains("canvas_dwd.cdp_user_event_fact")
                .contains("canvas_dws.user_event_metric_daily");
    }
}
