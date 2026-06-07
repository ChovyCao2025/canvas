package org.chovy.canvas.domain.bi.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.mapper.BiDatasetFieldMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.query.BiCompiledQuery;
import org.chovy.canvas.domain.bi.query.BiQueryExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BiSqlDatasetPreviewServiceTest {

    @Test
    void compilesAndExecutesParameterizedSqlSampleWithLineageImpact() {
        AtomicReference<BiCompiledQuery> capturedQuery = new AtomicReference<>();
        BiQueryExecutor executor = (query, dataset) -> {
            capturedQuery.set(query);
            return List.of(Map.of(
                    "stat_date", "2026-06-01",
                    "total_cost", 18.5));
        };
        BiSqlDatasetPreviewService service = new BiSqlDatasetPreviewService(resourceService(), executor);

        BiSqlDatasetPreviewResult result = service.preview(7L, new BiSqlDatasetPreviewCommand(
                parameterizedSqlDatasetResource(),
                Map.of("start_date", "2026-06-01"),
                5,
                true));

        assertThat(result.datasetKey()).isEqualTo("campaign_sql");
        assertThat(result.normalizedSqlTemplate())
                .isEqualTo("SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}");
        assertThat(result.compiledSql())
                .contains("FROM (SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset")
                .contains("WHERE tenant_id = ?")
                .contains("LIMIT 5");
        assertThat(result.parameterCount()).isEqualTo(3);
        assertThat(result.columns()).extracting(org.chovy.canvas.domain.bi.query.BiQueryColumn::key)
                .containsExactly("stat_date", "total_cost");
        assertThat(result.rows()).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("total_cost", 18.5));
        assertThat(result.sampleExecuted()).isTrue();
        assertThat(result.executionError()).isNull();
        assertThat(result.lineage().dataSourceConfigId()).isEqualTo(7L);
        assertThat(result.lineage().sourceTables()).containsExactly("campaign_daily");
        assertThat(result.lineage().parameterKeys()).containsExactly("start_date", "channel");
        assertThat(result.lineage().referencedFields()).containsExactly("stat_date", "total_cost");
        assertThat(result.lineage().referencedMetrics()).containsExactly("SUM(total_cost)");
        assertThat(result.lineage().approvalRequired()).isTrue();
        assertThat(result.impact().governanceGates())
                .contains("READ_ONLY_SQL_LINT", "TENANT_COLUMN_REQUIRED", "SQL_PARAMETER_BINDING",
                        "PUBLISH_APPROVAL_REQUIRED");
        assertThat(result.impact().warnings()).isEmpty();
        assertThat(capturedQuery.get().parameters()).containsExactly("2026-06-01", "PAID", 7L);
    }

    @Test
    void returnsCompiledLineageWhenSampleExecutionIsDisabled() {
        BiSqlDatasetPreviewService service = new BiSqlDatasetPreviewService(resourceService(),
                (query, dataset) -> List.of(Map.of("should_not_execute", true)));

        BiSqlDatasetPreviewResult result = service.preview(7L, new BiSqlDatasetPreviewCommand(
                sqlDatasetResource(),
                Map.of(),
                10,
                false));

        assertThat(result.sampleExecuted()).isFalse();
        assertThat(result.rows()).isEmpty();
        assertThat(result.impact().warnings()).contains("SAMPLE_EXECUTION_DISABLED");
        assertThat(result.lineage().sourceTables()).containsExactly("campaign_daily");
        assertThat(result.compiledSql()).contains("LIMIT 10");
    }

    private BiDatasetResourceService resourceService() {
        return new BiDatasetResourceService(
                mock(BiWorkspaceMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class),
                new ObjectMapper());
    }

    private BiDatasetResource sqlDatasetResource() {
        return new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                "SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0",
                "tenant_id",
                Map.of("dataSourceConfigId", 7L),
                List.of(
                        new BiDatasetFieldResource("stat_date", "Date", "stat_date", "DIMENSION", "DATE",
                                "DATE", null, "yyyy-MM-dd", null, true, "NORMAL", 10),
                        new BiDatasetFieldResource("total_cost", "Total Cost", "total_cost", "MEASURE",
                                "NUMBER", null, "SUM", "#,##0.00", "元", true, "NORMAL", 20)),
                List.of(new BiMetricResource("total_cost", "Total Cost", "SUM(total_cost)", "SUM", "NUMBER",
                        "元", "#,##0.00", List.of("stat_date"), "alice", "SQL metric", "ACTIVE")),
                "DRAFT",
                "CLIENT");
    }

    private BiDatasetResource parameterizedSqlDatasetResource() {
        return new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                "SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}",
                "tenant_id",
                Map.of(
                        "dataSourceConfigId", 7L,
                        "sqlParameters", List.of(
                                Map.of(
                                        "key", "start_date",
                                        "dataType", "DATE",
                                        "required", true),
                                Map.of(
                                        "key", "channel",
                                        "dataType", "STRING",
                                        "required", false,
                                        "defaultValue", "PAID",
                                        "allowedValues", List.of("PAID", "EMAIL")))),
                sqlDatasetResource().fields(),
                sqlDatasetResource().metrics(),
                "DRAFT",
                "CLIENT");
    }
}
