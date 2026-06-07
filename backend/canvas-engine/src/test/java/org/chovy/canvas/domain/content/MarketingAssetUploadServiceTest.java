package org.chovy.canvas.domain.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingAssetDO;
import org.chovy.canvas.dal.dataobject.MarketingAssetUploadIntentDO;
import org.chovy.canvas.dal.dataobject.MarketingContentAuditEventDO;
import org.chovy.canvas.dal.mapper.MarketingAssetMapper;
import org.chovy.canvas.dal.mapper.MarketingAssetUploadIntentMapper;
import org.chovy.canvas.dal.mapper.MarketingContentAuditEventMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingAssetUploadServiceTest {

    @Test
    void createIntentReturnsProviderHandoffAndWritesAudit() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingContentAuditEventMapper auditMapper = mock(MarketingContentAuditEventMapper.class);
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                mock(MarketingAssetMapper.class),
                auditMapper,
                new ObjectMapper());

        MarketingAssetUploadService.UploadIntentView view = service.createIntent(operator(),
                new MarketingAssetUploadService.UploadIntentCommand(
                        " Hero Video ",
                        "video",
                        "mux",
                        "video/mp4",
                        "hero.mp4",
                        12_000L,
                        "operator-1"));

        assertThat(view.assetKey()).isEqualTo("hero-video");
        assertThat(view.assetType()).isEqualTo("VIDEO");
        assertThat(view.provider()).isEqualTo("MUX");
        assertThat(view.uploadToken()).isNotBlank();
        assertThat(view.uploadUrl()).isEqualTo("/provider/mux/direct-upload");
        assertThat(view.uploadParams()).containsEntry("mimeType", "video/mp4");
        assertThat(view.uploadParams()).containsEntry("maxSizeBytes", 2_147_483_648L);
        assertThat(view.uploadParams()).containsKey("objectKey");
        assertThat(view.uploadParams()).containsKey("requiredHeaders");
        assertThat(view.uploadParams()).containsKey("callbackRequirement");
        verify(intentMapper).insert(argThat((MarketingAssetUploadIntentDO row) ->
                row.getTenantId().equals(8L)
                        && row.getAssetKey().equals("hero-video")
                        && row.getStatus().equals("PENDING")
                        && row.getUploadToken() != null
                        && row.getExpiresAt() != null));
        verify(auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("ASSET_UPLOAD_INTENT_CREATED")
                        && row.getTargetType().equals("ASSET")
                        && row.getTargetKey().equals("hero-video")));
    }

    @Test
    void createS3IntentReturnsSigV4PresignedPutWithSignedHeadersAndStableCdnUrl() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingContentAuditEventMapper auditMapper = mock(MarketingContentAuditEventMapper.class);
        MarketingAssetUploadHandoffService handoffService = new MarketingAssetUploadHandoffService(
                true,
                "https://s3.example.com",
                "us-east-1",
                "canvas-assets",
                "asset-access-key",
                "asset-secret-key-1234",
                "marketing-assets",
                true,
                "https://cdn.example.com/assets",
                "/provider/cloudinary/direct-upload",
                "/provider/mux/direct-upload",
                "/provider/external/register",
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC));
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                mock(MarketingAssetMapper.class),
                auditMapper,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC),
                handoffService);

        MarketingAssetUploadService.UploadIntentView view = service.createIntent(operator(),
                new MarketingAssetUploadService.UploadIntentCommand(
                        "hero-image",
                        "IMAGE",
                        "S3",
                        "image/png",
                        "hero.png",
                        20_000L,
                        "operator-1"));

        assertThat(view.uploadUrl())
                .startsWith("https://s3.example.com/canvas-assets/marketing-assets/tenants/8/marketing-assets/hero-image/")
                .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .contains("X-Amz-Credential=asset-access-key%2F20260606%2Fus-east-1%2Fs3%2Faws4_request")
                .contains("X-Amz-SignedHeaders=content-type%3Bhost%3Bx-amz-server-side-encryption")
                .contains("X-Amz-Signature=");
        assertThat(view.uploadParams())
                .containsEntry("handoffMode", "PRESIGNED_PUT")
                .containsEntry("storageProvider", "S3")
                .containsEntry("checksumRequiredAtCallback", true);
        assertThat(view.uploadParams().get("storageUrl").toString())
                .startsWith("https://cdn.example.com/assets/marketing-assets/tenants/8/marketing-assets/hero-image/");
        assertThat(view.uploadParams().get("requiredHeaders"))
                .isInstanceOfSatisfying(Map.class, headers -> assertThat(headers)
                        .containsEntry("content-type", "image/png")
                        .containsEntry("x-amz-server-side-encryption", "aws:kms"));
        verify(intentMapper).insert(argThat((MarketingAssetUploadIntentDO row) ->
                row.getUploadUrl().contains("X-Amz-Signature=")
                        && row.getUploadParamsJson().contains("\"handoffMode\":\"PRESIGNED_PUT\"")
                        && row.getUploadParamsJson().contains("https://cdn.example.com/assets")));
    }

    @Test
    void providerFailureUpdatesIntentWithoutRequiringStorageUrl() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        MarketingContentAuditEventMapper auditMapper = mock(MarketingContentAuditEventMapper.class);
        when(intentMapper.selectOne(any())).thenReturn(uploadIntent());
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                auditMapper,
                new ObjectMapper());

        MarketingAssetUploadService.UploadIntentView view = service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "S3",
                        "upload-token",
                        "provider-asset-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        null,
                        "FAILED",
                        "FAILED",
                        12_000L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("errorCode", "virus_scan_failed")));

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.providerAssetId()).isEqualTo("provider-asset-1");
        verify(intentMapper).updateById(argThat((MarketingAssetUploadIntentDO row) ->
                row.getStatus().equals("FAILED")
                        && row.getProviderAssetId().equals("provider-asset-1")
                        && row.getCallbackJson().contains("virus_scan_failed")
                        && row.getErrorMessage().equals("provider upload failed")));
        verify(assetMapper, never()).insert(any(MarketingAssetDO.class));
        verify(assetMapper, never()).updateById(any(MarketingAssetDO.class));
        verify(auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("ASSET_UPLOAD_UPDATED")
                        && row.getTargetType().equals("ASSET")
                        && row.getTargetKey().equals("hero-video")));
    }

    @Test
    void createIntentRejectsUnsafeMimeExtensionAndOversizedAssets() {
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                mock(MarketingAssetUploadIntentMapper.class),
                mock(MarketingAssetMapper.class),
                mock(MarketingContentAuditEventMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.createIntent(operator(),
                new MarketingAssetUploadService.UploadIntentCommand(
                        "hero-video",
                        "VIDEO",
                        "S3",
                        "application/octet-stream",
                        "hero.mp4",
                        12_000L,
                        "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported asset mimeType");

        assertThatThrownBy(() -> service.createIntent(operator(),
                new MarketingAssetUploadService.UploadIntentCommand(
                        "hero-video",
                        "VIDEO",
                        "S3",
                        "video/mp4",
                        "../hero.mp4",
                        12_000L,
                        "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileName must not contain path separators");

        assertThatThrownBy(() -> service.createIntent(operator(),
                new MarketingAssetUploadService.UploadIntentCommand(
                        "huge-video",
                        "VIDEO",
                        "S3",
                        "video/mp4",
                        "huge.mp4",
                        3L * 1024 * 1024 * 1024,
                        "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds VIDEO max size");
    }

    @Test
    void readyCallbackUpsertsAssetOnlyAfterChecksumScanAndTranscodeAreReady() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        MarketingContentAuditEventMapper auditMapper = mock(MarketingContentAuditEventMapper.class);
        MarketingAssetUploadIntentDO intent = uploadIntent("S3");
        intent.setUploadParamsJson("""
                {"handoffMode":"PRESIGNED_PUT","storageUrl":"https://cdn.example.com/hero.mp4"}
                """);
        when(intentMapper.selectOne(any())).thenReturn(intent);
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                auditMapper,
                new ObjectMapper());

        MarketingAssetUploadService.UploadIntentView view = service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "S3",
                        "upload-token",
                        "s3-object-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        "https://cdn.example.com/hero.mp4",
                        "READY",
                        "READY",
                        12_000L,
                        61_000L,
                        1920,
                        1080,
                        "https://cdn.example.com/hero.jpg",
                        checksum(),
                        "PASSED",
                        Map.of("providerEventId", "evt-1")));

        assertThat(view.status()).isEqualTo("COMPLETED");
        verify(assetMapper).insert(argThat((MarketingAssetDO row) ->
                row.getAssetKey().equals("hero-video")
                        && row.getStatus().equals("READY")
                        && row.getChecksumSha256().equals(checksum())
                        && row.getTranscodeStatus().equals("READY")
                        && row.getMetadataJson().contains("\"scanStatus\":\"PASSED\"")
                        && row.getMetadataJson().contains("\"providerEventId\":\"evt-1\"")
                        && row.getReviewNotes().contains("scan passed")));
        verify(auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("ASSET_UPLOAD_COMPLETED")
                        && row.getTargetKey().equals("hero-video")));
    }

    @Test
    void readyCallbackRejectsS3StorageUrlThatDoesNotMatchSignedHandoff() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        MarketingAssetUploadIntentDO intent = uploadIntent("S3");
        intent.setUploadParamsJson("""
                {"handoffMode":"PRESIGNED_PUT","storageUrl":"https://cdn.example.com/expected.mp4"}
                """);
        when(intentMapper.selectOne(any())).thenReturn(intent);
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                mock(MarketingContentAuditEventMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "S3",
                        "upload-token",
                        "s3-object-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        "https://cdn.example.com/other.mp4",
                        "READY",
                        "READY",
                        12_000L,
                        61_000L,
                        1920,
                        1080,
                        "https://cdn.example.com/hero.jpg",
                        checksum(),
                        "PASSED",
                        Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storageUrl does not match upload intent");
        verify(intentMapper, never()).updateById(any(MarketingAssetUploadIntentDO.class));
        verify(assetMapper, never()).insert(any(MarketingAssetDO.class));
    }

    @Test
    void readyCallbackRejectsMissingChecksumForDirectUpload() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        when(intentMapper.selectOne(any())).thenReturn(uploadIntent("S3"));
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                mock(MarketingContentAuditEventMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "S3",
                        "upload-token",
                        "s3-object-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        "https://cdn.example.com/hero.mp4",
                        "READY",
                        "READY",
                        12_000L,
                        61_000L,
                        1920,
                        1080,
                        "https://cdn.example.com/hero.jpg",
                        null,
                        "PASSED",
                        Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checksumSha256 is required");
        verify(intentMapper, never()).updateById(any(MarketingAssetUploadIntentDO.class));
        verify(assetMapper, never()).insert(any(MarketingAssetDO.class));
    }

    @Test
    void readyVideoCallbackRejectsPendingTranscode() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        when(intentMapper.selectOne(any())).thenReturn(uploadIntent("MUX"));
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                mock(MarketingContentAuditEventMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "MUX",
                        "upload-token",
                        "mux-asset-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        "https://stream.example.com/hero.mp4",
                        "READY",
                        "PENDING",
                        12_000L,
                        61_000L,
                        1920,
                        1080,
                        "https://stream.example.com/hero.jpg",
                        null,
                        "PROVIDER_VERIFIED",
                        Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("video transcode must be READY");
        verify(intentMapper, never()).updateById(any(MarketingAssetUploadIntentDO.class));
        verify(assetMapper, never()).insert(any(MarketingAssetDO.class));
    }

    @Test
    void expiredIntentRejectsCallbackBeforeAssetMutation() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        MarketingAssetUploadIntentDO intent = uploadIntent("S3");
        intent.setExpiresAt(LocalDateTime.of(2026, 6, 6, 7, 59));
        when(intentMapper.selectOne(any())).thenReturn(intent);
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                mock(MarketingContentAuditEventMapper.class),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T08:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "S3",
                        "upload-token",
                        "s3-object-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        "https://cdn.example.com/hero.mp4",
                        "READY",
                        "READY",
                        12_000L,
                        61_000L,
                        1920,
                        1080,
                        "https://cdn.example.com/hero.jpg",
                        checksum(),
                        "PASSED",
                        Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asset upload intent has expired");
        verify(intentMapper, never()).updateById(any(MarketingAssetUploadIntentDO.class));
        verify(assetMapper, never()).insert(any(MarketingAssetDO.class));
    }

    @Test
    void terminalIntentReplayReturnsExistingViewWithoutMutation() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        MarketingAssetUploadIntentDO intent = uploadIntent("S3");
        intent.setStatus("COMPLETED");
        intent.setProviderAssetId("s3-object-1");
        when(intentMapper.selectOne(any())).thenReturn(intent);
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                assetMapper,
                mock(MarketingContentAuditEventMapper.class),
                new ObjectMapper());

        MarketingAssetUploadService.UploadIntentView view = service.handleCallback(operator(),
                new MarketingAssetUploadService.ProviderCallbackCommand(
                        "S3",
                        "upload-token",
                        "s3-object-1",
                        null,
                        "VIDEO",
                        "video/mp4",
                        "https://cdn.example.com/hero.mp4",
                        "READY",
                        "READY",
                        12_000L,
                        61_000L,
                        1920,
                        1080,
                        "https://cdn.example.com/hero.jpg",
                        checksum(),
                        "PASSED",
                        Map.of()));

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.providerAssetId()).isEqualTo("s3-object-1");
        verify(intentMapper, never()).updateById(any(MarketingAssetUploadIntentDO.class));
        verify(assetMapper, never()).insert(any(MarketingAssetDO.class));
    }

    @Test
    void expireStalePendingUploadsMarksExpiredRowsFailedAndAudits() {
        MarketingAssetUploadIntentMapper intentMapper = mock(MarketingAssetUploadIntentMapper.class);
        MarketingContentAuditEventMapper auditMapper = mock(MarketingContentAuditEventMapper.class);
        MarketingAssetUploadIntentDO expired = uploadIntent("S3");
        expired.setId(12L);
        expired.setIntentKey("s3-expired-upload");
        expired.setExpiresAt(LocalDateTime.of(2026, 6, 6, 7, 59));
        when(intentMapper.selectList(any())).thenReturn(List.of(expired));
        MarketingAssetUploadService service = new MarketingAssetUploadService(
                intentMapper,
                mock(MarketingAssetMapper.class),
                auditMapper,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T08:00:00Z"), ZoneOffset.UTC));

        MarketingAssetUploadService.UploadIntentCleanupResult result = service.expireStalePendingUploads(
                operator(),
                new MarketingAssetUploadService.UploadIntentCleanupCommand(25, "cleanup-operator"));

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.expired()).isEqualTo(1);
        assertThat(result.cutoff()).isEqualTo(LocalDateTime.of(2026, 6, 6, 8, 0));
        verify(intentMapper).updateById(argThat((MarketingAssetUploadIntentDO row) ->
                row.getIntentKey().equals("s3-expired-upload")
                        && row.getStatus().equals("FAILED")
                        && row.getErrorMessage().equals("asset upload intent expired without callback")));
        verify(auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("ASSET_UPLOAD_EXPIRED")
                        && row.getTargetKey().equals("hero-video")
                        && row.getActor().equals("operator-1")));
    }

    private MarketingAssetUploadIntentDO uploadIntent() {
        return uploadIntent("S3");
    }

    private MarketingAssetUploadIntentDO uploadIntent(String provider) {
        MarketingAssetUploadIntentDO row = new MarketingAssetUploadIntentDO();
        row.setTenantId(8L);
        row.setIntentKey(provider.toLowerCase() + "-hero-video-upload");
        row.setAssetKey("hero-video");
        row.setAssetType("VIDEO");
        row.setProvider(provider);
        row.setMimeType("video/mp4");
        row.setFileName("hero.mp4");
        row.setSizeBytes(12_000L);
        row.setUploadToken("upload-token");
        row.setUploadUrl("MUX".equals(provider) ? "/provider/mux/direct-upload" : "/provider/s3/presigned-put");
        row.setUploadParamsJson("{\"assetKey\":\"hero-video\"}");
        row.setStatus("PENDING");
        row.setExpiresAt(LocalDateTime.now().plusHours(1));
        row.setCreatedBy("operator-1");
        return row;
    }

    private String checksum() {
        return "a".repeat(64);
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
