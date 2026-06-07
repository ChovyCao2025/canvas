package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MarketingContentReleaseService {

    private static final Set<String> SOURCE_TYPES = Set.of("TEMPLATE", "ENTRY");

    private final MarketingContentReleaseMapper releaseMapper;
    private final MarketingContentReleaseItemMapper itemMapper;
    private final MarketingContentAuditEventMapper auditMapper;
    private final MarketingContentTemplateMapper templateMapper;
    private final MarketingContentEntryMapper entryMapper;
    private final MarketingAssetMapper assetMapper;
    private final ObjectMapper objectMapper;

    public MarketingContentReleaseService(MarketingContentReleaseMapper releaseMapper,
                                          MarketingContentReleaseItemMapper itemMapper,
                                          MarketingContentAuditEventMapper auditMapper,
                                          MarketingContentTemplateMapper templateMapper,
                                          MarketingContentEntryMapper entryMapper,
                                          MarketingAssetMapper assetMapper,
                                          ObjectMapper objectMapper) {
        this.releaseMapper = releaseMapper;
        this.itemMapper = itemMapper;
        this.auditMapper = auditMapper;
        this.templateMapper = templateMapper;
        this.entryMapper = entryMapper;
        this.assetMapper = assetMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public ValidationResult validate(TenantContext tenant, ValidationCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        SourceSnapshot source = loadSource(tenantId, command.sourceType(), command.sourceKey());
        List<ReleaseBlocker> blockers = new ArrayList<>();
        if (source.row() == null) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), "source not found"));
            return new ValidationResult(false, List.copyOf(blockers), List.of());
        }
        if ("TEMPLATE".equals(source.sourceType()) && !"APPROVED".equals(source.status())) {
            blockers.add(new ReleaseBlocker("TEMPLATE", source.sourceKey(), "template is not approved"));
        }
        if ("ENTRY".equals(source.sourceType()) && !"PUBLISHED".equals(source.status())) {
            blockers.add(new ReleaseBlocker("ENTRY", source.sourceKey(), "entry is not published"));
        }
        validateSourceShape(source, blockers);
        List<String> assetRefs = List.of();
        try {
            assetRefs = assetRefs(source.assetRefsJson());
        } catch (IllegalArgumentException ex) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), ex.getMessage()));
        }
        for (String assetKey : assetRefs) {
            MarketingAssetDO asset = findAsset(tenantId, assetKey);
            if (asset == null) {
                blockers.add(new ReleaseBlocker("ASSET", assetKey, "asset not found"));
                continue;
            }
            if (!"READY".equals(asset.getStatus())) {
                blockers.add(new ReleaseBlocker("ASSET", assetKey, "asset is not ready"));
            }
            String transcodeStatus = asset.getTranscodeStatus();
            if ("VIDEO".equals(asset.getAssetType())
                    && !"READY".equals(transcodeStatus)
                    && !"EXTERNAL".equals(transcodeStatus)) {
                blockers.add(new ReleaseBlocker("ASSET", assetKey, "video transcode is not ready"));
            }
        }
        return new ValidationResult(blockers.isEmpty(), List.copyOf(blockers), assetRefs);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReleaseView publish(TenantContext tenant, ReleaseCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String actor = MarketingContentSupport.operator(tenant, command.createdBy());
        ValidationResult validation = validate(tenant, new ValidationCommand(command.sourceType(), command.sourceKey()));
        if (!validation.ready()) {
            throw new IllegalStateException("content release gate failed: " + validation.blockers());
        }
        SourceSnapshot source = loadSource(tenantId, command.sourceType(), command.sourceKey());
        String releaseKey = releaseKey(source.sourceType(), source.sourceKey());
        int version = nextVersion(tenantId, releaseKey);
        MarketingContentReleaseDO previousActive = latestActiveRelease(tenantId, releaseKey);
        String snapshotJson = snapshotJson(source);
        String assetRefsJson = writeJson(validation.assetRefs(), "assetRefs");
        String checksum = sha256(snapshotJson + assetRefsJson);

        MarketingContentReleaseDO release = new MarketingContentReleaseDO();
        release.setTenantId(tenantId);
        release.setReleaseKey(releaseKey);
        release.setSourceType(source.sourceType());
        release.setSourceKey(source.sourceKey());
        release.setSourceVersion(version);
        release.setChannel(source.channel());
        release.setStatus("ACTIVE");
        release.setSnapshotJson(snapshotJson);
        release.setAssetRefsJson(assetRefsJson);
        release.setChecksumSha256(checksum);
        release.setCreatedBy(actor);
        release.setPublishedAt(LocalDateTime.now());
        releaseMapper.insert(release);

        supersedePreviousActiveRelease(tenantId, previousActive, actor, command.note());
        writeSourceItem(tenantId, release, source, snapshotJson);
        for (String assetKey : validation.assetRefs()) {
            MarketingAssetDO asset = findAsset(tenantId, assetKey);
            writeAssetItem(tenantId, release, asset);
            incrementReferenceCount(asset);
        }
        writeAudit(tenantId, "RELEASE_PUBLISHED", "RELEASE", releaseKey, actor, null,
                toMap(release), command.note());
        return toView(release);
    }

    public List<ReleaseView> list(TenantContext tenant, String sourceType, String sourceKey, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentReleaseDO> query = new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .orderByDesc(MarketingContentReleaseDO::getPublishedAt)
                .orderByDesc(MarketingContentReleaseDO::getId);
        if (MarketingContentSupport.hasText(sourceType)) {
            query.eq(MarketingContentReleaseDO::getSourceType, normalizeSourceType(sourceType));
        }
        if (MarketingContentSupport.hasText(sourceKey)) {
            query.eq(MarketingContentReleaseDO::getSourceKey,
                    MarketingContentSupport.normalizeKey(sourceKey, "sourceKey"));
        }
        if (MarketingContentSupport.hasText(status)) {
            query.eq(MarketingContentReleaseDO::getStatus, status.trim().toUpperCase());
        }
        return releaseMapper.selectList(query).stream().map(this::toView).toList();
    }

    public ResolvedRelease resolve(TenantContext tenant, String releaseKey, Map<String, Object> context) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        MarketingContentReleaseDO release = requireActiveRelease(tenantId, releaseKey);
        String renderedSubject = null;
        String renderedBody = null;
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        if ("TEMPLATE".equals(release.getSourceType())) {
            JsonNode node = readJson(release.getSnapshotJson(), "snapshotJson");
            renderedSubject = MarketingContentSupport.render(text(node, "subject"), context, missing);
            renderedBody = MarketingContentSupport.render(text(node, "body"), context, missing);
        }
        return new ResolvedRelease(
                release.getReleaseKey(),
                release.getSourceType(),
                release.getSourceKey(),
                release.getSourceVersion(),
                release.getStatus(),
                renderedSubject,
                renderedBody,
                List.copyOf(missing),
                release.getSnapshotJson(),
                releaseItems(tenantId, release.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ReleaseView rollback(TenantContext tenant, String releaseKey, RollbackCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String actor = MarketingContentSupport.operator(tenant, command == null ? null : command.actor());
        MarketingContentReleaseDO release = requireActiveRelease(tenantId, releaseKey);
        Map<String, Object> oldActiveValue = toMap(release);
        MarketingContentReleaseDO restoreTarget = latestRestorableRelease(tenantId, release);
        Map<String, Object> oldRestoreValue = restoreTarget == null ? null : toMap(restoreTarget);
        release.setStatus("ROLLED_BACK");
        release.setRollbackReason(MarketingContentSupport.trimToNull(command == null ? null : command.reason()));
        releaseMapper.updateById(release);
        writeAudit(tenantId, "RELEASE_ROLLED_BACK", "RELEASE", release.getReleaseKey(), actor,
                oldActiveValue,
                toMap(release),
                release.getRollbackReason());
        if (restoreTarget == null) {
            return toView(release);
        }
        restoreTarget.setStatus("ACTIVE");
        restoreTarget.setRollbackReason(null);
        releaseMapper.updateById(restoreTarget);
        writeAudit(tenantId, "RELEASE_RESTORED", "RELEASE", restoreTarget.getReleaseKey(), actor,
                oldRestoreValue,
                toMap(restoreTarget),
                release.getRollbackReason());
        return toView(restoreTarget);
    }

    public List<AuditEventView> auditEvents(TenantContext tenant, String targetType, String targetKey, Integer limit) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentAuditEventDO> query = new LambdaQueryWrapper<MarketingContentAuditEventDO>()
                .eq(MarketingContentAuditEventDO::getTenantId, tenantId)
                .orderByDesc(MarketingContentAuditEventDO::getCreatedAt)
                .orderByDesc(MarketingContentAuditEventDO::getId)
                .last("LIMIT " + Math.min(Math.max(limit == null ? 50 : limit, 1), 200));
        if (MarketingContentSupport.hasText(targetType)) {
            query.eq(MarketingContentAuditEventDO::getTargetType, targetType.trim().toUpperCase());
        }
        if (MarketingContentSupport.hasText(targetKey)) {
            query.eq(MarketingContentAuditEventDO::getTargetKey, MarketingContentSupport.normalizeKey(targetKey, "targetKey"));
        }
        return auditMapper.selectList(query).stream()
                .map(row -> new AuditEventView(
                        row.getEventType(),
                        row.getTargetType(),
                        row.getTargetKey(),
                        row.getActor(),
                        row.getNote(),
                        row.getCreatedAt()))
                .toList();
    }

    private SourceSnapshot loadSource(Long tenantId, String sourceTypeValue, String sourceKeyValue) {
        String sourceType = normalizeSourceType(sourceTypeValue);
        String sourceKey = MarketingContentSupport.normalizeKey(sourceKeyValue, "sourceKey");
        if ("TEMPLATE".equals(sourceType)) {
            MarketingContentTemplateDO row = templateMapper.selectOne(new LambdaQueryWrapper<MarketingContentTemplateDO>()
                    .eq(MarketingContentTemplateDO::getTenantId, tenantId)
                    .eq(MarketingContentTemplateDO::getTemplateKey, sourceKey)
                    .last("LIMIT 1"));
            if (row == null) {
                return SourceSnapshot.empty(sourceType, sourceKey);
            }
            return new SourceSnapshot(sourceType, sourceKey, row.getStatus(), row.getChannel(), row.getAssetRefsJson(),
                    null, row);
        }
        MarketingContentEntryDO row = entryMapper.selectOne(new LambdaQueryWrapper<MarketingContentEntryDO>()
                .eq(MarketingContentEntryDO::getTenantId, tenantId)
                .eq(MarketingContentEntryDO::getEntryKey, sourceKey)
                .last("LIMIT 1"));
        if (row == null) {
            return SourceSnapshot.empty(sourceType, sourceKey);
        }
        return new SourceSnapshot(sourceType, sourceKey, row.getStatus(), "WEB", row.getAssetRefsJson(),
                null, row);
    }

    private MarketingAssetDO findAsset(Long tenantId, String assetKey) {
        return assetMapper.selectOne(new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, tenantId)
                .eq(MarketingAssetDO::getAssetKey, assetKey)
                .last("LIMIT 1"));
    }

    private MarketingContentReleaseDO requireActiveRelease(Long tenantId, String releaseKey) {
        MarketingContentReleaseDO release = releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, MarketingContentSupport.normalizeKey(releaseKey, "releaseKey"))
                .eq(MarketingContentReleaseDO::getStatus, "ACTIVE")
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
        if (release == null) {
            throw new IllegalArgumentException("active release not found: " + releaseKey);
        }
        return release;
    }

    private int nextVersion(Long tenantId, String releaseKey) {
        MarketingContentReleaseDO latest = releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, releaseKey)
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getSourceVersion() == null ? 1 : latest.getSourceVersion() + 1;
    }

    private MarketingContentReleaseDO latestActiveRelease(Long tenantId, String releaseKey) {
        return releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, releaseKey)
                .eq(MarketingContentReleaseDO::getStatus, "ACTIVE")
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
    }

    private MarketingContentReleaseDO latestRestorableRelease(Long tenantId, MarketingContentReleaseDO release) {
        return releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, release.getReleaseKey())
                .eq(MarketingContentReleaseDO::getStatus, "SUPERSEDED")
                .lt(MarketingContentReleaseDO::getSourceVersion, release.getSourceVersion())
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
    }

    private void supersedePreviousActiveRelease(Long tenantId,
                                                MarketingContentReleaseDO previousActive,
                                                String actor,
                                                String note) {
        if (previousActive == null) {
            return;
        }
        Map<String, Object> oldValue = toMap(previousActive);
        previousActive.setStatus("SUPERSEDED");
        releaseMapper.updateById(previousActive);
        writeAudit(tenantId, "RELEASE_SUPERSEDED", "RELEASE", previousActive.getReleaseKey(), actor,
                oldValue,
                toMap(previousActive),
                note);
    }

    private List<String> assetRefs(String assetRefsJson) {
        JsonNode node = readJson(MarketingContentSupport.hasText(assetRefsJson) ? assetRefsJson : "[]", "assetRefsJson");
        if (!node.isArray()) {
            throw new IllegalArgumentException("assetRefsJson must be a JSON array");
        }
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                refs.add(MarketingContentSupport.normalizeKey(item.asText(), "assetKey"));
            } else if (item.isObject() && item.hasNonNull("assetKey")) {
                refs.add(MarketingContentSupport.normalizeKey(item.get("assetKey").asText(), "assetKey"));
            } else {
                throw new IllegalArgumentException("assetRefsJson items must be strings or objects with assetKey");
            }
        }
        return List.copyOf(refs);
    }

    private void validateSourceShape(SourceSnapshot source, List<ReleaseBlocker> blockers) {
        if (source.row() instanceof MarketingContentTemplateDO row) {
            requireJsonObject(row.getDesignJson(), "designJson", source, blockers);
            requireJsonArray(row.getVariablesJson(), "variablesJson", source, blockers);
            return;
        }
        if (source.row() instanceof MarketingContentEntryDO row) {
            requireJsonObject(row.getBodyJson(), "bodyJson", source, blockers);
            requireJsonObject(row.getSeoJson(), "seoJson", source, blockers);
        }
    }

    private void requireJsonObject(String value,
                                   String field,
                                   SourceSnapshot source,
                                   List<ReleaseBlocker> blockers) {
        JsonNode node = readJsonForGate(value, field, source, blockers);
        if (node != null && !node.isObject()) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), field + " must be a JSON object"));
        }
    }

    private void requireJsonArray(String value,
                                  String field,
                                  SourceSnapshot source,
                                  List<ReleaseBlocker> blockers) {
        JsonNode node = readJsonForGate(value, field, source, blockers);
        if (node != null && !node.isArray()) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), field + " must be a JSON array"));
        }
    }

    private JsonNode readJsonForGate(String value,
                                     String field,
                                     SourceSnapshot source,
                                     List<ReleaseBlocker> blockers) {
        try {
            return objectMapper.readTree(MarketingContentSupport.hasText(value) ? value : "");
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), field + " must be valid JSON"));
            return null;
        }
    }

    private void writeSourceItem(Long tenantId,
                                 MarketingContentReleaseDO release,
                                 SourceSnapshot source,
                                 String snapshotJson) {
        MarketingContentReleaseItemDO item = new MarketingContentReleaseItemDO();
        item.setTenantId(tenantId);
        item.setReleaseId(release.getId());
        item.setItemType(source.sourceType());
        item.setItemKey(source.sourceKey());
        item.setItemStatus(source.status());
        item.setSnapshotJson(snapshotJson);
        itemMapper.insert(item);
    }

    private void writeAssetItem(Long tenantId, MarketingContentReleaseDO release, MarketingAssetDO asset) {
        MarketingContentReleaseItemDO item = new MarketingContentReleaseItemDO();
        item.setTenantId(tenantId);
        item.setReleaseId(release.getId());
        item.setItemType("ASSET");
        item.setItemKey(asset.getAssetKey());
        item.setItemStatus(asset.getStatus());
        item.setSnapshotJson(assetSnapshot(asset));
        itemMapper.insert(item);
    }

    private void incrementReferenceCount(MarketingAssetDO asset) {
        asset.setReferenceCount((asset.getReferenceCount() == null ? 0 : asset.getReferenceCount()) + 1);
        assetMapper.updateById(asset);
    }

    private List<ResolvedAsset> releaseItems(Long tenantId, Long releaseId) {
        if (releaseId == null) {
            return List.of();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<MarketingContentReleaseItemDO>()
                        .eq(MarketingContentReleaseItemDO::getTenantId, tenantId)
                        .eq(MarketingContentReleaseItemDO::getReleaseId, releaseId)
                        .eq(MarketingContentReleaseItemDO::getItemType, "ASSET"))
                .stream()
                .map(item -> new ResolvedAsset(item.getItemKey(), item.getItemStatus(), item.getSnapshotJson()))
                .toList();
    }

    private void writeAudit(Long tenantId,
                            String eventType,
                            String targetType,
                            String targetKey,
                            String actor,
                            Object oldValue,
                            Object newValue,
                            String note) {
        MarketingContentAuditEventDO row = new MarketingContentAuditEventDO();
        row.setTenantId(tenantId);
        row.setEventType(eventType);
        row.setTargetType(targetType);
        row.setTargetKey(targetKey);
        row.setActor(actor);
        row.setOldValueJson(oldValue == null ? null : writeJson(oldValue, "oldValue"));
        row.setNewValueJson(newValue == null ? null : writeJson(newValue, "newValue"));
        row.setNote(MarketingContentSupport.trimToNull(note));
        auditMapper.insert(row);
    }

    private String templateSnapshot(MarketingContentTemplateDO row) {
        return writeJson(Map.of(
                "templateKey", row.getTemplateKey(),
                "displayName", row.getDisplayName(),
                "channel", row.getChannel(),
                "subject", row.getSubject() == null ? "" : row.getSubject(),
                "body", row.getBody(),
                "designJson", readJson(row.getDesignJson(), "designJson"),
                "assetRefsJson", readJson(row.getAssetRefsJson(), "assetRefsJson"),
                "variablesJson", readJson(row.getVariablesJson(), "variablesJson"),
                "status", row.getStatus()), "template snapshot");
    }

    private String entrySnapshot(MarketingContentEntryDO row) {
        return writeJson(Map.of(
                "entryKey", row.getEntryKey(),
                "contentType", row.getContentType(),
                "title", row.getTitle(),
                "slug", row.getSlug() == null ? "" : row.getSlug(),
                "locale", row.getLocale() == null ? "" : row.getLocale(),
                "summary", row.getSummary() == null ? "" : row.getSummary(),
                "bodyJson", readJson(row.getBodyJson(), "bodyJson"),
                "seoJson", readJson(row.getSeoJson(), "seoJson"),
                "assetRefsJson", readJson(row.getAssetRefsJson(), "assetRefsJson"),
                "status", row.getStatus()), "entry snapshot");
    }

    private String snapshotJson(SourceSnapshot source) {
        if (source.row() instanceof MarketingContentTemplateDO row) {
            return templateSnapshot(row);
        }
        if (source.row() instanceof MarketingContentEntryDO row) {
            return entrySnapshot(row);
        }
        return "{}";
    }

    private String assetSnapshot(MarketingAssetDO row) {
        return writeJson(Map.of(
                "assetKey", row.getAssetKey(),
                "name", row.getName(),
                "assetType", row.getAssetType(),
                "mimeType", row.getMimeType(),
                "storageUrl", row.getStorageUrl(),
                "status", row.getStatus(),
                "transcodeStatus", row.getTranscodeStatus() == null ? "" : row.getTranscodeStatus(),
                "posterUrl", row.getPosterUrl() == null ? "" : row.getPosterUrl()), "asset snapshot");
    }

    private Map<String, Object> toMap(MarketingContentReleaseDO row) {
        return Map.of(
                "releaseKey", row.getReleaseKey(),
                "sourceType", row.getSourceType(),
                "sourceKey", row.getSourceKey(),
                "sourceVersion", row.getSourceVersion(),
                "status", row.getStatus(),
                "checksumSha256", row.getChecksumSha256());
    }

    private ReleaseView toView(MarketingContentReleaseDO row) {
        return new ReleaseView(
                row.getReleaseKey(),
                row.getSourceType(),
                row.getSourceKey(),
                row.getSourceVersion(),
                row.getChannel(),
                row.getStatus(),
                row.getChecksumSha256());
    }

    private String releaseKey(String sourceType, String sourceKey) {
        return MarketingContentSupport.normalizeKey(sourceType.toLowerCase() + "-" + sourceKey, "releaseKey");
    }

    private String normalizeSourceType(String sourceType) {
        return MarketingContentSupport.normalizeUpper(sourceType, "TEMPLATE", SOURCE_TYPES, "content source type");
    }

    private JsonNode readJson(String value, String field) {
        try {
            return objectMapper.readTree(MarketingContentSupport.hasText(value) ? value : "{}");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    private String writeJson(Object value, String field) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record ValidationCommand(String sourceType, String sourceKey) {
    }

    public record ReleaseCommand(String sourceType, String sourceKey, String createdBy, String note) {
    }

    public record RollbackCommand(String actor, String reason) {
    }

    public record ReleaseBlocker(String scope, String key, String reason) {
    }

    public record ValidationResult(boolean ready, List<ReleaseBlocker> blockers, List<String> assetRefs) {
    }

    public record ReleaseView(
            String releaseKey,
            String sourceType,
            String sourceKey,
            Integer sourceVersion,
            String channel,
            String status,
            String checksumSha256) {
    }

    public record ResolvedAsset(String assetKey, String status, String snapshotJson) {
    }

    public record ResolvedRelease(
            String releaseKey,
            String sourceType,
            String sourceKey,
            Integer sourceVersion,
            String status,
            String renderedSubject,
            String renderedBody,
            List<String> missingVariables,
            String snapshotJson,
            List<ResolvedAsset> assets) {
    }

    public record AuditEventView(
            String eventType,
            String targetType,
            String targetKey,
            String actor,
            String note,
            LocalDateTime createdAt) {
    }

    private record SourceSnapshot(
            String sourceType,
            String sourceKey,
            String status,
            String channel,
            String assetRefsJson,
            String snapshotJson,
            Object row) {

        static SourceSnapshot empty(String sourceType, String sourceKey) {
            return new SourceSnapshot(sourceType, sourceKey, null, "WEB", "[]", "{}", null);
        }
    }
}
