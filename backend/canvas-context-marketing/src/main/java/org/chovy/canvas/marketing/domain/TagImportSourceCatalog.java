package org.chovy.canvas.marketing.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TagImportSourceCatalog {

    private static final String SOURCE_TYPE = "API_PULL";

    private final List<SourceState> sources = new ArrayList<>();
    private long nextId = 1L;

    public TagImportSourceCatalog() {
        seed(7L, 7001L, "Default CRM Source", 1);
        seed(7L, 7002L, "Disabled CDP Source", 0);
        seed(8L, 7001L, "Tenant 8 CRM Source", 1);
    }

    public synchronized Map<String, Object> listSources(Long tenantId, Integer enabled) {
        List<Map<String, Object>> rows = sources.stream()
                .filter(source -> Objects.equals(source.tenantId, tenantId))
                .filter(source -> enabled == null || Objects.equals(source.enabled, enabled))
                .sorted(Comparator.comparing((SourceState source) -> source.id).reversed())
                .map(SourceState::toMap)
                .toList();
        return Map.of("total", (long) rows.size(), "list", rows);
    }

    public synchronized Map<String, Object> createSource(Long tenantId, Map<String, Object> payload, String actor) {
        SourceState source = SourceState.from(tenantId, nextAvailableId(), requirePayload(payload), actor);
        sources.add(source);
        return source.toMap();
    }

    public synchronized Map<String, Object> updateSource(Long tenantId, Long id, Map<String, Object> payload,
                                                         String actor) {
        SourceState source = findRequired(tenantId, id);
        source.apply(requirePayload(payload), actor);
        return source.toMap();
    }

    public synchronized void deleteSource(Long tenantId, Long id) {
        SourceState source = findRequired(tenantId, id);
        sources.remove(source);
    }

    public synchronized Map<String, Object> runSource(Long tenantId, Long id) {
        SourceState source = findRequired(tenantId, id);
        if (!Objects.equals(source.enabled, 1)) {
            throw new IllegalArgumentException("tag import source is disabled: " + id);
        }
        return Map.of("batchId", 1L, "status", "SUCCESS", "totalRows", 2, "successRows", 2, "failedRows", 0);
    }

    private SourceState findRequired(Long tenantId, Long id) {
        return sources.stream()
                .filter(source -> Objects.equals(source.tenantId, tenantId))
                .filter(source -> Objects.equals(source.id, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("tag import source not found: " + id));
    }

    private void seed(Long tenantId, Long id, String name, int enabled) {
        SourceState source = new SourceState(tenantId, id);
        source.name = name;
        source.url = "https://crm.example.test/tags";
        source.method = "GET";
        source.headersJson = "{}";
        source.bodyTemplate = "";
        source.pageParam = "page";
        source.pageSizeParam = "size";
        source.pageSize = 500;
        source.recordsPath = "$";
        source.fieldMapping = "{\"idType\":\"idType\",\"idValue\":\"idValue\",\"tagCode\":\"tagCode\"}";
        source.enabled = enabled;
        source.createdBy = "operator-1";
        source.updatedBy = "operator-1";
        sources.add(source);
        nextId = Math.max(nextId, id + 1);
    }

    private long nextAvailableId() {
        while (containsId(nextId)) {
            nextId++;
        }
        return nextId++;
    }

    private boolean containsId(Long id) {
        return sources.stream().anyMatch(source -> Objects.equals(source.id, id));
    }

    private static Map<String, Object> requirePayload(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("tag import source body is required");
        }
        return payload;
    }

    private static String requiredString(Map<String, Object> payload, String field, String message) {
        Object value = payload.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return String.valueOf(value).trim();
    }

    private static String optionalString(Map<String, Object> payload, String field, String fallback) {
        Object value = payload.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }

    private static int optionalInt(Map<String, Object> payload, String field, int fallback) {
        Object value = payload.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static void requireJson(String value) {
        String trimmed = value.trim();
        boolean objectLike = trimmed.startsWith("{") && trimmed.endsWith("}");
        boolean arrayLike = trimmed.startsWith("[") && trimmed.endsWith("]");
        if (!objectLike && !arrayLike) {
            throw new IllegalArgumentException("fieldMapping must be JSON");
        }
    }

    private static String normalizeMethod(String method) {
        return method.toUpperCase(Locale.ROOT);
    }

    private static String normalizeRecordsPath(String recordsPath) {
        if ("$".equals(recordsPath) || "$.data".equals(recordsPath)) {
            return recordsPath;
        }
        throw new IllegalArgumentException("unsupported recordsPath: " + recordsPath);
    }

    private static final class SourceState {
        private final Long tenantId;
        private final Long id;
        private String name;
        private String url;
        private String method;
        private String headersJson;
        private String bodyTemplate;
        private String pageParam;
        private String pageSizeParam;
        private Integer pageSize;
        private String recordsPath;
        private String fieldMapping;
        private Integer enabled;
        private String createdBy;
        private String updatedBy;

        private SourceState(Long tenantId, Long id) {
            this.tenantId = tenantId;
            this.id = id;
        }

        private static SourceState from(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            SourceState source = new SourceState(tenantId, id);
            source.createdBy = actor;
            source.apply(payload, actor);
            return source;
        }

        private void apply(Map<String, Object> payload, String actor) {
            name = requiredString(payload, "name", "name is required");
            url = requiredString(payload, "url", "url is required");
            method = normalizeMethod(optionalString(payload, "method", "GET"));
            headersJson = optionalString(payload, "headersJson", "{}");
            bodyTemplate = optionalString(payload, "bodyTemplate", "");
            pageParam = optionalString(payload, "pageParam", "page");
            pageSizeParam = optionalString(payload, "pageSizeParam", "size");
            pageSize = optionalInt(payload, "pageSize", 500);
            recordsPath = normalizeRecordsPath(optionalString(payload, "recordsPath", "$"));
            fieldMapping = requiredString(payload, "fieldMapping", "fieldMapping is required");
            requireJson(fieldMapping);
            enabled = optionalInt(payload, "enabled", 1);
            updatedBy = actor;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("id", id);
            row.put("sourceType", SOURCE_TYPE);
            row.put("name", name);
            row.put("url", url);
            row.put("method", method);
            row.put("headersJson", headersJson);
            row.put("bodyTemplate", bodyTemplate);
            row.put("pageParam", pageParam);
            row.put("pageSizeParam", pageSizeParam);
            row.put("pageSize", pageSize);
            row.put("recordsPath", recordsPath);
            row.put("fieldMapping", fieldMapping);
            row.put("enabled", enabled);
            row.put("status", Objects.equals(enabled, 1) ? "enabled" : "disabled");
            row.put("createdBy", createdBy);
            row.put("updatedBy", updatedBy);
            return row;
        }
    }
}
