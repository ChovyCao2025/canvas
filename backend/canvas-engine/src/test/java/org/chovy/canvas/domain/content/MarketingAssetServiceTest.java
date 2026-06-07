package org.chovy.canvas.domain.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingAssetDO;
import org.chovy.canvas.dal.dataobject.MarketingAssetFolderDO;
import org.chovy.canvas.dal.mapper.MarketingAssetFolderMapper;
import org.chovy.canvas.dal.mapper.MarketingAssetMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingAssetServiceTest {

    @Test
    void createNormalizesAssetAndPreservesVideoMetadata() {
        MarketingAssetMapper mapper = mock(MarketingAssetMapper.class);
        MarketingAssetService service = new MarketingAssetService(mapper, mock(MarketingAssetFolderMapper.class), new ObjectMapper());

        MarketingAssetService.AssetView view = service.create(operator(), new MarketingAssetService.AssetCommand(
                " Hero Video ",
                "Hero Video",
                "video",
                "video/mp4",
                "https://cdn.example.com/hero.mp4",
                null,
                12_345_678L,
                "a".repeat(64),
                "https://cdn.example.com/hero-thumb.jpg",
                "https://cdn.example.com/hero-poster.jpg",
                1920,
                1080,
                61_000L,
                "external",
                List.of(" 618 ", "launch", "618"),
                Map.of("campaign", "summer", "scanStatus", "PROVIDER_VERIFIED", "provider", "MUX"),
                "ready",
                "checked",
                "operator-1"));

        assertThat(view.assetKey()).isEqualTo("hero-video");
        assertThat(view.folderId()).isNull();
        assertThat(view.assetType()).isEqualTo("VIDEO");
        assertThat(view.status()).isEqualTo("READY");
        assertThat(view.tags()).containsExactly("618", "launch");
        assertThat(view.durationMs()).isEqualTo(61_000L);
        assertThat(view.transcodeStatus()).isEqualTo("EXTERNAL");
        verify(mapper).insert(argThat((MarketingAssetDO row) ->
                row.getTenantId().equals(8L)
                        && row.getAssetKey().equals("hero-video")
                        && row.getAssetType().equals("VIDEO")
                        && row.getStatus().equals("READY")
                        && row.getCreatedBy().equals("operator-1")
                        && row.getTagsJson().equals("[\"618\",\"launch\"]")
                        && row.getMetadataJson().contains("\"campaign\":\"summer\"")));
    }

    @Test
    void createRejectsUnsupportedAssetTypeAndUnsafeUrl() {
        MarketingAssetService service = new MarketingAssetService(
                mock(MarketingAssetMapper.class),
                mock(MarketingAssetFolderMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.create(operator(), new MarketingAssetService.AssetCommand(
                "bad", "Bad", "html", "text/html", "https://example.com/bad.html",
                null, null, null, null, null, null, null, null, null,
                List.of(), Map.of(), "READY", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported asset type HTML");

        assertThatThrownBy(() -> service.create(operator(), new MarketingAssetService.AssetCommand(
                "bad", "Bad", "IMAGE", "image/png", "ftp://example.com/bad.png",
                null, null, null, null, null, null, null, null, null,
                List.of(), Map.of(), "READY", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asset storageUrl must use http or https");
    }

    @Test
    void setStatusRequiresSupportedStatus() {
        MarketingAssetService service = new MarketingAssetService(
                mock(MarketingAssetMapper.class),
                mock(MarketingAssetFolderMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.setStatus(operator(), "hero", new MarketingAssetService.AssetStatusCommand(
                "PUBLISHED", "nope")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported asset status PUBLISHED");
    }

    @Test
    void setStatusReadyReusesProductionReadyGate() {
        MarketingAssetMapper mapper = mock(MarketingAssetMapper.class);
        MarketingAssetDO row = asset("hero-video", "VIDEO", "DRAFT");
        row.setSizeBytes(12_000L);
        row.setDurationMs(61_000L);
        row.setTranscodeStatus("READY");
        row.setMetadataJson("{\"provider\":\"MUX\"}");
        when(mapper.selectOne(any())).thenReturn(row);
        MarketingAssetService service = new MarketingAssetService(
                mapper,
                mock(MarketingAssetFolderMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.setStatus(operator(), "hero-video",
                new MarketingAssetService.AssetStatusCommand("READY", "manual approval")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("READY asset requires PASSED or PROVIDER_VERIFIED scanStatus");
    }

    @Test
    void setStatusReadyAllowsPersistedScanAndVideoEvidence() {
        MarketingAssetMapper mapper = mock(MarketingAssetMapper.class);
        MarketingAssetDO row = asset("hero-video", "VIDEO", "DRAFT");
        row.setSizeBytes(12_000L);
        row.setChecksumSha256("a".repeat(64));
        row.setDurationMs(61_000L);
        row.setTranscodeStatus("READY");
        row.setMetadataJson("{\"scanStatus\":\"PROVIDER_VERIFIED\",\"provider\":\"MUX\"}");
        when(mapper.selectOne(any())).thenReturn(row);
        MarketingAssetService service = new MarketingAssetService(
                mapper,
                mock(MarketingAssetFolderMapper.class),
                new ObjectMapper());

        MarketingAssetService.AssetView view = service.setStatus(operator(), "hero-video",
                new MarketingAssetService.AssetStatusCommand("READY", "manual approval"));

        assertThat(view.status()).isEqualTo("READY");
        verify(mapper).updateById(argThat((MarketingAssetDO updated) ->
                updated.getAssetKey().equals("hero-video")
                        && updated.getStatus().equals("READY")
                        && updated.getReviewNotes().equals("manual approval")));
    }

    @Test
    void readyAssetRequiresScanEvidenceAndVideoTranscode() {
        MarketingAssetService service = new MarketingAssetService(
                mock(MarketingAssetMapper.class),
                mock(MarketingAssetFolderMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.create(operator(), new MarketingAssetService.AssetCommand(
                "hero", "Hero", "VIDEO", "video/mp4", "https://cdn.example.com/hero.mp4",
                null, 12_000L, null, null, null, 1920, 1080, 61_000L, "READY",
                List.of(), Map.of("provider", "MUX"), "READY", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("READY asset requires PASSED or PROVIDER_VERIFIED scanStatus");

        assertThatThrownBy(() -> service.create(operator(), new MarketingAssetService.AssetCommand(
                "hero", "Hero", "VIDEO", "video/mp4", "https://cdn.example.com/hero.mp4",
                null, 12_000L, null, null, null, 1920, 1080, null, "PENDING",
                List.of(), Map.of("scanStatus", "PROVIDER_VERIFIED", "provider", "MUX"),
                "READY", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("READY video asset requires READY or EXTERNAL transcodeStatus");
    }

    @Test
    void createFolderNormalizesKeyAndListReturnsTenantScopedFolders() {
        MarketingAssetFolderMapper folderMapper = mock(MarketingAssetFolderMapper.class);
        MarketingAssetService service = new MarketingAssetService(
                mock(MarketingAssetMapper.class),
                folderMapper,
                new ObjectMapper());
        when(folderMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(folder("campaign_assets")));

        MarketingAssetService.FolderView view = service.createFolder(operator(), new MarketingAssetService.FolderCommand(
                " Campaign Assets ",
                "Campaign Assets",
                null,
                "operator-1"));
        List<MarketingAssetService.FolderView> folders = service.listFolders(operator());

        assertThat(view.folderKey()).isEqualTo("campaign-assets");
        assertThat(folders).extracting(MarketingAssetService.FolderView::folderKey).containsExactly("campaign_assets");
        verify(folderMapper).insert(argThat((MarketingAssetFolderDO row) ->
                row.getTenantId().equals(8L)
                        && row.getFolderKey().equals("campaign-assets")
                        && row.getName().equals("Campaign Assets")
                        && row.getCreatedBy().equals("operator-1")));
    }

    @Test
    void createRejectsFolderOutsideTenant() {
        MarketingAssetFolderMapper folderMapper = mock(MarketingAssetFolderMapper.class);
        when(folderMapper.selectById(99L)).thenReturn(folder(99L, 99L, "outside"));
        MarketingAssetService service = new MarketingAssetService(
                mock(MarketingAssetMapper.class),
                folderMapper,
                new ObjectMapper());

        assertThatThrownBy(() -> service.create(operator(), new MarketingAssetService.AssetCommand(
                "asset", "Asset", "IMAGE", "image/png", "https://cdn.example.com/asset.png",
                99L, null, null, null, null, null, null, null, null,
                List.of(), Map.of(), "DRAFT", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asset folder not found");
    }

    @Test
    void createFolderRejectsParentOutsideTenant() {
        MarketingAssetFolderMapper folderMapper = mock(MarketingAssetFolderMapper.class);
        when(folderMapper.selectById(77L)).thenReturn(folder(77L, 99L, "outside"));
        MarketingAssetService service = new MarketingAssetService(
                mock(MarketingAssetMapper.class),
                folderMapper,
                new ObjectMapper());

        assertThatThrownBy(() -> service.createFolder(operator(), new MarketingAssetService.FolderCommand(
                "child",
                "Child",
                77L,
                "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent folder not found");
    }

    private MarketingAssetFolderDO folder(String folderKey) {
        return folder(42L, 8L, folderKey);
    }

    private MarketingAssetFolderDO folder(Long id, Long tenantId, String folderKey) {
        MarketingAssetFolderDO row = new MarketingAssetFolderDO();
        row.setTenantId(tenantId);
        row.setId(id);
        row.setFolderKey(folderKey);
        row.setName("Campaign Assets");
        row.setCreatedBy("operator-1");
        return row;
    }

    private MarketingAssetDO asset(String assetKey, String assetType, String status) {
        MarketingAssetDO row = new MarketingAssetDO();
        row.setTenantId(8L);
        row.setAssetKey(assetKey);
        row.setName("Hero Video");
        row.setAssetType(assetType);
        row.setMimeType("VIDEO".equals(assetType) ? "video/mp4" : "image/png");
        row.setStorageUrl("https://cdn.example.com/" + assetKey);
        row.setStatus(status);
        row.setTagsJson("[]");
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        return row;
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
