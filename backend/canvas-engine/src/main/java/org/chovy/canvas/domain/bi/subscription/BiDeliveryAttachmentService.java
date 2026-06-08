package org.chovy.canvas.domain.bi.subscription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.chovy.canvas.dal.dataobject.BiDeliveryAttachmentDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiDeliveryAttachmentMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.storage.BiFileStorage;
import org.chovy.canvas.domain.bi.storage.BiStoredFile;
import org.chovy.canvas.domain.bi.storage.LocalBiFileStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Creates, stores, serves, and expires BI delivery attachments.
 *
 * <p>Metadata is persisted in the database while bytes are delegated to {@link BiFileStorage}. Local storage remains
 * the development default, but storage-key reads and deletes are preferred when present.</p>
 */
@Service
public class BiDeliveryAttachmentService {

    private static final String JOB_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final BiDeliveryAttachmentMapper attachmentMapper;
    private final ObjectMapper objectMapper;
    private final BiSnapshotRenderer snapshotRenderer;
    private final BiPermissionService permissionService;
    private final Path attachmentRoot;
    private final BiFileStorage fileStorage;
    private final int retentionDays;

    /**
     * 执行 BiDeliveryAttachmentService 流程，围绕 bi delivery attachment service 完成校验、计算或结果组装。
     *
     * @param attachmentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotRendererProvider snapshot renderer provider 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param permissionServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param storageProvider storage provider 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param attachmentDir attachment dir 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       ObjectProvider<BiSnapshotRenderer> snapshotRendererProvider,
                                       ObjectProvider<BiPermissionService> permissionServiceProvider,
                                       ObjectProvider<BiFileStorage> storageProvider,
                                       @Value("${canvas.bi.delivery.attachment.dir:${java.io.tmpdir}/canvas-bi-delivery-attachments}") String attachmentDir,
                                       @Value("${canvas.bi.delivery.attachment.retention-days:7}") int retentionDays) {
        this(attachmentMapper,
                objectMapper,
                snapshotRendererProvider == null ? null : snapshotRendererProvider.getIfAvailable(),
                permissionServiceProvider == null ? null : permissionServiceProvider.getIfAvailable(),
                Path.of(attachmentDir),
                storageProvider == null ? null : storageProvider.getIfAvailable(),
                retentionDays);
    }

    /**
     * 执行 BiDeliveryAttachmentService 流程，围绕 bi delivery attachment service 完成校验、计算或结果组装。
     *
     * @param attachmentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param attachmentRoot attachment root 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     */
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       Path attachmentRoot) {
        this(attachmentMapper, objectMapper, null, attachmentRoot);
    }

    /**
     * 执行 BiDeliveryAttachmentService 流程，围绕 bi delivery attachment service 完成校验、计算或结果组装。
     *
     * @param attachmentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotRenderer snapshot renderer 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param attachmentRoot attachment root 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     */
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiSnapshotRenderer snapshotRenderer,
                                       Path attachmentRoot) {
        this(attachmentMapper, objectMapper, snapshotRenderer, null, attachmentRoot, null, 7);
    }

    /**
     * 执行 BiDeliveryAttachmentService 流程，围绕 bi delivery attachment service 完成校验、计算或结果组装。
     *
     * @param attachmentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotRenderer snapshot renderer 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param attachmentRoot attachment root 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     */
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiPermissionService permissionService,
                                       BiSnapshotRenderer snapshotRenderer,
                                       Path attachmentRoot) {
        this(attachmentMapper, objectMapper, snapshotRenderer, permissionService, attachmentRoot, null, 7);
    }

    /**
     * 执行 BiDeliveryAttachmentService 流程，围绕 bi delivery attachment service 完成校验、计算或结果组装。
     *
     * @param attachmentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotRenderer snapshot renderer 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param fileStorage file storage 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     */
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiSnapshotRenderer snapshotRenderer,
                                       BiFileStorage fileStorage,
                                       int retentionDays) {
        this(attachmentMapper,
                objectMapper,
                snapshotRenderer,
                null,
                Path.of(System.getProperty("java.io.tmpdir"), "canvas-bi-delivery-attachments"),
                fileStorage,
                retentionDays);
    }

    /**
     * 执行 BiDeliveryAttachmentService 流程，围绕 bi delivery attachment service 完成校验、计算或结果组装。
     *
     * @param attachmentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotRenderer snapshot renderer 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param attachmentRoot attachment root 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param fileStorage file storage 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiDeliveryAttachmentService 流程中的校验、计算或对象转换。
     */
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiSnapshotRenderer snapshotRenderer,
                                       BiPermissionService permissionService,
                                       Path attachmentRoot,
                                       BiFileStorage fileStorage,
                                       int retentionDays) {
        this.attachmentMapper = attachmentMapper;
        this.objectMapper = objectMapper;
        this.snapshotRenderer = snapshotRenderer;
        this.permissionService = permissionService;
        this.attachmentRoot = attachmentRoot;
        this.fileStorage = fileStorage == null ? new LocalBiFileStorage(attachmentRoot) : fileStorage;
        this.retentionDays = retentionDays;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subscription subscription 参数，用于 createSubscriptionAttachments 流程中的校验、计算或对象转换。
     * @param schedule schedule 参数，用于 createSubscriptionAttachments 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 createSubscriptionAttachments 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public List<BiDeliveryAttachmentView> createSubscriptionAttachments(Long tenantId,
                                                                        BiSubscriptionDO subscription,
                                                                        Map<String, Object> schedule,
                                                                        Map<String, Object> delivery,
                                                                        String username) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (subscription == null) {
            return List.of();
        }
        List<String> types = attachmentTypes(delivery);
        if (types.isEmpty()) {
            return List.of();
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        Map<String, Object> summary = summary(scopedTenantId, subscription, schedule, delivery);
        List<BiDeliveryAttachmentView> attachments = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String type : types) {
            attachments.add(createAttachment(scopedTenantId, subscription, type, summary, delivery, username));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return attachments;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param deliveryLogId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiDeliveryAttachmentView> listAttachments(Long tenantId,
                                                          String jobType,
                                                          Long jobId,
                                                          Long deliveryLogId,
                                                          int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        LambdaQueryWrapper<BiDeliveryAttachmentDO> query = new LambdaQueryWrapper<BiDeliveryAttachmentDO>()
                .eq(BiDeliveryAttachmentDO::getTenantId, scopedTenantId)
                .orderByDesc(BiDeliveryAttachmentDO::getCreatedAt)
                .orderByDesc(BiDeliveryAttachmentDO::getId)
                .last("LIMIT " + capped);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(jobType)) {
            query.eq(BiDeliveryAttachmentDO::getJobType, jobType.trim().toUpperCase(Locale.ROOT));
        }
        if (jobId != null) {
            query.eq(BiDeliveryAttachmentDO::getJobId, jobId);
        }
        if (deliveryLogId != null) {
            query.eq(BiDeliveryAttachmentDO::getDeliveryLogId, deliveryLogId);
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(attachmentMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 执行 download 流程，围绕 download 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attachmentId 业务对象 ID，用于定位具体记录。
     * @return 返回 download 流程生成的业务结果。
     */
    public BiDeliveryAttachmentDownload download(Long tenantId, Long attachmentId) {
        return download(tenantId, attachmentId, null, null);
    }

    /**
     * Loads a completed attachment after tenant, status, expiration, and SUBSCRIBE access checks.
     */
    public BiDeliveryAttachmentDownload download(Long tenantId, Long attachmentId, String username, String role) {
        if (attachmentId == null) {
            throw new IllegalArgumentException("attachmentId is required");
        }
        BiDeliveryAttachmentDO row = attachmentMapper.selectById(attachmentId);
        if (row == null || !normalizeTenant(row.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI delivery attachment not found: " + attachmentId);
        }
        if (!STATUS_COMPLETED.equals(row.getStatus())) {
            throw new IllegalStateException("BI delivery attachment is not ready: " + attachmentId);
        }
        enforceDownloadAccess(tenantId, row, username, role);
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(row, now)) {
            deleteFile(row);
            markExpired(row, "BI delivery attachment expired");
            throw new IllegalStateException("BI delivery attachment has expired: " + attachmentId);
        }
        if (!hasText(row.getFilePath())) {
            if (!hasText(row.getStorageKey())) {
                throw new IllegalStateException("BI delivery attachment file is not available: " + attachmentId);
            }
        }
        try {
            byte[] bytes = readFile(row);
            auditDownload(row, now);
            return new BiDeliveryAttachmentDownload(
                    row.getFileName(),
                    row.getContentType(),
                    bytes);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            throw new IllegalStateException("BI delivery attachment file is not available: " + attachmentId);
        }
    }

    /**
     * 执行 cleanupExpiredAttachments 流程，围绕 cleanup expired attachments 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 cleanupExpiredAttachments 流程生成的业务结果。
     */
    public BiDeliveryAttachmentCleanupResult cleanupExpiredAttachments(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 500));
        LocalDateTime now = LocalDateTime.now();
        List<BiDeliveryAttachmentDO> rows = safeList(attachmentMapper.selectList(new LambdaQueryWrapper<BiDeliveryAttachmentDO>()
                .eq(BiDeliveryAttachmentDO::getTenantId, scopedTenantId)
                .in(BiDeliveryAttachmentDO::getStatus, List.of(STATUS_COMPLETED, STATUS_FAILED))
                .isNotNull(BiDeliveryAttachmentDO::getExpiresAt)
                .le(BiDeliveryAttachmentDO::getExpiresAt, now)
                .orderByAsc(BiDeliveryAttachmentDO::getExpiresAt)
                .orderByAsc(BiDeliveryAttachmentDO::getId)
                .last("LIMIT " + capped)));
        int expired = 0;
        int filesDeleted = 0;
        int failed = 0;
        for (BiDeliveryAttachmentDO row : rows) {
            try {
                if (deleteFile(row)) {
                    filesDeleted++;
                }
                markExpired(row, "BI delivery attachment expired");
                expired++;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException e) {
                failed++;
                row.setErrorMessage(truncate(e.getMessage()));
                attachmentMapper.updateById(row);
            }
        }
        return new BiDeliveryAttachmentCleanupResult(rows.size(), expired, filesDeleted, failed);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subscription subscription 参数，用于 createAttachment 流程中的校验、计算或对象转换。
     * @param type 类型标识，用于选择对应处理分支。
     * @param summary summary 参数，用于 createAttachment 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 createAttachment 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    private BiDeliveryAttachmentView createAttachment(Long tenantId,
                                                      BiSubscriptionDO subscription,
                                                      String type,
                                                      Map<String, Object> summary,
                                                      Map<String, Object> delivery,
                                                      String username) {
        BiDeliveryAttachmentDO row = new BiDeliveryAttachmentDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(subscription.getWorkspaceId());
        row.setJobType(JOB_SUBSCRIPTION);
        row.setJobId(subscription.getId());
        row.setJobKey(subscription.getSubscriptionKey());
        row.setResourceType(subscription.getResourceType());
        row.setResourceId(subscription.getResourceId());
        row.setAttachmentType(type);
        row.setAttachmentKey(attachmentKey(subscription.getSubscriptionKey(), type));
        row.setFileName(row.getAttachmentKey() + "." + extension(type));
        row.setContentType(contentType(type));
        row.setRetentionDays(retentionDays > 0 ? retentionDays : null);
        row.setExpiresAt(retentionDays > 0 ? LocalDateTime.now().plusDays(retentionDays) : null);
        row.setDownloadCount(0);
        row.setStatus(STATUS_RUNNING);
        row.setCreatedBy(defaultUser(username));
        attachmentMapper.insert(row);
        if (row.getId() == null) {
            throw new IllegalStateException("BI delivery attachment was not persisted");
        }

        try {
            // Insert first so generated storage keys and download URLs can include the durable attachment id.
            byte[] bytes = render(type, summary, delivery);
            BiStoredFile storedFile = fileStorage.write(storageKey(tenantId, row.getId(), row.getAttachmentKey()), bytes);
            row.setStorageProvider(storedFile.provider());
            row.setStorageKey(storedFile.key());
            row.setFilePath(storedFile.path());
            row.setFileUrl("/canvas/bi/delivery-attachments/" + row.getId() + "/download");
            row.setSizeBytes(storedFile.sizeBytes());
            row.setStatus(STATUS_COMPLETED);
            row.setErrorMessage(null);
            attachmentMapper.updateById(row);
            return toView(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException | IOException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage()));
            attachmentMapper.updateById(row);
            throw new IllegalStateException("failed to generate BI delivery attachment", e);
        }
    }

    /**
     * 执行 attachmentTypes 流程，围绕 attachment types 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 attachmentTypes 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 attachmentTypes 流程中的校验、计算或对象转换。
     * @return 返回 attachment types 汇总后的集合、分页或映射视图。
     */
    private List<String> attachmentTypes(Map<String, Object> delivery) {
        Map<String, Object> config = delivery == null ? Map.of() : delivery;
        Set<String> types = new LinkedHashSet<>();
        String content = normalizeType(String.valueOf(config.getOrDefault("content", "")));
        if (content.contains("SNAPSHOT")) {
            types.add(snapshotType(config));
        }
        addAttachmentType(types, config.get("attachment"));
        addAttachmentType(types, config.get("attachments"));
        addAttachmentType(types, config.get("dataAttachment"));
        addAttachmentType(types, config.get("dataAttachments"));
        return List.copyOf(types);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param types types 参数，用于 addAttachmentType 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void addAttachmentType(Set<String> types, Object value) {
        if (value instanceof List<?> list) {
            list.forEach(item -> addAttachmentType(types, item));
            return;
        }
        if (value == null) {
            return;
        }
        String type = normalizeType(String.valueOf(value));
        if (!hasText(type) || "NONE".equals(type) || "FALSE".equals(type)) {
            return;
        }
        if ("EXCEL".equals(type)) {
            type = "XLSX";
        }
        if ("JPG".equals(type)) {
            type = "JPEG";
        }
        if ("SCREENSHOT".equals(type) || "IMAGE".equals(type)) {
            type = "PNG";
        }
        // Keep the allow-list explicit so delivery JSON cannot request arbitrary renderer modes.
        if (List.of("HTML", "CSV", "JSON", "XLSX", "PDF", "PNG", "JPEG").contains(type)) {
            types.add(type);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param type 类型标识，用于选择对应处理分支。
     * @param String string 参数，用于 render 流程中的校验、计算或对象转换。
     * @param summary summary 参数，用于 render 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 render 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 render 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private byte[] render(String type, Map<String, Object> summary, Map<String, Object> delivery) throws IOException {
        return switch (type) {
            case "JSON" -> json(summary).getBytes(StandardCharsets.UTF_8);
            case "XLSX" -> xlsx(summary);
            case "PDF" -> pdf(summary);
            case "PNG", "JPEG" -> image(type, summary, delivery);
            case "CSV" -> csv(summary).getBytes(StandardCharsets.UTF_8);
            default -> html(summary).getBytes(StandardCharsets.UTF_8);
        };
    }

    /**
     * 执行 image 流程，围绕 image 完成校验、计算或结果组装。
     *
     * @param type 类型标识，用于选择对应处理分支。
     * @param String string 参数，用于 image 流程中的校验、计算或对象转换。
     * @param summary summary 参数，用于 image 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 image 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 image 流程中的校验、计算或对象转换。
     * @return 返回 image 流程生成的业务结果。
     */
    private byte[] image(String type, Map<String, Object> summary, Map<String, Object> delivery) {
        if (snapshotRenderer == null || !snapshotRenderer.configured()) {
            throw new IllegalStateException("BI snapshot renderer is not configured");
        }
        // Snapshot dimensions come from delivery config but are bounded by intConfig/doubleConfig.
        BiSnapshotRenderResult result = snapshotRenderer.render(new BiSnapshotRenderRequest(
                html(summary),
                String.valueOf(summary.getOrDefault("resourceUrl", "/bi")),
                type,
                intConfig(delivery, 1440, "snapshotWidth", "screenshotWidth", "width"),
                intConfig(delivery, 900, "snapshotHeight", "screenshotHeight", "height"),
                doubleConfig(delivery, 1.0, "snapshotScale", "screenshotScale", "scale"),
                summary));
        if (result.bytes().length == 0) {
            throw new IllegalStateException("BI snapshot renderer returned empty image");
        }
        return result.bytes();
    }

    /**
     * 执行 html 流程，围绕 html 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 html 流程中的校验、计算或对象转换。
     * @param summary summary 参数，用于 html 流程中的校验、计算或对象转换。
     * @return 返回 html 生成的文本或业务键。
     */
    private String html(Map<String, Object> summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!doctype html><html><head><meta charset=\"UTF-8\">")
                .append("<title>BI Delivery Snapshot</title>")
                .append("<style>")
                .append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;margin:32px;color:#1f2937;}")
                .append("h1{font-size:22px;margin:0 0 16px;}table{border-collapse:collapse;width:100%;max-width:960px;}")
                .append("td,th{border:1px solid #d9dee8;padding:8px 10px;text-align:left;font-size:13px;}")
                .append("th{background:#f5f7fb;width:180px;}")
                .append("</style></head><body><h1>BI Delivery Snapshot</h1><table>");
        for (Map.Entry<String, Object> entry : summary.entrySet()) {
            builder.append("<tr><th>")
                    .append(htmlEscape(entry.getKey()))
                    .append("</th><td>")
                    .append(htmlEscape(summaryValue(entry.getValue())))
                    .append("</td></tr>");
        }
        builder.append("</table></body></html>");
        return builder.toString();
    }

    /**
     * 执行 csv 流程，围绕 csv 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 csv 流程中的校验、计算或对象转换。
     * @param summary summary 参数，用于 csv 流程中的校验、计算或对象转换。
     * @return 返回 csv 生成的文本或业务键。
     */
    private String csv(Map<String, Object> summary) {
        StringBuilder builder = new StringBuilder("key,value\n");
        for (Map.Entry<String, Object> entry : summary.entrySet()) {
            builder.append(csvCell(entry.getKey()))
                    .append(',')
                    .append(csvCell(summaryValue(entry.getValue())))
                    .append('\n');
        }
        return builder.toString();
    }

    /**
     * 执行 xlsx 流程，围绕 xlsx 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 xlsx 流程中的校验、计算或对象转换。
     * @param summary summary 参数，用于 xlsx 流程中的校验、计算或对象转换。
     * @return 返回 xlsx 流程生成的业务结果。
     */
    private byte[] xlsx(Map<String, Object> summary) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("BI Delivery");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("key");
            header.createCell(1).setCellValue("value");
            int rowIndex = 1;
            for (Map.Entry<String, Object> entry : summary.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(summaryValue(entry.getValue()));
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    /**
     * 执行 pdf 流程，围绕 pdf 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 pdf 流程中的校验、计算或对象转换。
     * @param summary summary 参数，用于 pdf 流程中的校验、计算或对象转换。
     * @return 返回 pdf 流程生成的业务结果。
     */
    private byte[] pdf(Map<String, Object> summary) {
        List<String> lines = new ArrayList<>();
        lines.add("BI Delivery Snapshot");
        summary.forEach((key, value) -> lines.add(key + ": " + summaryValue(value)));
        List<String> wrapped = lines.stream()
                .flatMap(line -> wrapAscii(line, 84).stream())
                .toList();
        List<List<String>> pages = pdfPages(wrapped, 48);
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) {
                kids.append(' ');
            }
            kids.append(4 + i * 2).append(" 0 R");
        }
        // Build a minimal ASCII PDF directly to avoid adding a heavyweight dependency for summary exports.
        List<String> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objects.add("2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>\nendobj\n");
        objects.add("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        for (int i = 0; i < pages.size(); i++) {
            int pageObjectId = 4 + i * 2;
            int contentObjectId = pageObjectId + 1;
            String contentText = pdfPageContent(pages.get(i), i + 1, pages.size());
            objects.add(pageObjectId + " 0 obj\n"
                    + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R >> >> "
                    + "/Contents " + contentObjectId + " 0 R >>\n"
                    + "endobj\n");
            objects.add(contentObjectId + " 0 obj\n<< /Length "
                    + contentText.getBytes(StandardCharsets.US_ASCII).length
                    + " >>\nstream\n"
                    + contentText
                    + "endstream\nendobj\n");
        }
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String object : objects) {
            offsets.add(pdf.length());
            pdf.append(object);
        }
        int xrefOffset = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append('\n')
                .append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * 执行 pdfPages 流程，围绕 pdf pages 完成校验、计算或结果组装。
     *
     * @param wrappedLines wrapped lines 参数，用于 pdfPages 流程中的校验、计算或对象转换。
     * @param linesPerPage lines per page 参数，用于 pdfPages 流程中的校验、计算或对象转换。
     * @return 返回 pdf pages 汇总后的集合、分页或映射视图。
     */
    private List<List<String>> pdfPages(List<String> wrappedLines, int linesPerPage) {
        List<String> lines = wrappedLines == null || wrappedLines.isEmpty() ? List.of("") : wrappedLines;
        int cappedLinesPerPage = Math.max(1, linesPerPage);
        List<List<String>> pages = new ArrayList<>();
        for (int index = 0; index < lines.size(); index += cappedLinesPerPage) {
            pages.add(lines.subList(index, Math.min(index + cappedLinesPerPage, lines.size())));
        }
        return pages;
    }

    /**
     * 执行 pdfPageContent 流程，围绕 pdf page content 完成校验、计算或结果组装。
     *
     * @param lines lines 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @param pageNumber page number 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @param pageCount page count 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @return 返回 pdf page content 生成的文本或业务键。
     */
    private String pdfPageContent(List<String> lines, int pageNumber, int pageCount) {
        StringBuilder content = new StringBuilder("BT\n/F1 11 Tf\n72 760 Td\n14 TL\n");
        for (String line : lines) {
            content.append('(').append(pdfEscape(ascii(line))).append(") Tj\nT*\n");
        }
        content.append("ET\n")
                .append("BT\n/F1 9 Tf\n72 42 Td\n")
                .append('(')
                .append(pdfEscape("Page " + pageNumber + " of " + pageCount))
                .append(") Tj\nET\n");
        return content.toString();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subscription subscription 参数，用于 summary 流程中的校验、计算或对象转换。
     * @param schedule schedule 参数，用于 summary 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 summary 流程中的校验、计算或对象转换。
     * @return 返回 summary 流程生成的业务结果。
     */
    private Map<String, Object> summary(Long tenantId,
                                        BiSubscriptionDO subscription,
                                        Map<String, Object> schedule,
                                        Map<String, Object> delivery) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", LocalDateTime.now().toString());
        summary.put("tenantId", tenantId);
        summary.put("workspaceId", subscription.getWorkspaceId());
        summary.put("jobType", JOB_SUBSCRIPTION);
        summary.put("jobId", subscription.getId());
        summary.put("jobKey", subscription.getSubscriptionKey());
        summary.put("title", subscription.getName());
        summary.put("resourceType", subscription.getResourceType());
        summary.put("resourceId", subscription.getResourceId());
        summary.put("resourceUrl", resourceUrl(subscription.getResourceType(), subscription.getResourceId()));
        summary.put("schedule", schedule == null ? Map.of() : schedule);
        summary.put("delivery", delivery == null ? Map.of() : delivery);
        return summary;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDeliveryAttachmentView toView(BiDeliveryAttachmentDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDeliveryAttachmentView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getJobType(),
                row.getJobId(),
                row.getJobKey(),
                row.getDeliveryLogId(),
                row.getResourceType(),
                row.getResourceId(),
                row.getAttachmentKey(),
                row.getAttachmentType(),
                row.getFileName(),
                row.getContentType(),
                row.getFileUrl(),
                row.getStorageProvider(),
                row.getStorageKey(),
                row.getSizeBytes(),
                row.getRetentionDays(),
                row.getExpiresAt(),
                row.getDownloadCount(),
                row.getLastDownloadedAt(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 summary value 生成的文本或业务键。
     */
    private String summaryValue(Object value) {
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return json(value);
        }
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI delivery attachment payload", e);
        }
    }

    /**
     * 执行 csvCell 流程，围绕 csv cell 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 csv cell 生成的文本或业务键。
     */
    private String csvCell(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /**
     * 执行 htmlEscape 流程，围绕 html escape 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 html escape 生成的文本或业务键。
     */
    private String htmlEscape(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 执行 wrapAscii 流程，围绕 wrap ascii 完成校验、计算或结果组装。
     *
     * @param text text 参数，用于 wrapAscii 流程中的校验、计算或对象转换。
     * @param width width 参数，用于 wrapAscii 流程中的校验、计算或对象转换。
     * @return 返回 wrap ascii 汇总后的集合、分页或映射视图。
     */
    private List<String> wrapAscii(String text, int width) {
        String ascii = ascii(text);
        List<String> lines = new ArrayList<>();
        int cursor = 0;
        while (cursor < ascii.length()) {
            int end = Math.min(cursor + width, ascii.length());
            lines.add(ascii.substring(cursor, end));
            cursor = end;
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    /**
     * 执行 ascii 流程，围绕 ascii 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 ascii 生成的文本或业务键。
     */
    private String ascii(String value) {
        StringBuilder builder = new StringBuilder();
        for (char ch : (value == null ? "" : value).toCharArray()) {
            builder.append(ch >= 32 && ch < 127 ? ch : '?');
        }
        return builder.toString();
    }

    /**
     * 执行 pdfEscape 流程，围绕 pdf escape 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 pdf escape 生成的文本或业务键。
     */
    private String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    /**
     * 执行 attachmentKey 流程，围绕 attachment key 完成校验、计算或结果组装。
     *
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param type 类型标识，用于选择对应处理分支。
     * @return 返回 attachment key 生成的文本或业务键。
     */
    private String attachmentKey(String jobKey, String type) {
        return safeSlug(jobKey) + "-" + type.toLowerCase(Locale.ROOT) + "-" + Long.toHexString(System.nanoTime());
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe slug 生成的文本或业务键。
     */
    private String safeSlug(String value) {
        String slug = (value == null || value.isBlank() ? "bi-delivery" : value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "bi-delivery" : slug;
    }

    /**
     * 执行 attachmentPath 流程，围绕 attachment path 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attachmentId 业务对象 ID，用于定位具体记录。
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 attachmentPath 流程生成的业务结果。
     */
    private Path attachmentPath(Long tenantId, Long attachmentId, String fileName) {
        return attachmentRoot
                .resolve("tenant-" + normalizeTenant(tenantId))
                .resolve("attachment-" + attachmentId)
                .resolve(fileName);
    }

    /**
     * 执行 storageKey 流程，围绕 storage key 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attachmentId 业务对象 ID，用于定位具体记录。
     * @param attachmentKey 业务键，用于在同一租户下定位资源。
     * @return 返回 storage key 生成的文本或业务键。
     */
    private String storageKey(Long tenantId, Long attachmentId, String attachmentKey) {
        return "attachments/tenant-" + normalizeTenant(tenantId)
                + "/attachment-" + attachmentId
                /**
                 * 按安全边界裁剪或保护输入值。
                 *
                 * @param attachmentKey 业务键，用于在同一租户下定位资源。
                 * @return 返回 safeSlug 流程生成的业务结果。
                 */
                + "/" + safeSlug(attachmentKey);
    }

    /**
     * 执行 extension 流程，围绕 extension 完成校验、计算或结果组装。
     *
     * @param type 类型标识，用于选择对应处理分支。
     * @return 返回 extension 生成的文本或业务键。
     */
    private String extension(String type) {
        return switch (type) {
            case "JSON" -> "json";
            case "XLSX" -> "xlsx";
            case "PDF" -> "pdf";
            case "PNG" -> "png";
            case "JPEG" -> "jpg";
            case "CSV" -> "csv";
            default -> "html";
        };
    }

    /**
     * 执行 contentType 流程，围绕 content type 完成校验、计算或结果组装。
     *
     * @param type 类型标识，用于选择对应处理分支。
     * @return 返回 content type 生成的文本或业务键。
     */
    private String contentType(String type) {
        return switch (type) {
            case "JSON" -> "application/json";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PDF" -> "application/pdf";
            case "PNG" -> "image/png";
            case "JPEG" -> "image/jpeg";
            case "CSV" -> "text/csv; charset=UTF-8";
            default -> "text/html; charset=UTF-8";
        };
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param String string 参数，用于 snapshotType 流程中的校验、计算或对象转换。
     * @param delivery delivery 参数，用于 snapshotType 流程中的校验、计算或对象转换。
     * @return 返回 snapshot type 生成的文本或业务键。
     */
    private String snapshotType(Map<String, Object> delivery) {
        String type = normalizeType(String.valueOf(firstValue(delivery, "snapshotFormat", "screenshotFormat", "imageFormat")));
        if ("JPG".equals(type)) {
            return "JPEG";
        }
        if (List.of("PNG", "JPEG", "HTML").contains(type)) {
            return type;
        }
        // HTML is renderable without a browser service and is therefore the safest snapshot fallback.
        return "HTML";
    }

    /**
     * 执行 firstValue 流程，围绕 first value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @return 返回 firstValue 流程生成的业务结果。
     */
    private Object firstValue(Map<String, Object> values, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        Object snapshot = values.get("snapshot");
        if (snapshot instanceof Map<?, ?> nested) {
            for (String key : keys) {
                if (nested.containsKey(key)) {
                    return nested.get(key);
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 执行 intConfig 流程，围绕 int config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 intConfig 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 intConfig 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param keys keys 参数，用于 intConfig 流程中的校验、计算或对象转换。
     * @return 返回 int config 计算得到的数量、金额或指标值。
     */
    private int intConfig(Map<String, Object> values, int defaultValue, String... keys) {
        Object raw = firstValue(values, keys);
        if (raw instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        try {
            return raw == null ? defaultValue : Math.max(1, Integer.parseInt(String.valueOf(raw)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 执行 doubleConfig 流程，围绕 double config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 doubleConfig 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 doubleConfig 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param keys keys 参数，用于 doubleConfig 流程中的校验、计算或对象转换。
     * @return 返回 double config 计算得到的数量、金额或指标值。
     */
    private double doubleConfig(Map<String, Object> values, double defaultValue, String... keys) {
        Object raw = firstValue(values, keys);
        if (raw instanceof Number number) {
            return Math.max(0.1, number.doubleValue());
        }
        try {
            return raw == null ? defaultValue : Math.max(0.1, Double.parseDouble(String.valueOf(raw)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 执行 resourceUrl 流程，围绕 resource url 完成校验、计算或结果组装。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 resource url 生成的文本或业务键。
     */
    private String resourceUrl(String resourceType, Long resourceId) {
        return BiDeliveryResourceUrls.workbenchUrl(resourceType, resourceId);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 执行 truncate 流程，围绕 truncate 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    /**
     * 执行 enforceDownloadAccess 流程，围绕 enforce download access 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     */
    private void enforceDownloadAccess(Long tenantId, BiDeliveryAttachmentDO row, String username, String role) {
        if (permissionService == null || row == null) {
            return;
        }
        permissionService.enforceResourceAccess(
                normalizeTenant(tenantId),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceId(),
                new BiQueryContext(normalizeTenant(tenantId), defaultUser(username), role),
                BiPermissionService.ACTION_SUBSCRIBE);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isExpired(BiDeliveryAttachmentDO row, LocalDateTime now) {
        return row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now);
    }

    /**
     * 执行 auditDownload 流程，围绕 audit download 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     */
    private void auditDownload(BiDeliveryAttachmentDO row, LocalDateTime now) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiDeliveryAttachmentDO update = new BiDeliveryAttachmentDO();
        update.setId(row.getId());
        update.setDownloadCount((row.getDownloadCount() == null ? 0 : row.getDownloadCount()) + 1);
        update.setLastDownloadedAt(now);
        attachmentMapper.updateById(update);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private void markExpired(BiDeliveryAttachmentDO row, String message) {
        row.setStatus(STATUS_EXPIRED);
        row.setErrorMessage(message);
        attachmentMapper.updateById(row);
    }

    /**
     * 查询或读取业务数据。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 readFile 流程生成的业务结果。
     */
    private byte[] readFile(BiDeliveryAttachmentDO row) {
        if (hasText(row.getStorageKey())) {
            // Storage-backed attachments are portable across nodes; filePath is retained for legacy rows.
            byte[] bytes = fileStorage.read(row.getStorageKey());
            if (bytes == null) {
                throw new IllegalStateException("BI delivery attachment storage object is not available: "
                        + row.getStorageKey());
            }
            return bytes;
        }
        try {
            return Files.readAllBytes(Path.of(row.getFilePath()));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("BI delivery attachment file is not available: " + row.getId(), e);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 delete file 的布尔判断结果。
     */
    private boolean deleteFile(BiDeliveryAttachmentDO row) {
        if (row != null && hasText(row.getStorageKey())) {
            return fileStorage.delete(row.getStorageKey());
        }
        return deleteLocalFile(row);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 delete local file 的布尔判断结果。
     */
    private boolean deleteLocalFile(BiDeliveryAttachmentDO row) {
        if (row == null || !hasText(row.getFilePath())) {
            return false;
        }
        try {
            Path root = attachmentRoot.toAbsolutePath().normalize();
            Path file = Path.of(row.getFilePath()).toAbsolutePath().normalize();
            if (!file.startsWith(root)) {
                // Never delete a path outside the configured BI attachment root, even if the row is corrupt.
                return false;
            }
            boolean deleted = Files.deleteIfExists(file);
            deleteEmptyParents(file.getParent(), root);
            return deleted;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete BI delivery attachment file: " + row.getId(), e);
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param parent parent 参数，用于 deleteEmptyParents 流程中的校验、计算或对象转换。
     * @param root root 参数，用于 deleteEmptyParents 流程中的校验、计算或对象转换。
     */
    private void deleteEmptyParents(Path parent, Path root) throws IOException {
        Path cursor = parent;
        while (cursor != null && cursor.startsWith(root) && !cursor.equals(root)) {
            try {
                Files.deleteIfExists(cursor);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DirectoryNotEmptyException e) {
                return;
            }
            cursor = cursor.getParent();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
