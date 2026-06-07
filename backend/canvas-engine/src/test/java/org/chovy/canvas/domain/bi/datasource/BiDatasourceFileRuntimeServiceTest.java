package org.chovy.canvas.domain.bi.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDatasourceSchemaSnapshotDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.BiDatasourceSchemaSnapshotMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.OutputStream;
import java.sql.Types;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasourceFileRuntimeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void previewsCsvFileSchemaWithoutOpeningJdbcConnection() throws Exception {
        Path csv = writeOrdersCsv();
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectById(91L)).thenReturn(csvSource(csv));
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                null,
                null,
                new ObjectMapper(),
                failIfJdbcOpened());

        BiDatasourceSchemaPreview preview = service.previewSchema(91L, 7L, 50);

        assertThat(preview.sourceKey()).isEqualTo("file-91");
        assertThat(preview.connectorType()).isEqualTo("CSV_EXCEL");
        assertThat(preview.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("orders");
            assertThat(table.tableType()).isEqualTo("CSV");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::name)
                    .containsExactly("order_id", "amount", "paid", "buyer");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::typeName)
                    .containsExactly("VARCHAR", "DOUBLE", "BOOLEAN", "VARCHAR");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::dataType)
                    .containsExactly(Types.VARCHAR, Types.DOUBLE, Types.BOOLEAN, Types.VARCHAR);
        });
    }

    @Test
    void syncsCsvFileSchemaSnapshotWithoutOpeningJdbcConnection() throws Exception {
        Path csv = writeOrdersCsv();
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        BiDatasourceSchemaSnapshotMapper snapshotMapper = mock(BiDatasourceSchemaSnapshotMapper.class);
        when(mapper.selectById(91L)).thenReturn(csvSource(csv));
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                snapshotMapper,
                null,
                null,
                new ObjectMapper(),
                failIfJdbcOpened());

        BiDatasourceSchemaSnapshotView snapshot = service.syncSchema(91L, 7L, "alice", 50);

        assertThat(snapshot.sourceKey()).isEqualTo("file-91");
        assertThat(snapshot.connectorType()).isEqualTo("CSV_EXCEL");
        assertThat(snapshot.syncStatus()).isEqualTo("SUCCESS");
        assertThat(snapshot.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("orders");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::name)
                    .containsExactly("order_id", "amount", "paid", "buyer");
        });
        ArgumentCaptor<BiDatasourceSchemaSnapshotDO> row = ArgumentCaptor.forClass(BiDatasourceSchemaSnapshotDO.class);
        verify(snapshotMapper).insert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(7L);
        assertThat(row.getValue().getSourceKey()).isEqualTo("file-91");
        assertThat(row.getValue().getConnectorType()).isEqualTo("CSV_EXCEL");
        assertThat(row.getValue().getTableCount()).isEqualTo(1);
        assertThat(row.getValue().getColumnCount()).isEqualTo(4);
        assertThat(row.getValue().getSchemaJson()).contains("orders").contains("order_id").contains("amount");
    }

    @Test
    void previewsCsvFileRowsWithoutOpeningJdbcConnection() throws Exception {
        Path csv = writeOrdersCsv();
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectById(91L)).thenReturn(csvSource(csv));
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                null,
                null,
                new ObjectMapper(),
                failIfJdbcOpened());

        BiDatasourceApiPreview preview = service.previewFileData(91L, 7L, 1);

        assertThat(preview.sourceKey()).isEqualTo("file-91");
        assertThat(preview.connectorType()).isEqualTo("CSV_EXCEL");
        assertThat(preview.columns()).extracting(BiQueryColumn::key)
                .containsExactly("order_id", "amount", "paid", "buyer");
        assertThat(preview.rows()).containsExactly(Map.of(
                "order_id", "O-1",
                "amount", 12.5,
                "paid", true,
                "buyer", "Alice"));
        assertThat(preview.rowCount()).isEqualTo(1);
        assertThat(preview.truncated()).isTrue();
    }

    @Test
    void previewsXlsxFileSchemaWithoutOpeningJdbcConnection() throws Exception {
        Path workbook = writeOrdersWorkbook("orders.xlsx", new XSSFWorkbook());
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectById(92L)).thenReturn(fileSource(92L, workbook, "XLSX"));
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                null,
                null,
                null,
                new ObjectMapper(),
                failIfJdbcOpened());

        BiDatasourceSchemaPreview preview = service.previewSchema(92L, 7L, 50);

        assertThat(preview.sourceKey()).isEqualTo("file-92");
        assertThat(preview.connectorType()).isEqualTo("CSV_EXCEL");
        assertThat(preview.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("orders");
            assertThat(table.tableType()).isEqualTo("XLSX");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::name)
                    .containsExactly("order_id", "amount", "paid", "buyer");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::typeName)
                    .containsExactly("VARCHAR", "DOUBLE", "BOOLEAN", "VARCHAR");
        });
    }

    @Test
    void syncsXlsFileSchemaSnapshotWithoutOpeningJdbcConnection() throws Exception {
        Path workbook = writeOrdersWorkbook("orders.xls", new HSSFWorkbook());
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        BiDatasourceSchemaSnapshotMapper snapshotMapper = mock(BiDatasourceSchemaSnapshotMapper.class);
        when(mapper.selectById(93L)).thenReturn(fileSource(93L, workbook, "XLS"));
        BiDatasourceRuntimeService service = new BiDatasourceRuntimeService(
                mapper,
                snapshotMapper,
                null,
                null,
                new ObjectMapper(),
                failIfJdbcOpened());

        BiDatasourceSchemaSnapshotView snapshot = service.syncSchema(93L, 7L, "alice", 50);

        assertThat(snapshot.sourceKey()).isEqualTo("file-93");
        assertThat(snapshot.connectorType()).isEqualTo("CSV_EXCEL");
        assertThat(snapshot.syncStatus()).isEqualTo("SUCCESS");
        assertThat(snapshot.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("orders");
            assertThat(table.tableType()).isEqualTo("XLS");
            assertThat(table.columns()).extracting(BiDatasourceColumnPreview::name)
                    .containsExactly("order_id", "amount", "paid", "buyer");
        });
        ArgumentCaptor<BiDatasourceSchemaSnapshotDO> row = ArgumentCaptor.forClass(BiDatasourceSchemaSnapshotDO.class);
        verify(snapshotMapper).insert(row.capture());
        assertThat(row.getValue().getSourceKey()).isEqualTo("file-93");
        assertThat(row.getValue().getConnectorType()).isEqualTo("CSV_EXCEL");
        assertThat(row.getValue().getTableCount()).isEqualTo(1);
        assertThat(row.getValue().getColumnCount()).isEqualTo(4);
        assertThat(row.getValue().getSchemaJson()).contains("orders").contains("order_id").contains("amount");
    }

    private Path writeOrdersCsv() throws Exception {
        Path csv = tempDir.resolve("orders.csv");
        Files.writeString(csv, """
                order_id,amount,paid,buyer
                O-1,12.50,true,Alice
                O-2,7,false,Bob
                """);
        return csv;
    }

    private static DataSourceConfigDO csvSource(Path csv) {
        return fileSource(91L, csv, "CSV");
    }

    private Path writeOrdersWorkbook(String fileName, Workbook workbook) throws Exception {
        Path path = tempDir.resolve(fileName);
        try (workbook; OutputStream output = Files.newOutputStream(path)) {
            Sheet sheet = workbook.createSheet("Orders");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("order_id");
            header.createCell(1).setCellValue("amount");
            header.createCell(2).setCellValue("paid");
            header.createCell(3).setCellValue("buyer");
            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("O-1");
            first.createCell(1).setCellValue(12.50);
            first.createCell(2).setCellValue(true);
            first.createCell(3).setCellValue("Alice");
            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("O-2");
            second.createCell(1).setCellValue(7);
            second.createCell(2).setCellValue(false);
            second.createCell(3).setCellValue("Bob");
            workbook.write(output);
        }
        return path;
    }

    private static DataSourceConfigDO fileSource(Long id, Path file, String fileType) {
        DataSourceConfigDO source = new DataSourceConfigDO();
        source.setId(id);
        source.setTenantId(7L);
        source.setName("Upload extract");
        source.setType("FILE");
        source.setConnectorType("CSV_EXCEL");
        source.setConnectionMode("EXTRACT");
        source.setDriverClassName("FILE_UPLOAD");
        source.setUrl(file.toUri().toString());
        source.setUsername("file_upload");
        source.setPassword("");
        source.setConnectorConfigJson(String.format(
                "{\"fileName\":\"%s\",\"fileType\":\"%s\",\"delimiter\":\",\",\"headerRow\":true,\"encoding\":\"UTF-8\",\"sheetName\":\"Orders\"}",
                file.getFileName(),
                fileType));
        source.setEnabled(1);
        return source;
    }

    private static BiDatasourceRuntimeService.JdbcConnectionFactory failIfJdbcOpened() {
        return (source, password) -> {
            throw new AssertionError("CSV/Excel datasource runtime must not open JDBC connections");
        };
    }
}
