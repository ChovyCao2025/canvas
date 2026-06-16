package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingContentFacade;
import org.chovy.canvas.marketing.domain.MarketingContentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排MarketingContent相关的应用层用例。
 */
@Service
public class MarketingContentApplicationService implements MarketingContentFacade {

    private final MarketingContentCatalog catalog = new MarketingContentCatalog();

    /**
     * 查询assetFolders列表。
     */
    @Override
    public List<Map<String, Object>> listAssetFolders(Long tenantId) {
        return catalog.listAssetFolders(safeTenantId(tenantId));
    }

    /**
     * 创建assetFolder业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAssetFolder(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createAssetFolder(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询assets列表。
     */
    @Override
    public List<Map<String, Object>> listAssets(Long tenantId, String keyword, String assetType, String status) {
        return catalog.listAssets(safeTenantId(tenantId), keyword, assetType, status);
    }

    /**
     * 创建asset业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAsset(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createAsset(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 创建uploadIntent业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUploadIntent(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createUploadIntent(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行expireStaleUploadIntents业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> expireStaleUploadIntents(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.expireStaleUploadIntents(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 设置assetStatus字段值。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setAssetStatus(Long tenantId, String assetKey, Map<String, Object> payload,
                                              String actor) {
        return catalog.setAssetStatus(safeTenantId(tenantId), assetKey, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询templates列表。
     */
    @Override
    public List<Map<String, Object>> listTemplates(Long tenantId, String keyword, String channel, String status) {
        return catalog.listTemplates(safeTenantId(tenantId), keyword, channel, status);
    }

    /**
     * 执行saveTemplate业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveTemplate(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.saveTemplate(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行previewTemplate业务操作。
     */
    @Override
    public Map<String, Object> previewTemplate(Long tenantId, String templateKey, Map<String, Object> variables) {
        return catalog.previewTemplate(safeTenantId(tenantId), templateKey, safePayload(variables));
    }

    /**
     * 设置templateStatus字段值。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setTemplateStatus(Long tenantId, String templateKey, Map<String, Object> payload,
                                                 String actor) {
        return catalog.setTemplateStatus(safeTenantId(tenantId), templateKey, safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 查询entries列表。
     */
    @Override
    public List<Map<String, Object>> listEntries(Long tenantId, String keyword, String contentType, String status) {
        return catalog.listEntries(safeTenantId(tenantId), keyword, contentType, status);
    }

    /**
     * 执行saveEntry业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveEntry(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.saveEntry(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行publishEntry业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishEntry(Long tenantId, String entryKey, Map<String, Object> payload,
                                            String actor) {
        return catalog.publishEntry(safeTenantId(tenantId), entryKey, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行archiveEntry业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> archiveEntry(Long tenantId, String entryKey, Map<String, Object> payload,
                                            String actor) {
        return catalog.archiveEntry(safeTenantId(tenantId), entryKey, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行validateRelease业务操作。
     */
    @Override
    public Map<String, Object> validateRelease(Long tenantId, Map<String, Object> payload) {
        return catalog.validateRelease(safeTenantId(tenantId), safePayload(payload));
    }

    /**
     * 执行publishRelease业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishRelease(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.publishRelease(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询releases列表。
     */
    @Override
    public List<Map<String, Object>> listReleases(Long tenantId, String sourceType, String sourceKey, String status) {
        return catalog.listReleases(safeTenantId(tenantId), sourceType, sourceKey, status);
    }

    /**
     * 执行resolveRelease业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resolveRelease(Long tenantId, String releaseKey, Map<String, Object> payload,
                                              String actor) {
        return catalog.resolveRelease(safeTenantId(tenantId), releaseKey, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行rollbackRelease业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollbackRelease(Long tenantId, String releaseKey, Map<String, Object> payload,
                                               String actor) {
        return catalog.rollbackRelease(safeTenantId(tenantId), releaseKey, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 执行auditEvents业务操作。
     */
    @Override
    public List<Map<String, Object>> auditEvents(Long tenantId, String targetType, String targetKey, Integer limit) {
        return catalog.auditEvents(safeTenantId(tenantId), targetType, targetKey, normalizedLimit(limit));
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行safePayload业务操作。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 规范化dLimit输入值。
     */
    private static int normalizedLimit(Integer limit) {
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }
}
