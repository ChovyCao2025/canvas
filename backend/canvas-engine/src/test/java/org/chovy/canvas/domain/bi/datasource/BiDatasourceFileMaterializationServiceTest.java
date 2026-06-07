package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFieldResource;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.domain.bi.dataset.BiMetricResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasourceFileMaterializationServiceTest {

    @Test
    void uploadsSyncsCreatesDatasetAndRefreshesExtractMaterialization() {
        BiDatasourceFileUploadService uploadService = mock(BiDatasourceFileUploadService.class);
        BiDatasourceRuntimeService runtimeService = mock(BiDatasourceRuntimeService.class);
        BiDatasetFromDatasourceService datasetService = mock(BiDatasetFromDatasourceService.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        byte[] content = "order_id,amount\nO-1,12.5\n".getBytes(StandardCharsets.UTF_8);
        BiDatasourceFileMaterializationCommand command = new BiDatasourceFileMaterializationCommand(
                "Uploaded Orders",
                "Browser CSV upload",
                null,
                ",",
                true,
                "UTF-8",
                null,
                null,
                "tenant_id",
                200,
                5_000L);
        when(uploadService.upload(eq(7L), eq("alice"), eq("orders.csv"), eq(content), any(BiDatasourceFileUploadCommand.class)))
                .thenReturn(source());
        when(runtimeService.syncSchema(91L, 7L, "alice", 200, null))
                .thenReturn(fileSnapshot());
        when(datasetService.createTableDataset(eq(7L), eq("alice"), eq("TENANT_ADMIN"), any(BiDatasetFromDatasourceCommand.class)))
                .thenReturn(dataset());
        when(accelerationService.upsertPolicy(eq(7L), eq("file_91_orders"), any(BiDatasetAccelerationPolicyCommand.class), eq("alice")))
                .thenReturn(policy());
        when(accelerationService.refreshNow(7L, "file_91_orders", "alice"))
                .thenReturn(refreshRun());
        BiDatasourceFileMaterializationService service = new BiDatasourceFileMaterializationService(
                uploadService,
                runtimeService,
                datasetService,
                accelerationService);

        BiDatasourceFileMaterializationResult result = service.uploadAndMaterialize(
                7L,
                "alice",
                "TENANT_ADMIN",
                "orders.csv",
                content,
                command);

        assertThat(result.source().id()).isEqualTo(91L);
        assertThat(result.schemaSnapshot().sourceKey()).isEqualTo("file-91");
        assertThat(result.dataset().datasetKey()).isEqualTo("file_91_orders");
        assertThat(result.accelerationPolicy().accelerationMode()).isEqualTo("EXTRACT");
        assertThat(result.refreshRun().materializedTable()).isEqualTo("bi_extract.t7_file_91_orders_20260606101530");

        ArgumentCaptor<BiDatasourceFileUploadCommand> uploadCommand =
                ArgumentCaptor.forClass(BiDatasourceFileUploadCommand.class);
        verify(uploadService).upload(eq(7L), eq("alice"), eq("orders.csv"), eq(content), uploadCommand.capture());
        assertThat(uploadCommand.getValue().name()).isEqualTo("Uploaded Orders");
        assertThat(uploadCommand.getValue().delimiter()).isEqualTo(",");

        ArgumentCaptor<BiDatasetFromDatasourceCommand> datasetCommand =
                ArgumentCaptor.forClass(BiDatasetFromDatasourceCommand.class);
        verify(datasetService).createTableDataset(eq(7L), eq("alice"), eq("TENANT_ADMIN"), datasetCommand.capture());
        assertThat(datasetCommand.getValue().dataSourceConfigId()).isEqualTo(91L);
        assertThat(datasetCommand.getValue().tableName()).isEqualTo("orders");
        assertThat(datasetCommand.getValue().datasetKey()).isEqualTo("file_91_orders");
        assertThat(datasetCommand.getValue().name()).isEqualTo("Uploaded Orders orders");
        assertThat(datasetCommand.getValue().tenantColumn()).isEqualTo("tenant_id");
        assertThat(datasetCommand.getValue().selectedColumns()).containsExactly("order_id", "amount");

        ArgumentCaptor<BiDatasetAccelerationPolicyCommand> policyCommand =
                ArgumentCaptor.forClass(BiDatasetAccelerationPolicyCommand.class);
        verify(accelerationService).upsertPolicy(eq(7L), eq("file_91_orders"), policyCommand.capture(), eq("alice"));
        assertThat(policyCommand.getValue().enabled()).isTrue();
        assertThat(policyCommand.getValue().accelerationMode()).isEqualTo("EXTRACT");
        assertThat(policyCommand.getValue().refreshMode()).isEqualTo("MANUAL");
        assertThat(policyCommand.getValue().maxRows()).isEqualTo(5_000L);
    }

    private BiDatasourceOnboardingView source() {
        return new BiDatasourceOnboardingView(
                91L,
                "file-91",
                "Uploaded Orders",
                "FILE",
                "CSV_EXCEL",
                true,
                "FILE_UPLOAD",
                "file:///tmp/bi-datasource-uploads/tenant-7/orders.csv",
                "fi***ad",
                "EXTRACT",
                "NOT_SYNCED",
                0,
                null,
                List.of("EXTRACT"),
                "AVAILABLE",
                List.of("TABLE_DATASET"));
    }

    private BiDatasourceSchemaSnapshotView fileSnapshot() {
        return new BiDatasourceSchemaSnapshotView(
                202L,
                91L,
                "file-91",
                "Uploaded Orders",
                "CSV_EXCEL",
                "SUCCESS",
                null,
                1,
                2,
                List.of(new BiDatasourceTablePreview(
                        "orders",
                        "CSV",
                        List.of(
                                new BiDatasourceColumnPreview("order_id", "VARCHAR", 12, true, 1),
                                new BiDatasourceColumnPreview("amount", "DOUBLE", 8, true, 2)))),
                LocalDateTime.parse("2026-06-06T10:15:30"),
                "alice");
    }

    private BiDatasetResource dataset() {
        return new BiDatasetResource(
                "file_91_orders",
                "Uploaded Orders orders",
                "TABLE",
                "orders",
                "tenant_id",
                Map.of("dataSourceConfigId", 91L, "sourceKey", "file-91", "connectorType", "CSV_EXCEL"),
                List.of(
                        new BiDatasetFieldResource(
                                "order_id",
                                "Order Id",
                                "order_id",
                                "DIMENSION",
                                "STRING",
                                "STRING",
                                null,
                                null,
                                null,
                                true,
                                "NORMAL",
                                1)),
                List.of(new BiMetricResource(
                        "row_count",
                        "Row Count",
                        "COUNT(1)",
                        "COUNT",
                        "NUMBER",
                        null,
                        "#,##0",
                        List.of("order_id"),
                        "alice",
                        "Datasource row count",
                        "ACTIVE")),
                "DRAFT",
                "DATASOURCE_SCHEMA");
    }

    private BiDatasetAccelerationPolicyView policy() {
        return new BiDatasetAccelerationPolicyView(
                "file_91_orders",
                true,
                "EXTRACT",
                "MANUAL",
                60L,
                300L,
                5_000L,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }

    private BiDatasetExtractRefreshRunView refreshRun() {
        return new BiDatasetExtractRefreshRunView(
                31L,
                "file_91_orders",
                "SUCCESS",
                1L,
                12L,
                "bi_extract.t7_file_91_orders_20260606101530",
                "alice",
                LocalDateTime.parse("2026-06-06T10:15:30"),
                LocalDateTime.parse("2026-06-06T10:15:31"),
                null);
    }
}
