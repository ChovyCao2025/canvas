package org.chovy.canvas.bi.adapter.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.bi.domain.BiDataset;
import org.chovy.canvas.bi.domain.BiPermissionGrant;
import org.chovy.canvas.bi.domain.BiResourceKey;
import org.chovy.canvas.bi.domain.BiResourceStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * BiPersistenceMappingTest 测试类。
 */
class BiPersistenceMappingTest {
    /**
     * converter 字段值。
     */
    private final BiPersistenceConverter converter = new BiPersistenceConverter();

    /**
     * 执行 bi Do Classes Own Representative Legacy Tables Inside Bi Persistence Adapter 相关处理。
     */
    @Test
    void biDoClassesOwnRepresentativeLegacyTablesInsideBiPersistenceAdapter() {
        assertThat(BiWorkspaceDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_workspace");
        assertThat(BiDatasetDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_dataset");
        assertThat(BiDatasetFieldDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_dataset_field");
        assertThat(BiMetricDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_metric");
        assertThat(BiChartDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_chart");
        assertThat(BiDashboardDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_dashboard");
        assertThat(BiDashboardWidgetDO.class.getAnnotation(TableName.class).value()).isEqualTo("bi_dashboard_widget");
        assertThat(BiResourcePermissionDO.class.getAnnotation(TableName.class).value())
                .isEqualTo("bi_resource_permission");
        assertThat(BiDatasourceSchemaSnapshotDO.class.getAnnotation(TableName.class).value())
                .isEqualTo("bi_datasource_schema_snapshot");
        assertThat(BiDatasourceHealthSnapshotDO.class.getAnnotation(TableName.class).value())
                .isEqualTo("bi_datasource_health_snapshot");

        assertThat(BaseMapper.class).isAssignableFrom(BiWorkspaceMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiDatasetMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiDatasetFieldMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiMetricMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiChartMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiDashboardMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiDashboardWidgetMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiResourcePermissionMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiDatasourceSchemaSnapshotMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(BiDatasourceHealthSnapshotMapper.class);
    }
    /**
     * 执行 converter Round Trips Dataset And Permission Rows 相关处理。
     */
    @Test
    void converterRoundTripsDatasetAndPermissionRows() {
        BiDataset dataset = new BiDataset(
                10L,
                7L,
                5L,
                BiResourceKey.of("orders-daily", "datasetKey"),
                "Orders daily",
                "SQL",
                99L,
                "fact_order",
                "tenant_id",
                Map.of("grain", "day"),
                List.of(),
                List.of(),
                BiResourceStatus.PUBLISHED,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-02T00:00:00"));
        BiDatasetDO datasetRow = converter.toDatasetRow(dataset);

        assertThat(datasetRow.getModelJson()).contains("\"grain\":\"day\"");
        assertThat(converter.toDataset(datasetRow, List.of(), List.of()).model()).containsEntry("grain", "day");

        BiPermissionGrant permission = new BiPermissionGrant(
                20L,
                7L,
                5L,
                "DASHBOARD",
                10L,
                "USER",
                "alice",
                "VIEW",
                "DENY",
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"));
        BiResourcePermissionDO permissionRow = converter.toPermissionRow(permission);

        assertThat(permissionRow.getEffect()).isEqualTo("DENY");
        assertThat(converter.toPermissionGrant(permissionRow).subjectId()).isEqualTo("alice");
    }
}
