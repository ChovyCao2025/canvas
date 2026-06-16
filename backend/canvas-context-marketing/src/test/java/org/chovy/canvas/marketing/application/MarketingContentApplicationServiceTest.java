package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingContentFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证MarketingContentApplicationService的关键兼容行为。
 */
class MarketingContentApplicationServiceTest {

    /**
     * 验证 content state is tenant scoped deterministic and audited 场景的兼容行为。
     */
    @Test
    void contentStateIsTenantScopedDeterministicAndAudited() {
        MarketingContentFacade service = new MarketingContentApplicationService();

        Map<String, Object> folder = service.createAssetFolder(7L,
                Map.of("folderName", "Hero Assets", "parentKey", "root"), "operator-1");
        Map<String, Object> asset = service.createAsset(7L,
                Map.of("assetName", "Launch Hero", "assetType", "image", "folderKey", folder.get("folderKey")),
                "operator-1");
        Map<String, Object> intent = service.createUploadIntent(7L,
                Map.of("fileName", "hero.png", "assetType", "image", "sizeBytes", 1024), "operator-1");
        Map<String, Object> template = service.saveTemplate(7L,
                Map.of("templateName", "Welcome", "channel", "email", "body", "Hello {{name}}"), "operator-1");
        Map<String, Object> preview = service.previewTemplate(7L, String.valueOf(template.get("templateKey")),
                Map.of("name", "Ada"));
        Map<String, Object> entry = service.saveEntry(7L,
                Map.of("title", "Launch copy", "contentType", "email", "templateKey", template.get("templateKey")),
                "operator-1");
        Map<String, Object> publishedEntry = service.publishEntry(7L, String.valueOf(entry.get("entryKey")),
                Map.of("reason", "ready"), "operator-2");
        Map<String, Object> validation = service.validateRelease(7L,
                Map.of("sourceType", "entry", "sourceKey", entry.get("entryKey"), "channel", "email"));
        Map<String, Object> release = service.publishRelease(7L,
                Map.of("sourceType", "entry", "sourceKey", entry.get("entryKey"), "channel", "email"),
                "operator-2");
        Map<String, Object> resolved = service.resolveRelease(7L, String.valueOf(release.get("releaseKey")),
                Map.of("externalStatus", "delivered"), "operator-3");
        Map<String, Object> rollback = service.rollbackRelease(7L, String.valueOf(release.get("releaseKey")),
                Map.of("reason", "incorrect audience"), "operator-4");

        assertThat(folder).containsEntry("tenantId", 7L)
                .containsEntry("folderKey", "folder-1")
                .containsEntry("updatedBy", "operator-1");
        assertThat(asset).containsEntry("assetKey", "asset-1")
                .containsEntry("assetType", "IMAGE")
                .containsEntry("status", "ACTIVE");
        assertThat(intent).containsEntry("uploadIntentKey", "upload-intent-1")
                .containsEntry("status", "PENDING");
        assertThat(preview).containsEntry("templateKey", "template-1")
                .containsEntry("renderedBody", "Hello Ada");
        assertThat(publishedEntry).containsEntry("status", "PUBLISHED")
                .containsEntry("updatedBy", "operator-2");
        assertThat(validation).containsEntry("valid", true)
                .containsEntry("blockerCount", 0);
        assertThat(release).containsEntry("releaseKey", "release-1")
                .containsEntry("status", "PUBLISHED");
        assertThat(resolved).containsEntry("resolved", true)
                .containsEntry("status", "RESOLVED");
        assertThat(rollback).containsEntry("status", "ROLLED_BACK")
                .containsEntry("updatedBy", "operator-4");

        assertThat(service.listAssets(8L, null, null, null)).isEmpty();
        assertThat(service.listReleases(7L, "ENTRY", String.valueOf(entry.get("entryKey")), null)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("releaseKey", "release-1"));
        assertThat(service.auditEvents(7L, "RELEASE", "release-1", 20))
                .extracting(row -> row.get("operation"))
                .contains("publishRelease", "resolveRelease", "rollbackRelease");
    }

    /**
     * 验证 filters cleanup status and validation follow compatibility rules 场景的兼容行为。
     */
    @Test
    void filtersCleanupStatusAndValidationFollowCompatibilityRules() {
        MarketingContentFacade service = new MarketingContentApplicationService();

        service.createAsset(7L, Map.of("assetName", "Hero", "assetType", "image"), "operator-1");
        service.createAsset(7L, Map.of("assetName", "Footer", "assetType", "html"), "operator-1");
        Map<String, Object> template = service.saveTemplate(7L,
                Map.of("templateName", "Promo", "channel", "sms", "status", "draft"), "operator-1");
        service.saveEntry(7L, Map.of("title", "Promo entry", "contentType", "sms"), "operator-1");
        service.createUploadIntent(7L, Map.of("fileName", "old.png"), "operator-1");

        Map<String, Object> inactiveAsset = service.setAssetStatus(7L, "asset-1",
                Map.of("status", "inactive"), "operator-2");
        Map<String, Object> activeTemplate = service.setTemplateStatus(7L, String.valueOf(template.get("templateKey")),
                Map.of("status", "active"), "operator-2");
        Map<String, Object> archive = service.archiveEntry(7L, "entry-1", Map.of("reason", "done"), "operator-2");
        Map<String, Object> cleanup = service.expireStaleUploadIntents(7L, Map.of("olderThanMinutes", 1), "operator-2");

        assertThat(inactiveAsset).containsEntry("status", "INACTIVE");
        assertThat(activeTemplate).containsEntry("status", "ACTIVE");
        assertThat(archive).containsEntry("status", "ARCHIVED");
        assertThat(cleanup).containsEntry("expiredCount", 1);
        assertThat(service.listAssets(7L, "hero", "image", "inactive")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("assetKey", "asset-1"));
        assertThat(service.listTemplates(7L, "promo", "sms", "active")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("templateKey", "template-1"));
        assertThat(service.listEntries(7L, "promo", "sms", "archived")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("entryKey", "entry-1"));

        assertThatThrownBy(() -> service.createAssetFolder(7L, Map.of("folderName", ""), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("folderName is required");
    }
}
