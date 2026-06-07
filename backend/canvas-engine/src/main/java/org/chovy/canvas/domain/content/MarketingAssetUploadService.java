package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingAssetDO;
import org.chovy.canvas.dal.dataobject.MarketingAssetUploadIntentDO;
import org.chovy.canvas.dal.dataobject.MarketingContentAuditEventDO;
import org.chovy.canvas.dal.mapper.MarketingAssetMapper;
import org.chovy.canvas.dal.mapper.MarketingAssetUploadIntentMapper;
import org.chovy.canvas.dal.mapper.MarketingContentAuditEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MarketingAssetUploadService {

    private static final Set<String> PROVIDERS = Set.of("CLOUDINARY", "MUX", "S3", "EXTERNAL");
    private static final Set<String> ASSET_TYPES = Set.of("IMAGE", "FILE", "VIDEO", "AUDIO");
    private static final Set<String> CALLBACK_STATUSES = Set.of("PENDING", "READY", "FAILED");
    private static final Set<String> TRANSCODE_STATUSES = Set.of("PENDING", "READY", "FAILED", "EXTERNAL");
    private static final Set<String> SCAN_STATUSES = Set.of("PENDING", "PASSED", "FAILED", "PROVIDER_VERIFIED", "NOT_REQUIRED");
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final long DIRECT_UPLOAD_TTL_SECONDS = 3600;
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
    private static final Map<String, Set<String>> EXTENSIONS = Map.of(
            "IMAGE", Set.of("jpg", "jpeg", "png", "webp", "gif"),
            "FILE", Set.of("pdf", "txt", "csv", "json"),
            "AUDIO", Set.of("mp3", "m4a", "wav", "webm"),
            "VIDEO", Set.of("mp4", "mov", "webm"));
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MarketingAssetUploadIntentMapper intentMapper;
    private final MarketingAssetMapper assetMapper;
    private final MarketingContentAuditEventMapper auditMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final MarketingAssetUploadHandoffService handoffService;

    @Autowired
    public MarketingAssetUploadService(MarketingAssetUploadIntentMapper intentMapper,
                                       MarketingAssetMapper assetMapper,
                                       MarketingContentAuditEventMapper auditMapper,
                                       ObjectMapper objectMapper,
                                       MarketingAssetUploadHandoffService handoffService) {
        this.intentMapper = intentMapper;
        this.assetMapper = assetMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = Clock.systemDefaultZone();
        this.handoffService = handoffService == null ? MarketingAssetUploadHandoffService.contractOnly() : handoffService;
    }

    MarketingAssetUploadService(MarketingAssetUploadIntentMapper intentMapper,
                                MarketingAssetMapper assetMapper,
                                MarketingContentAuditEventMapper auditMapper,
                                ObjectMapper objectMapper) {
        this(intentMapper, assetMapper, auditMapper, objectMapper, Clock.systemDefaultZone(),
                MarketingAssetUploadHandoffService.contractOnly());
    }

    MarketingAssetUploadService(MarketingAssetUploadIntentMapper intentMapper,
                                MarketingAssetMapper assetMapper,
                                MarketingContentAuditEventMapper auditMapper,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this(intentMapper, assetMapper, auditMapper, objectMapper, clock,
                MarketingAssetUploadHandoffService.contractOnly());
    }

    MarketingAssetUploadService(MarketingAssetUploadIntentMapper intentMapper,
                                MarketingAssetMapper assetMapper,
                                MarketingContentAuditEventMapper auditMapper,
                                ObjectMapper objectMapper,
                                Clock clock,
                                MarketingAssetUploadHandoffService handoffService) {
        this.intentMapper = intentMapper;
        this.assetMapper = assetMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.handoffService = handoffService == null ? MarketingAssetUploadHandoffService.contractOnly() : handoffService;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadIntentView createIntent(TenantContext tenant, UploadIntentCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String assetKey = MarketingContentSupport.normalizeKey(command.assetKey(), "assetKey");
        String provider = MarketingContentSupport.normalizeUpper(command.provider(), "S3", PROVIDERS, "asset upload provider");
        String assetType = MarketingContentSupport.normalizeUpper(command.assetType(), "FILE", ASSET_TYPES, "asset type");
        String mimeType = MarketingContentSupport.requireText(command.mimeType(), "asset mimeType");
        validateMimeType(assetType, mimeType);
        validateFileName(command.fileName(), assetType);
        long sizeBytes = requireSize(command.sizeBytes(), assetType, "asset sizeBytes");
        String token = UUID.randomUUID().toString().replace("-", "");
        String intentKey = MarketingContentSupport.normalizeKey(provider.toLowerCase() + "-"
                + assetKey.substring(0, Math.min(assetKey.length(), 80)) + "-"
                + token.substring(0, 12), "intentKey");
        String objectKey = storageObjectKey(tenantId, assetKey, intentKey, command.fileName());
        Map<String, String> requiredHeaders = requiredUploadHeaders(provider, mimeType);
        MarketingAssetUploadHandoff handoff = handoffService.create(new MarketingAssetUploadHandoffRequest(
                tenantId,
                assetKey,
                assetType,
                provider,
                mimeType,
                command.fileName(),
                sizeBytes,
                intentKey,
                token,
                objectKey,
                Duration.ofSeconds(DIRECT_UPLOAD_TTL_SECONDS),
                requiredHeaders));

        Map<String, Object> uploadParams = new LinkedHashMap<>();
        uploadParams.putAll(handoff.uploadParams());
        uploadParams.put("objectKey", handoff.uploadParams().getOrDefault("objectKey", objectKey));
        uploadParams.put("expiresInSeconds", DIRECT_UPLOAD_TTL_SECONDS);
        uploadParams.put("maxSizeBytes", maxSize(assetType));
        uploadParams.put("allowedMimeTypes", List.copyOf(MIME_TYPES.get(assetType)));
        uploadParams.put("requiredHeaders", handoff.requiredHeaders());
        if (MarketingContentSupport.hasText(handoff.storageUrl())) {
            uploadParams.put("storageUrl", handoff.storageUrl());
        }
        uploadParams.put("callbackRequirement", Map.of(
                "readyRequiresStorageUrl", true,
                "readyRequiresChecksum", requiresChecksum(provider),
                "readyRequiresScanStatus", true,
                "readyVideoRequiresTranscodeReady", "VIDEO".equals(assetType)));

        MarketingAssetUploadIntentDO row = new MarketingAssetUploadIntentDO();
        row.setTenantId(tenantId);
        row.setIntentKey(intentKey);
        row.setAssetKey(assetKey);
        row.setAssetType(assetType);
        row.setProvider(provider);
        row.setMimeType(mimeType);
        row.setFileName(MarketingContentSupport.trimToNull(command.fileName()));
        row.setSizeBytes(sizeBytes);
        row.setUploadToken(token);
        row.setUploadUrl(handoff.uploadUrl());
        row.setUploadParamsJson(writeJson(uploadParams, "uploadParams"));
        row.setStatus("PENDING");
        row.setExpiresAt(now().plusSeconds(DIRECT_UPLOAD_TTL_SECONDS));
        row.setCreatedBy(MarketingContentSupport.operator(tenant, command.createdBy()));
        intentMapper.insert(row);

        writeAudit(tenantId, "ASSET_UPLOAD_INTENT_CREATED", "ASSET", assetKey, row.getCreatedBy(), null,
                Map.of("intentKey", intentKey, "provider", provider, "status", "PENDING"), null);
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadIntentView handleCallback(TenantContext tenant, ProviderCallbackCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String provider = MarketingContentSupport.normalizeUpper(command.provider(), "S3", PROVIDERS, "asset upload provider");
        MarketingAssetUploadIntentDO intent = requireIntent(tenantId, provider, command);
        String status = MarketingContentSupport.normalizeUpper(command.status(), "PENDING", CALLBACK_STATUSES, "provider upload status");
        if (isTerminal(intent.getStatus())) {
            return toView(intent);
        }
        assertIntentNotExpired(intent);
        String assetKey = MarketingContentSupport.hasText(command.assetKey())
                ? MarketingContentSupport.normalizeKey(command.assetKey(), "assetKey")
                : intent.getAssetKey();
        if ("READY".equals(status)) {
            validateReadyCallback(intent, assetKey, command);
        }

        intent.setProviderAssetId(MarketingContentSupport.trimToNull(command.providerAssetId()));
        intent.setCallbackJson(writeJson(command, "providerCallback"));
        intent.setStatus(switch (status) {
            case "READY" -> "COMPLETED";
            case "FAILED" -> "FAILED";
            default -> "PENDING";
        });
        intent.setErrorMessage("FAILED".equals(status) ? "provider upload failed" : null);
        intentMapper.updateById(intent);

        if ("READY".equals(status)) {
            upsertAsset(tenant, intent, assetKey, status, command);
        }
        writeAudit(tenantId,
                "READY".equals(status) ? "ASSET_UPLOAD_COMPLETED" : "ASSET_UPLOAD_UPDATED",
                "ASSET",
                assetKey,
                MarketingContentSupport.operator(tenant, null),
                null,
                Map.of("intentKey", intent.getIntentKey(), "provider", provider, "status", intent.getStatus()),
                null);
        return toView(intent);
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadIntentCleanupResult expireStalePendingUploads(TenantContext tenant, UploadIntentCleanupCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        int limit = cleanupLimit(command == null ? null : command.limit());
        LocalDateTime cutoff = now();
        List<MarketingAssetUploadIntentDO> rows = intentMapper.selectList(
                new LambdaQueryWrapper<MarketingAssetUploadIntentDO>()
                        .eq(MarketingAssetUploadIntentDO::getTenantId, tenantId)
                        .eq(MarketingAssetUploadIntentDO::getStatus, "PENDING")
                        .le(MarketingAssetUploadIntentDO::getExpiresAt, cutoff)
                        .orderByAsc(MarketingAssetUploadIntentDO::getExpiresAt)
                        .orderByAsc(MarketingAssetUploadIntentDO::getId)
                        .last("LIMIT " + limit));
        int expired = 0;
        String actor = MarketingContentSupport.operator(tenant, command == null ? null : command.actor());
        for (MarketingAssetUploadIntentDO row : rows) {
            row.setStatus("FAILED");
            row.setErrorMessage("asset upload intent expired without callback");
            intentMapper.updateById(row);
            expired++;
            writeAudit(tenantId,
                    "ASSET_UPLOAD_EXPIRED",
                    "ASSET",
                    row.getAssetKey(),
                    actor,
                    null,
                    Map.of("intentKey", row.getIntentKey(), "provider", row.getProvider(), "status", row.getStatus()),
                    "asset upload intent expired without callback");
        }
        return new UploadIntentCleanupResult(rows.size(), expired, cutoff);
    }

    private boolean isTerminal(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }

    private void assertIntentNotExpired(MarketingAssetUploadIntentDO intent) {
        if (intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(now())) {
            throw new IllegalStateException("asset upload intent has expired");
        }
    }

    private MarketingAssetUploadIntentDO requireIntent(Long tenantId, String provider, ProviderCallbackCommand command) {
        LambdaQueryWrapper<MarketingAssetUploadIntentDO> query = new LambdaQueryWrapper<MarketingAssetUploadIntentDO>()
                .eq(MarketingAssetUploadIntentDO::getTenantId, tenantId)
                .eq(MarketingAssetUploadIntentDO::getProvider, provider)
                .last("LIMIT 1");
        if (MarketingContentSupport.hasText(command.uploadToken())) {
            query.eq(MarketingAssetUploadIntentDO::getUploadToken, command.uploadToken().trim());
        } else if (MarketingContentSupport.hasText(command.providerAssetId())) {
            query.eq(MarketingAssetUploadIntentDO::getProviderAssetId, command.providerAssetId().trim());
        } else {
            throw new IllegalArgumentException("uploadToken or providerAssetId is required");
        }
        MarketingAssetUploadIntentDO row = intentMapper.selectOne(query);
        if (row == null) {
            throw new IllegalArgumentException("asset upload intent not found");
        }
        return row;
    }

    private void upsertAsset(TenantContext tenant,
                             MarketingAssetUploadIntentDO intent,
                             String assetKey,
                             String providerStatus,
                             ProviderCallbackCommand command) {
        String assetType = MarketingContentSupport.normalizeUpper(
                MarketingContentSupport.hasText(command.assetType()) ? command.assetType() : intent.getAssetType(),
                "FILE",
                ASSET_TYPES,
                "asset type");
        String mimeType = MarketingContentSupport.hasText(command.mimeType()) ? command.mimeType() : intent.getMimeType();
        String storageUrl = MarketingContentSupport.requireText(command.storageUrl(), "asset storageUrl");
        MarketingContentSupport.validateHttpUrl(storageUrl, "asset storageUrl");
        String checksum = normalizeChecksum(command.checksumSha256(), intent.getProvider());
        String scanStatus = normalizeScanStatus(command.scanStatus(), intent.getProvider(), assetType);
        long sizeBytes = requireSize(command.sizeBytes() == null ? intent.getSizeBytes() : command.sizeBytes(),
                assetType,
                "asset sizeBytes");
        validateMimeType(assetType, mimeType);

        MarketingAssetDO row = assetMapper.selectOne(new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, intent.getTenantId())
                .eq(MarketingAssetDO::getAssetKey, assetKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingAssetDO();
            row.setTenantId(intent.getTenantId());
            row.setAssetKey(assetKey);
            row.setReferenceCount(0);
            row.setCreatedBy(MarketingContentSupport.operator(tenant, intent.getCreatedBy()));
        }
        row.setName(MarketingContentSupport.hasText(intent.getFileName()) ? intent.getFileName() : assetKey);
        row.setAssetType(assetType);
        row.setMimeType(MarketingContentSupport.requireText(mimeType, "asset mimeType"));
        row.setStorageUrl(storageUrl);
        row.setSizeBytes(sizeBytes);
        row.setChecksumSha256(checksum);
        row.setPosterUrl(MarketingContentSupport.trimToNull(command.posterUrl()));
        row.setWidth(command.width());
        row.setHeight(command.height());
        row.setDurationMs(command.durationMs());
        row.setTranscodeStatus(transcodeStatus(assetType, providerStatus, command.transcodeStatus()));
        row.setTagsJson("[]");
        row.setMetadataJson(MarketingContentSupport.objectJson(objectMapper,
                providerMetadata(command.metadata(), intent, checksum, scanStatus),
                "metadata"));
        row.setStatus(assetStatus(providerStatus));
        row.setReviewNotes("provider upload " + providerStatus.toLowerCase() + ", scan " + scanStatus.toLowerCase(Locale.ROOT));
        if (insert) {
            assetMapper.insert(row);
        } else {
            assetMapper.updateById(row);
        }
    }

    private void validateReadyCallback(MarketingAssetUploadIntentDO intent,
                                       String assetKey,
                                       ProviderCallbackCommand command) {
        String assetType = MarketingContentSupport.normalizeUpper(
                MarketingContentSupport.hasText(command.assetType()) ? command.assetType() : intent.getAssetType(),
                "FILE",
                ASSET_TYPES,
                "asset type");
        String mimeType = MarketingContentSupport.hasText(command.mimeType()) ? command.mimeType() : intent.getMimeType();
        validateMimeType(assetType, mimeType);
        if (!assetType.equals(intent.getAssetType())) {
            throw new IllegalArgumentException("provider asset type does not match upload intent");
        }
        if (!assetKey.equals(intent.getAssetKey())) {
            throw new IllegalArgumentException("provider asset key does not match upload intent");
        }
        long sizeBytes = requireSize(command.sizeBytes() == null ? intent.getSizeBytes() : command.sizeBytes(),
                assetType,
                "asset sizeBytes");
        if (intent.getSizeBytes() != null && sizeBytes > intent.getSizeBytes()) {
            throw new IllegalArgumentException("provider asset size exceeds upload intent size");
        }
        validateStorageBinding(intent, command);
        normalizeChecksum(command.checksumSha256(), intent.getProvider());
        String scanStatus = normalizeScanStatus(command.scanStatus(), intent.getProvider(), assetType);
        if ("FAILED".equals(scanStatus) || "PENDING".equals(scanStatus)) {
            throw new IllegalArgumentException("asset scan must pass before asset can be READY");
        }
        if ("VIDEO".equals(assetType)) {
            String transcode = transcodeStatus(assetType, "READY", command.transcodeStatus());
            if (!"READY".equals(transcode) && !"EXTERNAL".equals(transcode)) {
                throw new IllegalArgumentException("video transcode must be READY or EXTERNAL before asset can be READY");
            }
            if (command.durationMs() == null || command.durationMs() <= 0) {
                throw new IllegalArgumentException("video durationMs is required before asset can be READY");
            }
        }
    }

    private void validateStorageBinding(MarketingAssetUploadIntentDO intent, ProviderCallbackCommand command) {
        String storageUrl = MarketingContentSupport.requireText(command.storageUrl(), "asset storageUrl");
        MarketingContentSupport.validateHttpUrl(storageUrl, "asset storageUrl");
        Map<String, Object> uploadParams = readMap(intent.getUploadParamsJson());
        String handoffMode = stringParam(uploadParams, "handoffMode");
        if (!"S3".equals(intent.getProvider()) || !"PRESIGNED_PUT".equals(handoffMode)) {
            return;
        }
        String expectedStorageUrl = stringParam(uploadParams, "storageUrl");
        if (!MarketingContentSupport.hasText(expectedStorageUrl)) {
            throw new IllegalArgumentException("upload intent storageUrl is required for S3 callback verification");
        }
        if (!expectedStorageUrl.equals(storageUrl.trim())) {
            throw new IllegalArgumentException("asset storageUrl does not match upload intent");
        }
    }

    private String stringParam(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null ? null : value.toString();
    }

    private Map<String, String> requiredUploadHeaders(String provider, String mimeType) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", mimeType);
        if ("S3".equals(provider)) {
            headers.put("x-amz-server-side-encryption", "aws:kms");
        }
        return headers;
    }

    private String storageObjectKey(Long tenantId, String assetKey, String intentKey, String fileName) {
        String extension = fileExtension(fileName);
        String suffix = extension == null ? "" : "." + extension;
        return "tenants/" + tenantId + "/marketing-assets/" + assetKey + "/" + intentKey + suffix;
    }

    private void validateMimeType(String assetType, String mimeType) {
        String normalized = MarketingContentSupport.requireText(mimeType, "asset mimeType").toLowerCase(Locale.ROOT);
        if (!MIME_TYPES.getOrDefault(assetType, Set.of()).contains(normalized)) {
            throw new IllegalArgumentException("unsupported asset mimeType " + mimeType + " for " + assetType);
        }
    }

    private void validateFileName(String fileName, String assetType) {
        if (!MarketingContentSupport.hasText(fileName)) {
            return;
        }
        String text = fileName.trim();
        if (text.contains("/") || text.contains("\\") || text.contains("\0")) {
            throw new IllegalArgumentException("asset fileName must not contain path separators");
        }
        String extension = fileExtension(text);
        if (extension == null || !EXTENSIONS.getOrDefault(assetType, Set.of()).contains(extension)) {
            throw new IllegalArgumentException("unsupported asset file extension for " + assetType);
        }
    }

    private String fileExtension(String fileName) {
        if (!MarketingContentSupport.hasText(fileName)) {
            return null;
        }
        String text = fileName.trim();
        int index = text.lastIndexOf('.');
        if (index <= 0 || index == text.length() - 1) {
            return null;
        }
        return text.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private long requireSize(Long sizeBytes, String assetType, String field) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        long maxSize = maxSize(assetType);
        if (sizeBytes > maxSize) {
            throw new IllegalArgumentException(field + " exceeds " + assetType + " max size");
        }
        return sizeBytes;
    }

    private long maxSize(String assetType) {
        Long max = MAX_SIZE_BYTES.get(assetType);
        if (max == null) {
            throw new IllegalArgumentException("unsupported asset type " + assetType);
        }
        return max;
    }

    private int cleanupLimit(Integer value) {
        if (value == null || value <= 0) {
            return 100;
        }
        return Math.min(value, 500);
    }

    private String normalizeChecksum(String checksum, String provider) {
        if (!MarketingContentSupport.hasText(checksum)) {
            if (requiresChecksum(provider)) {
                throw new IllegalArgumentException("asset checksumSha256 is required for " + provider + " uploads");
            }
            return null;
        }
        String normalized = checksum.trim().toLowerCase(Locale.ROOT);
        if (!SHA256_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("asset checksumSha256 must be a 64 character hex SHA-256");
        }
        return normalized;
    }

    private boolean requiresChecksum(String provider) {
        return "S3".equals(provider) || "EXTERNAL".equals(provider);
    }

    private String normalizeScanStatus(String scanStatus, String provider, String assetType) {
        String status = MarketingContentSupport.normalizeUpper(scanStatus, "PENDING", SCAN_STATUSES, "asset scan status");
        if ("NOT_REQUIRED".equals(status) && ("S3".equals(provider) || "EXTERNAL".equals(provider))) {
            throw new IllegalArgumentException("asset scanStatus cannot be NOT_REQUIRED for direct uploads");
        }
        if ("PROVIDER_VERIFIED".equals(status) && !Set.of("CLOUDINARY", "MUX").contains(provider)) {
            throw new IllegalArgumentException("asset scanStatus PROVIDER_VERIFIED is only allowed for trusted media providers");
        }
        if ("VIDEO".equals(assetType) && "NOT_REQUIRED".equals(status)) {
            throw new IllegalArgumentException("asset scanStatus is required for video uploads");
        }
        return status;
    }

    private Map<String, Object> providerMetadata(Map<String, Object> metadata,
                                                 MarketingAssetUploadIntentDO intent,
                                                 String checksum,
                                                 String scanStatus) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (metadata != null) {
            values.putAll(metadata);
        }
        values.put("provider", intent.getProvider());
        values.put("providerAssetId", intent.getProviderAssetId());
        values.put("uploadIntentKey", intent.getIntentKey());
        values.put("scanStatus", scanStatus);
        if (checksum != null) {
            values.put("checksumSha256", checksum);
        }
        return values;
    }

    private String assetStatus(String providerStatus) {
        return switch (providerStatus) {
            case "READY" -> "READY";
            case "FAILED" -> "REJECTED";
            default -> "DRAFT";
        };
    }

    private String transcodeStatus(String assetType, String providerStatus, String value) {
        if (!"VIDEO".equals(assetType) && !MarketingContentSupport.hasText(value)) {
            return null;
        }
        if (MarketingContentSupport.hasText(value)) {
            return MarketingContentSupport.normalizeUpper(value, "PENDING", TRANSCODE_STATUSES, "transcode status");
        }
        return "READY".equals(providerStatus) ? "READY" : "PENDING";
    }

    private UploadIntentView toView(MarketingAssetUploadIntentDO row) {
        return new UploadIntentView(
                row.getIntentKey(),
                row.getAssetKey(),
                row.getAssetType(),
                row.getProvider(),
                row.getUploadToken(),
                row.getUploadUrl(),
                readMap(row.getUploadParamsJson()),
                row.getStatus(),
                row.getProviderAssetId(),
                row.getExpiresAt());
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

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("uploadParams must be valid JSON", e);
        }
    }

    private String writeJson(Object value, String field) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record UploadIntentCommand(
            String assetKey,
            String assetType,
            String provider,
            String mimeType,
            String fileName,
            Long sizeBytes,
            String createdBy) {
    }

    public record ProviderCallbackCommand(
            String provider,
            String uploadToken,
            String providerAssetId,
            String assetKey,
            String assetType,
            String mimeType,
            String storageUrl,
            String status,
            String transcodeStatus,
            Long sizeBytes,
            Long durationMs,
            Integer width,
            Integer height,
            String posterUrl,
            String checksumSha256,
            String scanStatus,
            Map<String, Object> metadata) {
    }

    public record UploadIntentView(
            String intentKey,
            String assetKey,
            String assetType,
            String provider,
            String uploadToken,
            String uploadUrl,
            Map<String, Object> uploadParams,
            String status,
            String providerAssetId,
            LocalDateTime expiresAt) {
    }

    public record UploadIntentCleanupCommand(Integer limit, String actor) {
    }

    public record UploadIntentCleanupResult(int scanned, int expired, LocalDateTime cutoff) {
    }
}
