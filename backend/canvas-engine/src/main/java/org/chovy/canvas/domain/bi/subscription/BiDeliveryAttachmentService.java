package org.chovy.canvas.domain.bi.subscription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.chovy.canvas.dal.dataobject.BiDeliveryAttachmentDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiDeliveryAttachmentMapper;
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
    private final Path attachmentRoot;
    private final BiFileStorage fileStorage;
    private final int retentionDays;

    @Autowired
    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       ObjectProvider<BiSnapshotRenderer> snapshotRendererProvider,
                                       ObjectProvider<BiFileStorage> storageProvider,
                                       @Value("${canvas.bi.delivery.attachment.dir:${java.io.tmpdir}/canvas-bi-delivery-attachments}") String attachmentDir,
                                       @Value("${canvas.bi.delivery.attachment.retention-days:7}") int retentionDays) {
        this(attachmentMapper,
                objectMapper,
                snapshotRendererProvider == null ? null : snapshotRendererProvider.getIfAvailable(),
                Path.of(attachmentDir),
                storageProvider == null ? null : storageProvider.getIfAvailable(),
                retentionDays);
    }

    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       Path attachmentRoot) {
        this(attachmentMapper, objectMapper, null, attachmentRoot);
    }

    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiSnapshotRenderer snapshotRenderer,
                                       Path attachmentRoot) {
        this(attachmentMapper, objectMapper, snapshotRenderer, attachmentRoot, null, 7);
    }

    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiSnapshotRenderer snapshotRenderer,
                                       BiFileStorage fileStorage,
                                       int retentionDays) {
        this(attachmentMapper,
                objectMapper,
                snapshotRenderer,
                Path.of(System.getProperty("java.io.tmpdir"), "canvas-bi-delivery-attachments"),
                fileStorage,
                retentionDays);
    }

    public BiDeliveryAttachmentService(BiDeliveryAttachmentMapper attachmentMapper,
                                       ObjectMapper objectMapper,
                                       BiSnapshotRenderer snapshotRenderer,
                                       Path attachmentRoot,
                                       BiFileStorage fileStorage,
                                       int retentionDays) {
        this.attachmentMapper = attachmentMapper;
        this.objectMapper = objectMapper;
        this.snapshotRenderer = snapshotRenderer;
        this.attachmentRoot = attachmentRoot;
        this.fileStorage = fileStorage == null ? new LocalBiFileStorage(attachmentRoot) : fileStorage;
        this.retentionDays = retentionDays;
    }

    public List<BiDeliveryAttachmentView> createSubscriptionAttachments(Long tenantId,
                                                                        BiSubscriptionDO subscription,
                                                                        Map<String, Object> schedule,
                                                                        Map<String, Object> delivery,
                                                                        String username) {
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
        for (String type : types) {
            attachments.add(createAttachment(scopedTenantId, subscription, type, summary, delivery, username));
        }
        return attachments;
    }

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
        if (hasText(jobType)) {
            query.eq(BiDeliveryAttachmentDO::getJobType, jobType.trim().toUpperCase(Locale.ROOT));
        }
        if (jobId != null) {
            query.eq(BiDeliveryAttachmentDO::getJobId, jobId);
        }
        if (deliveryLogId != null) {
            query.eq(BiDeliveryAttachmentDO::getDeliveryLogId, deliveryLogId);
        }
        return safeList(attachmentMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    public BiDeliveryAttachmentDownload download(Long tenantId, Long attachmentId) {
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
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(row, now)) {
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
        } catch (RuntimeException e) {
            throw new IllegalStateException("BI delivery attachment file is not available: " + attachmentId);
        }
    }

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
            } catch (RuntimeException e) {
                failed++;
                row.setErrorMessage(truncate(e.getMessage()));
                attachmentMapper.updateById(row);
            }
        }
        return new BiDeliveryAttachmentCleanupResult(rows.size(), expired, filesDeleted, failed);
    }

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
        } catch (RuntimeException | IOException e) {
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncate(e.getMessage()));
            attachmentMapper.updateById(row);
            throw new IllegalStateException("failed to generate BI delivery attachment", e);
        }
    }

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
        if (List.of("HTML", "CSV", "JSON", "XLSX", "PDF", "PNG", "JPEG").contains(type)) {
            types.add(type);
        }
    }

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

    private byte[] image(String type, Map<String, Object> summary, Map<String, Object> delivery) {
        if (snapshotRenderer == null || !snapshotRenderer.configured()) {
            throw new IllegalStateException("BI snapshot renderer is not configured");
        }
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
        List<String> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objects.add("2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>\nendobj\n");
        objects.add("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        for (int i = 0; i < pages.size(); i++) {
            int pageObjectId = 4 + i * 2;
            int contentObjectId = pageObjectId + 1;
            String contentText = pdfPageContent(pages.get(i));
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

    private List<List<String>> pdfPages(List<String> wrappedLines, int linesPerPage) {
        List<String> lines = wrappedLines == null || wrappedLines.isEmpty() ? List.of("") : wrappedLines;
        int cappedLinesPerPage = Math.max(1, linesPerPage);
        List<List<String>> pages = new ArrayList<>();
        for (int index = 0; index < lines.size(); index += cappedLinesPerPage) {
            pages.add(lines.subList(index, Math.min(index + cappedLinesPerPage, lines.size())));
        }
        return pages;
    }

    private String pdfPageContent(List<String> lines) {
        StringBuilder content = new StringBuilder("BT\n/F1 11 Tf\n72 760 Td\n14 TL\n");
        for (String line : lines) {
            content.append('(').append(pdfEscape(ascii(line))).append(") Tj\nT*\n");
        }
        content.append("ET\n");
        return content.toString();
    }

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

    private BiDeliveryAttachmentView toView(BiDeliveryAttachmentDO row) {
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
                row.getUpdatedAt());
    }

    private String summaryValue(Object value) {
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return json(value);
        }
        return value == null ? "" : String.valueOf(value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI delivery attachment payload", e);
        }
    }

    private String csvCell(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String htmlEscape(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

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

    private String ascii(String value) {
        StringBuilder builder = new StringBuilder();
        for (char ch : (value == null ? "" : value).toCharArray()) {
            builder.append(ch >= 32 && ch < 127 ? ch : '?');
        }
        return builder.toString();
    }

    private String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String attachmentKey(String jobKey, String type) {
        return safeSlug(jobKey) + "-" + type.toLowerCase(Locale.ROOT) + "-" + Long.toHexString(System.nanoTime());
    }

    private String safeSlug(String value) {
        String slug = (value == null || value.isBlank() ? "bi-delivery" : value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "bi-delivery" : slug;
    }

    private Path attachmentPath(Long tenantId, Long attachmentId, String fileName) {
        return attachmentRoot
                .resolve("tenant-" + normalizeTenant(tenantId))
                .resolve("attachment-" + attachmentId)
                .resolve(fileName);
    }

    private String storageKey(Long tenantId, Long attachmentId, String attachmentKey) {
        return "attachments/tenant-" + normalizeTenant(tenantId)
                + "/attachment-" + attachmentId
                + "/" + safeSlug(attachmentKey);
    }

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

    private String snapshotType(Map<String, Object> delivery) {
        String type = normalizeType(String.valueOf(firstValue(delivery, "snapshotFormat", "screenshotFormat", "imageFormat")));
        if ("JPG".equals(type)) {
            return "JPEG";
        }
        if (List.of("PNG", "JPEG", "HTML").contains(type)) {
            return type;
        }
        return "HTML";
    }

    private Object firstValue(Map<String, Object> values, String... keys) {
        if (values == null) {
            return null;
        }
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
        return null;
    }

    private int intConfig(Map<String, Object> values, int defaultValue, String... keys) {
        Object raw = firstValue(values, keys);
        if (raw instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        try {
            return raw == null ? defaultValue : Math.max(1, Integer.parseInt(String.valueOf(raw)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double doubleConfig(Map<String, Object> values, double defaultValue, String... keys) {
        Object raw = firstValue(values, keys);
        if (raw instanceof Number number) {
            return Math.max(0.1, number.doubleValue());
        }
        try {
            return raw == null ? defaultValue : Math.max(0.1, Double.parseDouble(String.valueOf(raw)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String resourceUrl(String resourceType, Long resourceId) {
        return "/bi?resourceType=" + resourceType + "&resourceId=" + resourceId;
    }

    private String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private boolean isExpired(BiDeliveryAttachmentDO row, LocalDateTime now) {
        return row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now);
    }

    private void auditDownload(BiDeliveryAttachmentDO row, LocalDateTime now) {
        BiDeliveryAttachmentDO update = new BiDeliveryAttachmentDO();
        update.setId(row.getId());
        update.setDownloadCount((row.getDownloadCount() == null ? 0 : row.getDownloadCount()) + 1);
        update.setLastDownloadedAt(now);
        attachmentMapper.updateById(update);
    }

    private void markExpired(BiDeliveryAttachmentDO row, String message) {
        row.setStatus(STATUS_EXPIRED);
        row.setErrorMessage(message);
        attachmentMapper.updateById(row);
    }

    private byte[] readFile(BiDeliveryAttachmentDO row) {
        if (hasText(row.getStorageKey())) {
            byte[] bytes = fileStorage.read(row.getStorageKey());
            if (bytes == null) {
                throw new IllegalStateException("BI delivery attachment storage object is not available: "
                        + row.getStorageKey());
            }
            return bytes;
        }
        try {
            return Files.readAllBytes(Path.of(row.getFilePath()));
        } catch (IOException e) {
            throw new IllegalStateException("BI delivery attachment file is not available: " + row.getId(), e);
        }
    }

    private boolean deleteFile(BiDeliveryAttachmentDO row) {
        if (row != null && hasText(row.getStorageKey())) {
            return fileStorage.delete(row.getStorageKey());
        }
        return deleteLocalFile(row);
    }

    private boolean deleteLocalFile(BiDeliveryAttachmentDO row) {
        if (row == null || !hasText(row.getFilePath())) {
            return false;
        }
        try {
            Path root = attachmentRoot.toAbsolutePath().normalize();
            Path file = Path.of(row.getFilePath()).toAbsolutePath().normalize();
            if (!file.startsWith(root)) {
                return false;
            }
            boolean deleted = Files.deleteIfExists(file);
            deleteEmptyParents(file.getParent(), root);
            return deleted;
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete BI delivery attachment file: " + row.getId(), e);
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

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
