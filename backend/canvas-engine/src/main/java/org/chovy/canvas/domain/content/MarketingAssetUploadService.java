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
/**
 * MarketingAssetUploadService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 MarketingAssetUploadService 实例。
     *
     * @param intentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param assetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param handoffService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 MarketingAssetUploadService 实例。
     *
     * @param intentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param assetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    MarketingAssetUploadService(MarketingAssetUploadIntentMapper intentMapper,
                                MarketingAssetMapper assetMapper,
                                MarketingContentAuditEventMapper auditMapper,
                                ObjectMapper objectMapper) {
        this(intentMapper, assetMapper, auditMapper, objectMapper, Clock.systemDefaultZone(),
                MarketingAssetUploadHandoffService.contractOnly());
    }

    /**
     * 初始化 MarketingAssetUploadService 实例。
     *
     * @param intentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param assetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingAssetUploadService(MarketingAssetUploadIntentMapper intentMapper,
                                MarketingAssetMapper assetMapper,
                                MarketingContentAuditEventMapper auditMapper,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this(intentMapper, assetMapper, auditMapper, objectMapper, clock,
                MarketingAssetUploadHandoffService.contractOnly());
    }

    /**
     * 初始化 MarketingAssetUploadService 实例。
     *
     * @param intentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param assetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param handoffService 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenant tenant 参数，用于 createIntent 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public UploadIntentView createIntent(TenantContext tenant, UploadIntentCommand command) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        intentMapper.insert(row);

        writeAudit(tenantId, "ASSET_UPLOAD_INTENT_CREATED", "ASSET", assetKey, row.getCreatedBy(), null,
                Map.of("intentKey", intentKey, "provider", provider, "status", "PENDING"), null);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 handleCallback 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 handleCallback 流程生成的业务结果。
     */
    public UploadIntentView handleCallback(TenantContext tenant, ProviderCallbackCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String provider = MarketingContentSupport.normalizeUpper(command.provider(), "S3", PROVIDERS, "asset upload provider");
        MarketingAssetUploadIntentDO intent = requireIntent(tenantId, provider, command);
        String status = MarketingContentSupport.normalizeUpper(command.status(), "PENDING", CALLBACK_STATUSES, "provider upload status");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(intent);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenant tenant 参数，用于 expireStalePendingUploads 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 expireStalePendingUploads 流程生成的业务结果。
     */
    public UploadIntentCleanupResult expireStalePendingUploads(TenantContext tenant, UploadIntentCleanupCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        int limit = cleanupLimit(command == null ? null : command.limit());
        LocalDateTime cutoff = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new UploadIntentCleanupResult(rows.size(), expired, cutoff);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isTerminal(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param intent intent 参数，用于 assertIntentNotExpired 流程中的校验、计算或对象转换。
     */
    private void assertIntentNotExpired(MarketingAssetUploadIntentDO intent) {
        if (intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(now())) {
            throw new IllegalStateException("asset upload intent has expired");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 requireIntent 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 requireIntent 流程生成的业务结果。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenant tenant 参数，用于 upsertAsset 流程中的校验、计算或对象转换。
     * @param intent intent 参数，用于 upsertAsset 流程中的校验、计算或对象转换。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param providerStatus 业务状态，用于筛选或推进状态流转。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    private void upsertAsset(TenantContext tenant,
                             MarketingAssetUploadIntentDO intent,
                             String assetKey,
                             String providerStatus,
                             ProviderCallbackCommand command) {
        // 准备本次处理所需的上下文和中间变量。
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

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param intent intent 参数，用于 validateReadyCallback 流程中的校验、计算或对象转换。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    private void validateReadyCallback(MarketingAssetUploadIntentDO intent,
                                       String assetKey,
                                       ProviderCallbackCommand command) {
        // 准备本次处理所需的上下文和中间变量。
        String assetType = MarketingContentSupport.normalizeUpper(
                MarketingContentSupport.hasText(command.assetType()) ? command.assetType() : intent.getAssetType(),
                "FILE",
                ASSET_TYPES,
                "asset type");
        String mimeType = MarketingContentSupport.hasText(command.mimeType()) ? command.mimeType() : intent.getMimeType();
        validateMimeType(assetType, mimeType);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param intent intent 参数，用于 validateStorageBinding 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    private void validateStorageBinding(MarketingAssetUploadIntentDO intent, ProviderCallbackCommand command) {
        // 准备本次处理所需的上下文和中间变量。
        String storageUrl = MarketingContentSupport.requireText(command.storageUrl(), "asset storageUrl");
        MarketingContentSupport.validateHttpUrl(storageUrl, "asset storageUrl");
        Map<String, Object> uploadParams = readMap(intent.getUploadParamsJson());
        String handoffMode = stringParam(uploadParams, "handoffMode");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"S3".equals(intent.getProvider()) || !"PRESIGNED_PUT".equals(handoffMode)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 stringParam 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 stringParam 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 string param 生成的文本或业务键。
     */
    private String stringParam(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param provider provider 参数，用于 requiredUploadHeaders 流程中的校验、计算或对象转换。
     * @param mimeType 类型标识，用于选择对应处理分支。
     * @return 返回 required upload headers 生成的文本或业务键。
     */
    private Map<String, String> requiredUploadHeaders(String provider, String mimeType) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", mimeType);
        if ("S3".equals(provider)) {
            headers.put("x-amz-server-side-encryption", "aws:kms");
        }
        return headers;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param intentKey 业务键，用于在同一租户下定位资源。
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 storage object key 生成的文本或业务键。
     */
    private String storageObjectKey(Long tenantId, String assetKey, String intentKey, String fileName) {
        String extension = fileExtension(fileName);
        String suffix = extension == null ? "" : "." + extension;
        return "tenants/" + tenantId + "/marketing-assets/" + assetKey + "/" + intentKey + suffix;
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
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @param assetType 类型标识，用于选择对应处理分支。
     */
    private void validateFileName(String fileName, String assetType) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!MarketingContentSupport.hasText(fileName)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 file extension 生成的文本或业务键。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param sizeBytes size bytes 参数，用于 requireSize 流程中的校验、计算或对象转换。
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require size 计算得到的数量、金额或指标值。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetType 类型标识，用于选择对应处理分支。
     * @return 返回 max size 计算得到的数量、金额或指标值。
     */
    private long maxSize(String assetType) {
        Long max = MAX_SIZE_BYTES.get(assetType);
        if (max == null) {
            throw new IllegalArgumentException("unsupported asset type " + assetType);
        }
        return max;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 cleanup limit 计算得到的数量、金额或指标值。
     */
    private int cleanupLimit(Integer value) {
        if (value == null || value <= 0) {
            return 100;
        }
        return Math.min(value, 500);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param checksum checksum 参数，用于 normalizeChecksum 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 normalizeChecksum 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChecksum(String checksum, String provider) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return normalized;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param provider provider 参数，用于 requiresChecksum 流程中的校验、计算或对象转换。
     * @return 返回 requires checksum 的布尔判断结果。
     */
    private boolean requiresChecksum(String provider) {
        return "S3".equals(provider) || "EXTERNAL".equals(provider);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param scanStatus 业务状态，用于筛选或推进状态流转。
     * @param provider provider 参数，用于 normalizeScanStatus 流程中的校验、计算或对象转换。
     * @param assetType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeScanStatus(String scanStatus, String provider, String assetType) {
        // 准备本次处理所需的上下文和中间变量。
        String status = MarketingContentSupport.normalizeUpper(scanStatus, "PENDING", SCAN_STATUSES, "asset scan status");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("NOT_REQUIRED".equals(status) && ("S3".equals(provider) || "EXTERNAL".equals(provider))) {
            throw new IllegalArgumentException("asset scanStatus cannot be NOT_REQUIRED for direct uploads");
        }
        if ("PROVIDER_VERIFIED".equals(status) && !Set.of("CLOUDINARY", "MUX").contains(provider)) {
            throw new IllegalArgumentException("asset scanStatus PROVIDER_VERIFIED is only allowed for trusted media providers");
        }
        if ("VIDEO".equals(assetType) && "NOT_REQUIRED".equals(status)) {
            throw new IllegalArgumentException("asset scanStatus is required for video uploads");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return status;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param metadata metadata 参数，用于 providerMetadata 流程中的校验、计算或对象转换。
     * @param intent intent 参数，用于 providerMetadata 流程中的校验、计算或对象转换。
     * @param checksum checksum 参数，用于 providerMetadata 流程中的校验、计算或对象转换。
     * @param scanStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回 providerMetadata 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param providerStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回 asset status 生成的文本或业务键。
     */
    private String assetStatus(String providerStatus) {
        return switch (providerStatus) {
            case "READY" -> "READY";
            case "FAILED" -> "REJECTED";
            default -> "DRAFT";
        };
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param providerStatus 业务状态，用于筛选或推进状态流转。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 transcode status 生成的文本或业务键。
     */
    private String transcodeStatus(String assetType, String providerStatus, String value) {
        if (!"VIDEO".equals(assetType) && !MarketingContentSupport.hasText(value)) {
            return null;
        }
        if (MarketingContentSupport.hasText(value)) {
            return MarketingContentSupport.normalizeUpper(value, "PENDING", TRANSCODE_STATUSES, "transcode status");
        }
        return "READY".equals(providerStatus) ? "READY" : "PENDING";
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param targetType 类型标识，用于选择对应处理分支。
     * @param targetKey 业务键，用于在同一租户下定位资源。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param oldValue 待处理值，用于规则计算或转换。
     * @param newValue 待处理值，用于规则计算或转换。
     * @param note note 参数，用于 writeAudit 流程中的校验、计算或对象转换。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 readMap 流程生成的业务结果。
     */
    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("uploadParams must be valid JSON", e);
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value, String field) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * UploadIntentCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record UploadIntentCommand(
            String assetKey,
            String assetType,
            String provider,
            String mimeType,
            String fileName,
            Long sizeBytes,
            String createdBy) {
    }

    /**
     * ProviderCallbackCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * UploadIntentView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * UploadIntentCleanupCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record UploadIntentCleanupCommand(Integer limit, String actor) {
    }

    /**
     * UploadIntentCleanupResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record UploadIntentCleanupResult(int scanned, int expired, LocalDateTime cutoff) {
    }
}
