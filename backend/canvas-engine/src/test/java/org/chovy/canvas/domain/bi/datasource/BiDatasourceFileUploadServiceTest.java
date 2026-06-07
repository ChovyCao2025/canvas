package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasourceFileUploadServiceTest {

    @TempDir
    Path uploadRoot;

    @Test
    void storesUploadedFileAndCreatesCsvExcelDatasource() throws Exception {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.insert(any(DataSourceConfigDO.class))).thenAnswer(invocation -> {
            DataSourceConfigDO row = invocation.getArgument(0);
            row.setId(94L);
            return 1;
        });
        BiDatasourceOnboardingService onboardingService = new BiDatasourceOnboardingService(mapper);
        BiDatasourceFileUploadService uploadService = new BiDatasourceFileUploadService(
                onboardingService,
                uploadRoot.toString(),
                1024 * 1024);

        BiDatasourceOnboardingView view = uploadService.upload(
                7L,
                "alice",
                "../orders.csv",
                "order_id,amount\nO-1,12.5\n".getBytes(StandardCharsets.UTF_8),
                new BiDatasourceFileUploadCommand(
                        "Uploaded Orders",
                        "Browser CSV upload",
                        null,
                        ",",
                        true,
                        "UTF-8"));

        assertThat(view.id()).isEqualTo(94L);
        assertThat(view.sourceKey()).isEqualTo("file-94");
        assertThat(view.connectorType()).isEqualTo("CSV_EXCEL");
        ArgumentCaptor<DataSourceConfigDO> row = ArgumentCaptor.forClass(DataSourceConfigDO.class);
        verify(mapper).insert(row.capture());
        DataSourceConfigDO inserted = row.getValue();
        assertThat(inserted.getTenantId()).isEqualTo(7L);
        assertThat(inserted.getCreatedBy()).isEqualTo("alice");
        assertThat(inserted.getType()).isEqualTo("FILE");
        assertThat(inserted.getConnectorType()).isEqualTo("CSV_EXCEL");
        assertThat(inserted.getDriverClassName()).isEqualTo("FILE_UPLOAD");
        assertThat(inserted.getUsername()).isEqualTo("file_upload");
        assertThat(inserted.getPassword()).isEqualTo("");
        assertThat(inserted.getConnectionMode()).isEqualTo("EXTRACT");
        assertThat(inserted.getConnectorConfigJson())
                .contains("\"fileName\":\"orders.csv\"")
                .contains("\"fileType\":\"CSV\"")
                .contains("\"delimiter\":\",\"")
                .contains("\"headerRow\":true")
                .contains("\"encoding\":\"UTF-8\"")
                .doesNotContain("..");
        Path stored = Path.of(URI.create(inserted.getUrl()));
        assertThat(stored.normalize()).startsWith(uploadRoot.resolve("tenant-7").normalize());
        assertThat(stored.getFileName().toString()).contains("orders").endsWith(".csv").doesNotContain("..");
        assertThat(Files.readString(stored)).contains("order_id,amount").contains("O-1,12.5");
    }

    @Test
    void rejectsUnsupportedOrOversizedDatasourceUploads() {
        BiDatasourceFileUploadService uploadService = new BiDatasourceFileUploadService(
                mock(BiDatasourceOnboardingService.class),
                uploadRoot.toString(),
                4);

        assertThatThrownBy(() -> uploadService.upload(
                7L,
                "alice",
                "orders.exe",
                "abc".getBytes(StandardCharsets.UTF_8),
                new BiDatasourceFileUploadCommand("Bad", "", null, ",", true, "UTF-8")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported BI datasource upload file type");

        assertThatThrownBy(() -> uploadService.upload(
                7L,
                "alice",
                "orders.csv",
                "too-large".getBytes(StandardCharsets.UTF_8),
                new BiDatasourceFileUploadCommand("Large", "", null, ",", true, "UTF-8")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds BI datasource upload limit");
    }
}
