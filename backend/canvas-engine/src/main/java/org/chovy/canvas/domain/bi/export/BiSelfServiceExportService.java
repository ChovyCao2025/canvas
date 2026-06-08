package org.chovy.canvas.domain.bi.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiExportJobDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiExportJobMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.storage.BiFileStorage;
import org.chovy.canvas.domain.bi.storage.BiStoredFile;
import org.chovy.canvas.domain.bi.storage.LocalBiFileStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Manages self-service BI export jobs from preview through approval, file generation, download, retry, and cleanup.
 *
 * <p>Export bytes are written through {@link BiFileStorage}; large CSV jobs are partitioned into object-backed parts
 * with a zip manifest so downloads remain bounded while rows can exceed the single-query export limit.</p>
 */
@Service
public class BiSelfServiceExportService {

    private static final String RESOURCE_DATASET = "DATASET";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String AUDIT_RESOURCE_EXPORT_JOB = "BI_EXPORT_JOB";
    private static final String AUDIT_DOWNLOAD = "BI_EXPORT_DOWNLOAD";
    private static final String AUDIT_DOWNLOAD_RATE_LIMITED = "BI_EXPORT_DOWNLOAD_RATE_LIMITED";
    private static final int PROGRESS_QUEUED = 0;
    private static final int PROGRESS_RUNNING = 50;
    private static final int PROGRESS_COMPLETED = 100;
    private static final int MAX_PREVIEW_LIMIT = 500;
    private static final int SINGLE_FILE_EXPORT_LIMIT = 10000;
    private static final int MAX_EXPORT_LIMIT = 1_000_000;

    private final BiDatasetMapper datasetMapper;
    private final BiExportJobMapper exportJobMapper;
    private final BiQueryExecutionService queryExecutionService;
    private final BiPermissionService permissionService;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;
    private final BiFileStorage fileStorage;
    private final int retentionDays;
    private final int approvalRowThreshold;
    private final int maxRetryCount;
    private final long retryInitialDelayMinutes;
    private final double retryBackoffMultiplier;
    private final long retryMaxDelayMinutes;
    private final int downloadRateLimitPerMinute;
    private final Map<String, DownloadRateWindow> downloadRateWindows = new ConcurrentHashMap<>();
    @Value("${canvas.bi.export.queue.enabled:false}")
    private boolean exportQueueEnabled;
    @Value("${canvas.bi.export.queue.tenant-id:0}")
    private Long exportQueueTenantId;
    @Value("${canvas.bi.export.queue.operator:bi-export-worker}")
    private String exportQueueOperator;
    @Value("${canvas.bi.export.queue.role:SYSTEM}")
    private String exportQueueRole;
    @Value("${canvas.bi.export.queue.limit:20}")
    private int exportQueueLimit;

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param storageProvider storage provider 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param auditLogMapperProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportDir export dir 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param approvalRowThreshold approval row threshold 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param maxRetryCount max retry count 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryInitialDelayMinutes retry initial delay minutes 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryBackoffMultiplier retry backoff multiplier 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryMaxDelayMinutes retry max delay minutes 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param downloadRateLimitPerMinute download rate limit per minute 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      ObjectProvider<BiPermissionService> permissionServiceProvider,
                                      ObjectMapper objectMapper,
                                      ObjectProvider<BiFileStorage> storageProvider,
                                      ObjectProvider<BiAuditLogMapper> auditLogMapperProvider,
                                      @Value("${canvas.bi.export.dir:${java.io.tmpdir}/canvas-bi-exports}") String exportDir,
                                      @Value("${canvas.bi.export.retention-days:7}") int retentionDays,
                                      @Value("${canvas.bi.export.approval.row-threshold:5000}") int approvalRowThreshold,
                                      @Value("${canvas.bi.export.retry.max-attempts:3}") int maxRetryCount,
                                      @Value("${canvas.bi.export.retry.initial-delay-minutes:15}") long retryInitialDelayMinutes,
                                      @Value("${canvas.bi.export.retry.backoff-multiplier:2}") double retryBackoffMultiplier,
                                      @Value("${canvas.bi.export.retry.max-delay-minutes:1440}") long retryMaxDelayMinutes,
                                      @Value("${canvas.bi.export.download.rate-limit-per-minute:0}") int downloadRateLimitPerMinute) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionServiceProvider.getIfAvailable(),
                objectMapper,
                Path.of(exportDir),
                storageProvider == null ? null : storageProvider.getIfAvailable(),
                retentionDays,
                approvalRowThreshold,
                maxRetryCount,
                retryInitialDelayMinutes,
                retryBackoffMultiplier,
                retryMaxDelayMinutes,
                auditLogMapperProvider == null ? null : auditLogMapperProvider.getIfAvailable(),
                downloadRateLimitPerMinute);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportRoot export root 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      Path exportRoot) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                objectMapper,
                exportRoot,
                null,
                7,
                5000);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportRoot export root 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      Path exportRoot,
                                      int retentionDays) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                objectMapper,
                exportRoot,
                null,
                retentionDays,
                5000);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fileStorage file storage 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param approvalRowThreshold approval row threshold 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      BiFileStorage fileStorage,
                                      int retentionDays,
                                      int approvalRowThreshold) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                objectMapper,
                Path.of(System.getProperty("java.io.tmpdir"), "canvas-bi-exports"),
                fileStorage,
                retentionDays,
                approvalRowThreshold,
                3,
                15,
                2,
                1440);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fileStorage file storage 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param approvalRowThreshold approval row threshold 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param downloadRateLimitPerMinute download rate limit per minute 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      BiFileStorage fileStorage,
                                      int retentionDays,
                                      int approvalRowThreshold,
                                      BiAuditLogMapper auditLogMapper,
                                      int downloadRateLimitPerMinute) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                objectMapper,
                Path.of(System.getProperty("java.io.tmpdir"), "canvas-bi-exports"),
                fileStorage,
                retentionDays,
                approvalRowThreshold,
                3,
                15,
                2,
                1440,
                auditLogMapper,
                downloadRateLimitPerMinute);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportRoot export root 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param fileStorage file storage 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param approvalRowThreshold approval row threshold 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      Path exportRoot,
                                      BiFileStorage fileStorage,
                                      int retentionDays,
                                      int approvalRowThreshold) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                objectMapper,
                exportRoot,
                fileStorage,
                retentionDays,
                approvalRowThreshold,
                3,
                15,
                2,
                1440);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportRoot export root 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param fileStorage file storage 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param approvalRowThreshold approval row threshold 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param maxRetryCount max retry count 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryInitialDelayMinutes retry initial delay minutes 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryBackoffMultiplier retry backoff multiplier 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryMaxDelayMinutes retry max delay minutes 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      Path exportRoot,
                                      BiFileStorage fileStorage,
                                      int retentionDays,
                                      int approvalRowThreshold,
                                      int maxRetryCount,
                                      long retryInitialDelayMinutes,
                                      double retryBackoffMultiplier,
                                      long retryMaxDelayMinutes) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionService,
                objectMapper,
                exportRoot,
                fileStorage,
                retentionDays,
                approvalRowThreshold,
                maxRetryCount,
                retryInitialDelayMinutes,
                retryBackoffMultiplier,
                retryMaxDelayMinutes,
                null,
                0);
    }

    /**
     * 执行 BiSelfServiceExportService 流程，围绕 bi self service export service 完成校验、计算或结果组装。
     *
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportJobMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param exportRoot export root 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param fileStorage file storage 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retentionDays retention days 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param approvalRowThreshold approval row threshold 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param maxRetryCount max retry count 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryInitialDelayMinutes retry initial delay minutes 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryBackoffMultiplier retry backoff multiplier 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param retryMaxDelayMinutes retry max delay minutes 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param downloadRateLimitPerMinute download rate limit per minute 参数，用于 BiSelfServiceExportService 流程中的校验、计算或对象转换。
     */
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      BiPermissionService permissionService,
                                      ObjectMapper objectMapper,
                                      Path exportRoot,
                                      BiFileStorage fileStorage,
                                      int retentionDays,
                                      int approvalRowThreshold,
                                      int maxRetryCount,
                                      long retryInitialDelayMinutes,
                                      double retryBackoffMultiplier,
                                      long retryMaxDelayMinutes,
                                      BiAuditLogMapper auditLogMapper,
                                      int downloadRateLimitPerMinute) {
        this.datasetMapper = datasetMapper;
        this.exportJobMapper = exportJobMapper;
        this.queryExecutionService = queryExecutionService;
        this.permissionService = permissionService;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.exportRoot = exportRoot;
        this.fileStorage = fileStorage == null ? new LocalBiFileStorage(exportRoot) : fileStorage;
        this.retentionDays = retentionDays;
        this.approvalRowThreshold = approvalRowThreshold;
        this.maxRetryCount = Math.max(0, maxRetryCount);
        this.retryInitialDelayMinutes = Math.max(1, retryInitialDelayMinutes);
        this.retryBackoffMultiplier = retryBackoffMultiplier <= 0 ? 1 : retryBackoffMultiplier;
        this.retryMaxDelayMinutes = Math.max(1, retryMaxDelayMinutes);
        this.downloadRateLimitPerMinute = Math.max(0, downloadRateLimitPerMinute);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 preview 流程生成的业务结果。
     */
    public BiQueryResult preview(Long tenantId, String username, String role, BiSelfServicePreviewRequest request) {
        if (request == null || request.query() == null) {
            throw new IllegalArgumentException("self-service preview query is required");
        }
        BiQueryRequest query = withLimit(request.query(), cappedLimit(request.previewLimit(), MAX_PREVIEW_LIMIT));
        return queryExecutionService.execute(query, new BiQueryContext(tenantId, username, role));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public BiExportJobView createExport(Long tenantId, String username, String role, BiExportJobCommand command) {
        if (command == null || command.query() == null) {
            throw new IllegalArgumentException("export query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiDatasetDO dataset = resolveDataset(scopedTenantId, command.query().datasetKey());
        String format = exportFormat(command.exportFormat());
        int rowLimit = cappedLimit(command.rowLimit(), MAX_EXPORT_LIMIT);
        BiQueryRequest query = withLimit(command.query(), rowLimit);
        enforceExportPermission(scopedTenantId, dataset, username, role);

        // Persist the normalized request snapshot so queue workers and retries do not depend on caller state.
        BiExportJobDO row = new BiExportJobDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(dataset.getWorkspaceId());
        row.setResourceType(RESOURCE_DATASET);
        row.setResourceId(dataset.getId());
        row.setExportFormat(format);
        row.setRequestJson(json(command));
        row.setRowLimit(rowLimit);
        row.setStatus(STATUS_QUEUED);
        row.setProgressPercent(PROGRESS_QUEUED);
        row.setRetentionDays(retentionDays > 0 ? retentionDays : null);
        row.setExpiresAt(retentionDays > 0 ? LocalDateTime.now().plusDays(retentionDays) : null);
        row.setDownloadCount(0);
        row.setRetryCount(0);
        row.setMaxRetryCount(maxRetryCount);
        row.setCreatedBy(defaultUser(username));
        if (requiresApproval(command, rowLimit)) {
            // Approval pauses the job before any data is queried or written.
            row.setStatus(STATUS_PENDING_APPROVAL);
            row.setApprovalStatus(APPROVAL_PENDING);
            row.setApprovalReason(approvalReason(command, rowLimit));
            row.setRequestedBy(defaultUser(username));
            row.setRequestedAt(LocalDateTime.now());
        }
        exportJobMapper.insert(row);
        if (row.getId() == null) {
            throw new IllegalStateException("BI export job was not persisted");
        }

        if (STATUS_PENDING_APPROVAL.equals(row.getStatus())) {
            return toView(row, dataset.getDatasetKey());
        }

        return toView(row, dataset.getDatasetKey());
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 reviewExport 流程生成的业务结果。
     */
    public BiExportJobView reviewExport(Long tenantId,
                                        String username,
                                        String role,
                                        Long exportId,
                                        BiExportApprovalReviewCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (exportId == null || exportId <= 0) {
            throw new IllegalArgumentException("exportId is required");
        }
        if (command == null) {
            throw new IllegalArgumentException("export review command is required");
        }
        requireApprover(role);
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !scopedTenantId.equals(normalizeTenant(row.getTenantId()))) {
            throw new IllegalArgumentException("BI export job not found: " + exportId);
        }
        if (!STATUS_PENDING_APPROVAL.equals(row.getStatus())
                || !APPROVAL_PENDING.equalsIgnoreCase(row.getApprovalStatus())) {
            throw new IllegalStateException("BI export job is not pending approval: " + exportId);
        }

        String status = reviewStatus(command.status());
        row.setApprovalStatus(status);
        row.setReviewedBy(defaultUser(username));
        row.setReviewedAt(LocalDateTime.now());
        row.setReviewComment(optionalText(command.reviewComment(), "reviewComment"));
        if (APPROVAL_REJECTED.equals(status)) {
            row.setStatus(STATUS_REJECTED);
            row.setErrorMessage("BI export rejected");
            exportJobMapper.updateById(row);
            return toView(row, datasetKey(row.getResourceId()));
        }

        row.setStatus(STATUS_QUEUED);
        row.setProgressPercent(PROGRESS_QUEUED);
        row.setErrorMessage(null);
        exportJobMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row, datasetKey(row.getResourceId()));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     */
    @Scheduled(fixedDelayString = "${canvas.bi.export.queue.fixed-delay-ms:60000}")
    public void scheduledExportQueueCycle() {
        if (!exportQueueEnabled) {
            return;
        }
        processQueuedExports(
                exportQueueTenantId,
                exportQueueOperator,
                exportQueueRole,
                exportQueueLimit);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 processQueuedExports 流程生成的业务结果。
     */
    public BiExportQueueResult processQueuedExports(Long tenantId, String username, String role, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
        // Jobs are processed oldest first so approval releases and retries preserve user-visible ordering.
        List<BiExportJobDO> queued = safeList(exportJobMapper.selectList(new LambdaQueryWrapper<BiExportJobDO>()
                .eq(BiExportJobDO::getTenantId, scopedTenantId)
                .eq(BiExportJobDO::getStatus, STATUS_QUEUED)
                .orderByAsc(BiExportJobDO::getCreatedAt)
                .orderByAsc(BiExportJobDO::getId)
                .last("LIMIT " + capped)));
        List<BiExportJobView> jobs = new ArrayList<>();
        int completed = 0;
        int failed = 0;
        for (BiExportJobDO row : queued) {
            BiExportJobView view = processQueuedExport(scopedTenantId, row, username, role);
            jobs.add(view);
            if (STATUS_COMPLETED.equals(view.status())) {
                completed++;
            // 根据前序判断结果进入后续条件分支。
            } else if (STATUS_FAILED.equals(view.status())) {
                failed++;
            }
        }
        return new BiExportQueueResult(queued.size(), jobs.size(), completed, failed, jobs);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回 processQueuedExport 流程生成的业务结果。
     */
    private BiExportJobView processQueuedExport(Long tenantId, BiExportJobDO row, String username, String role) {
        String resourceKey = datasetKey(row.getResourceId());
        try {
            BiExportJobCommand original = exportCommand(row);
            int exportLimit = cappedLimit(original.rowLimit(), MAX_EXPORT_LIMIT);
            BiDatasetDO dataset = datasetMapper.selectById(row.getResourceId());
            if (dataset != null) {
                enforceExportPermission(tenantId, dataset, username, role);
                resourceKey = dataset.getDatasetKey();
            }
            return runApprovedExport(tenantId, row, original.query(), exportLimit, username, role, resourceKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            scheduleRetryAfterFailure(row, LocalDateTime.now());
            exportJobMapper.updateById(row);
            return toView(row, resourceKey);
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param query query 参数，用于 runApprovedExport 流程中的校验、计算或对象转换。
     * @param exportLimit export limit 参数，用于 runApprovedExport 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    private BiExportJobView runApprovedExport(Long tenantId,
                                              BiExportJobDO row,
                                              BiQueryRequest query,
                                              int exportLimit,
                                              String username,
                                              String role,
                                              String resourceKey) {
        try {
            row.setStatus(STATUS_RUNNING);
            row.setProgressPercent(PROGRESS_RUNNING);
            exportJobMapper.updateById(row);
            BiQueryContext context = new BiQueryContext(tenantId, username, role);
            // Partition only CSV exports; JSON/XLSX/PDF must be generated from one bounded result set.
            BiStoredFile storedFile = shouldPartition(row.getExportFormat(), exportLimit)
                    /**
                     * 写入或更新业务数据，并保持关联状态一致。
                     *
                     * @param tenantId 租户 ID，用于限定数据隔离范围。
                     * @param query query 参数，用于 writePartitionedCsvFile 流程中的校验、计算或对象转换。
                     * @param exportLimit export limit 参数，用于 writePartitionedCsvFile 流程中的校验、计算或对象转换。
                     * @return 返回 writePartitionedCsvFile 流程生成的业务结果。
                     */
                    ? writePartitionedCsvFile(tenantId, row.getId(), query, exportLimit, context)
                    /**
                     * 写入或更新业务数据，并保持关联状态一致。
                     *
                     * @param tenantId 租户 ID，用于限定数据隔离范围。
                     * @return 返回 writeFile 流程生成的业务结果。
                     */
                    : writeFile(tenantId,
                            row.getId(),
                            row.getExportFormat(),
                            queryExecutionService.execute(withLimit(query, Math.min(exportLimit, SINGLE_FILE_EXPORT_LIMIT)), context));
            row.setStatus(STATUS_COMPLETED);
            row.setProgressPercent(PROGRESS_COMPLETED);
            row.setFileUrl("/canvas/bi/self-service/exports/" + row.getId() + "/download");
            row.setStorageProvider(storedFile.provider());
            row.setStorageKey(storedFile.key());
            row.setNextRetryAt(null);
            row.setRetryExhaustedAt(null);
            row.setErrorMessage(null);
            exportJobMapper.updateById(row);
            return toView(row, resourceKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            scheduleRetryAfterFailure(row, LocalDateTime.now());
            exportJobMapper.updateById(row);
            throw e;
        }
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public BiExportRetryResult retryFailedExports(Long tenantId, String username, String role, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiExportJobDO> retryable = safeList(exportJobMapper.selectList(new LambdaQueryWrapper<BiExportJobDO>()
                .eq(BiExportJobDO::getTenantId, scopedTenantId)
                .eq(BiExportJobDO::getStatus, STATUS_FAILED)
                .lt(BiExportJobDO::getRetryCount, maxRetryCount)
                .isNull(BiExportJobDO::getRetryExhaustedAt)
                .and(query -> query.isNull(BiExportJobDO::getNextRetryAt)
                        .or()
                        .le(BiExportJobDO::getNextRetryAt, LocalDateTime.now()))
                .orderByAsc(BiExportJobDO::getNextRetryAt)
                .orderByAsc(BiExportJobDO::getCreatedAt)
                .orderByAsc(BiExportJobDO::getId)
                .last("LIMIT " + capped)));
        List<BiExportJobView> jobs = new ArrayList<>();
        int completed = 0;
        int failed = 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiExportJobDO row : retryable) {
            BiExportJobView view = retryFailedExport(scopedTenantId, row, username, role);
            jobs.add(view);
            if (STATUS_COMPLETED.equals(view.status())) {
                completed++;
            } else {
                failed++;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiExportRetryResult(retryable.size(), jobs.size(), completed, failed, jobs);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    public BiExportJobView cancelExport(Long tenantId, String username, Long exportId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (exportId == null || exportId <= 0) {
            throw new IllegalArgumentException("exportId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !scopedTenantId.equals(normalizeTenant(row.getTenantId()))) {
            throw new IllegalArgumentException("BI export job not found: " + exportId);
        }
        if (!List.of(STATUS_QUEUED, STATUS_PENDING_APPROVAL, STATUS_FAILED).contains(row.getStatus())) {
            throw new IllegalStateException("BI export job cannot be canceled: " + exportId);
        }
        row.setStatus(STATUS_CANCELED);
        row.setProgressPercent(PROGRESS_COMPLETED);
        row.setFileUrl(null);
        row.setNextRetryAt(null);
        row.setRetryExhaustedAt(null);
        row.setErrorMessage("BI export canceled by " + defaultUser(username));
        exportJobMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row, datasetKey(row.getResourceId()));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回流程执行后的业务结果。
     */
    private BiExportJobView retryFailedExport(Long tenantId, BiExportJobDO row, String username, String role) {
        String resourceKey = datasetKey(row.getResourceId());
        try {
            int attempt = retryAttempt(row);
            row.setRetryCount(attempt);
            row.setMaxRetryCount(configuredMaxRetryCount(row));
            row.setLastRetryAt(LocalDateTime.now());
            row.setNextRetryAt(null);
            row.setRetryExhaustedAt(null);
            row.setProgressPercent(PROGRESS_QUEUED);
            exportJobMapper.updateById(row);

            BiExportJobCommand original = exportCommand(row);
            int exportLimit = cappedLimit(original.rowLimit(), MAX_EXPORT_LIMIT);
            BiDatasetDO dataset = datasetMapper.selectById(row.getResourceId());
            if (dataset != null) {
                enforceExportPermission(tenantId, dataset, username, role);
                resourceKey = dataset.getDatasetKey();
            }
            return runApprovedExport(tenantId, row, original.query(), exportLimit, username, role, resourceKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            scheduleRetryAfterFailure(row, LocalDateTime.now());
            exportJobMapper.updateById(row);
            return toView(row, resourceKey);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiExportJobView> listExports(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return safeList(exportJobMapper.selectList(new LambdaQueryWrapper<BiExportJobDO>()
                        .eq(BiExportJobDO::getTenantId, scopedTenantId)
                        .orderByDesc(BiExportJobDO::getCreatedAt)
                        .orderByDesc(BiExportJobDO::getId)
                        .last("LIMIT " + capped)))
                .stream()
                .map(row -> toView(row, datasetKey(row.getResourceId())))
                .toList();
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @return 返回 getExportDetail 流程生成的业务结果。
     */
    public BiExportJobDetailView getExportDetail(Long tenantId, Long exportId) {
        if (exportId == null) {
            throw new IllegalArgumentException("exportId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !scopedTenantId.equals(normalizeTenant(row.getTenantId()))) {
            throw new IllegalArgumentException("BI export job not found: " + exportId);
        }
        return new BiExportJobDetailView(
                toView(row, datasetKey(row.getResourceId())),
                exportCommand(row),
                partitionDownloadAudit(row));
    }

    /**
     * 执行 download 流程，围绕 download 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @return 返回 download 流程生成的业务结果。
     */
    public BiExportDownload download(Long tenantId, Long exportId) {
        return download(tenantId, null, exportId);
    }

    /**
     * 执行 download 流程，围绕 download 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @return 返回 download 流程生成的业务结果。
     */
    public BiExportDownload download(Long tenantId, String username, Long exportId) {
        if (exportId == null) {
            throw new IllegalArgumentException("exportId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String actor = defaultUser(username);
        BiExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !normalizeTenant(row.getTenantId()).equals(scopedTenantId)) {
            throw new IllegalArgumentException("BI export job not found: " + exportId);
        }
        if (!STATUS_COMPLETED.equals(row.getStatus())) {
            throw new IllegalStateException("BI export job is not ready: " + exportId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(row, now)) {
            deleteFile(row);
            markExpired(row, "BI export file expired");
            throw new IllegalStateException("BI export job has expired: " + exportId);
        }
        if (!tryAcquireDownload(scopedTenantId, actor)) {
            // Rate-limited attempts are audited even though the file bytes are never read.
            auditExport(row, actor, AUDIT_DOWNLOAD_RATE_LIMITED, Map.of(
                    "limitPerMinute", downloadRateLimitPerMinute,
                    /**
                     * 执行 optionalString 流程，围绕 optional string 完成校验、计算或结果组装。
                     *
                     * @return 返回 optionalString 流程生成的业务结果。
                     */
                    "storageKey", optionalString(row.getStorageKey()),
                    /**
                     * 执行 optionalString 流程，围绕 optional string 完成校验、计算或结果组装。
                     *
                     * @return 返回 optionalString 流程生成的业务结果。
                     */
                    "exportFormat", optionalString(row.getExportFormat())));
            throw new IllegalStateException("BI export download rate limit exceeded");
        }
        try {
            byte[] bytes = readFile(row);
            auditDownload(row, actor, now, bytes);
            return new BiExportDownload(
                    downloadFileName(row),
                    downloadContentType(row),
                    bytes);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            throw new IllegalStateException("BI export file is not available: " + exportId, e);
        }
    }

    /**
     * 执行 restoreExportObjects 流程，围绕 restore export objects 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param fallbackStorage fallback storage 参数，用于 restoreExportObjects 流程中的校验、计算或对象转换。
     * @return 返回 restoreExportObjects 流程生成的业务结果。
     */
    public BiExportObjectRestoreResult restoreExportObjects(
            Long tenantId,
            Long exportId,
            BiFileStorage fallbackStorage) {
        if (exportId == null) {
            throw new IllegalArgumentException("exportId is required");
        }
        if (fallbackStorage == null) {
            throw new IllegalArgumentException("fallbackStorage is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !normalizeTenant(row.getTenantId()).equals(scopedTenantId)) {
            throw new IllegalArgumentException("BI export job not found: " + exportId);
        }
        if (!STATUS_COMPLETED.equals(row.getStatus())) {
            throw new IllegalStateException("BI export job is not ready: " + exportId);
        }
        if (!hasText(row.getStorageKey())) {
            throw new IllegalStateException("BI export job does not use object storage: " + exportId);
        }

        // Restore the zip manifest first, then use it to recover any missing partition objects.
        List<String> checkedKeys = new ArrayList<>();
        List<String> restoredKeys = new ArrayList<>();
        List<String> missingKeys = new ArrayList<>();
        String rootKey = row.getStorageKey();
        checkedKeys.add(rootKey);
        byte[] rootBytes = restoreObjectIfMissing(rootKey, fallbackStorage, restoredKeys, missingKeys);
        if (rootBytes == null) {
            rootBytes = safeRead(fileStorage, rootKey);
        }
        for (String partKey : partitionManifestPartKeys(rootBytes)) {
            checkedKeys.add(partKey);
            restoreObjectIfMissing(partKey, fallbackStorage, restoredKeys, missingKeys);
        }
        return new BiExportObjectRestoreResult(
                row.getId(),
                fileStorage.provider(),
                fallbackStorage.provider(),
                checkedKeys.size(),
                restoredKeys.size(),
                missingKeys.size(),
                List.copyOf(restoredKeys),
                List.copyOf(missingKeys));
    }

    /**
     * 执行 cleanupExpiredExports 流程，围绕 cleanup expired exports 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 cleanupExpiredExports 流程生成的业务结果。
     */
    public BiExportCleanupResult cleanupExpiredExports(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 500));
        LocalDateTime now = LocalDateTime.now();
        List<BiExportJobDO> rows = safeList(exportJobMapper.selectList(new LambdaQueryWrapper<BiExportJobDO>()
                .eq(BiExportJobDO::getTenantId, scopedTenantId)
                .in(BiExportJobDO::getStatus, List.of(STATUS_COMPLETED, STATUS_FAILED))
                .isNotNull(BiExportJobDO::getExpiresAt)
                .le(BiExportJobDO::getExpiresAt, now)
                .orderByAsc(BiExportJobDO::getExpiresAt)
                .orderByAsc(BiExportJobDO::getId)
                .last("LIMIT " + capped)));
        int expired = 0;
        int filesDeleted = 0;
        int failed = 0;
        for (BiExportJobDO row : rows) {
            try {
                if (deleteFile(row)) {
                    filesDeleted++;
                }
                markExpired(row, "BI export file expired");
                expired++;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException e) {
                failed++;
                row.setErrorMessage(truncate(e.getMessage(), 1000));
                exportJobMapper.updateById(row);
            }
        }
        return new BiExportCleanupResult(rows.size(), expired, filesDeleted, failed);
    }

    /**
     * 执行 enforceExportPermission 流程，围绕 enforce export permission 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 enforceExportPermission 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     */
    private void enforceExportPermission(Long tenantId, BiDatasetDO dataset, String username, String role) {
        if (permissionService == null) {
            return;
        }
        permissionService.enforceResourceAccess(
                tenantId,
                dataset.getWorkspaceId(),
                RESOURCE_DATASET,
                dataset.getId(),
                new BiQueryContext(tenantId, username, role),
                BiPermissionService.ACTION_EXPORT);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @param result result 参数，用于 writeFile 流程中的校验、计算或对象转换。
     * @return 返回 writeFile 流程生成的业务结果。
     */
    private BiStoredFile writeFile(Long tenantId, Long exportId, String format, BiQueryResult result) {
        try {
            byte[] bytes = switch (format) {
                case "JSON" -> json(result.rows()).getBytes(StandardCharsets.UTF_8);
                case "XLSX" -> xlsx(result);
                case "PDF" -> pdf(result);
                default -> csv(result).getBytes(StandardCharsets.UTF_8);
            };
            return fileStorage.write(storageKey(tenantId, exportId, format), bytes);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI export file", e);
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param query query 参数，用于 writePartitionedCsvFile 流程中的校验、计算或对象转换。
     * @param exportLimit export limit 参数，用于 writePartitionedCsvFile 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 writePartitionedCsvFile 流程生成的业务结果。
     */
    private BiStoredFile writePartitionedCsvFile(Long tenantId,
                                                 Long exportId,
                                                 BiQueryRequest query,
                                                 int exportLimit,
                                                 BiQueryContext context) {
        int generatedRows = 0;
        List<CsvExportPart> parts = new ArrayList<>();
        try {
            // Query pages sequentially so each part has its own checksum and storage object.
            for (int offset = 0; offset < exportLimit; offset += SINGLE_FILE_EXPORT_LIMIT) {
                int pageLimit = Math.min(SINGLE_FILE_EXPORT_LIMIT, exportLimit - offset);
                BiQueryResult result = queryExecutionService.execute(withLimitAndOffset(query, pageLimit, offset), context);
                if (result.rowCount() <= 0 || result.rows().isEmpty()) {
                    break;
                }
                generatedRows += result.rowCount();
                String partName = partitionPartName(parts.size() + 1);
                byte[] bytes = csv(result).getBytes(StandardCharsets.UTF_8);
                String storageKey = partitionPartStorageKey(tenantId, exportId, partName);
                BiStoredFile storedPart = fileStorage.write(storageKey, bytes);
                parts.add(new CsvExportPart(
                        partName,
                        storageKey,
                        result.rowCount(),
                        storedPart.sizeBytes() == null ? (long) bytes.length : storedPart.sizeBytes(),
                        sha256Hex(bytes)));
                if (result.rowCount() < pageLimit) {
                    break;
                }
            }
            int finalGeneratedRows = generatedRows;
            // The downloadable zip contains the manifest plus every part object read back from storage.
            return fileStorage.write(partitionStorageKey(tenantId, exportId), output -> {
                try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                    writeZipEntry(zip, "manifest.json", partitionManifest(query, exportLimit, finalGeneratedRows, parts)
                            .getBytes(StandardCharsets.UTF_8));
                    for (CsvExportPart part : parts) {
                        writeZipEntry(zip, part.name(), fileStorage.read(part.storageKey()));
                    }
                    zip.finish();
                }
            });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            deleteGeneratedPartObjects(parts);
            throw e;
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param parts parts 参数，用于 deleteGeneratedPartObjects 流程中的校验、计算或对象转换。
     */
    private void deleteGeneratedPartObjects(List<CsvExportPart> parts) {
        for (CsvExportPart part : safeList(parts)) {
            try {
                fileStorage.delete(part.storageKey());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException ignored) {
                // Failed exports still need their retry state persisted; cleanup is best-effort.
            }
        }
    }

    /**
     * CsvExportPart 数据记录。
     */
    private record CsvExportPart(String name, String storageKey, int rowCount, long sizeBytes, String sha256) {
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param zip zip 参数，用于 writeZipEntry 流程中的校验、计算或对象转换。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param bytes bytes 参数，用于 writeZipEntry 流程中的校验、计算或对象转换。
     */
    private void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    /**
     * 执行 partitionManifest 流程，围绕 partition manifest 完成校验、计算或结果组装。
     *
     * @param query query 参数，用于 partitionManifest 流程中的校验、计算或对象转换。
     * @param requestedRows requested rows 参数，用于 partitionManifest 流程中的校验、计算或对象转换。
     * @param generatedRows generated rows 参数，用于 partitionManifest 流程中的校验、计算或对象转换。
     * @param parts parts 参数，用于 partitionManifest 流程中的校验、计算或对象转换。
     * @return 返回 partition manifest 生成的文本或业务键。
     */
    private String partitionManifest(BiQueryRequest query,
                                     int requestedRows,
                                     int generatedRows,
                                     List<CsvExportPart> parts) {
        return json(Map.of(
                "datasetKey", query.datasetKey(),
                "format", "CSV",
                "requestedRows", requestedRows,
                "generatedRows", generatedRows,
                "partCount", parts.size(),
                "partSize", SINGLE_FILE_EXPORT_LIMIT,
                "complete", generatedRows >= requestedRows,
                "storageLayout", "OBJECT_PER_PART_ZIP",
                "parts", parts.stream()
                        .map(part -> Map.of(
                                "name", part.name(),
                                "storageKey", part.storageKey(),
                                "rowCount", part.rowCount(),
                                "sizeBytes", part.sizeBytes(),
                                "sha256", part.sha256()))
                        .toList()));
    }

    /**
     * 执行 partitionPartName 流程，围绕 partition part name 完成校验、计算或结果组装。
     *
     * @param part part 参数，用于 partitionPartName 流程中的校验、计算或对象转换。
     * @return 返回 partition part name 生成的文本或业务键。
     */
    private String partitionPartName(int part) {
        return String.format(Locale.ROOT, "part-%05d.csv", part);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @param exportLimit export limit 参数，用于 shouldPartition 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean shouldPartition(String format, int exportLimit) {
        return "CSV".equals(exportFormat(format)) && exportLimit > SINGLE_FILE_EXPORT_LIMIT;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 readFile 流程生成的业务结果。
     */
    private byte[] readFile(BiExportJobDO row) {
        if (hasText(row.getStorageKey())) {
            // Object storage rows prefer storageKey; local paths are retained for legacy single-file exports.
            byte[] bytes = fileStorage.read(row.getStorageKey());
            if (bytes == null) {
                throw new IllegalStateException("BI export storage object is not available: " + row.getStorageKey());
            }
            return bytes;
        }
        try {
            return Files.readAllBytes(exportPath(row.getTenantId(), row.getId(), row.getExportFormat()));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("BI export file is not available: " + row.getId(), e);
        }
    }

    /**
     * 执行 csv 流程，围绕 csv 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 csv 流程中的校验、计算或对象转换。
     * @return 返回 csv 生成的文本或业务键。
     */
    private String csv(BiQueryResult result) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> columns = result.columns().stream().map(BiQueryColumn::key).toList();
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", columns.stream().map(this::csvCell).toList())).append('\n');
        for (Map<String, Object> row : result.rows()) {
            builder.append(String.join(",", columns.stream()
                    .map(column -> csvCell(row.get(column)))
                    .toList())).append('\n');
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return builder.toString();
    }

    /**
     * 执行 xlsx 流程，围绕 xlsx 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 xlsx 流程中的校验、计算或对象转换。
     * @return 返回 xlsx 流程生成的业务结果。
     */
    private byte[] xlsx(BiQueryResult result) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("BI Export");
            List<String> columns = result.columns().stream().map(BiQueryColumn::key).toList();
            // Style and freeze the header so downloaded workbooks remain usable for ad-hoc analysis.
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }
            for (int rowIndex = 0; rowIndex < result.rows().size(); rowIndex++) {
                Row sheetRow = sheet.createRow(rowIndex + 1);
                Map<String, Object> source = result.rows().get(rowIndex);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    Cell cell = sheetRow.createCell(columnIndex);
                    Object value = source.get(columns.get(columnIndex));
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }
            if (!columns.isEmpty()) {
                sheet.createFreezePane(0, 1);
                sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, result.rows().size()), 0, columns.size() - 1));
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    sheet.autoSizeColumn(columnIndex);
                    int readableWidth = Math.max(12, columns.get(columnIndex).length() + 2) * 256;
                    sheet.setColumnWidth(columnIndex, Math.min(40 * 256, Math.max(sheet.getColumnWidth(columnIndex), readableWidth)));
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    /**
     * 执行 pdf 流程，围绕 pdf 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 pdf 流程中的校验、计算或对象转换。
     * @return 返回 pdf 流程生成的业务结果。
     */
    private byte[] pdf(BiQueryResult result) {
        List<String> columns = result.columns().stream().map(BiQueryColumn::key).toList();
        List<String> lines = new ArrayList<>();
        lines.add("BI Self-Service Export");
        lines.add("Dataset: " + result.datasetKey());
        lines.add("Rows: " + result.rowCount());
        lines.add(String.join(" | ", columns));
        for (Map<String, Object> row : result.rows()) {
            lines.add(String.join(" | ", columns.stream()
                    .map(column -> String.valueOf(row.get(column) == null ? "" : row.get(column)))
                    .toList()));
        }
        int maxLinesPerPage = 34;
        int pageCount = Math.max(1, (int) Math.ceil(lines.size() / (double) maxLinesPerPage));
        List<String> objects = new ArrayList<>();
        // Build a small PDF directly; BI exports only need a readable tabular snapshot.
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int page = 0; page < pageCount; page++) {
            int pageObject = 3 + page * 2;
            kids.append(pageObject).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
        for (int page = 0; page < pageCount; page++) {
            int pageObject = 3 + page * 2;
            int contentObject = pageObject + 1;
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> "
                    + "/Contents " + contentObject + " 0 R >>");
            String content = pdfPageContent(lines, page, pageCount, maxLinesPerPage);
            objects.add("<< /Length " + content.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n"
                    + content + "\nendstream");
        }
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * 执行 pdfPageContent 流程，围绕 pdf page content 完成校验、计算或结果组装。
     *
     * @param lines lines 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @param pageIndex page index 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @param pageCount page count 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @param maxLinesPerPage max lines per page 参数，用于 pdfPageContent 流程中的校验、计算或对象转换。
     * @return 返回 pdf page content 生成的文本或业务键。
     */
    private String pdfPageContent(List<String> lines, int pageIndex, int pageCount, int maxLinesPerPage) {
        int start = pageIndex * maxLinesPerPage;
        int end = Math.min(lines.size(), start + maxLinesPerPage);
        StringBuilder content = new StringBuilder("BT\n/F1 10 Tf\n50 742 Td\n14 TL\n");
        for (int i = start; i < end; i++) {
            if (i > start) {
                content.append("T*\n");
            }
            content.append("(").append(pdfLiteral(lines.get(i))).append(") Tj\n");
        }
        content.append("ET\n");
        content.append("BT\n/F1 9 Tf\n270 32 Td\n(")
                .append(pdfLiteral("Page " + (pageIndex + 1) + " of " + pageCount))
                .append(") Tj\nET");
        return content.toString();
    }

    /**
     * 执行 pdfLiteral 流程，围绕 pdf literal 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 pdf literal 生成的文本或业务键。
     */
    private String pdfLiteral(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    /**
     * 执行 csvCell 流程，围绕 csv cell 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 csv cell 生成的文本或业务键。
     */
    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /**
     * 执行 sha256Hex 流程，围绕 sha256 hex 完成校验、计算或结果组装。
     *
     * @param bytes bytes 参数，用于 sha256Hex 流程中的校验、计算或对象转换。
     * @return 返回 sha256 hex 生成的文本或业务键。
     */
    private String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes == null ? new byte[0] : bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }

    /**
     * 执行 exportPath 流程，围绕 export path 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 exportPath 流程生成的业务结果。
     */
    private Path exportPath(Long tenantId, Long exportId, String format) {
        return exportRoot
                .resolve("tenant-" + normalizeTenant(tenantId))
                .resolve("export-" + exportId + "." + extension(format));
    }

    /**
     * 执行 storageKey 流程，围绕 storage key 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 storage key 生成的文本或业务键。
     */
    private String storageKey(Long tenantId, Long exportId, String format) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/" + exportFileName(exportId, format);
    }

    /**
     * 执行 partitionStorageKey 流程，围绕 partition storage key 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @return 返回 partition storage key 生成的文本或业务键。
     */
    private String partitionStorageKey(Long tenantId, Long exportId) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/export-" + exportId + ".zip";
    }

    /**
     * 执行 partitionPartStorageKey 流程，围绕 partition part storage key 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param partName 名称文本，用于展示或唯一性校验。
     * @return 返回 partition part storage key 生成的文本或业务键。
     */
    private String partitionPartStorageKey(Long tenantId, Long exportId, String partName) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/export-" + exportId + "/parts/" + partName;
    }

    /**
     * 执行 exportFileName 流程，围绕 export file name 完成校验、计算或结果组装。
     *
     * @param exportId 业务对象 ID，用于定位具体记录。
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 export file name 生成的文本或业务键。
     */
    private String exportFileName(Long exportId, String format) {
        return "export-" + exportId + "." + extension(format);
    }

    /**
     * 执行 extension 流程，围绕 extension 完成校验、计算或结果组装。
     *
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 extension 生成的文本或业务键。
     */
    private String extension(String format) {
        return switch (exportFormat(format)) {
            case "JSON" -> "json";
            case "XLSX" -> "xlsx";
            case "PDF" -> "pdf";
            default -> "csv";
        };
    }

    /**
     * 执行 contentType 流程，围绕 content type 完成校验、计算或结果组装。
     *
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 content type 生成的文本或业务键。
     */
    private String contentType(String format) {
        return switch (exportFormat(format)) {
            case "JSON" -> "application/json";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PDF" -> "application/pdf";
            default -> "text/csv; charset=UTF-8";
        };
    }

    /**
     * 执行 downloadFileName 流程，围绕 download file name 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 download file name 生成的文本或业务键。
     */
    private String downloadFileName(BiExportJobDO row) {
        if (hasText(row.getStorageKey()) && row.getStorageKey().endsWith(".zip")) {
            return "export-" + row.getId() + ".zip";
        }
        return exportFileName(row.getId(), row.getExportFormat());
    }

    /**
     * 执行 downloadContentType 流程，围绕 download content type 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 download content type 生成的文本或业务键。
     */
    private String downloadContentType(BiExportJobDO row) {
        if (hasText(row.getStorageKey()) && row.getStorageKey().endsWith(".zip")) {
            return "application/zip";
        }
        return contentType(row.getExportFormat());
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param query query 参数，用于 withLimit 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 withLimit 流程生成的业务结果。
     */
    private BiQueryRequest withLimit(BiQueryRequest query, int limit) {
        return withLimitAndOffset(query, limit, 0);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param query query 参数，用于 withLimitAndOffset 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param offset 分页或数量限制，避免一次处理过多数据。
     * @return 返回 withLimitAndOffset 流程生成的业务结果。
     */
    private BiQueryRequest withLimitAndOffset(BiQueryRequest query, int limit, int offset) {
        return new BiQueryRequest(
                query.datasetKey(),
                query.dashboardKey(),
                query.dimensions(),
                query.metrics(),
                query.filters(),
                query.sorts(),
                limit,
                offset);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param requested requested 参数，用于 cappedLimit 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 cappedLimit 流程中的校验、计算或对象转换。
     * @return 返回 capped limit 计算得到的数量、金额或指标值。
     */
    private int cappedLimit(Integer requested, int max) {
        if (requested == null || requested <= 0) {
            return Math.min(500, max);
        }
        return Math.max(1, Math.min(requested, max));
    }

    /**
     * 执行 exportFormat 流程，围绕 export format 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 export format 生成的文本或业务键。
     */
    private String exportFormat(String value) {
        String format = value == null || value.isBlank() ? "CSV" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CSV", "JSON", "XLSX", "PDF").contains(format)) {
            throw new IllegalArgumentException("unsupported export format: " + value);
        }
        return format;
    }

    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 resolveDataset 流程生成的业务结果。
     */
    private BiDatasetDO resolveDataset(Long tenantId, String datasetKey) {
        BiDatasetDO dataset = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, tenantScope(tenantId))
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
        if (dataset == null || dataset.getId() == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        return dataset;
    }

    /**
     * 执行 datasetKey 流程，围绕 dataset key 完成校验、计算或结果组装。
     *
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 dataset key 生成的文本或业务键。
     */
    private String datasetKey(Long datasetId) {
        if (datasetId == null) {
            return null;
        }
        BiDatasetDO dataset = datasetMapper.selectById(datasetId);
        return dataset == null ? null : dataset.getDatasetKey();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param rowLimit row limit 参数，用于 requiresApproval 流程中的校验、计算或对象转换。
     * @return 返回 requires approval 的布尔判断结果。
     */
    private boolean requiresApproval(BiExportJobCommand command, int rowLimit) {
        if (Boolean.TRUE.equals(command.approvalRequired()) || Boolean.TRUE.equals(command.sensitive())) {
            return true;
        }
        return approvalRowThreshold > 0 && rowLimit > approvalRowThreshold;
    }

    /**
     * 执行 approvalReason 流程，围绕 approval reason 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param rowLimit row limit 参数，用于 approvalReason 流程中的校验、计算或对象转换。
     * @return 返回 approval reason 生成的文本或业务键。
     */
    private String approvalReason(BiExportJobCommand command, int rowLimit) {
        // 准备本次处理所需的上下文和中间变量。
        String reason = optionalText(command.approvalReason(), "approvalReason");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (reason != null) {
            return reason;
        }
        if (Boolean.TRUE.equals(command.sensitive())) {
            return "sensitive export requires approval";
        }
        if (approvalRowThreshold > 0 && rowLimit > approvalRowThreshold) {
            return "row limit " + rowLimit + " exceeds approval threshold " + approvalRowThreshold;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "export approval required";
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     */
    private void requireApprover(String role) {
        String value = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!List.of(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN).contains(value)) {
            throw new IllegalStateException("BI export approval reviewer role is required");
        }
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 review status 生成的文本或业务键。
     */
    private String reviewStatus(String value) {
        String status = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(APPROVAL_APPROVED, APPROVAL_REJECTED).contains(status)) {
            throw new IllegalArgumentException("unsupported BI export review status: " + value);
        }
        return status;
    }

    /**
     * 执行 exportCommand 流程，围绕 export command 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 exportCommand 流程生成的业务结果。
     */
    private BiExportJobCommand exportCommand(BiExportJobDO row) {
        try {
            BiExportJobCommand command = objectMapper.readValue(row.getRequestJson(), BiExportJobCommand.class);
            if (command == null || command.query() == null) {
                throw new IllegalArgumentException("export query is required");
            }
            return new BiExportJobCommand(
                    command.resourceType(),
                    command.resourceKey(),
                    command.resourceId(),
                    command.exportFormat(),
                    normalizeRestoredQuery(command.query()),
                    command.rowLimit(),
                    command.approvalRequired(),
                    command.sensitive(),
                    command.approvalReason());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("BI export request cannot be restored: " + row.getId(), e);
        }
    }

    /**
     * 规范化输入值。
     *
     * @param query query 参数，用于 normalizeRestoredQuery 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private BiQueryRequest normalizeRestoredQuery(BiQueryRequest query) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQueryRequest(
                query.datasetKey(),
                query.dimensions(),
                query.metrics(),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                query.filters().stream()
                        .filter(filter -> filter != null)
                        .map(this::normalizeRestoredFilter)
                        .toList(),
                query.sorts(),
                query.limit());
    }

    /**
     * 规范化输入值。
     *
     * @param filter filter 参数，用于 normalizeRestoredFilter 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private BiFilter normalizeRestoredFilter(BiFilter filter) {
        return new BiFilter(filter.field(), filter.operator(), normalizeRestoredFilterValue(filter.value()));
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Object normalizeRestoredFilterValue(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        if (value instanceof List<?> values) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return values.stream().map(this::normalizeRestoredFilterValue).toList();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value;
    }

    /**
     * 执行 optionalText 流程，围绕 optional text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 optional text 生成的文本或业务键。
     */
    private String optionalText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        return trimmed;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回组装或转换后的结果对象。
     */
    private BiExportJobView toView(BiExportJobDO row, String resourceKey) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiExportJobView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                resourceKey,
                row.getResourceId(),
                row.getExportFormat(),
                row.getRowLimit(),
                row.getStatus(),
                row.getProgressPercent(),
                row.getFileUrl(),
                row.getStorageProvider(),
                row.getStorageKey(),
                row.getRetentionDays(),
                row.getExpiresAt(),
                row.getDownloadCount(),
                row.getLastDownloadedAt(),
                row.getApprovalStatus(),
                row.getApprovalReason(),
                row.getRequestedBy(),
                row.getRequestedAt(),
                row.getReviewedBy(),
                row.getReviewedAt(),
                row.getReviewComment(),
                row.getErrorMessage(),
                row.getRetryCount(),
                row.getMaxRetryCount(),
                row.getNextRetryAt(),
                row.getLastRetryAt(),
                row.getRetryExhaustedAt(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
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
            throw new IllegalArgumentException("invalid BI export payload", e);
        }
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     */
    private void scheduleRetryAfterFailure(BiExportJobDO row, LocalDateTime now) {
        int retryCount = retryCount(row);
        int rowMaxRetryCount = configuredMaxRetryCount(row);
        row.setMaxRetryCount(rowMaxRetryCount);
        if (row.getProgressPercent() == null) {
            row.setProgressPercent(PROGRESS_QUEUED);
        }
        if (rowMaxRetryCount <= 0 || retryCount >= rowMaxRetryCount) {
            row.setNextRetryAt(null);
            row.setRetryExhaustedAt(now);
            return;
        }
        // Retry delay is stored on the row so any worker can resume after process restart.
        row.setNextRetryAt(now.plusMinutes(retryDelayMinutes(retryCount + 1)));
        row.setRetryExhaustedAt(null);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    private int retryAttempt(BiExportJobDO row) {
        return retryCount(row) + 1;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    private int retryCount(BiExportJobDO row) {
        return Math.max(0, row == null || row.getRetryCount() == null ? 0 : row.getRetryCount());
    }

    /**
     * 执行 configuredMaxRetryCount 流程，围绕 configured max retry count 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 configured max retry count 计算得到的数量、金额或指标值。
     */
    private int configuredMaxRetryCount(BiExportJobDO row) {
        int rowMaxRetryCount = row == null || row.getMaxRetryCount() == null
                ? maxRetryCount
                : row.getMaxRetryCount();
        return Math.max(0, rowMaxRetryCount);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param nextAttempt next attempt 参数，用于 retryDelayMinutes 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private long retryDelayMinutes(int nextAttempt) {
        int attempt = Math.max(1, nextAttempt);
        double multiplier = Math.pow(retryBackoffMultiplier, attempt - 1);
        long delay = Math.round(retryInitialDelayMinutes * multiplier);
        return Math.max(1, Math.min(delay, retryMaxDelayMinutes));
    }

    /**
     * 执行 tenantScope 流程，围绕 tenant scope 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
     * 判断业务条件是否成立。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isExpired(BiExportJobDO row, LocalDateTime now) {
        return row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now);
    }

    /**
     * 执行 tryAcquireDownload 流程，围绕 try acquire download 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 try acquire download 的布尔判断结果。
     */
    private boolean tryAcquireDownload(Long tenantId, String actor) {
        if (downloadRateLimitPerMinute <= 0) {
            return true;
        }
        long windowMinute = System.currentTimeMillis() / 60_000L;
        String key = normalizeTenant(tenantId) + ":" + defaultUser(actor);
        // The in-memory window is a per-node throttle; audit logs still expose throttled attempts.
        synchronized (downloadRateWindows) {
            DownloadRateWindow current = downloadRateWindows.get(key);
            if (current == null || current.windowMinute() != windowMinute) {
                downloadRateWindows.put(key, new DownloadRateWindow(windowMinute, 1));
                return true;
            }
            if (current.count() >= downloadRateLimitPerMinute) {
                return false;
            }
            downloadRateWindows.put(key, new DownloadRateWindow(windowMinute, current.count() + 1));
            return true;
        }
    }

    /**
     * 执行 auditDownload 流程，围绕 audit download 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param bytes bytes 参数，用于 auditDownload 流程中的校验、计算或对象转换。
     */
    private void auditDownload(BiExportJobDO row, String actor, LocalDateTime now, byte[] bytes) {
        // 准备本次处理所需的上下文和中间变量。
        int downloadCount = (row.getDownloadCount() == null ? 0 : row.getDownloadCount()) + 1;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiExportJobDO update = new BiExportJobDO();
        update.setId(row.getId());
        update.setDownloadCount(downloadCount);
        update.setLastDownloadedAt(now);
        exportJobMapper.updateById(update);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("exportFormat", optionalString(row.getExportFormat()));
        detail.put("filename", downloadFileName(row));
        detail.put("contentType", downloadContentType(row));
        detail.put("storageProvider", optionalString(row.getStorageProvider()));
        detail.put("storageKey", optionalString(row.getStorageKey()));
        detail.put("sizeBytes", bytes == null ? 0 : bytes.length);
        detail.put("downloadCount", downloadCount);
        Map<String, Object> partition = partitionDownloadAudit(row);
        if (!partition.isEmpty()) {
            detail.put("partition", partition);
        }
        auditExport(row, actor, AUDIT_DOWNLOAD, detail);
    }

    /**
     * 执行 partitionDownloadAudit 流程，围绕 partition download audit 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 partitionDownloadAudit 流程生成的业务结果。
     */
    private Map<String, Object> partitionDownloadAudit(BiExportJobDO row) {
        if (row == null || !hasText(row.getStorageKey()) || !row.getStorageKey().endsWith(".zip")) {
            return Map.of();
        }
        try {
            byte[] bytes = fileStorage.read(row.getStorageKey());
            if (bytes == null || bytes.length == 0) {
                return Map.of();
            }
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if ("manifest.json".equals(entry.getName())) {
                        JsonNode manifest = objectMapper.readTree(zip);
                        List<Map<String, Object>> parts = new ArrayList<>();
                        JsonNode manifestParts = manifest.path("parts");
                        if (manifestParts.isArray()) {
                            for (JsonNode part : manifestParts) {
                                Map<String, Object> partAudit = new LinkedHashMap<>();
                                partAudit.put("name", part.path("name").asText(""));
                                partAudit.put("storageKey", part.path("storageKey").asText(""));
                                partAudit.put("rowCount", part.path("rowCount").asInt(0));
                                partAudit.put("sizeBytes", part.path("sizeBytes").asLong(0));
                                partAudit.put("sha256", part.path("sha256").asText(""));
                                parts.add(partAudit);
                            }
                        }
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("storageLayout", manifest.path("storageLayout").asText(""));
                        detail.put("requestedRows", manifest.path("requestedRows").asInt(0));
                        detail.put("generatedRows", manifest.path("generatedRows").asInt(0));
                        detail.put("partCount", manifest.path("partCount").asInt(parts.size()));
                        detail.put("partSize", manifest.path("partSize").asInt(0));
                        detail.put("partStorageKeys", parts.stream()
                                .map(part -> String.valueOf(part.get("storageKey")))
                                .filter(this::hasText)
                                .toList());
                        detail.put("parts", parts);
                        return detail;
                    }
                }
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
        return Map.of();
    }

    /**
     * 执行 auditExport 流程，围绕 audit export 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @param String string 参数，用于 auditExport 流程中的校验、计算或对象转换。
     * @param detail detail 参数，用于 auditExport 流程中的校验、计算或对象转换。
     */
    private void auditExport(BiExportJobDO row, String actor, String actionKey, Map<String, Object> detail) {
        if (auditLogMapper == null || row == null) {
            return;
        }
        BiAuditLogDO audit = new BiAuditLogDO();
        audit.setTenantId(normalizeTenant(row.getTenantId()));
        audit.setWorkspaceId(row.getWorkspaceId());
        audit.setActorId(defaultUser(actor));
        audit.setActionKey(actionKey);
        audit.setResourceType(AUDIT_RESOURCE_EXPORT_JOB);
        audit.setResourceId(row.getId());
        audit.setDetailJson(json(detail));
        audit.setCreatedAt(LocalDateTime.now());
        try {
            auditLogMapper.insert(audit);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            // Export governance decisions should not depend on audit storage availability.
        }
    }

    /**
     * 执行 optionalString 流程，围绕 optional string 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional string 生成的文本或业务键。
     */
    private String optionalString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private void markExpired(BiExportJobDO row, String message) {
        row.setStatus(STATUS_EXPIRED);
        row.setErrorMessage(message);
        exportJobMapper.updateById(row);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 delete file 的布尔判断结果。
     */
    private boolean deleteFile(BiExportJobDO row) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row != null && hasText(row.getStorageKey())) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            boolean partDeleted = deletePartitionPartObjects(row);
            boolean rootDeleted = fileStorage.delete(row.getStorageKey());
            return rootDeleted || partDeleted;
        }
        return deleteLocalFile(row);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 delete partition part objects 的布尔判断结果。
     */
    private boolean deletePartitionPartObjects(BiExportJobDO row) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || !hasText(row.getStorageKey()) || !row.getStorageKey().endsWith(".zip")) {
            return false;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean deleted = false;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String partKey : partitionManifestPartKeys(row.getStorageKey())) {
            if (fileStorage.delete(partKey)) {
                deleted = true;
            }
        }
        return deleted;
    }

    /**
     * 执行 partitionManifestPartKeys 流程，围绕 partition manifest part keys 完成校验、计算或结果组装。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @return 返回 partition manifest part keys 汇总后的集合、分页或映射视图。
     */
    private List<String> partitionManifestPartKeys(String storageKey) {
        try {
            byte[] bytes = fileStorage.read(storageKey);
            if (bytes == null || bytes.length == 0) {
                return List.of();
            }
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if ("manifest.json".equals(entry.getName())) {
                        JsonNode manifest = objectMapper.readTree(zip);
                        JsonNode parts = manifest.path("parts");
                        if (!parts.isArray()) {
                            return List.of();
                        }
                        List<String> keys = new ArrayList<>();
                        for (JsonNode part : parts) {
                            String partKey = part.path("storageKey").asText(null);
                            if (hasText(partKey)) {
                                keys.add(partKey);
                            }
                        }
                        return keys;
                    }
                }
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        return List.of();
    }

    /**
     * 执行 partitionManifestPartKeys 流程，围绕 partition manifest part keys 完成校验、计算或结果组装。
     *
     * @param bytes bytes 参数，用于 partitionManifestPartKeys 流程中的校验、计算或对象转换。
     * @return 返回 partition manifest part keys 汇总后的集合、分页或映射视图。
     */
    private List<String> partitionManifestPartKeys(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    JsonNode manifest = objectMapper.readTree(zip);
                    JsonNode parts = manifest.path("parts");
                    if (!parts.isArray()) {
                        return List.of();
                    }
                    List<String> keys = new ArrayList<>();
                    for (JsonNode part : parts) {
                        String partKey = part.path("storageKey").asText(null);
                        if (hasText(partKey)) {
                            keys.add(partKey);
                        }
                    }
                    return keys;
                }
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        return List.of();
    }

    /**
     * 执行 restoreObjectIfMissing 流程，围绕 restore object if missing 完成校验、计算或结果组装。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @param fallbackStorage fallback storage 参数，用于 restoreObjectIfMissing 流程中的校验、计算或对象转换。
     * @param restoredKeys restored keys 参数，用于 restoreObjectIfMissing 流程中的校验、计算或对象转换。
     * @param missingKeys missing keys 参数，用于 restoreObjectIfMissing 流程中的校验、计算或对象转换。
     * @return 返回 restoreObjectIfMissing 流程生成的业务结果。
     */
    private byte[] restoreObjectIfMissing(
            String storageKey,
            BiFileStorage fallbackStorage,
            List<String> restoredKeys,
            List<String> missingKeys) {
        byte[] current = safeRead(fileStorage, storageKey);
        if (current != null) {
            return current;
        }
        byte[] fallbackBytes = safeRead(fallbackStorage, storageKey);
        if (fallbackBytes == null) {
            missingKeys.add(storageKey);
            return null;
        }
        fileStorage.write(storageKey, fallbackBytes);
        restoredKeys.add(storageKey);
        return fallbackBytes;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param storage storage 参数，用于 safeRead 流程中的校验、计算或对象转换。
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @return 返回 safeRead 流程生成的业务结果。
     */
    private byte[] safeRead(BiFileStorage storage, String storageKey) {
        try {
            return storage.read(storageKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 delete local file 的布尔判断结果。
     */
    private boolean deleteLocalFile(BiExportJobDO row) {
        if (row == null || row.getId() == null || row.getExportFormat() == null) {
            return false;
        }
        try {
            Path root = exportRoot.toAbsolutePath().normalize();
            Path file = exportPath(row.getTenantId(), row.getId(), row.getExportFormat()).toAbsolutePath().normalize();
            if (!file.startsWith(root)) {
                // Never delete outside the configured export root, even if persisted metadata is corrupt.
                return false;
            }
            boolean deleted = Files.deleteIfExists(file);
            deleteEmptyParents(file.getParent(), root);
            return deleted;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete BI export file: " + row.getId(), e);
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
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 truncate 流程，围绕 truncate 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 truncate 流程中的校验、计算或对象转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * DownloadRateWindow 数据记录。
     */
    private record DownloadRateWindow(long windowMinute, int count) {
    }
}
