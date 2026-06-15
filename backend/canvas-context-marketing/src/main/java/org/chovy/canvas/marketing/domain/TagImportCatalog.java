package org.chovy.canvas.marketing.domain;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TagImportCatalog {

    private static final List<String> TEMPLATE_HEADERS = List.of("idType", "idValue", "tagCode", "tagValue",
            "tagTime");

    private final List<BatchState> batches = new ArrayList<>();
    private final List<ErrorState> errors = new ArrayList<>();
    private long nextErrorId = 1L;

    public TagImportCatalog() {
        seedBatch(7L, 7001L, "API_PUSH", "SUCCESS", 3, 3, 0);
        seedBatch(7L, 7002L, "EXCEL_IMPORT", "FAILED", 2, 0, 2);
    }

    public synchronized Map<String, Object> importRows(
            Long tenantId,
            String sourceType,
            String fileName,
            List<Map<String, Object>> rows) {
        List<Map<String, Object>> safeRows = rows == null ? List.of() : rows;
        if (safeRows.isEmpty()) {
            throw new IllegalArgumentException("rows are required");
        }
        long batchId = nextBatchId(tenantId);
        int successRows = 0;
        int failedRows = 0;
        for (int i = 0; i < safeRows.size(); i++) {
            Map<String, Object> row = safeRows.get(i);
            String validationError = validateRow(row);
            if (validationError == null) {
                successRows++;
            } else {
                failedRows++;
                errors.add(new ErrorState(nextErrorId++, tenantId, batchId, rowNo(row, i), "ROW_ERROR",
                        validationError, row));
            }
        }
        String status = status(successRows, failedRows);
        BatchState batch = new BatchState(tenantId, batchId, sourceType, status, fileName, safeRows.size(),
                successRows, failedRows, LocalDateTime.now());
        batches.add(batch);
        return result(batch);
    }

    public byte[] excelTemplate() {
        return String.join(",", TEMPLATE_HEADERS)
                .concat("\nemail,user@example.com,tier,vip,2026-05-23 10:30:00\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    public List<Map<String, Object>> readCsvLikeRows(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("rows are required");
        }
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("rows are required");
        }
        String[] lines = text.split("\\R");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] columns = lines[i].split(",", -1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowNo", i + 1);
            row.put("idType", column(columns, 0));
            row.put("idValue", column(columns, 1));
            row.put("tagCode", column(columns, 2));
            row.put("tagValue", column(columns, 3));
            row.put("tagTime", column(columns, 4));
            rows.add(row);
        }
        return rows;
    }

    public synchronized List<Map<String, Object>> listBatches(Long tenantId) {
        return batches.stream()
                .filter(batch -> Objects.equals(batch.tenantId, tenantId))
                .sorted(Comparator.comparing((BatchState batch) -> batch.id).reversed())
                .map(BatchState::toMap)
                .toList();
    }

    public synchronized List<Map<String, Object>> listErrors(Long tenantId, Long batchId) {
        return errors.stream()
                .filter(error -> Objects.equals(error.tenantId, tenantId))
                .filter(error -> Objects.equals(error.batchId, batchId))
                .sorted(Comparator.comparing((ErrorState error) -> error.rowNo).thenComparing(error -> error.id))
                .map(ErrorState::toMap)
                .toList();
    }

    private void seedBatch(Long tenantId, Long id, String sourceType, String status, int totalRows, int successRows,
                           int failedRows) {
        batches.add(new BatchState(tenantId, id, sourceType, status, null, totalRows, successRows, failedRows,
                LocalDateTime.now()));
    }

    private long nextBatchId(Long tenantId) {
        return batches.stream()
                .filter(batch -> Objects.equals(batch.tenantId, tenantId))
                .map(batch -> batch.id)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private static Map<String, Object> result(BatchState batch) {
        return Map.of("batchId", batch.id, "status", batch.status, "totalRows", batch.totalRows,
                "successRows", batch.successRows, "failedRows", batch.failedRows);
    }

    private static String validateRow(Map<String, Object> row) {
        if (isBlank(row == null ? null : row.get("idType"))) {
            return "idType is required";
        }
        if (isBlank(row.get("idValue"))) {
            return "idValue is required";
        }
        if (isBlank(row.get("tagCode"))) {
            return "tagCode is required";
        }
        return null;
    }

    private static int rowNo(Map<String, Object> row, int index) {
        Object rowNo = row == null ? null : row.get("rowNo");
        if (rowNo instanceof Number number) {
            return number.intValue();
        }
        if (rowNo != null && !String.valueOf(rowNo).isBlank()) {
            return Integer.parseInt(String.valueOf(rowNo));
        }
        return index + 1;
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private static String column(String[] columns, int index) {
        return index >= columns.length ? null : columns[index].trim();
    }

    private static String status(int successRows, int failedRows) {
        if (failedRows == 0) {
            return "SUCCESS";
        }
        if (successRows == 0) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }

    private record BatchState(
            Long tenantId,
            Long id,
            String sourceType,
            String status,
            String fileName,
            int totalRows,
            int successRows,
            int failedRows,
            LocalDateTime createdAt) {
        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("sourceType", sourceType);
            row.put("status", status);
            row.put("fileName", fileName);
            row.put("totalRows", totalRows);
            row.put("successRows", successRows);
            row.put("failedRows", failedRows);
            row.put("createdAt", createdAt);
            return row;
        }
    }

    private record ErrorState(
            Long id,
            Long tenantId,
            Long batchId,
            int rowNo,
            String errorCode,
            String errorMsg,
            Map<String, Object> rawPayload) {
        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("tenantId", tenantId);
            row.put("batchId", batchId);
            row.put("rowNo", rowNo);
            row.put("errorCode", errorCode);
            row.put("errorMsg", errorMsg);
            row.put("rawPayload", rawPayload);
            return row;
        }
    }
}
