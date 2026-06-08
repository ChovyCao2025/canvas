package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDeliveryAttachmentDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiDeliveryAttachmentMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.storage.BiFileStorage;
import org.chovy.canvas.domain.bi.storage.BiStoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDeliveryAttachmentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createsSnapshotAndDataAttachmentsForSubscription() {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        AtomicReference<BiDeliveryAttachmentDO> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(row.getAttachmentType().equals("HTML") ? 101L : 102L);
            persisted.set(row);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).updateById(any(BiDeliveryAttachmentDO.class));
        when(mapper.selectById(101L)).thenAnswer(invocation -> persisted.get());
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(mapper, new ObjectMapper(), tempDir);

        List<BiDeliveryAttachmentView> views = service.createSubscriptionAttachments(
                7L,
                subscription(),
                Map.of("frequency", "DAILY", "time", "09:00"),
                Map.of("content", "SNAPSHOT_LINK", "attachments", List.of("PDF")),
                "alice");

        assertThat(views).extracting(BiDeliveryAttachmentView::attachmentType)
                .containsExactly("HTML", "PDF");
        assertThat(views).allSatisfy(view -> {
            assertThat(view.status()).isEqualTo("COMPLETED");
            assertThat(view.fileUrl()).startsWith("/canvas/bi/delivery-attachments/");
            assertThat(view.sizeBytes()).isPositive();
            assertThat(view.retentionDays()).isEqualTo(7);
            assertThat(view.expiresAt()).isAfter(LocalDateTime.now().plusDays(6));
            assertThat(view.downloadCount()).isZero();
        });
        ArgumentCaptor<BiDeliveryAttachmentDO> updateCaptor = ArgumentCaptor.forClass(BiDeliveryAttachmentDO.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues()).allSatisfy(row ->
                assertThat(Path.of(row.getFilePath())).exists());
    }

    @Test
    void downloadsCompletedAttachmentWithinTenant() {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        AtomicReference<BiDeliveryAttachmentDO> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(201L);
            persisted.set(row);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).updateById(any(BiDeliveryAttachmentDO.class));
        when(mapper.selectById(201L)).thenAnswer(invocation -> persisted.get());
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(mapper, new ObjectMapper(), tempDir);

        service.createSubscriptionAttachments(
                7L,
                subscription(),
                Map.of("frequency", "DAILY"),
                Map.of("attachment", "CSV"),
                "alice");

        BiDeliveryAttachmentDownload download = service.download(7L, 201L);

        assertThat(download.filename()).endsWith(".csv");
        assertThat(download.contentType()).isEqualTo("text/csv; charset=UTF-8");
        assertThat(new String(download.bytes())).contains("jobKey,canvas-daily");

        ArgumentCaptor<BiDeliveryAttachmentDO> updateCaptor = ArgumentCaptor.forClass(BiDeliveryAttachmentDO.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        BiDeliveryAttachmentDO audit = updateCaptor.getAllValues().get(1);
        assertThat(audit.getDownloadCount()).isEqualTo(1);
        assertThat(audit.getLastDownloadedAt()).isNotNull();
    }

    @Test
    void downloadChecksSubscribePermissionForBoundResource() throws Exception {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        BiPermissionService permissionService = mock(BiPermissionService.class);
        Path file = tempDir.resolve("canvas-daily.csv");
        Files.writeString(file, "key,value\njobKey,canvas-daily\n");
        when(mapper.selectById(201L)).thenReturn(completedAttachment(201L, file));
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(
                mapper,
                new ObjectMapper(),
                permissionService,
                null,
                tempDir);

        service.download(7L, 201L, "alice", "OPERATOR");

        verify(permissionService).enforceResourceAccess(
                eq(7L),
                eq(5L),
                eq("DASHBOARD"),
                eq(21L),
                any(BiQueryContext.class),
                eq(BiPermissionService.ACTION_SUBSCRIBE));
    }

    @Test
    void createsAndDownloadsAttachmentThroughConfiguredStorage() {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        AtomicReference<BiDeliveryAttachmentDO> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(601L);
            persisted.set(row);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).updateById(any(BiDeliveryAttachmentDO.class));
        when(mapper.selectById(601L)).thenAnswer(invocation -> persisted.get());
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(
                mapper,
                new ObjectMapper(),
                null,
                storage,
                7);

        List<BiDeliveryAttachmentView> views = service.createSubscriptionAttachments(
                7L,
                subscription(),
                Map.of("frequency", "DAILY"),
                Map.of("attachment", "CSV"),
                "alice");

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.status()).isEqualTo("COMPLETED");
            assertThat(view.storageProvider()).isEqualTo("MEMORY");
            assertThat(view.storageKey())
                    .startsWith("attachments/tenant-7/attachment-601/canvas-daily-csv-");
        });
        String storageKey = views.getFirst().storageKey();
        assertThat(storage.bytesByKey).containsKey(storageKey);
        BiDeliveryAttachmentDownload download = service.download(7L, 601L);
        assertThat(download.filename()).endsWith(".csv");
        assertThat(new String(download.bytes())).contains("jobKey,canvas-daily");
    }

    @Test
    void createsBrowserRenderedPngSnapshotWhenRendererConfigured() {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        FakeSnapshotRenderer renderer = new FakeSnapshotRenderer();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(301L);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(
                mapper,
                new ObjectMapper(),
                renderer,
                tempDir);

        List<BiDeliveryAttachmentView> views = service.createSubscriptionAttachments(
                7L,
                subscription(),
                Map.of("frequency", "DAILY"),
                Map.of(
                        "content", "SNAPSHOT",
                        "snapshotFormat", "PNG",
                        "snapshotWidth", 1280,
                        "snapshotHeight", 720,
                        "snapshotScale", 2),
                "alice");

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.attachmentType()).isEqualTo("PNG");
            assertThat(view.contentType()).isEqualTo("image/png");
            assertThat(view.fileName()).endsWith(".png");
            assertThat(view.sizeBytes()).isEqualTo((long) "PNG_BYTES".getBytes(StandardCharsets.UTF_8).length);
        });
        assertThat(renderer.request.format()).isEqualTo("PNG");
        assertThat(renderer.request.width()).isEqualTo(1280);
        assertThat(renderer.request.height()).isEqualTo(720);
        assertThat(renderer.request.scale()).isEqualTo(2.0);
        assertThat(renderer.request.html()).contains("BI Delivery Snapshot");
    }

    @Test
    void browserSnapshotUsesBigScreenWorkbenchModeUrl() {
        BiSnapshotRenderRequest request = renderSnapshotFor("BIG_SCREEN", 51L);

        assertThat(request.resourceUrl())
                .isEqualTo("/bi?resourceType=BIG_SCREEN&resourceId=51&mode=big-screen");
        assertThat(request.metadata().get("resourceUrl"))
                .isEqualTo("/bi?resourceType=BIG_SCREEN&resourceId=51&mode=big-screen");
    }

    @Test
    void browserSnapshotUsesSpreadsheetWorkbenchModeUrl() {
        BiSnapshotRenderRequest request = renderSnapshotFor("SPREADSHEET", 61L);

        assertThat(request.resourceUrl())
                .isEqualTo("/bi?resourceType=SPREADSHEET&resourceId=61&mode=spreadsheet");
        assertThat(request.metadata().get("resourceUrl"))
                .isEqualTo("/bi?resourceType=SPREADSHEET&resourceId=61&mode=spreadsheet");
    }

    @Test
    void createsMultiPagePdfWhenSummaryExceedsOnePage() throws Exception {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        AtomicReference<BiDeliveryAttachmentDO> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(501L);
            persisted.set(row);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).updateById(any(BiDeliveryAttachmentDO.class));
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(mapper, new ObjectMapper(), tempDir);

        List<BiDeliveryAttachmentView> views = service.createSubscriptionAttachments(
                7L,
                subscription(),
                Map.of("frequency", "DAILY"),
                Map.of("attachment", "PDF", "notes", "long audit note ".repeat(700)),
                "alice");

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.attachmentType()).isEqualTo("PDF");
            assertThat(view.contentType()).isEqualTo("application/pdf");
        });
        byte[] bytes = Files.readAllBytes(Path.of(persisted.get().getFilePath()));
        String pdf = new String(bytes, StandardCharsets.US_ASCII);
        int pageCount = pdf.split("/Type /Page[^s]").length - 1;
        assertThat(pageCount).isGreaterThanOrEqualTo(2);
        assertThat(pdf).contains("/Count " + pageCount);
    }

    @Test
    void multiPagePdfIncludesPageFootersAndEscapesLiteralText() throws Exception {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        AtomicReference<BiDeliveryAttachmentDO> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(502L);
            persisted.set(row);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).updateById(any(BiDeliveryAttachmentDO.class));
        BiSubscriptionDO subscription = subscription();
        subscription.setName("Canvas Daily (Ops) \\ Review");
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(mapper, new ObjectMapper(), tempDir);

        service.createSubscriptionAttachments(
                7L,
                subscription,
                Map.of("frequency", "DAILY"),
                Map.of("attachment", "PDF", "notes", "long audit note ".repeat(700)),
                "alice");

        String pdf = Files.readString(Path.of(persisted.get().getFilePath()), StandardCharsets.US_ASCII);
        int pageCount = pdf.split("/Type /Page[^s]").length - 1;
        assertThat(pageCount).isGreaterThanOrEqualTo(2);
        assertThat(pdf).contains("(Page 1 of " + pageCount + ") Tj");
        assertThat(pdf).contains("(Page " + pageCount + " of " + pageCount + ") Tj");
        assertThat(pdf).contains("(title: Canvas Daily \\(Ops\\) \\\\ Review) Tj");
    }

    @Test
    void rejectsExpiredAttachmentDownloadsAndMarksExpired() throws Exception {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        Path file = tempDir.resolve("tenant-7/attachment-401/report.csv");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "key,value\n");
        BiDeliveryAttachmentDO row = completedAttachment(401L, file);
        row.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(mapper.selectById(401L)).thenReturn(row);
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(mapper, new ObjectMapper(), tempDir);

        assertThatThrownBy(() -> service.download(7L, 401L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        ArgumentCaptor<BiDeliveryAttachmentDO> updateCaptor = ArgumentCaptor.forClass(BiDeliveryAttachmentDO.class);
        verify(mapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void rejectsExpiredStorageBackedAttachmentDownloadsAndDeletesObject() {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        CapturingStorage storage = new CapturingStorage("MEMORY");
        String storageKey = "attachments/tenant-7/attachment-501/report.csv";
        storage.write(storageKey, "key,value\n".getBytes(StandardCharsets.UTF_8));
        BiDeliveryAttachmentDO row = completedAttachment(501L, tempDir.resolve("unused/report.csv"));
        row.setFilePath(null);
        row.setStorageProvider("MEMORY");
        row.setStorageKey(storageKey);
        row.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(mapper.selectById(501L)).thenReturn(row);
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(
                mapper,
                new ObjectMapper(),
                null,
                storage,
                7);

        assertThatThrownBy(() -> service.download(7L, 501L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        assertThat(storage.bytesByKey).doesNotContainKey(storageKey);
        ArgumentCaptor<BiDeliveryAttachmentDO> updateCaptor = ArgumentCaptor.forClass(BiDeliveryAttachmentDO.class);
        verify(mapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void cleanupExpiredAttachmentsDeletesLocalFilesAndMarksExpired() throws Exception {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        Path file = tempDir.resolve("tenant-7/attachment-402/report.csv");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "key,value\n");
        BiDeliveryAttachmentDO row = completedAttachment(402L, file);
        row.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(mapper.selectList(any())).thenReturn(List.of(row));
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(mapper, new ObjectMapper(), tempDir);

        BiDeliveryAttachmentCleanupResult result = service.cleanupExpiredAttachments(7L, 20);

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.expired()).isEqualTo(1);
        assertThat(result.filesDeleted()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(file).doesNotExist();
        ArgumentCaptor<BiDeliveryAttachmentDO> updateCaptor = ArgumentCaptor.forClass(BiDeliveryAttachmentDO.class);
        verify(mapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    private BiSubscriptionDO subscription() {
        BiSubscriptionDO row = new BiSubscriptionDO();
        row.setId(31L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSubscriptionKey("canvas-daily");
        row.setName("Canvas Daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setEnabled(true);
        return row;
    }

    private BiSnapshotRenderRequest renderSnapshotFor(String resourceType, Long resourceId) {
        BiDeliveryAttachmentMapper mapper = mock(BiDeliveryAttachmentMapper.class);
        FakeSnapshotRenderer renderer = new FakeSnapshotRenderer();
        doAnswer(invocation -> {
            BiDeliveryAttachmentDO row = invocation.getArgument(0);
            row.setId(303L);
            return 1;
        }).when(mapper).insert(any(BiDeliveryAttachmentDO.class));
        BiDeliveryAttachmentService service = new BiDeliveryAttachmentService(
                mapper,
                new ObjectMapper(),
                renderer,
                tempDir);
        BiSubscriptionDO subscription = subscription();
        subscription.setResourceType(resourceType);
        subscription.setResourceId(resourceId);

        service.createSubscriptionAttachments(
                7L,
                subscription,
                Map.of("frequency", "DAILY"),
                Map.of("content", "SNAPSHOT", "snapshotFormat", "PNG"),
                "alice");

        return renderer.request;
    }

    private BiDeliveryAttachmentDO completedAttachment(Long id, Path file) {
        BiDeliveryAttachmentDO row = new BiDeliveryAttachmentDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setJobType("SUBSCRIPTION");
        row.setJobId(31L);
        row.setJobKey("canvas-daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setAttachmentKey("canvas-daily-csv");
        row.setAttachmentType("CSV");
        row.setFileName("canvas-daily.csv");
        row.setContentType("text/csv; charset=UTF-8");
        row.setFilePath(file.toString());
        row.setFileUrl("/canvas/bi/delivery-attachments/" + id + "/download");
        row.setSizeBytes(10L);
        row.setRetentionDays(7);
        row.setDownloadCount(0);
        row.setStatus("COMPLETED");
        return row;
    }

    private static final class FakeSnapshotRenderer implements BiSnapshotRenderer {
        private BiSnapshotRenderRequest request;

        @Override
        public boolean configured() {
            return true;
        }

        @Override
        public BiSnapshotRenderResult render(BiSnapshotRenderRequest request) {
            this.request = request;
            return new BiSnapshotRenderResult(
                    request.format(),
                    "image/png",
                    "PNG_BYTES".getBytes(StandardCharsets.UTF_8));
        }
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
