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
/**
 * MarketingAssetService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 MarketingAssetService 实例。
     *
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param folderMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingAssetService(MarketingAssetMapper mapper,
                                 MarketingAssetFolderMapper folderMapper,
                                 ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.folderMapper = folderMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenant tenant 参数，用于 listFolders 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenant tenant 参数，用于 createFolder 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public FolderView createFolder(TenantContext tenant, FolderCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String folderKey = MarketingContentSupport.normalizeKey(command.folderKey(), "folderKey");

        MarketingAssetFolderDO row = findFolder(tenantId, folderKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean insert = row == null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toFolderView(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenant tenant 参数，用于 list 流程中的校验、计算或对象转换。
     * @param keyword keyword 参数，用于 list 流程中的校验、计算或对象转换。
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<AssetView> list(TenantContext tenant, String keyword, String assetType, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingAssetDO> query = new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, tenantId)
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .orderByDesc(MarketingAssetDO::getUpdatedAt)
                .orderByDesc(MarketingAssetDO::getId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return mapper.selectList(query).stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenant tenant 参数，用于 create 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean insert = row == null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 setStatus 流程中的校验、计算或对象转换。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 setStatus 流程生成的业务结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 require 流程生成的业务结果。
     */
    private MarketingAssetDO require(Long tenantId, String assetKey) {
        MarketingAssetDO row = find(tenantId, MarketingContentSupport.normalizeKey(assetKey, "assetKey"));
        if (row == null) {
            throw new IllegalArgumentException("asset not found: " + assetKey);
        }
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingAssetDO find(Long tenantId, String assetKey) {
        return mapper.selectOne(new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, tenantId)
                .eq(MarketingAssetDO::getAssetKey, assetKey)
                .last("LIMIT 1"));
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeTranscode(String value) {
        if (!MarketingContentSupport.hasText(value)) {
            return null;
        }
        return MarketingContentSupport.normalizeUpper(value, "PENDING", TRANSCODE_STATUSES, "transcode status");
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param mimeType 类型标识，用于选择对应处理分支。
     */
    private void validateMimeType(String assetType, String mimeType) {
        String normalized = MarketingContentSupport.requireText(mimeType, "asset mimeType").toLowerCase(Locale.ROOT);
        if (!MIME_TYPES.getOrDefault(assetType, Set.of()).contains(normalized)) {
            throw new IllegalArgumentException("unsupported asset mimeType " + mimeType + " for " + assetType);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param sizeBytes size bytes 参数，用于 validateSize 流程中的校验、计算或对象转换。
     */
    private void validateSize(String assetType, Long sizeBytes) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sizeBytes == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    private void validateReadyGate(String assetType, String status, AssetCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"READY".equals(status)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     */
    private void assertReadyAllowed(MarketingAssetDO row) {
        // 准备本次处理所需的上下文和中间变量。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 metadata 流程生成的业务结果。
     */
    private Map<String, Object> metadata(MarketingAssetDO row) {
        try {
            String json = MarketingContentSupport.hasText(row.getMetadataJson()) ? row.getMetadataJson() : "{}";
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("asset metadata must be valid JSON", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 uppercaseMetadata 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 uppercaseMetadata 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 uppercase metadata 生成的文本或业务键。
     */
    private String uppercaseMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text.toUpperCase(Locale.ROOT);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param folderKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingAssetFolderDO findFolder(Long tenantId, String folderKey) {
        return folderMapper.selectOne(new LambdaQueryWrapper<MarketingAssetFolderDO>()
                .eq(MarketingAssetFolderDO::getTenantId, tenantId)
                .eq(MarketingAssetFolderDO::getFolderKey, folderKey)
                .last("LIMIT 1"));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param folderId 业务对象 ID，用于定位具体记录。
     * @param label label 参数，用于 requireFolderInTenant 流程中的校验、计算或对象转换。
     * @return 返回 require folder in tenant 计算得到的数量、金额或指标值。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private FolderView toFolderView(MarketingAssetFolderDO row) {
        return new FolderView(row.getId(), row.getFolderKey(), row.getName(), row.getParentId());
    }

    /**
     * FolderCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record FolderCommand(
            String folderKey,
            String name,
            Long parentId,
            String createdBy) {
    }

    /**
     * FolderView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record FolderView(
            Long id,
            String folderKey,
            String name,
            Long parentId) {
    }

    /**
     * AssetCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * AssetStatusCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AssetStatusCommand(String status, String reviewNotes) {
    }

    /**
     * AssetView 承载对应领域的业务规则、流程编排和结果转换。
     */
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
