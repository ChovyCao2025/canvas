package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasourceControllerTest {

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
}
