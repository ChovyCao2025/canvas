package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.OutboundUrlValidator;
import org.chovy.canvas.dto.TagImportResult;
import org.chovy.canvas.dto.TagImportRow;
import org.chovy.canvas.infrastructure.reactor.BlockingWorkScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
import java.util.Set;
import org.chovy.canvas.dal.dataobject.TagImportSourceDO;
import org.chovy.canvas.dal.mapper.TagImportSourceMapper;

/**
 * 标签导入来源 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class TagImportSourceService {

    /** API 来源中标签时间字段的默认解析格式。 */
    private static final DateTimeFormatter TAG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 禁止由配置覆盖的 HTTP 请求头，避免 SSRF 绕过和请求走私风险。 */
    private static final Set<String> BLOCKED_HEADER_NAMES = Set.of(
            "host", "cookie", "set-cookie", "content-length", "transfer-encoding",
            "connection", "proxy-authorization", "proxy-authenticate", "forwarded",
            "x-forwarded-for", "x-forwarded-host", "x-forwarded-proto");

    /** 标签导入来源 Mapper。 */
    private final TagImportSourceMapper tagImportSourceMapper;
    /** 标签导入服务，用于复用行级导入逻辑。 */
    private final TagImportService tagImportService;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** WebClient 构建器，用于创建远程 HTTP 调用客户端。 */
    private final WebClient.Builder webClientBuilder;
    /** 统一阻塞适配器，避免在 Netty 事件循环线程上等待远程调用结果。 */
    private final BlockingWorkScheduler blockingWorkScheduler;

    @Autowired
    public TagImportSourceService(TagImportSourceMapper tagImportSourceMapper,
                                  TagImportService tagImportService,
                                  ObjectMapper objectMapper,
                                  WebClient.Builder webClientBuilder) {
        this(tagImportSourceMapper, tagImportService, objectMapper, webClientBuilder, new BlockingWorkScheduler());
    }

    /** 按条件查询列表数据。 */
    public List<TagImportSourceDO> list(Integer enabled) {
        return tagImportSourceMapper.selectList(new LambdaQueryWrapper<TagImportSourceDO>()
                .eq(enabled != null, TagImportSourceDO::getEnabled, enabled)
                .orderByDesc(TagImportSourceDO::getId));
    }

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public TagImportSourceDO create(TagImportSourceDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        parseFieldMapping(body);
        tagImportSourceMapper.insert(body);
        return body;
    }

    /** 更新已有记录，仅修改允许变更的字段。 */
    public void update(Long id, TagImportSourceDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        parseFieldMapping(body);
        requireExisting(id);
        body.setId(id);
        tagImportSourceMapper.updateById(body);
    }

    /** 删除标签导入来源配置，删除前确认配置存在。 */
    public void delete(Long id) {
        requireExisting(id);
        tagImportSourceMapper.deleteById(id);
    }

    /** 执行一次远程标签来源拉取并导入结果。 */
    public TagImportResult run(Long id) {
        TagImportSourceDO source = requireExisting(id);
        if (source.getEnabled() == null || source.getEnabled() != 1) {
            throw new IllegalArgumentException("tag import source is disabled: " + id);
        }

        // 来源拉取只负责取数和字段映射，行级校验、批次统计和 CDP 写入统一交给 TagImportService。
        JsonNode response = executeRequest(source);
        List<Map<String, Object>> records = resolveRecords(source, response);
        List<TagImportRow> rows = mapRows(source, records);
        return tagImportService.importRows("API_PULL", null, source.getUrl(), rows);
    }

    /** 异步执行一次远程标签来源拉取，避免阻塞 WebFlux 事件循环。 */
    public Mono<TagImportResult> runAsync(Long id) {
        return blockingWorkScheduler.call("tag import source run", () -> run(id));
    }

    /**
     * 构建、解析或转换 map Rows 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param source source 方法执行所需的业务参数
     * @param records records 待处理的数据集合
     * @return 查询、转换或计算得到的结果集合
     */
    List<TagImportRow> mapRows(TagImportSourceDO source, List<Map<String, Object>> records) {
        Map<String, String> fieldMapping = parseFieldMapping(source);
        List<Map<String, Object>> safeRecords = records == null ? List.of() : records;
        // 保留远程数组顺序生成 rowNo，方便错误明细回溯到原始响应位置。
        return java.util.stream.IntStream.range(0, safeRecords.size())
                .mapToObj(index -> toImportRow(index + 1, safeRecords.get(index), fieldMapping))
                .toList();
    }

    /** 查询标签导入来源配置，不存在时抛出异常。 */
    private TagImportSourceDO requireExisting(Long id) {
        TagImportSourceDO source = tagImportSourceMapper.selectById(id);
        if (source == null) {
            throw new IllegalArgumentException("tag import source not found: " + id);
        }
        return source;
    }

    /** 按来源配置发起 HTTP 请求并返回 JSON 响应体。 */
    private JsonNode executeRequest(TagImportSourceDO source) {
        OutboundUrlValidator.validateHttpUrl(source.getUrl());
        HttpMethod method = resolveMethod(source.getMethod());
        WebClient.RequestBodyUriSpec request = webClientBuilder.build().method(method);
        WebClient.RequestHeadersSpec<?> spec = request.uri(source.getUrl());
        spec = applyHeaders(spec, source.getHeadersJson());
        if (method == HttpMethod.POST && hasText(source.getBodyTemplate())) {
            // bodyTemplate 已按 JSON 解析后发送，避免把模板字符串当普通文本提交给远端。
            spec = ((WebClient.RequestBodySpec) spec)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(parseBodyTemplate(source.getBodyTemplate()));
        }
        return blockingWorkScheduler.await(
                "tag-import-source remote request",
                spec.retrieve().bodyToMono(JsonNode.class));
    }

    /** 将配置中的 JSON 头信息追加到 WebClient 请求。 */
    private WebClient.RequestHeadersSpec<?> applyHeaders(WebClient.RequestHeadersSpec<?> spec, String headersJson) {
        if (!hasText(headersJson)) {
            return spec;
        }
        try {
            Map<String, Object> headers = objectMapper.readValue(headersJson, new TypeReference<>() {});
            WebClient.RequestHeadersSpec<?> current = spec;
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if (entry.getValue() != null) {
                    String headerName = normalizeHeaderName(entry.getKey());
                    if (BLOCKED_HEADER_NAMES.contains(headerName)
                            || headerName.startsWith("proxy-")) {
                        throw new IllegalArgumentException("headersJson 包含不允许的请求头: " + entry.getKey());
                    }
                    current = current.header(entry.getKey().trim(), String.valueOf(entry.getValue()));
                }
            }
            return current;
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid headersJson", ex);
        }
    }

    /** 将 POST 请求体模板解析为可直接发送的 JSON 对象。 */
    private Object parseBodyTemplate(String bodyTemplate) {
        try {
            return objectMapper.readValue(bodyTemplate, Object.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid bodyTemplate", ex);
        }
    }

    /** 根据 recordsPath 从远程响应中定位记录数组。 */
    private List<Map<String, Object>> resolveRecords(TagImportSourceDO source, JsonNode response) {
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

    /** 解析并规范化字段映射配置，保证必需映射不为空。 */
    private Map<String, String> parseFieldMapping(TagImportSourceDO source) {
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

    /** 将远程记录按字段映射转换为统一的标签导入行。 */
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

    /** 从远程记录读取字段原值，字段名缺失时返回 null。 */
    private static Object readValue(Map<String, Object> record, String fieldName) {
        if (record == null || !hasText(fieldName)) {
            return null;
        }
        return record.get(fieldName);
    }

    /** 从远程记录读取字符串字段，空白值统一视为 null。 */
    private static String readString(Map<String, Object> record, String fieldName) {
        Object value = readValue(record, fieldName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /** 兼容多种时间对象和默认字符串格式，解析标签发生时间。 */
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

    /** 校验来源配置必填字段并规范化请求、路径和模板字段。 */
    private static void validateAndNormalize(TagImportSourceDO body) {
        if (body == null) {
            throw new IllegalArgumentException("tag import source body is required");
        }
        if (!hasText(body.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (!hasText(body.getUrl())) {
            throw new IllegalArgumentException("url is required");
        }
        OutboundUrlValidator.validateHttpUrl(body.getUrl().trim());
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

    /** 为来源配置写入启用、请求方法、分页大小和记录路径默认值。 */
    private static void applyDefaults(TagImportSourceDO body) {
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

    /** 将配置的 HTTP 方法文本转换为 Spring HttpMethod。 */
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

    /** 将 HTTP 方法统一为大写，缺省使用 GET。 */
    private static String normalizeMethod(String method) {
        return hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : "GET";
    }

    /** 规范化记录数组路径，缺省读取根节点数组。 */
    private static String normalizeRecordsPath(String recordsPath) {
        return hasText(recordsPath) ? recordsPath.trim() : "$";
    }

    /** 将可选配置文本去除首尾空白，空串统一保存为 null。 */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 判断字符串是否包含非空白字符。 */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalizeHeaderName(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("headersJson 包含空请求头名");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
