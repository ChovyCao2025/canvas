package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.chovy.canvas.web.bi.BiDatasourceController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

class BiDatasourceFileUploadControllerContractTest {

    @Test
    void uploadsFileDatasourceForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasourceOnboardingService onboardingService = mock(BiDatasourceOnboardingService.class);
        BiDatasourceFileUploadService uploadService = mock(BiDatasourceFileUploadService.class);
        byte[] content = "order_id,amount\nO-1,12.5\n".getBytes(StandardCharsets.UTF_8);
        FilePart filePart = mock(FilePart.class);
        when(filePart.filename()).thenReturn("../orders.csv");
        when(filePart.content()).thenReturn(Flux.just(new DefaultDataBufferFactory().wrap(content)));
        when(uploadService.upload(
                eq(7L),
                eq("alice"),
                eq("../orders.csv"),
                any(byte[].class),
                any(BiDatasourceFileUploadCommand.class)))
                .thenReturn(fileOnboardingView(94L, "Uploaded Orders"));
        BiDatasourceController controller = new BiDatasourceController(
                resolver,
                onboardingService,
                null,
                null,
                uploadService);

        StepVerifier.create(controller.uploadFileDatasource(
                        filePart,
                        "Uploaded Orders",
                        "Browser CSV upload",
                        null,
                        ",",
                        true,
                        "UTF-8"))
                .assertNext(response -> {
                    assertThat(response.getData().sourceKey()).isEqualTo("file-94");
                    assertThat(response.getData().connectorType()).isEqualTo("CSV_EXCEL");
                })
                .verifyComplete();

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(uploadService).upload(
                eq(7L),
                eq("alice"),
                eq("../orders.csv"),
                contentCaptor.capture(),
                org.mockito.ArgumentMatchers.argThat(command ->
                        "Uploaded Orders".equals(command.name())
                                && "Browser CSV upload".equals(command.description())
                                && ",".equals(command.delimiter())
                                && Boolean.TRUE.equals(command.headerRow())
                                && "UTF-8".equals(command.encoding())));
        assertThat(contentCaptor.getValue()).isEqualTo(content);
    }

    @Test
    void uploadsAndMaterializesFileDatasourceForCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiDatasourceOnboardingService onboardingService = mock(BiDatasourceOnboardingService.class);
        BiDatasourceFileUploadService uploadService = mock(BiDatasourceFileUploadService.class);
        BiDatasourceFileMaterializationService materializationService = mock(BiDatasourceFileMaterializationService.class);
        byte[] content = "order_id,amount\nO-1,12.5\n".getBytes(StandardCharsets.UTF_8);
        FilePart filePart = mock(FilePart.class);
        when(filePart.filename()).thenReturn("../orders.csv");
        when(filePart.content()).thenReturn(Flux.just(new DefaultDataBufferFactory().wrap(content)));
        when(materializationService.uploadAndMaterialize(
                eq(7L),
                eq("alice"),
                eq("TENANT_ADMIN"),
                eq("../orders.csv"),
                any(byte[].class),
                any(BiDatasourceFileMaterializationCommand.class)))
                .thenReturn(materializationResult());
        BiDatasourceController controller = new BiDatasourceController(
                resolver,
                onboardingService,
                null,
                null,
                uploadService,
                materializationService);

        StepVerifier.create(controller.uploadAndMaterializeFileDatasource(
                        filePart,
                        "Uploaded Orders",
                        "Browser CSV upload",
                        null,
                        ",",
                        true,
                        "UTF-8",
                        "file_91_orders",
                        "Uploaded Orders Dataset",
                        "tenant_id",
                        200,
                        5_000L))
                .assertNext(response -> {
                    assertThat(response.getData().source().sourceKey()).isEqualTo("file-91");
                    assertThat(response.getData().dataset().datasetKey()).isEqualTo("file_91_orders");
                    assertThat(response.getData().refreshRun().status()).isEqualTo("SUCCESS");
                })
                .verifyComplete();

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<BiDatasourceFileMaterializationCommand> commandCaptor =
                ArgumentCaptor.forClass(BiDatasourceFileMaterializationCommand.class);
        verify(materializationService).uploadAndMaterialize(
                eq(7L),
                eq("alice"),
                eq("TENANT_ADMIN"),
                eq("../orders.csv"),
                contentCaptor.capture(),
                commandCaptor.capture());
        assertThat(contentCaptor.getValue()).isEqualTo(content);
        assertThat(commandCaptor.getValue().name()).isEqualTo("Uploaded Orders");
        assertThat(commandCaptor.getValue().datasetKey()).isEqualTo("file_91_orders");
        assertThat(commandCaptor.getValue().datasetName()).isEqualTo("Uploaded Orders Dataset");
        assertThat(commandCaptor.getValue().tenantColumn()).isEqualTo("tenant_id");
        assertThat(commandCaptor.getValue().schemaLimit()).isEqualTo(200);
        assertThat(commandCaptor.getValue().maxRows()).isEqualTo(5_000L);
    }

    private static BiDatasourceOnboardingView fileOnboardingView(Long id, String name) {
        return new BiDatasourceOnboardingView(
                id,
                "file-" + id,
                name,
                "FILE",
                "CSV_EXCEL",
                true,
                "FILE_UPLOAD",
                "file:///tmp/canvas-bi-datasources/orders.csv",
                "fi***ad",
                "EXTRACT",
                "NOT_SYNCED",
                0,
                null,
                List.of("EXTRACT"),
                "AVAILABLE",
                List.of("TABLE_DATASET"));
    }

    private static BiDatasourceFileMaterializationResult materializationResult() {
        return new BiDatasourceFileMaterializationResult(
                fileOnboardingView(91L, "Uploaded Orders"),
                new BiDatasourceSchemaSnapshotView(
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
                        "alice"),
                new BiDatasetResource(
                        "file_91_orders",
                        "Uploaded Orders Dataset",
                        "TABLE",
                        "orders",
                        "tenant_id",
                        Map.of("dataSourceConfigId", 91L),
                        List.of(),
                        List.of(),
                        "DRAFT",
                        "DATASOURCE_SCHEMA"),
                new BiDatasetAccelerationPolicyView(
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
                        List.of()),
                new BiDatasetExtractRefreshRunView(
                        31L,
                        "file_91_orders",
                        "SUCCESS",
                        1L,
                        12L,
                        "bi_extract.t7_file_91_orders_20260606101530",
                        "alice",
                        LocalDateTime.parse("2026-06-06T10:15:30"),
                        LocalDateTime.parse("2026-06-06T10:15:31"),
                        null));
    }
}
