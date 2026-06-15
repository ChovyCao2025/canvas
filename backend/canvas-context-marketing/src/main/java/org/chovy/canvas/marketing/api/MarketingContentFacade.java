package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface MarketingContentFacade {

    List<Map<String, Object>> listAssetFolders(Long tenantId);

    Map<String, Object> createAssetFolder(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listAssets(Long tenantId, String keyword, String assetType, String status);

    Map<String, Object> createAsset(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> createUploadIntent(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> expireStaleUploadIntents(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> setAssetStatus(Long tenantId, String assetKey, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listTemplates(Long tenantId, String keyword, String channel, String status);

    Map<String, Object> saveTemplate(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> previewTemplate(Long tenantId, String templateKey, Map<String, Object> variables);

    Map<String, Object> setTemplateStatus(Long tenantId, String templateKey, Map<String, Object> payload,
                                          String actor);

    List<Map<String, Object>> listEntries(Long tenantId, String keyword, String contentType, String status);

    Map<String, Object> saveEntry(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> publishEntry(Long tenantId, String entryKey, Map<String, Object> payload, String actor);

    Map<String, Object> archiveEntry(Long tenantId, String entryKey, Map<String, Object> payload, String actor);

    Map<String, Object> validateRelease(Long tenantId, Map<String, Object> payload);

    Map<String, Object> publishRelease(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listReleases(Long tenantId, String sourceType, String sourceKey, String status);

    Map<String, Object> resolveRelease(Long tenantId, String releaseKey, Map<String, Object> payload, String actor);

    Map<String, Object> rollbackRelease(Long tenantId, String releaseKey, Map<String, Object> payload, String actor);

    List<Map<String, Object>> auditEvents(Long tenantId, String targetType, String targetKey, Integer limit);
}
