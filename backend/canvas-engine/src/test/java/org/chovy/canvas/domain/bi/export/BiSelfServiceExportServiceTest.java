package org.chovy.canvas.domain.bi.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiExportJobDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiExportJobMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.storage.BiFileStorage;
import org.chovy.canvas.domain.bi.storage.BiFileStorageWriter;
import org.chovy.canvas.domain.bi.storage.BiStoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiSelfServiceExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void previewCapsLimitAndUsesCurrentContext() {
        Fixture fixture = fixture();
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());

        BiQueryResult preview = fixture.service.preview(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiSelfServicePreviewRequest(query(2000), 2000));

        assertThat(preview.rowCount()).isEqualTo(1);
        verify(fixture.queryExecutionService).execute(
                eq(query(500)),
                eq(new BiQueryContext(7L, "alice", RoleNames.OPERATOR)));
    }

    @Test
    void createExportRequiresExportPermissionAndWritesDownloadableCsv() {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset());
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            BiExportJobDO row = invocation.getArgument(0);
            row.setId(55L);
            persisted.set(row);
            return 1;
        }).when(fixture.exportJobMapper).insert(any(BiExportJobDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));
        when(fixture.exportJobMapper.selectById(55L)).thenAnswer(invocation -> persisted.get());

        BiExportJobView view = fixture.service.createExport(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiExportJobCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "CSV",
                        query(100),
                        100,
                        false,
                        false,
                        null));

        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.fileUrl()).isNull();
        verify(fixture.queryExecutionService, never()).execute(any(), any());
        when(fixture.exportJobMapper.selectList(any())).thenAnswer(invocation -> List.of(persisted.get()));

        BiExportQueueResult queueResult = fixture.service.processQueuedExports(
                7L,
                "alice",
                RoleNames.OPERATOR,
                10);
        view = queueResult.jobs().getFirst();

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.fileUrl()).isEqualTo("/canvas/bi/self-service/exports/55/download");
        assertThat(view.retentionDays()).isEqualTo(7);
        assertThat(view.expiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(view.downloadCount()).isZero();
        verify(fixture.permissionService, atLeast(1)).enforceResourceAccess(
                eq(7L),
                eq(3L),
                eq("DATASET"),
                eq(11L),
                eq(new BiQueryContext(7L, "alice", RoleNames.OPERATOR)),
                eq(BiPermissionService.ACTION_EXPORT));

        BiExportDownload download = fixture.service.download(7L, 55L);
        assertThat(download.filename()).isEqualTo("export-55.csv");
        assertThat(new String(download.bytes()))
                .contains("stat_date,total_executions")
                .contains("2026-06-05,42");
        ArgumentCaptor<BiExportJobDO> updateCaptor = ArgumentCaptor.forClass(BiExportJobDO.class);
        verify(fixture.exportJobMapper, atLeast(3)).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues()).anySatisfy(update -> {
            assertThat(update.getId()).isEqualTo(55L);
            assertThat(update.getDownloadCount()).isEqualTo(1);
            assertThat(update.getLastDownloadedAt()).isNotNull();
        });
    }

    @Test
    void createExportQueuesJobWithoutExecutingWarehouseQuery() {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset());
        doAnswer(invocation -> {
            BiExportJobDO row = invocation.getArgument(0);
            row.setId(73L);
            persisted.set(row);
            return 1;
        }).when(fixture.exportJobMapper).insert(any(BiExportJobDO.class));

        BiExportJobView view = fixture.service.createExport(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiExportJobCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "CSV",
                        query(100),
                        100,
                        false,
                        false,
                        null));

        assertThat(view.id()).isEqualTo(73L);
        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.progressPercent()).isZero();
        assertThat(view.fileUrl()).isNull();
        assertThat(view.storageProvider()).isNull();
        assertThat(view.storageKey()).isNull();
        verify(fixture.permissionService).enforceResourceAccess(
                eq(7L),
                eq(3L),
                eq("DATASET"),
                eq(11L),
                eq(new BiQueryContext(7L, "alice", RoleNames.OPERATOR)),
                eq(BiPermissionService.ACTION_EXPORT));
        verify(fixture.queryExecutionService, never()).execute(any(), any());
        assertThat(persisted.get().getStatus()).isEqualTo("QUEUED");
        assertThat(persisted.get().getProgressPercent()).isZero();
        assertThat(persisted.get().getFileUrl()).isNull();
    }

    @Test
    void processQueuedExportsRunsPersistedRequestsAndWritesDownloadableFiles() throws Exception {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(100),
                100,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(74L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportQueueResult result = fixture.service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.completed()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(result.jobs()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo(74L);
            assertThat(view.status()).isEqualTo("COMPLETED");
            assertThat(view.progressPercent()).isEqualTo(100);
            assertThat(view.fileUrl()).isEqualTo("/canvas/bi/self-service/exports/74/download");
        });
        assertThat(persisted.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(persisted.get().getProgressPercent()).isEqualTo(100);
        verify(fixture.queryExecutionService).execute(
                eq(query(100)),
                eq(new BiQueryContext(7L, "export-worker", RoleNames.OPERATOR)));
    }

    @Test
    void processQueuedPdfExportWritesDownloadablePdf() throws Exception {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "PDF",
                query(100),
                100,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(75L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setExportFormat("PDF");
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(fixture.exportJobMapper.selectById(75L)).thenAnswer(invocation -> persisted.get());
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportQueueResult queueResult = fixture.service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);
        BiExportJobView view = queueResult.jobs().getFirst();

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.exportFormat()).isEqualTo("PDF");
        assertThat(view.storageKey()).isEqualTo("exports/tenant-7/export-75.pdf");
        BiExportDownload download = fixture.service.download(7L, 75L);
        assertThat(download.filename()).isEqualTo("export-75.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        String pdf = new String(download.bytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThat(pdf).startsWith("%PDF-");
        assertThat(pdf)
                .contains("stat_date")
                .contains("total_executions")
                .contains("2026-06-05")
                .contains("42");
    }

    @Test
    void processQueuedXlsxExportAppliesReadableWorkbookStyling() throws Exception {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "XLSX",
                query(100),
                100,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(78L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setExportFormat("XLSX");
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(fixture.exportJobMapper.selectById(78L)).thenAnswer(invocation -> persisted.get());
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        fixture.service.processQueuedExports(7L, "export-worker", RoleNames.OPERATOR, 10);
        BiExportDownload download = fixture.service.download(7L, 78L);

        assertThat(download.filename()).isEqualTo("export-78.xlsx");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.bytes()))) {
            XSSFSheet sheet = (XSSFSheet) workbook.getSheet("BI Export");
            assertThat(sheet.getPaneInformation()).isNotNull();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getCTWorksheet().getAutoFilter()).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(sheet.getColumnWidth(0)).isGreaterThan(8 * 256);
        }
    }

    @Test
    void processQueuedLargeCsvExportWritesPartitionedZipWithManifestAndPagedQueries() throws Exception {
        Fixture fixture = fixture(20_000);
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        List<BiQueryRequest> executedRequests = new ArrayList<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(15_000),
                15_000,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(79L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setExportFormat("CSV");
        row.setRowLimit(15_000);
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(fixture.exportJobMapper.selectById(79L)).thenAnswer(invocation -> persisted.get());
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenAnswer(invocation -> {
            BiQueryRequest request = invocation.getArgument(0);
            executedRequests.add(request);
            if (request.offset() == 0) {
                return resultRows(10_000, 0);
            }
            if (request.offset() == 10_000) {
                return resultRows(5_000, 10_000);
            }
            return resultRows(0, request.offset());
        });
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportQueueResult queueResult = fixture.service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);

        BiExportJobView view = queueResult.jobs().getFirst();
        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.rowLimit()).isEqualTo(15_000);
        assertThat(view.storageKey()).isEqualTo("exports/tenant-7/export-79.zip");
        assertThat(executedRequests)
                .extracting(request -> request.limit() + ":" + request.offset())
                .containsExactly("10000:0", "5000:10000");

        BiExportDownload download = fixture.service.download(7L, 79L);
        assertThat(download.filename()).isEqualTo("export-79.zip");
        assertThat(download.contentType()).isEqualTo("application/zip");
        Map<String, String> entries = unzipTextEntries(download.bytes());
        assertThat(entries.keySet()).containsExactly("manifest.json", "part-00001.csv", "part-00002.csv");
        assertThat(entries.get("manifest.json"))
                .contains("\"requestedRows\":15000")
                .contains("\"generatedRows\":15000")
                .contains("\"partCount\":2")
                .contains("\"partSize\":10000");
        assertThat(entries.get("part-00001.csv")).contains("stat_date,total_executions");
        assertThat(entries.get("part-00002.csv")).contains("2026-06-05-10000");
    }

    @Test
    void processQueuedLargeCsvExportStoresPartObjectsWithManifestChecksums() throws Exception {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        ObjectMapper mapper = new ObjectMapper();
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                mapper,
                storage,
                7,
                20_000);
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        List<BiQueryRequest> executedRequests = new ArrayList<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(15_000),
                15_000,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(80L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setExportFormat("CSV");
        row.setRowLimit(15_000);
        row.setRequestJson(mapper.writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(exportJobMapper.selectById(80L)).thenAnswer(invocation -> persisted.get());
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(), any())).thenAnswer(invocation -> {
            BiQueryRequest request = invocation.getArgument(0);
            executedRequests.add(request);
            if (request.offset() == 0) {
                return resultRows(10_000, 0);
            }
            if (request.offset() == 10_000) {
                return resultRows(5_000, 10_000);
            }
            return resultRows(0, request.offset());
        });
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportQueueResult queueResult = service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);

        BiExportJobView view = queueResult.jobs().getFirst();
        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.storageKey()).isEqualTo("exports/tenant-7/export-80.zip");
        assertThat(executedRequests)
                .extracting(request -> request.limit() + ":" + request.offset())
                .containsExactly("10000:0", "5000:10000");
        assertThat(storage.bytesByKey.keySet()).contains(
                "exports/tenant-7/export-80.zip",
                "exports/tenant-7/export-80/parts/part-00001.csv",
                "exports/tenant-7/export-80/parts/part-00002.csv");

        Map<String, String> entries = unzipTextEntries(storage.bytesByKey.get("exports/tenant-7/export-80.zip"));
        assertThat(entries.keySet()).containsExactly("manifest.json", "part-00001.csv", "part-00002.csv");
        JsonNode manifest = mapper.readTree(entries.get("manifest.json"));
        assertThat(manifest.path("storageLayout").asText()).isEqualTo("OBJECT_PER_PART_ZIP");
        assertThat(manifest.path("parts")).hasSize(2);
        JsonNode firstPart = manifest.path("parts").get(0);
        byte[] firstPartBytes = storage.bytesByKey.get("exports/tenant-7/export-80/parts/part-00001.csv");
        assertThat(firstPart.path("name").asText()).isEqualTo("part-00001.csv");
        assertThat(firstPart.path("storageKey").asText()).isEqualTo("exports/tenant-7/export-80/parts/part-00001.csv");
        assertThat(firstPart.path("rowCount").asInt()).isEqualTo(10_000);
        assertThat(firstPart.path("sizeBytes").asLong()).isEqualTo(firstPartBytes.length);
        assertThat(firstPart.path("sha256").asText()).isEqualTo(sha256Hex(firstPartBytes));
        assertThat(entries.get("part-00001.csv")).isEqualTo(new String(firstPartBytes, StandardCharsets.UTF_8));
    }

    @Test
    void restoreExportObjectsCopiesMissingRootAndPartitionObjectsFromFallbackProvider() throws Exception {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        CapturingStorage primary = new CapturingStorage("PRIMARY");
        CapturingStorage fallback = new CapturingStorage("ARCHIVE");
        String rootKey = "exports/tenant-7/export-83.zip";
        String partKey = "exports/tenant-7/export-83/parts/part-00001.csv";
        byte[] rootBytes = zipWithManifest("""
                {
                  "storageLayout":"OBJECT_PER_PART_ZIP",
                  "parts":[{"name":"part-00001.csv","storageKey":"exports/tenant-7/export-83/parts/part-00001.csv","rowCount":1,"sizeBytes":11,"sha256":"abc"}]
                }
                """);
        byte[] partBytes = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8);
        fallback.write(rootKey, rootBytes);
        fallback.write(partKey, partBytes);
        BiExportJobDO row = exportJob();
        row.setId(83L);
        row.setStorageProvider("PRIMARY");
        row.setStorageKey(rootKey);
        row.setExportFormat("CSV");
        when(exportJobMapper.selectById(83L)).thenReturn(row);
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                primary,
                7,
                20_000);

        BiExportObjectRestoreResult result = service.restoreExportObjects(7L, 83L, fallback);

        assertThat(result.exportId()).isEqualTo(83L);
        assertThat(result.primaryProvider()).isEqualTo("PRIMARY");
        assertThat(result.fallbackProvider()).isEqualTo("ARCHIVE");
        assertThat(result.checkedObjects()).isEqualTo(2);
        assertThat(result.restoredObjects()).isEqualTo(2);
        assertThat(result.missingObjects()).isZero();
        assertThat(result.restoredKeys()).containsExactly(rootKey, partKey);
        assertThat(primary.bytesByKey.get(rootKey)).isEqualTo(rootBytes);
        assertThat(primary.bytesByKey.get(partKey)).isEqualTo(partBytes);
    }

    @Test
    void failedPartitionedExportDeletesGeneratedPartObjects() throws Exception {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        FailingZipStorage storage = new FailingZipStorage("MEMORY");
        ObjectMapper mapper = new ObjectMapper();
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                mapper,
                storage,
                7,
                20_000);
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(15_000),
                15_000,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(81L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setExportFormat("CSV");
        row.setRowLimit(15_000);
        row.setRequestJson(mapper.writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(), any())).thenAnswer(invocation -> {
            BiQueryRequest request = invocation.getArgument(0);
            if (request.offset() == 0) {
                return resultRows(10_000, 0);
            }
            if (request.offset() == 10_000) {
                return resultRows(5_000, 10_000);
            }
            return resultRows(0, request.offset());
        });
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportQueueResult queueResult = service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);

        assertThat(queueResult.failed()).isEqualTo(1);
        assertThat(persisted.get().getStatus()).isEqualTo("FAILED");
        assertThat(persisted.get().getNextRetryAt()).isNotNull();
        assertThat(storage.bytesByKey.keySet())
                .noneMatch(key -> key.startsWith("exports/tenant-7/export-81/parts/"));
        assertThat(storage.bytesByKey).doesNotContainKey("exports/tenant-7/export-81.zip");
    }

    @Test
    void createExportStoresDownloadableFileThroughConfiguredStorage() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(datasetMapper.selectOne(any())).thenReturn(dataset());
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            BiExportJobDO row = invocation.getArgument(0);
            row.setId(70L);
            persisted.set(row);
            return 1;
        }).when(exportJobMapper).insert(any(BiExportJobDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(exportJobMapper).updateById(any(BiExportJobDO.class));
        when(exportJobMapper.selectById(70L)).thenAnswer(invocation -> persisted.get());
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                storage,
                7,
                5000);

        BiExportJobView view = service.createExport(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiExportJobCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "CSV",
                        query(100),
                        100,
                        false,
                        false,
                        null));

        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.storageProvider()).isNull();
        assertThat(view.storageKey()).isNull();
        when(exportJobMapper.selectList(any())).thenAnswer(invocation -> List.of(persisted.get()));

        BiExportQueueResult queueResult = service.processQueuedExports(
                7L,
                "alice",
                RoleNames.OPERATOR,
                10);
        view = queueResult.jobs().getFirst();

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.storageProvider()).isEqualTo("MEMORY");
        assertThat(view.storageKey()).isEqualTo("exports/tenant-7/export-70.csv");
        assertThat(storage.bytesByKey).containsKey("exports/tenant-7/export-70.csv");
        assertThat(new String(storage.bytesByKey.get("exports/tenant-7/export-70.csv")))
                .contains("stat_date,total_executions")
                .contains("2026-06-05,42");
        BiExportDownload download = service.download(7L, 70L);
        assertThat(download.filename()).isEqualTo("export-70.csv");
        assertThat(new String(download.bytes())).contains("2026-06-05,42");
    }

    @Test
    void failedExportKeepsProgressAndSchedulesRetry() {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any()))
                .thenThrow(new IllegalStateException("warehouse is busy"));
        doAnswer(invocation -> {
            BiExportJobDO row = invocation.getArgument(0);
            row.setId(71L);
            persisted.set(row);
            return 1;
        }).when(fixture.exportJobMapper).insert(any(BiExportJobDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportJobView queued = fixture.service.createExport(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiExportJobCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "CSV",
                        query(100),
                        100,
                        false,
                        false,
                        null));

        assertThat(queued.status()).isEqualTo("QUEUED");
        when(fixture.exportJobMapper.selectList(any())).thenAnswer(invocation -> List.of(persisted.get()));

        BiExportQueueResult result = fixture.service.processQueuedExports(
                7L,
                "alice",
                RoleNames.OPERATOR,
                10);

        assertThat(result.failed()).isEqualTo(1);

        BiExportJobDO row = persisted.get();
        assertThat(row.getStatus()).isEqualTo("FAILED");
        assertThat(row.getProgressPercent()).isEqualTo(50);
        assertThat(row.getRetryCount()).isZero();
        assertThat(row.getMaxRetryCount()).isEqualTo(3);
        assertThat(row.getNextRetryAt()).isAfter(LocalDateTime.now());
        assertThat(row.getLastRetryAt()).isNull();
        assertThat(row.getRetryExhaustedAt()).isNull();
        assertThat(row.getErrorMessage()).contains("warehouse is busy");
    }

    @Test
    void retryFailedExportsRunsDueJobsAndKeepsAttemptCount() throws Exception {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(100),
                100,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(72L);
        row.setStatus("FAILED");
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setRetryCount(0);
        row.setMaxRetryCount(3);
        row.setNextRetryAt(LocalDateTime.now().minusMinutes(1));
        row.setProgressPercent(50);
        persisted.set(row);
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportRetryResult result = fixture.service.retryFailedExports(
                7L,
                "operator",
                RoleNames.OPERATOR,
                10);

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.retried()).isEqualTo(1);
        assertThat(result.completed()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(result.jobs()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo(72L);
            assertThat(view.status()).isEqualTo("COMPLETED");
            assertThat(view.progressPercent()).isEqualTo(100);
            assertThat(view.retryCount()).isEqualTo(1);
        });
        assertThat(persisted.get().getRetryCount()).isEqualTo(1);
        assertThat(persisted.get().getLastRetryAt()).isNotNull();
        assertThat(persisted.get().getNextRetryAt()).isNull();
        assertThat(persisted.get().getRetryExhaustedAt()).isNull();
        assertThat(persisted.get().getProgressPercent()).isEqualTo(100);
        verify(fixture.queryExecutionService).execute(
                eq(query(100)),
                eq(new BiQueryContext(7L, "operator", RoleNames.OPERATOR)));
    }

    @Test
    void cancelQueuedExportMarksCanceledAndPreventsQueueExecution() throws Exception {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(100),
                100,
                false,
                false,
                null);
        BiExportJobDO row = exportJob();
        row.setId(82L);
        row.setStatus("QUEUED");
        row.setProgressPercent(0);
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setFileUrl(null);
        row.setStorageProvider(null);
        row.setStorageKey(null);
        persisted.set(row);
        when(fixture.exportJobMapper.selectById(82L)).thenAnswer(invocation -> persisted.get());
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());

        BiExportJobView canceled = fixture.service.cancelExport(7L, "alice", 82L);

        assertThat(canceled.status()).isEqualTo("CANCELED");
        assertThat(canceled.progressPercent()).isEqualTo(100);
        assertThat(canceled.fileUrl()).isNull();
        assertThat(canceled.errorMessage()).isEqualTo("BI export canceled by alice");
        assertThat(persisted.get().getStatus()).isEqualTo("CANCELED");
        assertThat(persisted.get().getNextRetryAt()).isNull();

        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of());
        BiExportQueueResult queueResult = fixture.service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);

        assertThat(queueResult.checked()).isZero();
        assertThat(queueResult.jobs()).isEmpty();
        verify(fixture.queryExecutionService, never()).execute(any(), any());
    }

    @Test
    void listExportsReturnsMostRecentJobsWithDatasetKey() {
        Fixture fixture = fixture();
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        BiExportJobDO row = exportJob();
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));

        List<BiExportJobView> rows = fixture.service.listExports(7L, 20);

        assertThat(rows).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo(55L);
            assertThat(view.resourceKey()).isEqualTo("canvas_daily_stats");
            assertThat(view.status()).isEqualTo("COMPLETED");
        });
    }

    @Test
    void getExportDetailReturnsTenantScopedAuditMetadataAndOriginalRequest() throws Exception {
        Fixture fixture = fixture();
        BiExportJobCommand command = new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "PDF",
                query(250),
                250,
                true,
                true,
                "finance audit");
        BiExportJobDO row = exportJob();
        row.setId(76L);
        row.setExportFormat("PDF");
        row.setRequestJson(new ObjectMapper().writeValueAsString(command));
        row.setStorageProvider("S3");
        row.setStorageKey("exports/tenant-7/export-76.pdf");
        row.setDownloadCount(3);
        row.setLastDownloadedAt(LocalDateTime.parse("2026-06-05T10:15:00"));
        row.setApprovalStatus("APPROVED");
        row.setRequestedBy("alice");
        row.setRequestedAt(LocalDateTime.parse("2026-06-05T09:55:00"));
        row.setReviewedBy("admin");
        row.setReviewedAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        row.setReviewComment("ok");
        when(fixture.exportJobMapper.selectById(76L)).thenReturn(row);
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());

        BiExportJobDetailView detail = fixture.service.getExportDetail(7L, 76L);

        assertThat(detail.job().id()).isEqualTo(76L);
        assertThat(detail.job().resourceKey()).isEqualTo("canvas_daily_stats");
        assertThat(detail.job().exportFormat()).isEqualTo("PDF");
        assertThat(detail.job().storageProvider()).isEqualTo("S3");
        assertThat(detail.job().storageKey()).isEqualTo("exports/tenant-7/export-76.pdf");
        assertThat(detail.job().downloadCount()).isEqualTo(3);
        assertThat(detail.job().lastDownloadedAt()).isEqualTo(LocalDateTime.parse("2026-06-05T10:15:00"));
        assertThat(detail.job().approvalStatus()).isEqualTo("APPROVED");
        assertThat(detail.request().exportFormat()).isEqualTo("PDF");
        assertThat(detail.request().approvalReason()).isEqualTo("finance audit");
        assertThat(detail.request().query().datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(detail.request().query().limit()).isEqualTo(250);
        assertThat(detail.request().query().filters()).singleElement().satisfies(filter ->
                assertThat(String.valueOf(filter.value())).isEqualTo("12"));
    }

    @Test
    void getExportDetailReturnsPartitionManifestMetadataForObjectPerPartZip() throws Exception {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        CapturingStorage storage = new CapturingStorage("S3");
        String zipKey = "exports/tenant-7/export-91.zip";
        storage.write(zipKey, zipWithManifest("""
                {
                  "storageLayout": "OBJECT_PER_PART_ZIP",
                  "requestedRows": 1000000,
                  "generatedRows": 15000,
                  "partCount": 2,
                  "partSize": 10000,
                  "parts": [
                    { "name": "part-00001.csv", "storageKey": "exports/tenant-7/export-91/parts/part-00001.csv", "rowCount": 10000, "sizeBytes": 128, "sha256": "aaa" },
                    { "name": "part-00002.csv", "storageKey": "exports/tenant-7/export-91/parts/part-00002.csv", "rowCount": 5000, "sizeBytes": 64, "sha256": "bbb" }
                  ]
                }
                """));
        BiExportJobDO row = exportJob();
        row.setId(91L);
        row.setRequestJson(new ObjectMapper().writeValueAsString(new BiExportJobCommand(
                "DATASET",
                "canvas_daily_stats",
                null,
                "CSV",
                query(1000000),
                1000000,
                false,
                false,
                null)));
        row.setStorageProvider("S3");
        row.setStorageKey(zipKey);
        when(exportJobMapper.selectById(91L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                storage,
                7,
                5000);

        BiExportJobDetailView detail = service.getExportDetail(7L, 91L);

        assertThat(detail.partition()).containsEntry("storageLayout", "OBJECT_PER_PART_ZIP");
        assertThat(detail.partition()).containsEntry("requestedRows", 1000000);
        assertThat(detail.partition()).containsEntry("generatedRows", 15000);
        assertThat(detail.partition()).containsEntry("partCount", 2);
        assertThat(detail.partition()).containsEntry("partSize", 10000);
        assertThat(detail.partition().get("partStorageKeys")).asList()
                .containsExactly(
                        "exports/tenant-7/export-91/parts/part-00001.csv",
                        "exports/tenant-7/export-91/parts/part-00002.csv");
    }

    @Test
    void getExportDetailRejectsOtherTenantJob() {
        Fixture fixture = fixture();
        BiExportJobDO row = exportJob();
        row.setTenantId(8L);
        when(fixture.exportJobMapper.selectById(77L)).thenReturn(row);

        assertThatThrownBy(() -> fixture.service.getExportDetail(7L, 77L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void sensitiveExportWaitsForApprovalBeforeRunningQuery() {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset());
        doAnswer(invocation -> {
            BiExportJobDO row = invocation.getArgument(0);
            row.setId(58L);
            persisted.set(row);
            return 1;
        }).when(fixture.exportJobMapper).insert(any(BiExportJobDO.class));

        BiExportJobView view = fixture.service.createExport(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiExportJobCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "CSV",
                        query(100),
                        100,
                        true,
                        true,
                        "customer level detail"));

        assertThat(view.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(view.approvalStatus()).isEqualTo("PENDING");
        assertThat(view.approvalReason()).isEqualTo("customer level detail");
        assertThat(view.requestedBy()).isEqualTo("alice");
        assertThat(view.requestedAt()).isNotNull();
        assertThat(view.fileUrl()).isNull();
        verify(fixture.queryExecutionService, never()).execute(any(), any());
        assertThat(persisted.get().getStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void approvePendingExportRunsOriginalRequestAndWritesFile() {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset());
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        when(fixture.queryExecutionService.execute(any(), any())).thenReturn(result());
        doAnswer(invocation -> {
            BiExportJobDO row = invocation.getArgument(0);
            row.setId(59L);
            persisted.set(row);
            return 1;
        }).when(fixture.exportJobMapper).insert(any(BiExportJobDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));
        when(fixture.exportJobMapper.selectById(59L)).thenAnswer(invocation -> persisted.get());

        fixture.service.createExport(
                7L,
                "alice",
                RoleNames.OPERATOR,
                new BiExportJobCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "CSV",
                        query(100),
                        100,
                        true,
                        true,
                        "customer level detail"));

        BiExportJobView approved = fixture.service.reviewExport(
                7L,
                "admin",
                RoleNames.TENANT_ADMIN,
                59L,
                new BiExportApprovalReviewCommand("APPROVED", "ok"));

        assertThat(approved.status()).isEqualTo("QUEUED");
        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");
        assertThat(approved.reviewedBy()).isEqualTo("admin");
        assertThat(approved.reviewComment()).isEqualTo("ok");
        assertThat(approved.fileUrl()).isNull();
        verify(fixture.queryExecutionService, never()).execute(any(), any());
        when(fixture.exportJobMapper.selectList(any())).thenAnswer(invocation -> List.of(persisted.get()));

        BiExportQueueResult queueResult = fixture.service.processQueuedExports(
                7L,
                "export-worker",
                RoleNames.OPERATOR,
                10);
        BiExportJobView completed = queueResult.jobs().getFirst();

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.fileUrl()).isEqualTo("/canvas/bi/self-service/exports/59/download");
        ArgumentCaptor<BiQueryRequest> queryCaptor = ArgumentCaptor.forClass(BiQueryRequest.class);
        ArgumentCaptor<BiQueryContext> contextCaptor = ArgumentCaptor.forClass(BiQueryContext.class);
        verify(fixture.queryExecutionService).execute(queryCaptor.capture(), contextCaptor.capture());
        assertThat(queryCaptor.getValue().datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(queryCaptor.getValue().dimensions()).containsExactly("stat_date");
        assertThat(queryCaptor.getValue().metrics()).containsExactly("total_executions");
        assertThat(queryCaptor.getValue().limit()).isEqualTo(100);
        assertThat(queryCaptor.getValue().filters()).singleElement().satisfies(filter -> {
            assertThat(filter.field()).isEqualTo("canvas_id");
            assertThat(filter.operator()).isEqualTo(BiFilter.Operator.EQ);
            assertThat(String.valueOf(filter.value())).isEqualTo("12");
        });
        assertThat(contextCaptor.getValue()).isEqualTo(new BiQueryContext(7L, "export-worker", RoleNames.OPERATOR));
        assertThat(tempDir.resolve("exports").resolve("tenant-7").resolve("export-59.csv")).exists();
    }

    @Test
    void rejectPendingExportMarksRejectedWithoutRunningQuery() {
        Fixture fixture = fixture();
        AtomicReference<BiExportJobDO> persisted = new AtomicReference<>();
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset());
        BiExportJobDO row = exportJob();
        row.setId(60L);
        row.setStatus("PENDING_APPROVAL");
        row.setApprovalStatus("PENDING");
        row.setApprovalReason("sensitive export");
        row.setRequestedBy("alice");
        row.setRequestedAt(LocalDateTime.now());
        persisted.set(row);
        when(fixture.exportJobMapper.selectById(60L)).thenAnswer(invocation -> persisted.get());
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(fixture.exportJobMapper).updateById(any(BiExportJobDO.class));

        BiExportJobView rejected = fixture.service.reviewExport(
                7L,
                "admin",
                RoleNames.ADMIN,
                60L,
                new BiExportApprovalReviewCommand("REJECTED", "too broad"));

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.approvalStatus()).isEqualTo("REJECTED");
        assertThat(rejected.reviewedBy()).isEqualTo("admin");
        assertThat(rejected.reviewComment()).isEqualTo("too broad");
        verify(fixture.queryExecutionService, never()).execute(any(), any());
    }

    @Test
    void downloadRejectsExpiredExportAndMarksExpired() throws Exception {
        Fixture fixture = fixture();
        Path file = tempDir.resolve("tenant-7").resolve("export-56.csv");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "a,b\n");
        BiExportJobDO row = exportJob();
        row.setId(56L);
        row.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(fixture.exportJobMapper.selectById(56L)).thenReturn(row);

        assertThatThrownBy(() -> fixture.service.download(7L, 56L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        ArgumentCaptor<BiExportJobDO> updateCaptor = ArgumentCaptor.forClass(BiExportJobDO.class);
        verify(fixture.exportJobMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
        assertThat(updateCaptor.getValue().getErrorMessage()).contains("expired");
    }

    @Test
    void downloadRejectsExpiredStorageBackedExportAndDeletesObject() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        String storageKey = "exports/tenant-7/export-156.csv";
        storage.write(storageKey, "a,b\n".getBytes());
        BiExportJobDO row = exportJob();
        row.setId(156L);
        row.setStorageProvider("MEMORY");
        row.setStorageKey(storageKey);
        row.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(exportJobMapper.selectById(156L)).thenReturn(row);
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                storage,
                7,
                5000);

        assertThatThrownBy(() -> service.download(7L, 156L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        assertThat(storage.bytesByKey).doesNotContainKey(storageKey);
        ArgumentCaptor<BiExportJobDO> updateCaptor = ArgumentCaptor.forClass(BiExportJobDO.class);
        verify(exportJobMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void downloadAppliesUserRateLimitAndAuditsAllowedAndRejectedAttempts() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        String storageKey = "exports/tenant-7/export-90.csv";
        storage.write(storageKey, "a,b\n".getBytes(StandardCharsets.UTF_8));
        BiExportJobDO row = exportJob();
        row.setId(90L);
        row.setStorageProvider("MEMORY");
        row.setStorageKey(storageKey);
        when(exportJobMapper.selectById(90L)).thenReturn(row);
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                storage,
                7,
                5000,
                auditLogMapper,
                1);

        BiExportDownload first = service.download(7L, "alice", 90L);
        assertThat(new String(first.bytes(), StandardCharsets.UTF_8)).contains("a,b");

        assertThatThrownBy(() -> service.download(7L, "alice", 90L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rate limit");

        ArgumentCaptor<BiAuditLogDO> auditCaptor = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper, atLeast(2)).insert(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(BiAuditLogDO::getActionKey)
                .contains("BI_EXPORT_DOWNLOAD", "BI_EXPORT_DOWNLOAD_RATE_LIMITED");
        assertThat(auditCaptor.getAllValues()).allSatisfy(audit -> {
            assertThat(audit.getTenantId()).isEqualTo(7L);
            assertThat(audit.getActorId()).isEqualTo("alice");
            assertThat(audit.getResourceType()).isEqualTo("BI_EXPORT_JOB");
            assertThat(audit.getResourceId()).isEqualTo(90L);
        });
    }

    @Test
    void downloadRejectsExpiredPartitionedExportAndDeletesPartObjects() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        String zipKey = "exports/tenant-7/export-157.zip";
        String partKey = "exports/tenant-7/export-157/parts/part-00001.csv";
        storage.write(partKey, "a,b\n".getBytes());
        storage.write(zipKey, zipWithManifest("""
                {
                  "storageLayout": "OBJECT_PER_PART_ZIP",
                  "parts": [
                    { "name": "part-00001.csv", "storageKey": "exports/tenant-7/export-157/parts/part-00001.csv" }
                  ]
                }
                """));
        BiExportJobDO row = exportJob();
        row.setId(157L);
        row.setStorageProvider("MEMORY");
        row.setStorageKey(zipKey);
        row.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(exportJobMapper.selectById(157L)).thenReturn(row);
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                storage,
                7,
                5000);

        assertThatThrownBy(() -> service.download(7L, 157L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        assertThat(storage.bytesByKey).doesNotContainKeys(zipKey, partKey);
        ArgumentCaptor<BiExportJobDO> updateCaptor = ArgumentCaptor.forClass(BiExportJobDO.class);
        verify(exportJobMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void partitionedDownloadAuditsManifestPartsAndObjectKeys() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        String zipKey = "exports/tenant-7/export-91.zip";
        String partKey = "exports/tenant-7/export-91/parts/part-00001.csv";
        storage.write(partKey, "a,b\n".getBytes(StandardCharsets.UTF_8));
        storage.write(zipKey, zipWithManifest("""
                {
                  "storageLayout": "OBJECT_PER_PART_ZIP",
                  "requestedRows": 15000,
                  "generatedRows": 10000,
                  "partCount": 1,
                  "partSize": 10000,
                  "parts": [
                    {
                      "name": "part-00001.csv",
                      "storageKey": "exports/tenant-7/export-91/parts/part-00001.csv",
                      "rowCount": 10000,
                      "sizeBytes": 4,
                      "sha256": "abc123"
                    }
                  ]
                }
                """));
        BiExportJobDO row = exportJob();
        row.setId(91L);
        row.setStorageProvider("MEMORY");
        row.setStorageKey(zipKey);
        when(exportJobMapper.selectById(91L)).thenReturn(row);
        BiSelfServiceExportService service = new BiSelfServiceExportService(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new ObjectMapper(),
                storage,
                7,
                5000,
                auditLogMapper,
                0);

        BiExportDownload download = service.download(7L, "alice", 91L);

        assertThat(download.filename()).isEqualTo("export-91.zip");
        ArgumentCaptor<BiAuditLogDO> auditCaptor = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        BiAuditLogDO audit = auditCaptor.getValue();
        assertThat(audit.getActionKey()).isEqualTo("BI_EXPORT_DOWNLOAD");
        assertThat(audit.getDetailJson())
                .contains("\"storageLayout\":\"OBJECT_PER_PART_ZIP\"")
                .contains("\"partCount\":1")
                .contains("\"partStorageKeys\":[\"exports/tenant-7/export-91/parts/part-00001.csv\"]")
                .contains("\"sha256\":\"abc123\"");
    }

    @Test
    void cleanupExpiredExportsDeletesFileAndMarksExpired() throws Exception {
        Fixture fixture = fixture();
        Path file = tempDir.resolve("tenant-7").resolve("export-57.csv");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "a,b\n");
        BiExportJobDO row = exportJob();
        row.setId(57L);
        row.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(fixture.exportJobMapper.selectList(any())).thenReturn(List.of(row));

        BiExportCleanupResult result = fixture.service.cleanupExpiredExports(7L, 100);

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.expired()).isEqualTo(1);
        assertThat(result.filesDeleted()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(file).doesNotExist();
        ArgumentCaptor<BiExportJobDO> updateCaptor = ArgumentCaptor.forClass(BiExportJobDO.class);
        verify(fixture.exportJobMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    private Fixture fixture() {
        return fixture(5000);
    }

    private Fixture fixture(int approvalRowThreshold) {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiExportJobMapper exportJobMapper = mock(BiExportJobMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        return new Fixture(
                datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                new BiSelfServiceExportService(
                        datasetMapper,
                        exportJobMapper,
                        queryExecutionService,
                        permissionService,
                        new ObjectMapper(),
                        tempDir,
                        null,
                        7,
                        approvalRowThreshold));
    }

    private BiQueryRequest query(int limit) {
        return new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12L)),
                List.of(),
                limit);
    }

    private BiQueryResult result() {
        return new BiQueryResult(
                "canvas_daily_stats",
                List.of(
                        new BiQueryColumn("stat_date", "DIMENSION", "DATE"),
                        new BiQueryColumn("total_executions", "METRIC", "NUMBER")),
                List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                1,
                12L,
                "abcdef");
    }

    private BiQueryResult resultRows(int count, int offset) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(Map.of(
                    "stat_date", "2026-06-05-" + (offset + index),
                    "total_executions", (long) offset + index));
        }
        return new BiQueryResult(
                "canvas_daily_stats",
                List.of(
                        new BiQueryColumn("stat_date", "DIMENSION", "DATE"),
                        new BiQueryColumn("total_executions", "METRIC", "NUMBER")),
                rows,
                rows.size(),
                12L,
                "abcdef-" + offset);
    }

    private Map<String, String> unzipTextEntries(byte[] bytes) throws Exception {
        Map<String, String> entries = new java.util.LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                input.transferTo(output);
                entries.put(entry.getName(), output.toString(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private byte[] zipWithManifest(String manifestJson) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifestJson.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private BiDatasetDO dataset() {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(11L);
        row.setTenantId(0L);
        row.setWorkspaceId(3L);
        row.setDatasetKey("canvas_daily_stats");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiExportJobDO exportJob() {
        BiExportJobDO row = new BiExportJobDO();
        row.setId(55L);
        row.setTenantId(7L);
        row.setWorkspaceId(3L);
        row.setResourceType("DATASET");
        row.setResourceId(11L);
        row.setExportFormat("CSV");
        row.setRequestJson("{}");
        row.setRowLimit(100);
        row.setStatus("COMPLETED");
        row.setFileUrl("/canvas/bi/self-service/exports/55/download");
        row.setRetentionDays(7);
        row.setExpiresAt(LocalDateTime.now().plusDays(7));
        row.setDownloadCount(0);
        row.setCreatedBy("alice");
        return row;
    }

    private record Fixture(
            BiDatasetMapper datasetMapper,
            BiExportJobMapper exportJobMapper,
            BiQueryExecutionService queryExecutionService,
            BiPermissionService permissionService,
            BiSelfServiceExportService service) {
    }

    private static final class CapturingStorage implements BiFileStorage {
        private final String provider;
        private final Map<String, byte[]> bytesByKey = new HashMap<>();

        private CapturingStorage(String provider) {
            this.provider = provider;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public BiStoredFile write(String storageKey, byte[] bytes) {
            bytesByKey.put(storageKey, bytes);
            return new BiStoredFile(provider, storageKey, "memory://" + storageKey, (long) bytes.length);
        }

        @Override
        public byte[] read(String storageKey) {
            return bytesByKey.get(storageKey);
        }

        @Override
        public boolean delete(String storageKey) {
            return bytesByKey.remove(storageKey) != null;
        }
    }

    private static final class FailingZipStorage implements BiFileStorage {
        private final String provider;
        private final Map<String, byte[]> bytesByKey = new HashMap<>();

        private FailingZipStorage(String provider) {
            this.provider = provider;
        }

        @Override
        public String provider() {
            return provider;
        }

        @Override
        public BiStoredFile write(String storageKey, byte[] bytes) {
            if (storageKey.endsWith(".zip")) {
                throw new IllegalStateException("zip storage unavailable");
            }
            byte[] payload = bytes == null ? new byte[0] : bytes;
            bytesByKey.put(storageKey, payload);
            return new BiStoredFile(provider, storageKey, "memory://" + storageKey, (long) payload.length);
        }

        @Override
        public BiStoredFile write(String storageKey, BiFileStorageWriter writer) {
            if (storageKey.endsWith(".zip")) {
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    writer.write(output);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                throw new IllegalStateException("zip storage unavailable");
            }
            return BiFileStorage.super.write(storageKey, writer);
        }

        @Override
        public byte[] read(String storageKey) {
            return bytesByKey.get(storageKey);
        }

        @Override
        public boolean delete(String storageKey) {
            return bytesByKey.remove(storageKey) != null;
        }
    }
}
