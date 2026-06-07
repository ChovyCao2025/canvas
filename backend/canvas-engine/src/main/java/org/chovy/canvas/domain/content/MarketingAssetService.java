package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingAssetDO;
import org.chovy.canvas.dal.dataobject.MarketingAssetFolderDO;
import org.chovy.canvas.dal.mapper.MarketingAssetFolderMapper;
import org.chovy.canvas.dal.mapper.MarketingAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MarketingAssetService {

    private static final Set<String> ASSET_TYPES = Set.of("IMAGE", "FILE", "VIDEO", "AUDIO");
    private static final Set<String> ASSET_STATUSES = Set.of("DRAFT", "READY", "REJECTED", "ARCHIVED");
    private static final Set<String> TRANSCODE_STATUSES = Set.of("PENDING", "READY", "FAILED", "EXTERNAL");
    private static final Set<String> TRUSTED_MEDIA_PROVIDERS = Set.of("CLOUDINARY", "MUX");
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Map<String, Long> MAX_SIZE_BYTES = Map.of(
            "IMAGE", 20L * 1024 * 1024,
            "FILE", 50L * 1024 * 1024,
            "AUDIO", 100L * 1024 * 1024,
            "VIDEO", 2L * 1024 * 1024 * 1024);
    private static final Map<String, Set<String>> MIME_TYPES = Map.of(
            "IMAGE", Set.of("image/jpeg", "image/png", "image/webp", "image/gif"),
            "FILE", Set.of("application/pdf", "text/plain", "text/csv", "application/json"),
            "AUDIO", Set.of("audio/mpeg", "audio/mp4", "audio/wav", "audio/webm"),
            "VIDEO", Set.of("video/mp4", "video/quicktime", "video/webm"));

    private final MarketingAssetMapper mapper;
    private final MarketingAssetFolderMapper folderMapper;
    private final ObjectMapper objectMapper;

    public MarketingAssetService(MarketingAssetMapper mapper,
                                 MarketingAssetFolderMapper folderMapper,
                                 ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.folderMapper = folderMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public List<FolderView> listFolders(TenantContext tenant) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        return folderMapper.selectList(new LambdaQueryWrapper<MarketingAssetFolderDO>()
                        .eq(MarketingAssetFolderDO::getTenantId, tenantId)
                        .orderByAsc(MarketingAssetFolderDO::getParentId)
                        .orderByAsc(MarketingAssetFolderDO::getName)
                        .orderByAsc(MarketingAssetFolderDO::getId))
                .stream()
                .map(this::toFolderView)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public FolderView createFolder(TenantContext tenant, FolderCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String folderKey = MarketingContentSupport.normalizeKey(command.folderKey(), "folderKey");

        MarketingAssetFolderDO row = findFolder(tenantId, folderKey);
        boolean insert = row == null;
        if (insert) {
            row = new MarketingAssetFolderDO();
            row.setTenantId(tenantId);
            row.setFolderKey(folderKey);
            row.setCreatedBy(MarketingContentSupport.operator(tenant, command.createdBy()));
        }
        row.setName(MarketingContentSupport.requireText(command.name(), "folder name"));
        row.setParentId(requireFolderInTenant(tenantId, command.parentId(), "parent folder"));
        if (insert) {
            folderMapper.insert(row);
        } else {
            folderMapper.updateById(row);
        }
        return toFolderView(row);
    }

    public List<AssetView> list(TenantContext tenant, String keyword, String assetType, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingAssetDO> query = new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, tenantId)
                .orderByDesc(MarketingAssetDO::getUpdatedAt)
                .orderByDesc(MarketingAssetDO::getId);
        if (MarketingContentSupport.hasText(keyword)) {
            String pattern = "%" + keyword.trim() + "%";
            query.and(w -> w.like(MarketingAssetDO::getAssetKey, pattern)
                    .or()
                    .like(MarketingAssetDO::getName, pattern));
        }
        if (MarketingContentSupport.hasText(assetType)) {
            query.eq(MarketingAssetDO::getAssetType,
                    MarketingContentSupport.normalizeUpper(assetType, "IMAGE", ASSET_TYPES, "asset type"));
        }
        if (MarketingContentSupport.hasText(status)) {
            query.eq(MarketingAssetDO::getStatus,
                    MarketingContentSupport.normalizeUpper(status, "DRAFT", ASSET_STATUSES, "asset status"));
        }
        return mapper.selectList(query).stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AssetView create(TenantContext tenant, AssetCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String assetKey = MarketingContentSupport.normalizeKey(command.assetKey(), "assetKey");
        String assetType = MarketingContentSupport.normalizeUpper(command.assetType(), "IMAGE", ASSET_TYPES, "asset type");
        String status = MarketingContentSupport.normalizeUpper(command.status(), "DRAFT", ASSET_STATUSES, "asset status");
        String storageUrl = MarketingContentSupport.requireText(command.storageUrl(), "asset storageUrl");
        MarketingContentSupport.validateHttpUrl(storageUrl, "asset storageUrl");
        validateMimeType(assetType, command.mimeType());
        validateSize(assetType, command.sizeBytes());
        validateReadyGate(assetType, status, command);

        MarketingAssetDO row = find(tenantId, assetKey);
        boolean insert = row == null;
        if (insert) {
            row = new MarketingAssetDO();
            row.setTenantId(tenantId);
            row.setAssetKey(assetKey);
            row.setReferenceCount(0);
            row.setCreatedBy(MarketingContentSupport.operator(tenant, command.createdBy()));
        }
        row.setName(MarketingContentSupport.requireText(command.name(), "asset name"));
        row.setAssetType(assetType);
        row.setMimeType(MarketingContentSupport.requireText(command.mimeType(), "asset mimeType"));
        row.setStorageUrl(storageUrl);
        row.setFolderId(requireFolderInTenant(tenantId, command.folderId(), "asset folder"));
        row.setSizeBytes(command.sizeBytes());
        row.setChecksumSha256(MarketingContentSupport.trimToNull(command.checksumSha256()));
        row.setThumbnailUrl(MarketingContentSupport.trimToNull(command.thumbnailUrl()));
        row.setPosterUrl(MarketingContentSupport.trimToNull(command.posterUrl()));
        row.setWidth(command.width());
        row.setHeight(command.height());
        row.setDurationMs(command.durationMs());
        row.setTranscodeStatus(normalizeTranscode(command.transcodeStatus()));
        row.setTagsJson(MarketingContentSupport.tagsJson(objectMapper, command.tags()));
        row.setMetadataJson(MarketingContentSupport.objectJson(objectMapper, command.metadata(), "metadata"));
        row.setStatus(status);
        row.setReviewNotes(MarketingContentSupport.trimToNull(command.reviewNotes()));
        if (insert) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssetView setStatus(TenantContext tenant, String assetKey, AssetStatusCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String status = MarketingContentSupport.normalizeUpper(command.status(), "DRAFT", ASSET_STATUSES, "asset status");
        MarketingAssetDO row = require(tenantId, assetKey);
        if ("READY".equals(status)) {
            assertReadyAllowed(row);
        }
        row.setStatus(status);
        row.setReviewNotes(MarketingContentSupport.trimToNull(command.reviewNotes()));
        mapper.updateById(row);
        return toView(row);
    }

    private MarketingAssetDO require(Long tenantId, String assetKey) {
        MarketingAssetDO row = find(tenantId, MarketingContentSupport.normalizeKey(assetKey, "assetKey"));
        if (row == null) {
            throw new IllegalArgumentException("asset not found: " + assetKey);
        }
        return row;
    }

    private MarketingAssetDO find(Long tenantId, String assetKey) {
        return mapper.selectOne(new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, tenantId)
                .eq(MarketingAssetDO::getAssetKey, assetKey)
                .last("LIMIT 1"));
    }

    private String normalizeTranscode(String value) {
        if (!MarketingContentSupport.hasText(value)) {
            return null;
        }
        return MarketingContentSupport.normalizeUpper(value, "PENDING", TRANSCODE_STATUSES, "transcode status");
    }

    private void validateMimeType(String assetType, String mimeType) {
        String normalized = MarketingContentSupport.requireText(mimeType, "asset mimeType").toLowerCase(Locale.ROOT);
        if (!MIME_TYPES.getOrDefault(assetType, Set.of()).contains(normalized)) {
            throw new IllegalArgumentException("unsupported asset mimeType " + mimeType + " for " + assetType);
        }
    }

    private void validateSize(String assetType, Long sizeBytes) {
        if (sizeBytes == null) {
            return;
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("asset sizeBytes must be positive");
        }
        Long maxSize = MAX_SIZE_BYTES.get(assetType);
        if (maxSize != null && sizeBytes > maxSize) {
            throw new IllegalArgumentException("asset sizeBytes exceeds " + assetType + " max size");
        }
    }

    private void validateReadyGate(String assetType, String status, AssetCommand command) {
        if (!"READY".equals(status)) {
            return;
        }
        if (command.sizeBytes() == null || command.sizeBytes() <= 0) {
            throw new IllegalArgumentException("READY asset requires positive sizeBytes");
        }
        Map<String, Object> metadata = command.metadata() == null ? Map.of() : command.metadata();
        String scanStatus = uppercaseMetadata(metadata, "scanStatus");
        if (!"PASSED".equals(scanStatus) && !"PROVIDER_VERIFIED".equals(scanStatus)) {
            throw new IllegalArgumentException("READY asset requires PASSED or PROVIDER_VERIFIED scanStatus");
        }
        if ("PROVIDER_VERIFIED".equals(scanStatus)) {
            String provider = uppercaseMetadata(metadata, "provider");
            if (!TRUSTED_MEDIA_PROVIDERS.contains(provider)) {
                throw new IllegalArgumentException("PROVIDER_VERIFIED scanStatus requires a trusted media provider");
            }
        }
        String checksum = MarketingContentSupport.trimToNull(command.checksumSha256());
        if (checksum != null && !SHA256_PATTERN.matcher(checksum).matches()) {
            throw new IllegalArgumentException("asset checksumSha256 must be a 64 character hex SHA-256");
        }
        if ("VIDEO".equals(assetType)) {
            String transcode = normalizeTranscode(command.transcodeStatus());
            if (!"READY".equals(transcode) && !"EXTERNAL".equals(transcode)) {
                throw new IllegalArgumentException("READY video asset requires READY or EXTERNAL transcodeStatus");
            }
            if (command.durationMs() == null || command.durationMs() <= 0) {
                throw new IllegalArgumentException("READY video asset requires positive durationMs");
            }
        }
    }

    private void assertReadyAllowed(MarketingAssetDO row) {
        validateMimeType(row.getAssetType(), row.getMimeType());
        validateSize(row.getAssetType(), row.getSizeBytes());
        validateReadyGate(row.getAssetType(), "READY", new AssetCommand(
                row.getAssetKey(),
                row.getName(),
                row.getAssetType(),
                row.getMimeType(),
                row.getStorageUrl(),
                row.getFolderId(),
                row.getSizeBytes(),
                row.getChecksumSha256(),
                row.getThumbnailUrl(),
                row.getPosterUrl(),
                row.getWidth(),
                row.getHeight(),
                row.getDurationMs(),
                row.getTranscodeStatus(),
                MarketingContentSupport.tags(objectMapper, row.getTagsJson()),
                metadata(row),
                "READY",
                row.getReviewNotes(),
                row.getCreatedBy()));
    }

    private Map<String, Object> metadata(MarketingAssetDO row) {
        try {
            String json = MarketingContentSupport.hasText(row.getMetadataJson()) ? row.getMetadataJson() : "{}";
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("asset metadata must be valid JSON", e);
        }
    }

    private String uppercaseMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text.toUpperCase(Locale.ROOT);
    }

    private AssetView toView(MarketingAssetDO row) {
        return new AssetView(
                row.getAssetKey(),
                row.getName(),
                row.getAssetType(),
                row.getMimeType(),
                row.getStorageUrl(),
                row.getFolderId(),
                row.getStatus(),
                MarketingContentSupport.tags(objectMapper, row.getTagsJson()),
                row.getDurationMs(),
                row.getTranscodeStatus(),
                row.getPosterUrl());
    }

    private MarketingAssetFolderDO findFolder(Long tenantId, String folderKey) {
        return folderMapper.selectOne(new LambdaQueryWrapper<MarketingAssetFolderDO>()
                .eq(MarketingAssetFolderDO::getTenantId, tenantId)
                .eq(MarketingAssetFolderDO::getFolderKey, folderKey)
                .last("LIMIT 1"));
    }

    private Long requireFolderInTenant(Long tenantId, Long folderId, String label) {
        if (folderId == null) {
            return null;
        }
        MarketingAssetFolderDO folder = folderMapper.selectById(folderId);
        if (folder == null || !tenantId.equals(folder.getTenantId())) {
            throw new IllegalArgumentException(label + " not found: " + folderId);
        }
        return folderId;
    }

    private FolderView toFolderView(MarketingAssetFolderDO row) {
        return new FolderView(row.getId(), row.getFolderKey(), row.getName(), row.getParentId());
    }

    public record FolderCommand(
            String folderKey,
            String name,
            Long parentId,
            String createdBy) {
    }

    public record FolderView(
            Long id,
            String folderKey,
            String name,
            Long parentId) {
    }

    public record AssetCommand(
            String assetKey,
            String name,
            String assetType,
            String mimeType,
            String storageUrl,
            Long folderId,
            Long sizeBytes,
            String checksumSha256,
            String thumbnailUrl,
            String posterUrl,
            Integer width,
            Integer height,
            Long durationMs,
            String transcodeStatus,
            List<String> tags,
            Map<String, Object> metadata,
            String status,
            String reviewNotes,
            String createdBy) {
    }

    public record AssetStatusCommand(String status, String reviewNotes) {
    }

    public record AssetView(
            String assetKey,
            String name,
            String assetType,
            String mimeType,
            String storageUrl,
            Long folderId,
            String status,
            List<String> tags,
            Long durationMs,
            String transcodeStatus,
            String posterUrl) {
    }
}
