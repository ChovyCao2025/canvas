package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractMaterializationResult;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreviewRequest;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcBiDatasetExtractMaterializerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void materializesApiDatasourceRowsIntoExtractTableAndFiltersTenantRows() {
        JdbcTemplate jdbcTemplate = h2JdbcTemplate();
        jdbcTemplate.execute("CREATE SCHEMA bi_extract");
        BiDatasourceRuntimeService runtimeService = mock(BiDatasourceRuntimeService.class);
        when(runtimeService.previewApiData(eq(81L), eq(7L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(apiPreview());
        JdbcBiDatasetExtractMaterializer materializer = new JdbcBiDatasetExtractMaterializer(
                jdbcTemplate,
                null,
                "bi_extract",
                CLOCK,
                runtimeService);

        BiDatasetExtractMaterializationResult result = materializer.materialize(
                7L,
                apiDataset(),
                policy(25L));

        assertThat(result.materializedTable()).isEqualTo("bi_extract.t7_api_orders_20260606101530");
        assertThat(result.rowCount()).isEqualTo(1L);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tenant_id, order_id, amount FROM " + result.materializedTable());
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("TENANT_ID")).isEqualTo(7.0);
            assertThat(row.get("ORDER_ID")).isEqualTo("O-1");
            assertThat(row.get("AMOUNT")).isEqualTo(12.5);
        });

        ArgumentCaptor<BiDatasourceApiPreviewRequest> request =
                ArgumentCaptor.forClass(BiDatasourceApiPreviewRequest.class);
        verify(runtimeService).previewApiData(eq(81L), eq(7L), request.capture());
        assertThat(request.getValue().limit()).isEqualTo(25);
        assertThat(request.getValue().variables())
                .containsEntry("tenantId", "7")
                .containsEntry("campaignId", "cmp-9");
    }

    @Test
    void materializesUploadedFileDatasourceRowsIntoExtractTableAndInjectsTenantColumn() {
        JdbcTemplate jdbcTemplate = h2JdbcTemplate();
        jdbcTemplate.execute("CREATE SCHEMA bi_extract");
        BiDatasourceRuntimeService runtimeService = mock(BiDatasourceRuntimeService.class);
        when(runtimeService.previewFileData(eq(91L), eq(7L), eq(25)))
                .thenReturn(filePreview());
        JdbcBiDatasetExtractMaterializer materializer = new JdbcBiDatasetExtractMaterializer(
                jdbcTemplate,
                null,
                "bi_extract",
                CLOCK,
                runtimeService);

        BiDatasetExtractMaterializationResult result = materializer.materialize(
                7L,
                uploadedFileDataset(),
                policy("uploaded_orders", 25L));

        assertThat(result.materializedTable()).isEqualTo("bi_extract.t7_uploaded_orders_20260606101530");
        assertThat(result.rowCount()).isEqualTo(2L);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tenant_id, order_id, amount FROM " + result.materializedTable() + " ORDER BY order_id");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("TENANT_ID", 7.0)
                .containsEntry("ORDER_ID", "O-1")
                .containsEntry("AMOUNT", 12.5);
        assertThat(rows.get(1)).containsEntry("TENANT_ID", 7.0)
                .containsEntry("ORDER_ID", "O-2")
                .containsEntry("AMOUNT", 7.0);

        verify(runtimeService).previewFileData(eq(91L), eq(7L), eq(25));
    }

    private JdbcTemplate h2JdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:bi_extract_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return new JdbcTemplate(dataSource);
    }

    private BiDatasetSpec apiDataset() {
        Map<String, BiFieldSpec> fields = new LinkedHashMap<>();
        fields.put("tenant_id", new BiFieldSpec("tenant_id", "tenant_id", BiFieldSpec.Role.DIMENSION, "NUMBER"));
        fields.put("order_id", new BiFieldSpec("order_id", "order_id", BiFieldSpec.Role.DIMENSION, "STRING"));
        fields.put("amount", new BiFieldSpec("amount", "amount", BiFieldSpec.Role.MEASURE, "NUMBER"));
        return new BiDatasetSpec(
                "api_orders",
                "api_response",
                "tenant_id",
                fields,
                Map.of("amount", new BiMetricSpec("amount", "SUM(amount)", "NUMBER", List.of("order_id"))),
                List.of(),
                Map.of(
                        "dataSourceConfigId", 81L,
                        "sourceKey", "api-81",
                        "connectorType", "API",
                        "apiResponseVariables", Map.of(
                                "tenantId", "7",
                                "campaignId", "cmp-9")));
    }

    private BiDatasetAccelerationPolicyView policy(Long maxRows) {
        return policy("api_orders", maxRows);
    }

    private BiDatasetAccelerationPolicyView policy(String datasetKey, Long maxRows) {
        return new BiDatasetAccelerationPolicyView(
                datasetKey,
                true,
                "EXTRACT",
                "MANUAL",
                60L,
                300L,
                maxRows,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }

    private BiDatasetSpec uploadedFileDataset() {
        Map<String, BiFieldSpec> fields = new LinkedHashMap<>();
        fields.put("tenant_id", new BiFieldSpec("tenant_id", "tenant_id", BiFieldSpec.Role.DIMENSION, "NUMBER"));
        fields.put("order_id", new BiFieldSpec("order_id", "order_id", BiFieldSpec.Role.DIMENSION, "STRING"));
        fields.put("amount", new BiFieldSpec("amount", "amount", BiFieldSpec.Role.MEASURE, "NUMBER"));
        return new BiDatasetSpec(
                "uploaded_orders",
                "orders",
                "tenant_id",
                fields,
                Map.of("amount", new BiMetricSpec("amount", "SUM(amount)", "NUMBER", List.of("order_id"))),
                List.of(),
                Map.of(
                        "dataSourceConfigId", 91L,
                        "sourceKey", "file-91",
                        "connectorType", "CSV_EXCEL"));
    }

    private BiDatasourceApiPreview filePreview() {
        return new BiDatasourceApiPreview(
                91L,
                "file-91",
                "Uploaded Orders",
                "CSV_EXCEL",
                List.of(
                        new BiQueryColumn("order_id", "DIMENSION", "STRING"),
                        new BiQueryColumn("amount", "MEASURE", "NUMBER")),
                List.of(
                        Map.of("order_id", "O-1", "amount", 12.5),
                        Map.of("order_id", "O-2", "amount", 7.0)),
                2,
                false,
                12L,
                LocalDateTime.of(2026, 6, 6, 10, 15, 30));
    }

    private BiDatasourceApiPreview apiPreview() {
        return new BiDatasourceApiPreview(
                81L,
                "api-81",
                "Orders API",
                "API",
                List.of(
                        new BiQueryColumn("tenant_id", "DIMENSION", "NUMBER"),
                        new BiQueryColumn("order_id", "DIMENSION", "STRING"),
                        new BiQueryColumn("amount", "MEASURE", "NUMBER")),
                List.of(
                        Map.of("tenant_id", 7, "order_id", "O-1", "amount", 12.5),
                        Map.of("tenant_id", 8, "order_id", "O-2", "amount", 99.0)),
                2,
                false,
                37L,
                LocalDateTime.of(2026, 6, 6, 10, 15, 30));
    }
}
