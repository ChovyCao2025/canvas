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

    public BiQueryResult preview(Long tenantId, String username, String role, BiSelfServicePreviewRequest request) {
        if (request == null || request.query() == null) {
            throw new IllegalArgumentException("self-service preview query is required");
        }
        BiQueryRequest query = withLimit(request.query(), cappedLimit(request.previewLimit(), MAX_PREVIEW_LIMIT));
        return queryExecutionService.execute(query, new BiQueryContext(tenantId, username, role));
    }

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

    public BiExportJobView reviewExport(Long tenantId,
                                        String username,
                                        String role,
                                        Long exportId,
                                        BiExportApprovalReviewCommand command) {
        if (exportId == null || exportId <= 0) {
            throw new IllegalArgumentException("exportId is required");
        }
        if (command == null) {
            throw new IllegalArgumentException("export review command is required");
        }
        requireApprover(role);
        Long scopedTenantId = normalizeTenant(tenantId);
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
        return toView(row, datasetKey(row.getResourceId()));
    }

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

    public BiExportQueueResult processQueuedExports(Long tenantId, String username, String role, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
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
            } else if (STATUS_FAILED.equals(view.status())) {
                failed++;
            }
        }
        return new BiExportQueueResult(queued.size(), jobs.size(), completed, failed, jobs);
    }

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
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            scheduleRetryAfterFailure(row, LocalDateTime.now());
            exportJobMapper.updateById(row);
            return toView(row, resourceKey);
        }
    }

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
            BiStoredFile storedFile = shouldPartition(row.getExportFormat(), exportLimit)
                    ? writePartitionedCsvFile(tenantId, row.getId(), query, exportLimit, context)
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
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            scheduleRetryAfterFailure(row, LocalDateTime.now());
            exportJobMapper.updateById(row);
            throw e;
        }
    }

    public BiExportRetryResult retryFailedExports(Long tenantId, String username, String role, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
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
        for (BiExportJobDO row : retryable) {
            BiExportJobView view = retryFailedExport(scopedTenantId, row, username, role);
            jobs.add(view);
            if (STATUS_COMPLETED.equals(view.status())) {
                completed++;
            } else {
                failed++;
            }
        }
        return new BiExportRetryResult(retryable.size(), jobs.size(), completed, failed, jobs);
    }

    public BiExportJobView cancelExport(Long tenantId, String username, Long exportId) {
        if (exportId == null || exportId <= 0) {
            throw new IllegalArgumentException("exportId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
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
        return toView(row, datasetKey(row.getResourceId()));
    }

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
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            scheduleRetryAfterFailure(row, LocalDateTime.now());
            exportJobMapper.updateById(row);
            return toView(row, resourceKey);
        }
    }

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
                exportCommand(row));
    }

    public BiExportDownload download(Long tenantId, Long exportId) {
        return download(tenantId, null, exportId);
    }

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
            auditExport(row, actor, AUDIT_DOWNLOAD_RATE_LIMITED, Map.of(
                    "limitPerMinute", downloadRateLimitPerMinute,
                    "storageKey", optionalString(row.getStorageKey()),
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
        } catch (RuntimeException e) {
            throw new IllegalStateException("BI export file is not available: " + exportId, e);
        }
    }

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
            } catch (RuntimeException e) {
                failed++;
                row.setErrorMessage(truncate(e.getMessage(), 1000));
                exportJobMapper.updateById(row);
            }
        }
        return new BiExportCleanupResult(rows.size(), expired, filesDeleted, failed);
    }

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

    private BiStoredFile writeFile(Long tenantId, Long exportId, String format, BiQueryResult result) {
        try {
            byte[] bytes = switch (format) {
                case "JSON" -> json(result.rows()).getBytes(StandardCharsets.UTF_8);
                case "XLSX" -> xlsx(result);
                case "PDF" -> pdf(result);
                default -> csv(result).getBytes(StandardCharsets.UTF_8);
            };
            return fileStorage.write(storageKey(tenantId, exportId, format), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI export file", e);
        }
    }

    private BiStoredFile writePartitionedCsvFile(Long tenantId,
                                                 Long exportId,
                                                 BiQueryRequest query,
                                                 int exportLimit,
                                                 BiQueryContext context) {
        int generatedRows = 0;
        List<CsvExportPart> parts = new ArrayList<>();
        try {
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
        } catch (RuntimeException e) {
            deleteGeneratedPartObjects(parts);
            throw e;
        }
    }

    private void deleteGeneratedPartObjects(List<CsvExportPart> parts) {
        for (CsvExportPart part : safeList(parts)) {
            try {
                fileStorage.delete(part.storageKey());
            } catch (RuntimeException ignored) {
                // Failed exports still need their retry state persisted; cleanup is best-effort.
            }
        }
    }

    private record CsvExportPart(String name, String storageKey, int rowCount, long sizeBytes, String sha256) {
    }

    private void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

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

    private String partitionPartName(int part) {
        return String.format(Locale.ROOT, "part-%05d.csv", part);
    }

    private boolean shouldPartition(String format, int exportLimit) {
        return "CSV".equals(exportFormat(format)) && exportLimit > SINGLE_FILE_EXPORT_LIMIT;
    }

    private byte[] readFile(BiExportJobDO row) {
        if (hasText(row.getStorageKey())) {
            byte[] bytes = fileStorage.read(row.getStorageKey());
            if (bytes == null) {
                throw new IllegalStateException("BI export storage object is not available: " + row.getStorageKey());
            }
            return bytes;
        }
        try {
            return Files.readAllBytes(exportPath(row.getTenantId(), row.getId(), row.getExportFormat()));
        } catch (IOException e) {
            throw new IllegalStateException("BI export file is not available: " + row.getId(), e);
        }
    }

    private String csv(BiQueryResult result) {
        List<String> columns = result.columns().stream().map(BiQueryColumn::key).toList();
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", columns.stream().map(this::csvCell).toList())).append('\n');
        for (Map<String, Object> row : result.rows()) {
            builder.append(String.join(",", columns.stream()
                    .map(column -> csvCell(row.get(column)))
                    .toList())).append('\n');
        }
        return builder.toString();
    }

    private byte[] xlsx(BiQueryResult result) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("BI Export");
            List<String> columns = result.columns().stream().map(BiQueryColumn::key).toList();
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

    private String pdfLiteral(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes == null ? new byte[0] : bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }

    private Path exportPath(Long tenantId, Long exportId, String format) {
        return exportRoot
                .resolve("tenant-" + normalizeTenant(tenantId))
                .resolve("export-" + exportId + "." + extension(format));
    }

    private String storageKey(Long tenantId, Long exportId, String format) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/" + exportFileName(exportId, format);
    }

    private String partitionStorageKey(Long tenantId, Long exportId) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/export-" + exportId + ".zip";
    }

    private String partitionPartStorageKey(Long tenantId, Long exportId, String partName) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/export-" + exportId + "/parts/" + partName;
    }

    private String exportFileName(Long exportId, String format) {
        return "export-" + exportId + "." + extension(format);
    }

    private String extension(String format) {
        return switch (exportFormat(format)) {
            case "JSON" -> "json";
            case "XLSX" -> "xlsx";
            case "PDF" -> "pdf";
            default -> "csv";
        };
    }

    private String contentType(String format) {
        return switch (exportFormat(format)) {
            case "JSON" -> "application/json";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PDF" -> "application/pdf";
            default -> "text/csv; charset=UTF-8";
        };
    }

    private String downloadFileName(BiExportJobDO row) {
        if (hasText(row.getStorageKey()) && row.getStorageKey().endsWith(".zip")) {
            return "export-" + row.getId() + ".zip";
        }
        return exportFileName(row.getId(), row.getExportFormat());
    }

    private String downloadContentType(BiExportJobDO row) {
        if (hasText(row.getStorageKey()) && row.getStorageKey().endsWith(".zip")) {
            return "application/zip";
        }
        return contentType(row.getExportFormat());
    }

    private BiQueryRequest withLimit(BiQueryRequest query, int limit) {
        return withLimitAndOffset(query, limit, 0);
    }

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

    private int cappedLimit(Integer requested, int max) {
        if (requested == null || requested <= 0) {
            return Math.min(500, max);
        }
        return Math.max(1, Math.min(requested, max));
    }

    private String exportFormat(String value) {
        String format = value == null || value.isBlank() ? "CSV" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CSV", "JSON", "XLSX", "PDF").contains(format)) {
            throw new IllegalArgumentException("unsupported export format: " + value);
        }
        return format;
    }

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

    private String datasetKey(Long datasetId) {
        if (datasetId == null) {
            return null;
        }
        BiDatasetDO dataset = datasetMapper.selectById(datasetId);
        return dataset == null ? null : dataset.getDatasetKey();
    }

    private boolean requiresApproval(BiExportJobCommand command, int rowLimit) {
        if (Boolean.TRUE.equals(command.approvalRequired()) || Boolean.TRUE.equals(command.sensitive())) {
            return true;
        }
        return approvalRowThreshold > 0 && rowLimit > approvalRowThreshold;
    }

    private String approvalReason(BiExportJobCommand command, int rowLimit) {
        String reason = optionalText(command.approvalReason(), "approvalReason");
        if (reason != null) {
            return reason;
        }
        if (Boolean.TRUE.equals(command.sensitive())) {
            return "sensitive export requires approval";
        }
        if (approvalRowThreshold > 0 && rowLimit > approvalRowThreshold) {
            return "row limit " + rowLimit + " exceeds approval threshold " + approvalRowThreshold;
        }
        return "export approval required";
    }

    private void requireApprover(String role) {
        String value = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!List.of(RoleNames.ADMIN, RoleNames.SUPER_ADMIN, RoleNames.TENANT_ADMIN).contains(value)) {
            throw new IllegalStateException("BI export approval reviewer role is required");
        }
    }

    private String reviewStatus(String value) {
        String status = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(APPROVAL_APPROVED, APPROVAL_REJECTED).contains(status)) {
            throw new IllegalArgumentException("unsupported BI export review status: " + value);
        }
        return status;
    }

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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("BI export request cannot be restored: " + row.getId(), e);
        }
    }

    private BiQueryRequest normalizeRestoredQuery(BiQueryRequest query) {
        return new BiQueryRequest(
                query.datasetKey(),
                query.dimensions(),
                query.metrics(),
                query.filters().stream()
                        .filter(filter -> filter != null)
                        .map(this::normalizeRestoredFilter)
                        .toList(),
                query.sorts(),
                query.limit());
    }

    private BiFilter normalizeRestoredFilter(BiFilter filter) {
        return new BiFilter(filter.field(), filter.operator(), normalizeRestoredFilterValue(filter.value()));
    }

    private Object normalizeRestoredFilterValue(Object value) {
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        if (value instanceof List<?> values) {
            return values.stream().map(this::normalizeRestoredFilterValue).toList();
        }
        return value;
    }

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

    private BiExportJobView toView(BiExportJobDO row, String resourceKey) {
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
                row.getUpdatedAt());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI export payload", e);
        }
    }

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
        row.setNextRetryAt(now.plusMinutes(retryDelayMinutes(retryCount + 1)));
        row.setRetryExhaustedAt(null);
    }

    private int retryAttempt(BiExportJobDO row) {
        return retryCount(row) + 1;
    }

    private int retryCount(BiExportJobDO row) {
        return Math.max(0, row == null || row.getRetryCount() == null ? 0 : row.getRetryCount());
    }

    private int configuredMaxRetryCount(BiExportJobDO row) {
        int rowMaxRetryCount = row == null || row.getMaxRetryCount() == null
                ? maxRetryCount
                : row.getMaxRetryCount();
        return Math.max(0, rowMaxRetryCount);
    }

    private long retryDelayMinutes(int nextAttempt) {
        int attempt = Math.max(1, nextAttempt);
        double multiplier = Math.pow(retryBackoffMultiplier, attempt - 1);
        long delay = Math.round(retryInitialDelayMinutes * multiplier);
        return Math.max(1, Math.min(delay, retryMaxDelayMinutes));
    }

    private List<Long> tenantScope(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (scopedTenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, scopedTenantId);
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private boolean isExpired(BiExportJobDO row, LocalDateTime now) {
        return row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now);
    }

    private boolean tryAcquireDownload(Long tenantId, String actor) {
        if (downloadRateLimitPerMinute <= 0) {
            return true;
        }
        long windowMinute = System.currentTimeMillis() / 60_000L;
        String key = normalizeTenant(tenantId) + ":" + defaultUser(actor);
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

    private void auditDownload(BiExportJobDO row, String actor, LocalDateTime now, byte[] bytes) {
        int downloadCount = (row.getDownloadCount() == null ? 0 : row.getDownloadCount()) + 1;
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
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
        return Map.of();
    }

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
        } catch (RuntimeException ignored) {
            // Export governance decisions should not depend on audit storage availability.
        }
    }

    private String optionalString(String value) {
        return value == null ? "" : value;
    }

    private void markExpired(BiExportJobDO row, String message) {
        row.setStatus(STATUS_EXPIRED);
        row.setErrorMessage(message);
        exportJobMapper.updateById(row);
    }

    private boolean deleteFile(BiExportJobDO row) {
        if (row != null && hasText(row.getStorageKey())) {
            boolean partDeleted = deletePartitionPartObjects(row);
            boolean rootDeleted = fileStorage.delete(row.getStorageKey());
            return rootDeleted || partDeleted;
        }
        return deleteLocalFile(row);
    }

    private boolean deletePartitionPartObjects(BiExportJobDO row) {
        if (row == null || !hasText(row.getStorageKey()) || !row.getStorageKey().endsWith(".zip")) {
            return false;
        }
        boolean deleted = false;
        for (String partKey : partitionManifestPartKeys(row.getStorageKey())) {
            if (fileStorage.delete(partKey)) {
                deleted = true;
            }
        }
        return deleted;
    }

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
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        return List.of();
    }

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
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        return List.of();
    }

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

    private byte[] safeRead(BiFileStorage storage, String storageKey) {
        try {
            return storage.read(storageKey);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean deleteLocalFile(BiExportJobDO row) {
        if (row == null || row.getId() == null || row.getExportFormat() == null) {
            return false;
        }
        try {
            Path root = exportRoot.toAbsolutePath().normalize();
            Path file = exportPath(row.getTenantId(), row.getId(), row.getExportFormat()).toAbsolutePath().normalize();
            if (!file.startsWith(root)) {
                return false;
            }
            boolean deleted = Files.deleteIfExists(file);
            deleteEmptyParents(file.getParent(), root);
            return deleted;
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete BI export file: " + row.getId(), e);
        }
    }

    private void deleteEmptyParents(Path parent, Path root) throws IOException {
        Path cursor = parent;
        while (cursor != null && cursor.startsWith(root) && !cursor.equals(root)) {
            try {
                Files.deleteIfExists(cursor);
            } catch (DirectoryNotEmptyException e) {
                return;
            }
            cursor = cursor.getParent();
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private record DownloadRateWindow(long windowMinute, int count) {
    }
}
