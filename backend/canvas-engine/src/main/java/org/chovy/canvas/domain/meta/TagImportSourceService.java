package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dto.TagImportResult;
import org.chovy.canvas.dto.TagImportRow;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TagImportSourceService {

    private static final DateTimeFormatter TAG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TagImportSourceMapper tagImportSourceMapper;
    private final TagImportService tagImportService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public List<TagImportSource> list(Integer enabled) {
        return tagImportSourceMapper.selectList(new LambdaQueryWrapper<TagImportSource>()
                .eq(enabled != null, TagImportSource::getEnabled, enabled)
                .orderByDesc(TagImportSource::getId));
    }

    public TagImportSource create(TagImportSource body) {
        validateAndNormalize(body);
        applyDefaults(body);
        parseFieldMapping(body);
        tagImportSourceMapper.insert(body);
        return body;
    }

    public void update(Long id, TagImportSource body) {
        validateAndNormalize(body);
        applyDefaults(body);
        parseFieldMapping(body);
        requireExisting(id);
        body.setId(id);
        tagImportSourceMapper.updateById(body);
    }

    public void delete(Long id) {
        requireExisting(id);
        tagImportSourceMapper.deleteById(id);
    }

    public TagImportResult run(Long id) {
        TagImportSource source = requireExisting(id);
        if (source.getEnabled() == null || source.getEnabled() != 1) {
            throw new IllegalArgumentException("tag import source is disabled: " + id);
        }

        JsonNode response = executeRequest(source);
        List<Map<String, Object>> records = resolveRecords(source, response);
        List<TagImportRow> rows = mapRows(source, records);
        return tagImportService.importRows("API_PULL", null, source.getUrl(), rows);
    }

    List<TagImportRow> mapRows(TagImportSource source, List<Map<String, Object>> records) {
        Map<String, String> fieldMapping = parseFieldMapping(source);
        List<Map<String, Object>> safeRecords = records == null ? List.of() : records;
        return java.util.stream.IntStream.range(0, safeRecords.size())
                .mapToObj(index -> toImportRow(index + 1, safeRecords.get(index), fieldMapping))
                .toList();
    }

    private TagImportSource requireExisting(Long id) {
        TagImportSource source = tagImportSourceMapper.selectById(id);
        if (source == null) {
            throw new IllegalArgumentException("tag import source not found: " + id);
        }
        return source;
    }

    private JsonNode executeRequest(TagImportSource source) {
        HttpMethod method = resolveMethod(source.getMethod());
        WebClient.RequestBodyUriSpec request = webClientBuilder.build().method(method);
        WebClient.RequestHeadersSpec<?> spec = request.uri(source.getUrl());
        spec = applyHeaders(spec, source.getHeadersJson());
        if (method == HttpMethod.POST && hasText(source.getBodyTemplate())) {
            spec = ((WebClient.RequestBodySpec) spec)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(parseBodyTemplate(source.getBodyTemplate()));
        }
        return spec.retrieve().bodyToMono(JsonNode.class).block();
    }

    private WebClient.RequestHeadersSpec<?> applyHeaders(WebClient.RequestHeadersSpec<?> spec, String headersJson) {
        if (!hasText(headersJson)) {
            return spec;
        }
        try {
            Map<String, Object> headers = objectMapper.readValue(headersJson, new TypeReference<>() {});
            WebClient.RequestHeadersSpec<?> current = spec;
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if (entry.getValue() != null) {
                    current = current.header(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return current;
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid headersJson", ex);
        }
    }

    private Object parseBodyTemplate(String bodyTemplate) {
        try {
            return objectMapper.readValue(bodyTemplate, Object.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid bodyTemplate", ex);
        }
    }

    private List<Map<String, Object>> resolveRecords(TagImportSource source, JsonNode response) {
        String recordsPath = normalizeRecordsPath(source.getRecordsPath());
        JsonNode recordsNode;
        if ("$".equals(recordsPath)) {
            recordsNode = response;
        } else if ("$.data".equals(recordsPath)) {
            if (response == null || !response.isObject()) {
                throw new IllegalArgumentException("recordsPath $.data requires response object");
            }
            recordsNode = response.get("data");
        } else {
            throw new IllegalArgumentException("unsupported recordsPath: " + recordsPath);
        }

        if (recordsNode == null || !recordsNode.isArray()) {
            throw new IllegalArgumentException("recordsPath did not resolve to an array: " + recordsPath);
        }
        return objectMapper.convertValue(recordsNode, new TypeReference<>() {});
    }

    private Map<String, String> parseFieldMapping(TagImportSource source) {
        try {
            Map<String, String> mapping = objectMapper.readValue(source.getFieldMapping(), new TypeReference<>() {});
            if (mapping == null || mapping.isEmpty()) {
                throw new IllegalArgumentException("fieldMapping is required");
            }
            Map<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                if (hasText(entry.getKey()) && hasText(entry.getValue())) {
                    normalized.put(entry.getKey().trim(), entry.getValue().trim());
                }
            }
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("fieldMapping is required");
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid fieldMapping", ex);
        }
    }

    private TagImportRow toImportRow(int rowNo, Map<String, Object> record, Map<String, String> fieldMapping) {
        TagImportRow row = new TagImportRow();
        row.setRowNo(rowNo);
        row.setIdType(readString(record, fieldMapping.get("idType")));
        row.setIdValue(readString(record, fieldMapping.get("idValue")));
        row.setTagCode(readString(record, fieldMapping.get("tagCode")));
        row.setTagValue(readString(record, fieldMapping.get("tagValue")));
        row.setTagTime(parseTagTime(readValue(record, fieldMapping.get("tagTime"))));
        return row;
    }

    private static Object readValue(Map<String, Object> record, String fieldName) {
        if (record == null || !hasText(fieldName)) {
            return null;
        }
        return record.get(fieldName);
    }

    private static String readString(Map<String, Object> record, String fieldName) {
        Object value = readValue(record, fieldName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static LocalDateTime parseTagTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof TemporalAccessor temporalAccessor) {
            return LocalDateTime.from(temporalAccessor);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text, TAG_TIME_FORMATTER);
    }

    private static void validateAndNormalize(TagImportSource body) {
        if (body == null) {
            throw new IllegalArgumentException("tag import source body is required");
        }
        if (!hasText(body.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (!hasText(body.getUrl())) {
            throw new IllegalArgumentException("url is required");
        }
        if (!hasText(body.getFieldMapping())) {
            throw new IllegalArgumentException("fieldMapping is required");
        }
        body.setName(body.getName().trim());
        body.setUrl(body.getUrl().trim());
        body.setMethod(normalizeMethod(body.getMethod()));
        body.setRecordsPath(normalizeRecordsPath(body.getRecordsPath()));
        if (!"$".equals(body.getRecordsPath()) && !"$.data".equals(body.getRecordsPath())) {
            throw new IllegalArgumentException("unsupported recordsPath: " + body.getRecordsPath());
        }
        body.setHeadersJson(trimToNull(body.getHeadersJson()));
        body.setBodyTemplate(trimToNull(body.getBodyTemplate()));
        body.setPageParam(trimToNull(body.getPageParam()));
        body.setPageSizeParam(trimToNull(body.getPageSizeParam()));
        body.setFieldMapping(body.getFieldMapping().trim());
    }

    private static void applyDefaults(TagImportSource body) {
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
        if (!hasText(body.getMethod())) {
            body.setMethod("GET");
        }
        if (body.getPageSize() == null) {
            body.setPageSize(500);
        }
        if (!hasText(body.getRecordsPath())) {
            body.setRecordsPath("$");
        }
    }

    private static HttpMethod resolveMethod(String method) {
        String normalized = normalizeMethod(method);
        if ("GET".equals(normalized)) {
            return HttpMethod.GET;
        }
        if ("POST".equals(normalized)) {
            return HttpMethod.POST;
        }
        throw new IllegalArgumentException("unsupported method: " + normalized);
    }

    private static String normalizeMethod(String method) {
        return hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : "GET";
    }

    private static String normalizeRecordsPath(String recordsPath) {
        return hasText(recordsPath) ? recordsPath.trim() : "$";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
