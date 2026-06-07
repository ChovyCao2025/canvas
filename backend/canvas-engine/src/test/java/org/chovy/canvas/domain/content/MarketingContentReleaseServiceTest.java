package org.chovy.canvas.domain.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingAssetDO;
import org.chovy.canvas.dal.dataobject.MarketingContentAuditEventDO;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryDO;
import org.chovy.canvas.dal.dataobject.MarketingContentReleaseDO;
import org.chovy.canvas.dal.dataobject.MarketingContentReleaseItemDO;
import org.chovy.canvas.dal.dataobject.MarketingContentTemplateDO;
import org.chovy.canvas.dal.mapper.MarketingAssetMapper;
import org.chovy.canvas.dal.mapper.MarketingContentAuditEventMapper;
import org.chovy.canvas.dal.mapper.MarketingContentEntryMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseItemMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseMapper;
import org.chovy.canvas.dal.mapper.MarketingContentTemplateMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingContentReleaseServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateReportsMissingReferencedAsset() {
        Harness harness = harness();
        when(harness.templateMapper.selectOne(any())).thenReturn(template("welcome", "APPROVED", "[\"missing-hero\"]"));
        when(harness.assetMapper.selectOne(any())).thenReturn(null);

        MarketingContentReleaseService.ValidationResult result = harness.service.validate(
                operator(),
                new MarketingContentReleaseService.ValidationCommand("TEMPLATE", "welcome"));

        assertThat(result.ready()).isFalse();
        assertThat(result.assetRefs()).containsExactly("missing-hero");
        assertThat(result.blockers())
                .extracting(MarketingContentReleaseService.ReleaseBlocker::reason)
                .containsExactly("asset not found");
    }

    @Test
    void validateRejectsUnreadyVideoAsset() {
        Harness harness = harness();
        when(harness.templateMapper.selectOne(any())).thenReturn(template("welcome", "APPROVED", "[\"hero-video\"]"));
        when(harness.assetMapper.selectOne(any())).thenReturn(asset("hero-video", "VIDEO", "READY", "PENDING"));

        MarketingContentReleaseService.ValidationResult result = harness.service.validate(
                operator(),
                new MarketingContentReleaseService.ValidationCommand("TEMPLATE", "welcome"));

        assertThat(result.ready()).isFalse();
        assertThat(result.blockers())
                .extracting(MarketingContentReleaseService.ReleaseBlocker::reason)
                .containsExactly("video transcode is not ready");
    }

    @Test
    void validateReportsMalformedTemplateJsonAsStructuredBlocker() {
        Harness harness = harness();
        MarketingContentTemplateDO row = template("welcome", "APPROVED", "{}");
        row.setDesignJson("[]");
        when(harness.templateMapper.selectOne(any())).thenReturn(row);

        MarketingContentReleaseService.ValidationResult result = harness.service.validate(
                operator(),
                new MarketingContentReleaseService.ValidationCommand("TEMPLATE", "welcome"));

        assertThat(result.ready()).isFalse();
        assertThat(result.blockers())
                .extracting(MarketingContentReleaseService.ReleaseBlocker::reason)
                .contains("designJson must be a JSON object", "assetRefsJson must be a JSON array");
    }

    @Test
    void validateReportsMalformedEntryJsonAsStructuredBlocker() {
        Harness harness = harness();
        MarketingContentEntryDO row = entry("landing", "PUBLISHED", "[]");
        row.setBodyJson("[]");
        row.setSeoJson("{bad");
        when(harness.entryMapper.selectOne(any())).thenReturn(row);

        MarketingContentReleaseService.ValidationResult result = harness.service.validate(
                operator(),
                new MarketingContentReleaseService.ValidationCommand("ENTRY", "landing"));

        assertThat(result.ready()).isFalse();
        assertThat(result.blockers())
                .extracting(MarketingContentReleaseService.ReleaseBlocker::reason)
                .contains("bodyJson must be a JSON object", "seoJson must be valid JSON");
    }

    @Test
    void publishApprovedTemplateWritesReleaseItemsAuditAndReferenceCounts() {
        Harness harness = harness();
        when(harness.templateMapper.selectOne(any())).thenReturn(template("welcome", "APPROVED", "[\"hero-image\"]"));
        when(harness.assetMapper.selectOne(any())).thenReturn(asset("hero-image", "IMAGE", "READY", null));
        when(harness.releaseMapper.selectOne(any())).thenReturn(null);

        MarketingContentReleaseService.ReleaseView view = harness.service.publish(
                operator(),
                new MarketingContentReleaseService.ReleaseCommand("TEMPLATE", "welcome", "operator-1", "campaign launch"));

        assertThat(view.releaseKey()).isEqualTo("template-welcome");
        assertThat(view.sourceVersion()).isEqualTo(1);
        assertThat(view.status()).isEqualTo("ACTIVE");
        verify(harness.releaseMapper).insert(argThat((MarketingContentReleaseDO row) ->
                row.getTenantId().equals(8L)
                        && row.getReleaseKey().equals("template-welcome")
                        && row.getSourceType().equals("TEMPLATE")
                        && row.getSnapshotJson().contains("\"templateKey\":\"welcome\"")
                        && row.getAssetRefsJson().equals("[\"hero-image\"]")));
        verify(harness.itemMapper).insert(argThat((MarketingContentReleaseItemDO row) ->
                row.getTenantId().equals(8L)
                        && row.getItemType().equals("ASSET")
                        && row.getItemKey().equals("hero-image")
                        && row.getItemStatus().equals("READY")));
        verify(harness.assetMapper).updateById(argThat((MarketingAssetDO row) ->
                row.getAssetKey().equals("hero-image") && row.getReferenceCount().equals(1)));
        verify(harness.auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("RELEASE_PUBLISHED")
                        && row.getTargetType().equals("RELEASE")
                        && row.getTargetKey().equals("template-welcome")));
    }

    @Test
    void publishSupersedesPreviousActiveReleaseVersion() {
        Harness harness = harness();
        when(harness.templateMapper.selectOne(any())).thenReturn(template("welcome", "APPROVED", "[]"));
        when(harness.releaseMapper.selectOne(any())).thenReturn(release(
                "template-welcome",
                "TEMPLATE",
                "welcome",
                1,
                "{\"templateKey\":\"welcome\"}",
                "[]"));

        MarketingContentReleaseService.ReleaseView view = harness.service.publish(
                operator(),
                new MarketingContentReleaseService.ReleaseCommand("TEMPLATE", "welcome", "operator-1", "copy update"));

        assertThat(view.sourceVersion()).isEqualTo(2);
        verify(harness.releaseMapper).updateById(argThat((MarketingContentReleaseDO row) ->
                row.getReleaseKey().equals("template-welcome") && row.getStatus().equals("SUPERSEDED")));
        verify(harness.auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("RELEASE_SUPERSEDED")
                        && row.getTargetKey().equals("template-welcome")));
    }

    @Test
    void publishRequiresPublishedEntry() {
        Harness harness = harness();
        when(harness.entryMapper.selectOne(any())).thenReturn(entry("landing", "DRAFT", "[]"));

        assertThatThrownBy(() -> harness.service.publish(
                operator(),
                new MarketingContentReleaseService.ReleaseCommand("ENTRY", "landing", "operator-1", "not ready")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("content release gate failed");
    }

    @Test
    void resolveRendersTemplateFromImmutableReleaseSnapshot() {
        Harness harness = harness();
        when(harness.releaseMapper.selectOne(any())).thenReturn(release(
                "template-welcome",
                "TEMPLATE",
                "welcome",
                2,
                """
                        {"templateKey":"welcome","subject":"Hi {{ firstName }}","body":"Use {{couponCode}}","assetRefsJson":["hero-image"]}
                        """,
                "[\"hero-image\"]"));

        MarketingContentReleaseService.ResolvedRelease resolved = harness.service.resolve(
                operator(),
                "template-welcome",
                Map.of("firstName", "Alice"));

        assertThat(resolved.renderedSubject()).isEqualTo("Hi Alice");
        assertThat(resolved.renderedBody()).isEqualTo("Use {{couponCode}}");
        assertThat(resolved.missingVariables()).containsExactly("couponCode");
        assertThat(resolved.sourceVersion()).isEqualTo(2);
    }

    @Test
    void rollbackActiveReleaseWritesAudit() {
        Harness harness = harness();
        MarketingContentReleaseDO active = release(
                "template-welcome",
                "TEMPLATE",
                "welcome",
                1,
                "{\"templateKey\":\"welcome\"}",
                "[]");
        when(harness.releaseMapper.selectOne(any())).thenReturn(active).thenReturn(null);

        MarketingContentReleaseService.ReleaseView view = harness.service.rollback(
                operator(),
                "template-welcome",
                new MarketingContentReleaseService.RollbackCommand("operator-1", "bad copy"));

        assertThat(view.status()).isEqualTo("ROLLED_BACK");
        verify(harness.releaseMapper).updateById(argThat((MarketingContentReleaseDO row) ->
                row.getStatus().equals("ROLLED_BACK") && row.getRollbackReason().equals("bad copy")));
        verify(harness.auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("RELEASE_ROLLED_BACK")
                        && row.getTargetKey().equals("template-welcome")
                        && row.getNote().equals("bad copy")));
    }

    @Test
    void rollbackReactivatesLatestSupersededReleaseVersion() {
        Harness harness = harness();
        MarketingContentReleaseDO active = release(
                "template-welcome",
                "TEMPLATE",
                "welcome",
                2,
                "{\"templateKey\":\"welcome\",\"body\":\"bad copy\"}",
                "[]");
        MarketingContentReleaseDO previous = release(
                "template-welcome",
                "TEMPLATE",
                "welcome",
                1,
                "{\"templateKey\":\"welcome\",\"body\":\"stable copy\"}",
                "[]");
        previous.setStatus("SUPERSEDED");
        when(harness.releaseMapper.selectOne(any())).thenReturn(active).thenReturn(previous);

        MarketingContentReleaseService.ReleaseView view = harness.service.rollback(
                operator(),
                "template-welcome",
                new MarketingContentReleaseService.RollbackCommand("operator-1", "bad copy"));

        assertThat(view.sourceVersion()).isEqualTo(1);
        assertThat(view.status()).isEqualTo("ACTIVE");
        verify(harness.releaseMapper).updateById(argThat((MarketingContentReleaseDO row) ->
                row.getSourceVersion().equals(2)
                        && row.getStatus().equals("ROLLED_BACK")
                        && row.getRollbackReason().equals("bad copy")));
        verify(harness.releaseMapper).updateById(argThat((MarketingContentReleaseDO row) ->
                row.getSourceVersion().equals(1)
                        && row.getStatus().equals("ACTIVE")
                        && row.getRollbackReason() == null));
        verify(harness.auditMapper).insert(argThat((MarketingContentAuditEventDO row) ->
                row.getEventType().equals("RELEASE_RESTORED")
                        && row.getTargetKey().equals("template-welcome")
                        && row.getNote().equals("bad copy")));
    }

    private Harness harness() {
        MarketingContentReleaseMapper releaseMapper = mock(MarketingContentReleaseMapper.class);
        MarketingContentReleaseItemMapper itemMapper = mock(MarketingContentReleaseItemMapper.class);
        MarketingContentAuditEventMapper auditMapper = mock(MarketingContentAuditEventMapper.class);
        MarketingContentTemplateMapper templateMapper = mock(MarketingContentTemplateMapper.class);
        MarketingContentEntryMapper entryMapper = mock(MarketingContentEntryMapper.class);
        MarketingAssetMapper assetMapper = mock(MarketingAssetMapper.class);
        return new Harness(
                releaseMapper,
                itemMapper,
                auditMapper,
                templateMapper,
                entryMapper,
                assetMapper,
                new MarketingContentReleaseService(
                        releaseMapper,
                        itemMapper,
                        auditMapper,
                        templateMapper,
                        entryMapper,
                        assetMapper,
                        objectMapper));
    }

    private MarketingContentTemplateDO template(String key, String status, String assetRefsJson) {
        MarketingContentTemplateDO row = new MarketingContentTemplateDO();
        row.setTenantId(8L);
        row.setTemplateKey(key);
        row.setDisplayName("Welcome");
        row.setChannel("EMAIL");
        row.setSubject("Hi {{ firstName }}");
        row.setBody("Use {{couponCode}}");
        row.setDesignJson("{\"components\":[]}");
        row.setAssetRefsJson(assetRefsJson);
        row.setVariablesJson("[\"firstName\",\"couponCode\"]");
        row.setStatus(status);
        return row;
    }

    private MarketingContentEntryDO entry(String key, String status, String assetRefsJson) {
        MarketingContentEntryDO row = new MarketingContentEntryDO();
        row.setTenantId(8L);
        row.setEntryKey(key);
        row.setContentType("LANDING_PAGE");
        row.setTitle("Landing");
        row.setSlug("landing");
        row.setBodyJson("{\"blocks\":[]}");
        row.setSeoJson("{}");
        row.setAssetRefsJson(assetRefsJson);
        row.setStatus(status);
        return row;
    }

    private MarketingAssetDO asset(String key, String type, String status, String transcodeStatus) {
        MarketingAssetDO row = new MarketingAssetDO();
        row.setTenantId(8L);
        row.setAssetKey(key);
        row.setName(key);
        row.setAssetType(type);
        row.setMimeType("VIDEO".equals(type) ? "video/mp4" : "image/png");
        row.setStorageUrl("https://cdn.example.com/" + key);
        row.setStatus(status);
        row.setTranscodeStatus(transcodeStatus);
        row.setReferenceCount(0);
        row.setTagsJson("[]");
        row.setMetadataJson("{}");
        return row;
    }

    private MarketingContentReleaseDO release(String releaseKey,
                                              String sourceType,
                                              String sourceKey,
                                              Integer version,
                                              String snapshotJson,
                                              String assetRefsJson) {
        MarketingContentReleaseDO row = new MarketingContentReleaseDO();
        row.setTenantId(8L);
        row.setReleaseKey(releaseKey);
        row.setSourceType(sourceType);
        row.setSourceKey(sourceKey);
        row.setSourceVersion(version);
        row.setChannel("TEMPLATE".equals(sourceType) ? "EMAIL" : "WEB");
        row.setStatus("ACTIVE");
        row.setSnapshotJson(snapshotJson);
        row.setAssetRefsJson(assetRefsJson);
        row.setChecksumSha256("sha256");
        row.setCreatedBy("operator-1");
        return row;
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }

    private record Harness(
            MarketingContentReleaseMapper releaseMapper,
            MarketingContentReleaseItemMapper itemMapper,
            MarketingContentAuditEventMapper auditMapper,
            MarketingContentTemplateMapper templateMapper,
            MarketingContentEntryMapper entryMapper,
            MarketingAssetMapper assetMapper,
            MarketingContentReleaseService service) {
    }
}
