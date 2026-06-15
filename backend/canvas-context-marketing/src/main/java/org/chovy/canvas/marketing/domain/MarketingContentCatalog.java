package org.chovy.canvas.marketing.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MarketingContentCatalog {

    private final Map<Long, TenantContent> tenants = new LinkedHashMap<>();

    public List<Map<String, Object>> listAssetFolders(Long tenantId) {
        return copies(content(tenantId).folders);
    }

    public Map<String, Object> createAssetFolder(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "folderName");
        TenantContent content = content(tenantId);
        Map<String, Object> folder = record(payload);
        folder.put("tenantId", tenantId);
        folder.put("folderKey", "folder-" + (content.folders.size() + 1));
        folder.put("folderName", payload.get("folderName"));
        folder.put("updatedBy", actor);
        content.folders.add(folder);
        return copy(folder);
    }

    public List<Map<String, Object>> listAssets(Long tenantId, String keyword, String assetType, String status) {
        return content(tenantId).assets.stream()
                .filter(item -> contains(item, "assetName", keyword))
                .filter(item -> matches(item, "assetType", upper(assetType)))
                .filter(item -> matches(item, "status", upper(status)))
                .map(MarketingContentCatalog::copy)
                .toList();
    }

    public Map<String, Object> createAsset(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "assetName");
        TenantContent content = content(tenantId);
        Map<String, Object> asset = record(payload);
        asset.put("tenantId", tenantId);
        asset.put("assetKey", "asset-" + (content.assets.size() + 1));
        asset.put("assetType", upper(value(payload.get("assetType"), "UNKNOWN")));
        asset.put("status", upper(value(payload.get("status"), "ACTIVE")));
        asset.put("updatedBy", actor);
        content.assets.add(asset);
        audit(content, tenantId, "ASSET", String.valueOf(asset.get("assetKey")), "createAsset", actor);
        return copy(asset);
    }

    public Map<String, Object> createUploadIntent(Long tenantId, Map<String, Object> payload, String actor) {
        TenantContent content = content(tenantId);
        Map<String, Object> intent = record(payload);
        intent.put("tenantId", tenantId);
        intent.put("uploadIntentKey", "upload-intent-" + (content.uploadIntents.size() + 1));
        intent.put("status", "PENDING");
        intent.put("updatedBy", actor);
        content.uploadIntents.add(intent);
        return copy(intent);
    }

    public Map<String, Object> expireStaleUploadIntents(Long tenantId, Map<String, Object> payload, String actor) {
        TenantContent content = content(tenantId);
        int expired = 0;
        for (Map<String, Object> intent : content.uploadIntents) {
            if ("PENDING".equals(intent.get("status"))) {
                intent.put("status", "EXPIRED");
                intent.put("updatedBy", actor);
                expired++;
            }
        }
        return Map.of("tenantId", tenantId, "expiredCount", expired, "updatedBy", actor);
    }

    public Map<String, Object> setAssetStatus(Long tenantId, String assetKey, Map<String, Object> payload,
                                              String actor) {
        Map<String, Object> asset = find(content(tenantId).assets, "assetKey", assetKey, "asset not found");
        asset.put("status", upper(value(payload.get("status"), "ACTIVE")));
        asset.put("updatedBy", actor);
        return copy(asset);
    }

    public List<Map<String, Object>> listTemplates(Long tenantId, String keyword, String channel, String status) {
        return content(tenantId).templates.stream()
                .filter(item -> containsEither(item, keyword, "templateName", "name"))
                .filter(item -> matches(item, "channel", upper(channel)))
                .filter(item -> matches(item, "status", upper(status)))
                .map(MarketingContentCatalog::copy)
                .toList();
    }

    public Map<String, Object> saveTemplate(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "templateName");
        TenantContent content = content(tenantId);
        Map<String, Object> template = record(payload);
        template.put("tenantId", tenantId);
        template.put("templateKey", "template-" + (content.templates.size() + 1));
        template.put("channel", upper(value(payload.get("channel"), "DEFAULT")));
        template.put("status", upper(value(payload.get("status"), "DRAFT")));
        template.put("updatedBy", actor);
        content.templates.add(template);
        return copy(template);
    }

    public Map<String, Object> previewTemplate(Long tenantId, String templateKey, Map<String, Object> variables) {
        Map<String, Object> template = find(content(tenantId).templates, "templateKey", templateKey,
                "template not found");
        String rendered = value(template.get("body"), value(template.get("template"), ""));
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return Map.of("tenantId", tenantId, "templateKey", templateKey, "renderedBody", rendered);
    }

    public Map<String, Object> setTemplateStatus(Long tenantId, String templateKey, Map<String, Object> payload,
                                                 String actor) {
        Map<String, Object> template = find(content(tenantId).templates, "templateKey", templateKey,
                "template not found");
        template.put("status", upper(value(payload.get("status"), "ACTIVE")));
        template.put("updatedBy", actor);
        return copy(template);
    }

    public List<Map<String, Object>> listEntries(Long tenantId, String keyword, String contentType, String status) {
        return content(tenantId).entries.stream()
                .filter(item -> contains(item, "title", keyword))
                .filter(item -> matches(item, "contentType", upper(contentType)))
                .filter(item -> matches(item, "status", upper(status)))
                .map(MarketingContentCatalog::copy)
                .toList();
    }

    public Map<String, Object> saveEntry(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "title");
        TenantContent content = content(tenantId);
        Map<String, Object> entry = record(payload);
        entry.put("tenantId", tenantId);
        entry.put("entryKey", "entry-" + (content.entries.size() + 1));
        entry.put("contentType", upper(value(payload.get("contentType"), "DEFAULT")));
        entry.put("status", upper(value(payload.get("status"), "DRAFT")));
        entry.put("updatedBy", actor);
        content.entries.add(entry);
        return copy(entry);
    }

    public Map<String, Object> publishEntry(Long tenantId, String entryKey, Map<String, Object> payload, String actor) {
        Map<String, Object> entry = find(content(tenantId).entries, "entryKey", entryKey, "entry not found");
        entry.put("status", "PUBLISHED");
        entry.put("updatedBy", actor);
        return copy(entry);
    }

    public Map<String, Object> archiveEntry(Long tenantId, String entryKey, Map<String, Object> payload, String actor) {
        Map<String, Object> entry = find(content(tenantId).entries, "entryKey", entryKey, "entry not found");
        entry.put("status", "ARCHIVED");
        entry.put("updatedBy", actor);
        return copy(entry);
    }

    public Map<String, Object> validateRelease(Long tenantId, Map<String, Object> payload) {
        return Map.of("tenantId", tenantId, "valid", true, "blockerCount", 0,
                "sourceType", upper(value(payload.get("sourceType"), "ENTRY")),
                "sourceKey", value(payload.get("sourceKey"), ""));
    }

    public Map<String, Object> publishRelease(Long tenantId, Map<String, Object> payload, String actor) {
        TenantContent content = content(tenantId);
        Map<String, Object> release = record(payload);
        release.put("tenantId", tenantId);
        release.put("releaseKey", "release-" + (content.releases.size() + 1));
        release.put("sourceType", upper(value(payload.get("sourceType"), "ENTRY")));
        release.put("status", "PUBLISHED");
        release.put("updatedBy", actor);
        content.releases.add(release);
        audit(content, tenantId, "RELEASE", String.valueOf(release.get("releaseKey")), "publishRelease", actor);
        return copy(release);
    }

    public List<Map<String, Object>> listReleases(Long tenantId, String sourceType, String sourceKey, String status) {
        return content(tenantId).releases.stream()
                .filter(item -> matches(item, "sourceType", upper(sourceType)))
                .filter(item -> matches(item, "sourceKey", sourceKey))
                .filter(item -> matches(item, "status", upper(status)))
                .map(MarketingContentCatalog::copy)
                .toList();
    }

    public Map<String, Object> resolveRelease(Long tenantId, String releaseKey, Map<String, Object> payload,
                                              String actor) {
        TenantContent content = content(tenantId);
        Map<String, Object> release = find(content.releases, "releaseKey", releaseKey, "release not found");
        release.put("status", "RESOLVED");
        release.put("resolved", true);
        release.put("updatedBy", actor);
        audit(content, tenantId, "RELEASE", releaseKey, "resolveRelease", actor);
        return copy(release);
    }

    public Map<String, Object> rollbackRelease(Long tenantId, String releaseKey, Map<String, Object> payload,
                                               String actor) {
        TenantContent content = content(tenantId);
        Map<String, Object> release = find(content.releases, "releaseKey", releaseKey, "release not found");
        release.put("status", "ROLLED_BACK");
        release.put("updatedBy", actor);
        audit(content, tenantId, "RELEASE", releaseKey, "rollbackRelease", actor);
        return copy(release);
    }

    public List<Map<String, Object>> auditEvents(Long tenantId, String targetType, String targetKey, int limit) {
        return content(tenantId).audits.stream()
                .filter(item -> matches(item, "targetType", upper(targetType)))
                .filter(item -> matches(item, "targetKey", targetKey))
                .limit(limit)
                .map(MarketingContentCatalog::copy)
                .toList();
    }

    private TenantContent content(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new TenantContent());
    }

    private static void audit(TenantContent content, Long tenantId, String targetType, String targetKey,
                              String operation, String actor) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("tenantId", tenantId);
        event.put("auditEventKey", "audit-" + (content.audits.size() + 1));
        event.put("targetType", targetType);
        event.put("targetKey", targetKey);
        event.put("operation", operation);
        event.put("updatedBy", actor);
        content.audits.add(event);
    }

    private static Map<String, Object> record(Map<String, Object> payload) {
        return new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> find(List<Map<String, Object>> records, String key, String value,
                                            String message) {
        return records.stream()
                .filter(item -> Objects.equals(item.get(key), value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static boolean contains(Map<String, Object> item, String key, String keyword) {
        return keyword == null || value(item.get(key), "").toLowerCase(Locale.ROOT)
                .contains(keyword.toLowerCase(Locale.ROOT));
    }

    private static boolean containsEither(Map<String, Object> item, String keyword, String firstKey, String secondKey) {
        return keyword == null || contains(item, firstKey, keyword) || contains(item, secondKey, keyword);
    }

    private static boolean matches(Map<String, Object> item, String key, String expected) {
        return expected == null || Objects.equals(item.get(key), expected);
    }

    private static String required(Map<String, Object> payload, String key) {
        String value = value(payload.get(key), "");
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String value(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static List<Map<String, Object>> copies(List<Map<String, Object>> source) {
        return source.stream().map(MarketingContentCatalog::copy).toList();
    }

    private static final class TenantContent {
        private final List<Map<String, Object>> folders = new ArrayList<>();
        private final List<Map<String, Object>> assets = new ArrayList<>();
        private final List<Map<String, Object>> uploadIntents = new ArrayList<>();
        private final List<Map<String, Object>> templates = new ArrayList<>();
        private final List<Map<String, Object>> entries = new ArrayList<>();
        private final List<Map<String, Object>> releases = new ArrayList<>();
        private final List<Map<String, Object>> audits = new ArrayList<>();
    }
}
