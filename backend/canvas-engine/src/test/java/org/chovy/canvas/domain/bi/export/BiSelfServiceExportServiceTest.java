package org.chovy.canvas.domain.bi.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiExportJobDO;
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
import org.chovy.canvas.domain.bi.storage.BiStoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.fileUrl()).isEqualTo("/canvas/bi/self-service/exports/55/download");
        assertThat(view.retentionDays()).isEqualTo(7);
        assertThat(view.expiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(view.downloadCount()).isZero();
        verify(fixture.permissionService).enforceResourceAccess(
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

        assertThatThrownBy(() -> fixture.service.createExport(
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
                        null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("warehouse is busy");

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

        assertThat(approved.status()).isEqualTo("COMPLETED");
        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");
        assertThat(approved.reviewedBy()).isEqualTo("admin");
        assertThat(approved.reviewComment()).isEqualTo("ok");
        assertThat(approved.fileUrl()).isEqualTo("/canvas/bi/self-service/exports/59/download");
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
        assertThat(contextCaptor.getValue()).isEqualTo(new BiQueryContext(7L, "alice", RoleNames.OPERATOR));
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
                        tempDir));
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
}
