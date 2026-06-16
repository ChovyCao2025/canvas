package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义MarketingContentFacade的营销上下文访问契约。
 */
public interface MarketingContentFacade {

    /**
     * 查询assetFolders列表。
     */
    List<Map<String, Object>> listAssetFolders(Long tenantId);

    /**
     * 创建assetFolder业务对象。
     */
    Map<String, Object> createAssetFolder(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询assets列表。
     */
    List<Map<String, Object>> listAssets(Long tenantId, String keyword, String assetType, String status);

    /**
     * 创建asset业务对象。
     */
    Map<String, Object> createAsset(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 创建uploadIntent业务对象。
     */
    Map<String, Object> createUploadIntent(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行expireStaleUploadIntents业务操作。
     */
    Map<String, Object> expireStaleUploadIntents(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 设置assetStatus字段值。
     */
    Map<String, Object> setAssetStatus(Long tenantId, String assetKey, Map<String, Object> payload, String actor);

    /**
     * 查询templates列表。
     */
    List<Map<String, Object>> listTemplates(Long tenantId, String keyword, String channel, String status);

    /**
     * 执行saveTemplate业务操作。
     */
    Map<String, Object> saveTemplate(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行previewTemplate业务操作。
     */
    Map<String, Object> previewTemplate(Long tenantId, String templateKey, Map<String, Object> variables);

    /**
     * 设置templateStatus字段值。
     */
    Map<String, Object> setTemplateStatus(Long tenantId, String templateKey, Map<String, Object> payload,
                                          String actor);

    /**
     * 查询entries列表。
     */
    List<Map<String, Object>> listEntries(Long tenantId, String keyword, String contentType, String status);

    /**
     * 执行saveEntry业务操作。
     */
    Map<String, Object> saveEntry(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 执行publishEntry业务操作。
     */
    Map<String, Object> publishEntry(Long tenantId, String entryKey, Map<String, Object> payload, String actor);

    /**
     * 执行archiveEntry业务操作。
     */
    Map<String, Object> archiveEntry(Long tenantId, String entryKey, Map<String, Object> payload, String actor);

    /**
     * 执行validateRelease业务操作。
     */
    Map<String, Object> validateRelease(Long tenantId, Map<String, Object> payload);

    /**
     * 执行publishRelease业务操作。
     */
    Map<String, Object> publishRelease(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询releases列表。
     */
    List<Map<String, Object>> listReleases(Long tenantId, String sourceType, String sourceKey, String status);

    /**
     * 执行resolveRelease业务操作。
     */
    Map<String, Object> resolveRelease(Long tenantId, String releaseKey, Map<String, Object> payload, String actor);

    /**
     * 执行rollbackRelease业务操作。
     */
    Map<String, Object> rollbackRelease(Long tenantId, String releaseKey, Map<String, Object> payload, String actor);

    /**
     * 执行auditEvents业务操作。
     */
    List<Map<String, Object>> auditEvents(Long tenantId, String targetType, String targetKey, Integer limit);
}
