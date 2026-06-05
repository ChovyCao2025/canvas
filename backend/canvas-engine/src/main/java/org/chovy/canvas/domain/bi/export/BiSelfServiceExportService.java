package org.chovy.canvas.domain.bi.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiExportJobDO;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiExportJobMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BiSelfServiceExportService {

    private static final String RESOURCE_DATASET = "DATASET";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final int MAX_PREVIEW_LIMIT = 500;
    private static final int MAX_EXPORT_LIMIT = 10000;

    private final BiDatasetMapper datasetMapper;
    private final BiExportJobMapper exportJobMapper;
    private final BiQueryExecutionService queryExecutionService;
    private final BiPermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;
    private final BiFileStorage fileStorage;
    private final int retentionDays;
    private final int approvalRowThreshold;

    @Autowired
    public BiSelfServiceExportService(BiDatasetMapper datasetMapper,
                                      BiExportJobMapper exportJobMapper,
                                      BiQueryExecutionService queryExecutionService,
                                      ObjectProvider<BiPermissionService> permissionServiceProvider,
                                      ObjectMapper objectMapper,
                                      ObjectProvider<BiFileStorage> storageProvider,
                                      @Value("${canvas.bi.export.dir:${java.io.tmpdir}/canvas-bi-exports}") String exportDir,
                                      @Value("${canvas.bi.export.retention-days:7}") int retentionDays,
                                      @Value("${canvas.bi.export.approval.row-threshold:5000}") int approvalRowThreshold) {
        this(datasetMapper,
                exportJobMapper,
                queryExecutionService,
                permissionServiceProvider.getIfAvailable(),
                objectMapper,
                Path.of(exportDir),
                storageProvider == null ? null : storageProvider.getIfAvailable(),
                retentionDays,
                approvalRowThreshold);
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
                approvalRowThreshold);
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
        this.datasetMapper = datasetMapper;
        this.exportJobMapper = exportJobMapper;
        this.queryExecutionService = queryExecutionService;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.exportRoot = exportRoot;
        this.fileStorage = fileStorage == null ? new LocalBiFileStorage(exportRoot) : fileStorage;
        this.retentionDays = retentionDays;
        this.approvalRowThreshold = approvalRowThreshold;
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
        row.setRetentionDays(retentionDays > 0 ? retentionDays : null);
        row.setExpiresAt(retentionDays > 0 ? LocalDateTime.now().plusDays(retentionDays) : null);
        row.setDownloadCount(0);
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

        return runApprovedExport(scopedTenantId, row, query, username, role, dataset.getDatasetKey());
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

        exportJobMapper.updateById(row);
        BiExportJobCommand original = exportCommand(row);
        BiQueryRequest query = withLimit(original.query(), cappedLimit(original.rowLimit(), MAX_EXPORT_LIMIT));
        return runApprovedExport(
                scopedTenantId,
                row,
                query,
                defaultUser(row.getCreatedBy()),
                RoleNames.OPERATOR,
                datasetKey(row.getResourceId()));
    }

    private BiExportJobView runApprovedExport(Long tenantId,
                                              BiExportJobDO row,
                                              BiQueryRequest query,
                                              String username,
                                              String role,
                                              String resourceKey) {
        try {
            row.setStatus(STATUS_RUNNING);
            exportJobMapper.updateById(row);
            BiQueryResult result = queryExecutionService.execute(query, new BiQueryContext(tenantId, username, role));
            BiStoredFile storedFile = writeFile(tenantId, row.getId(), row.getExportFormat(), result);
            row.setStatus(STATUS_COMPLETED);
            row.setFileUrl("/canvas/bi/self-service/exports/" + row.getId() + "/download");
            row.setStorageProvider(storedFile.provider());
            row.setStorageKey(storedFile.key());
            row.setErrorMessage(null);
            exportJobMapper.updateById(row);
            return toView(row, resourceKey);
        } catch (RuntimeException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage(), 1000));
            exportJobMapper.updateById(row);
            throw e;
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

    public BiExportDownload download(Long tenantId, Long exportId) {
        if (exportId == null) {
            throw new IllegalArgumentException("exportId is required");
        }
        BiExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !normalizeTenant(row.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI export job not found: " + exportId);
        }
        if (!STATUS_COMPLETED.equals(row.getStatus())) {
            throw new IllegalStateException("BI export job is not ready: " + exportId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(row, now)) {
            markExpired(row, "BI export file expired");
            throw new IllegalStateException("BI export job has expired: " + exportId);
        }
        try {
            byte[] bytes = readFile(row);
            auditDownload(row, now);
            return new BiExportDownload(
                    exportFileName(row.getId(), row.getExportFormat()),
                    contentType(row.getExportFormat()),
                    bytes);
        } catch (RuntimeException e) {
            throw new IllegalStateException("BI export file is not available: " + exportId, e);
        }
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
                default -> csv(result).getBytes(StandardCharsets.UTF_8);
            };
            return fileStorage.write(storageKey(tenantId, exportId, format), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("failed to write BI export file", e);
        }
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
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                header.createCell(i).setCellValue(columns.get(i));
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
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Path exportPath(Long tenantId, Long exportId, String format) {
        return exportRoot
                .resolve("tenant-" + normalizeTenant(tenantId))
                .resolve("export-" + exportId + "." + extension(format));
    }

    private String storageKey(Long tenantId, Long exportId, String format) {
        return "exports/tenant-" + normalizeTenant(tenantId) + "/" + exportFileName(exportId, format);
    }

    private String exportFileName(Long exportId, String format) {
        return "export-" + exportId + "." + extension(format);
    }

    private String extension(String format) {
        return switch (exportFormat(format)) {
            case "JSON" -> "json";
            case "XLSX" -> "xlsx";
            default -> "csv";
        };
    }

    private String contentType(String format) {
        return switch (exportFormat(format)) {
            case "JSON" -> "application/json";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "text/csv; charset=UTF-8";
        };
    }

    private BiQueryRequest withLimit(BiQueryRequest query, int limit) {
        return new BiQueryRequest(
                query.datasetKey(),
                query.dimensions(),
                query.metrics(),
                query.filters(),
                query.sorts(),
                limit);
    }

    private int cappedLimit(Integer requested, int max) {
        if (requested == null || requested <= 0) {
            return Math.min(500, max);
        }
        return Math.max(1, Math.min(requested, max));
    }

    private String exportFormat(String value) {
        String format = value == null || value.isBlank() ? "CSV" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CSV", "JSON", "XLSX").contains(format)) {
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
            return command;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("BI export request cannot be restored: " + row.getId(), e);
        }
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

    private void auditDownload(BiExportJobDO row, LocalDateTime now) {
        BiExportJobDO update = new BiExportJobDO();
        update.setId(row.getId());
        update.setDownloadCount((row.getDownloadCount() == null ? 0 : row.getDownloadCount()) + 1);
        update.setLastDownloadedAt(now);
        exportJobMapper.updateById(update);
    }

    private void markExpired(BiExportJobDO row, String message) {
        row.setStatus(STATUS_EXPIRED);
        row.setErrorMessage(message);
        exportJobMapper.updateById(row);
    }

    private boolean deleteFile(BiExportJobDO row) {
        if (row != null && hasText(row.getStorageKey())) {
            return fileStorage.delete(row.getStorageKey());
        }
        return deleteLocalFile(row);
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
}
