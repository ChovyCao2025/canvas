package org.chovy.canvas.marketing.domain;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 维护TagImport相关的内存业务目录。
 */
public class TagImportCatalog {

    private static final List<String> TEMPLATE_HEADERS = List.of("idType", "idValue", "tagCode", "tagValue",
            "tagTime");

    private final List<BatchState> batches = new ArrayList<>();
    private final List<ErrorState> errors = new ArrayList<>();

    /**
     * 下一个错误记录内存标识。
     */
    private long nextErrorId = 1L;

    /**
     * 创建TagImportCatalog实例。
     */
    public TagImportCatalog() {
        seedBatch(7L, 7001L, "API_PUSH", "SUCCESS", 3, 3, 0);
        seedBatch(7L, 7002L, "EXCEL_IMPORT", "FAILED", 2, 0, 2);
    }

    /**
     * 执行importRows业务操作。
     */
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

    /**
     * 执行excelTemplate业务操作。
     */
    public byte[] excelTemplate() {
        return String.join(",", TEMPLATE_HEADERS)
                .concat("\nemail,user@example.com,tier,vip,2026-05-23 10:30:00\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 执行readCsvLikeRows业务操作。
     */
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

    /**
     * 查询batches列表。
     */
    public synchronized List<Map<String, Object>> listBatches(Long tenantId) {
        return batches.stream()
                .filter(batch -> Objects.equals(batch.tenantId, tenantId))
                .sorted(Comparator.comparing((BatchState batch) -> batch.id).reversed())
                .map(BatchState::toMap)
                .toList();
    }

    /**
     * 查询errors列表。
     */
    public synchronized List<Map<String, Object>> listErrors(Long tenantId, Long batchId) {
        return errors.stream()
                .filter(error -> Objects.equals(error.tenantId, tenantId))
                .filter(error -> Objects.equals(error.batchId, batchId))
                .sorted(Comparator.comparing((ErrorState error) -> error.rowNo).thenComparing(error -> error.id))
                .map(ErrorState::toMap)
                .toList();
    }

    /**
     * 执行seedBatch业务操作。
     */
    private void seedBatch(Long tenantId, Long id, String sourceType, String status, int totalRows, int successRows,
                           int failedRows) {
        batches.add(new BatchState(tenantId, id, sourceType, status, null, totalRows, successRows, failedRows,
                LocalDateTime.now()));
    }

    /**
     * 执行nextBatchId业务操作。
     */
    private long nextBatchId(Long tenantId) {
        return batches.stream()
                .filter(batch -> Objects.equals(batch.tenantId, tenantId))
                .map(batch -> batch.id)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    /**
     * 执行result业务操作。
     */
    private static Map<String, Object> result(BatchState batch) {
        return Map.of("batchId", batch.id, "status", batch.status, "totalRows", batch.totalRows,
                "successRows", batch.successRows, "failedRows", batch.failedRows);
    }

    /**
     * 执行validateRow业务操作。
     */
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

    /**
     * 执行rowNo业务操作。
     */
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

    /**
     * 判断isBlank条件是否成立。
     */
    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    /**
     * 执行column业务操作。
     */
    private static String column(String[] columns, int index) {
        return index >= columns.length ? null : columns[index].trim();
    }

    /**
     * 执行status业务操作。
     */
    private static String status(int successRows, int failedRows) {
        if (failedRows == 0) {
            return "SUCCESS";
        }
        if (successRows == 0) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }

    /**
     * 保存BatchState的内存状态。
     */
    private static final class BatchState {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 记录的唯一标识。
         */
        private final Long id;

        /**
         * sourceType 字段值。
         */
        private final String sourceType;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * fileName 字段值。
         */
        private final String fileName;

        /**
         * totalRows 字段值。
         */
        private final int totalRows;

        /**
         * successRows 字段值。
         */
        private final int successRows;

        /**
         * failedRows 字段值。
         */
        private final int failedRows;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 创建BatchState实例。
         */
        public BatchState(Long tenantId, Long id, String sourceType, String status, String fileName, int totalRows, int successRows, int failedRows, LocalDateTime createdAt) {
            this.tenantId = tenantId;
            this.id = id;
            this.sourceType = sourceType;
            this.status = status;
            this.fileName = fileName;
            this.totalRows = totalRows;
            this.successRows = successRows;
            this.failedRows = failedRows;
            this.createdAt = createdAt;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回sourceType 字段值。
         */
        public String sourceType() {
            return sourceType;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回fileName 字段值。
         */
        public String fileName() {
            return fileName;
        }

        /**
         * 返回totalRows 字段值。
         */
        public int totalRows() {
            return totalRows;
        }

        /**
         * 返回successRows 字段值。
         */
        public int successRows() {
            return successRows;
        }

        /**
         * 返回failedRows 字段值。
         */
        public int failedRows() {
            return failedRows;
        }

        /**
         * 返回创建时间。
         */
        public LocalDateTime createdAt() {
            return createdAt;
        }

        /**
         * 转换为map对象。
         */
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


        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BatchState that = (BatchState) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(id, that.id) &&
                    Objects.equals(sourceType, that.sourceType) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(fileName, that.fileName) &&
                    totalRows == that.totalRows &&
                    successRows == that.successRows &&
                    failedRows == that.failedRows &&
                    Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, id, sourceType, status, fileName, totalRows, successRows, failedRows, createdAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "BatchState[tenantId=" + tenantId + ", id=" + id + ", sourceType=" + sourceType + ", status=" + status + ", fileName=" + fileName + ", totalRows=" + totalRows + ", successRows=" + successRows + ", failedRows=" + failedRows + ", createdAt=" + createdAt + "]";
        }
    }

    /**
     * 保存ErrorState的内存状态。
     */
    private static final class ErrorState {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * batchId 字段值。
         */
        private final Long batchId;

        /**
         * rowNo 字段值。
         */
        private final int rowNo;

        /**
         * errorCode 字段值。
         */
        private final String errorCode;

        /**
         * errorMsg 字段值。
         */
        private final String errorMsg;

        /**
         * rawPayload 字段值。
         */
        private final Map<String, Object> rawPayload;

        /**
         * 创建ErrorState实例。
         */
        public ErrorState(Long id, Long tenantId, Long batchId, int rowNo, String errorCode, String errorMsg, Map<String, Object> rawPayload) {
            this.id = id;
            this.tenantId = tenantId;
            this.batchId = batchId;
            this.rowNo = rowNo;
            this.errorCode = errorCode;
            this.errorMsg = errorMsg;
            this.rawPayload = rawPayload;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回batchId 字段值。
         */
        public Long batchId() {
            return batchId;
        }

        /**
         * 返回rowNo 字段值。
         */
        public int rowNo() {
            return rowNo;
        }

        /**
         * 返回errorCode 字段值。
         */
        public String errorCode() {
            return errorCode;
        }

        /**
         * 返回errorMsg 字段值。
         */
        public String errorMsg() {
            return errorMsg;
        }

        /**
         * 返回rawPayload 字段值。
         */
        public Map<String, Object> rawPayload() {
            return rawPayload;
        }

        /**
         * 转换为map对象。
         */
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


        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ErrorState that = (ErrorState) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(batchId, that.batchId) &&
                    rowNo == that.rowNo &&
                    Objects.equals(errorCode, that.errorCode) &&
                    Objects.equals(errorMsg, that.errorMsg) &&
                    Objects.equals(rawPayload, that.rawPayload);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, batchId, rowNo, errorCode, errorMsg, rawPayload);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ErrorState[id=" + id + ", tenantId=" + tenantId + ", batchId=" + batchId + ", rowNo=" + rowNo + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", rawPayload=" + rawPayload + "]";
        }
    }
}
