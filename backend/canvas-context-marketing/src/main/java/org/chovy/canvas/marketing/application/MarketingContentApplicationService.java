package org.chovy.canvas.marketing.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingContentFacade;
import org.chovy.canvas.marketing.domain.MarketingContentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingContentApplicationService implements MarketingContentFacade {

    private final MarketingContentCatalog catalog = new MarketingContentCatalog();

    @Override
    public List<Map<String, Object>> listAssetFolders(Long tenantId) {
        return catalog.listAssetFolders(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAssetFolder(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createAssetFolder(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listAssets(Long tenantId, String keyword, String assetType, String status) {
        return catalog.listAssets(safeTenantId(tenantId), keyword, assetType, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAsset(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createAsset(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUploadIntent(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createUploadIntent(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> expireStaleUploadIntents(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.expireStaleUploadIntents(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setAssetStatus(Long tenantId, String assetKey, Map<String, Object> payload,
                                              String actor) {
        return catalog.setAssetStatus(safeTenantId(tenantId), assetKey, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listTemplates(Long tenantId, String keyword, String channel, String status) {
        return catalog.listTemplates(safeTenantId(tenantId), keyword, channel, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveTemplate(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.saveTemplate(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> previewTemplate(Long tenantId, String templateKey, Map<String, Object> variables) {
        return catalog.previewTemplate(safeTenantId(tenantId), templateKey, safePayload(variables));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setTemplateStatus(Long tenantId, String templateKey, Map<String, Object> payload,
                                                 String actor) {
        return catalog.setTemplateStatus(safeTenantId(tenantId), templateKey, safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listEntries(Long tenantId, String keyword, String contentType, String status) {
        return catalog.listEntries(safeTenantId(tenantId), keyword, contentType, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveEntry(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.saveEntry(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishEntry(Long tenantId, String entryKey, Map<String, Object> payload,
                                            String actor) {
        return catalog.publishEntry(safeTenantId(tenantId), entryKey, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> archiveEntry(Long tenantId, String entryKey, Map<String, Object> payload,
                                            String actor) {
        return catalog.archiveEntry(safeTenantId(tenantId), entryKey, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> validateRelease(Long tenantId, Map<String, Object> payload) {
        return catalog.validateRelease(safeTenantId(tenantId), safePayload(payload));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishRelease(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.publishRelease(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listReleases(Long tenantId, String sourceType, String sourceKey, String status) {
        return catalog.listReleases(safeTenantId(tenantId), sourceType, sourceKey, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resolveRelease(Long tenantId, String releaseKey, Map<String, Object> payload,
                                              String actor) {
        return catalog.resolveRelease(safeTenantId(tenantId), releaseKey, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollbackRelease(Long tenantId, String releaseKey, Map<String, Object> payload,
                                               String actor) {
        return catalog.rollbackRelease(safeTenantId(tenantId), releaseKey, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> auditEvents(Long tenantId, String targetType, String targetKey, Integer limit) {
        return catalog.auditEvents(safeTenantId(tenantId), targetType, targetKey, normalizedLimit(limit));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static int normalizedLimit(Integer limit) {
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }
}
