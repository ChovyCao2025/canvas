package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingMonitorProviderPollClient 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorProviderPollClient implements MarketingMonitorPollClient {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final String X_RECENT_SEARCH = "X_RECENT_SEARCH";
    private static final String YOUTUBE_SEARCH = "YOUTUBE_SEARCH";
    private static final String GOOGLE_BUSINESS_REVIEWS = "GOOGLE_BUSINESS_REVIEWS";
    private static final String TIKTOK_RESEARCH_VIDEO = "TIKTOK_RESEARCH_VIDEO";

    private final MarketingMonitorProviderHttpTransport transport;
    private final MarketingMonitorProviderCredentialResolver credentialResolver;
    private final ObjectMapper objectMapper;

    /**
     * 创建 MarketingMonitorProviderPollClient 实例并注入 domain.monitoring 场景依赖。
     * @param transport transport 参数，用于 MarketingMonitorProviderPollClient 流程中的校验、计算或对象转换。
     * @param credentialResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingMonitorProviderPollClient(MarketingMonitorProviderHttpTransport transport,
                                              MarketingMonitorProviderCredentialResolver credentialResolver,
                                              ObjectMapper objectMapper) {
        this.transport = transport;
        this.credentialResolver = credentialResolver;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 判断监控数据源类型是否由真实外部平台轮询客户端支持。
     *
     * @param sourceType 监控源类型，如 X 最近搜索、YouTube 搜索、Google 商家评论或 TikTok Research 视频
     * @return {@code true} 表示该类型会通过外部平台 API 拉取数据
     */
    @Override
    public boolean supports(String sourceType) {
        String normalized = normalizeSourceType(sourceType);
        return X_RECENT_SEARCH.equals(normalized)
                || YOUTUBE_SEARCH.equals(normalized)
                || GOOGLE_BUSINESS_REVIEWS.equals(normalized)
                || TIKTOK_RESEARCH_VIDEO.equals(normalized);
    }

    /**
     * 按监控源类型从外部平台拉取一页原始内容。
     *
     * <p>方法会校验请求、解析凭据引用、组装平台 API 参数并通过 HTTP transport 发起请求，再把平台响应转换为统一的
     * {@link MarketingMonitorPollItem} 列表、下一页游标和调用元数据；不负责写入监控表或做情感推断。</p>
     *
     * @param request 本次轮询的租户、源类型、时间窗口、游标、数量限制和源元数据
     * @return 标准化轮询响应，包含内容项、下一页游标和提供方响应元数据
     */
    @Override
    public MarketingMonitorPollResponse fetch(MarketingMonitorPollRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("monitor provider poll request is required");
        }
        String sourceType = normalizeSourceType(request.sourceType());
        return switch (sourceType) {
            case X_RECENT_SEARCH -> fetchXRecentSearch(request);
            case YOUTUBE_SEARCH -> fetchYouTubeSearch(request);
            case GOOGLE_BUSINESS_REVIEWS -> fetchGoogleBusinessReviews(request);
            case TIKTOK_RESEARCH_VIDEO -> fetchTikTokResearchVideos(request);
            default -> throw new IllegalStateException("unsupported monitoring provider source type: " + request.sourceType());
        };
    }

    /**
     * 执行 fetchXRecentSearch 流程，围绕 fetch xrecent search 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 fetchXRecentSearch 流程生成的业务结果。
     */
    private MarketingMonitorPollResponse fetchXRecentSearch(MarketingMonitorPollRequest request) {
        String token = bearerToken(request);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", required(metadataText(request, "query"), "query"));
        params.put("max_results", String.valueOf(bounded(request.maxItems(), 10, 100)));
        params.put("tweet.fields", "created_at,lang,author_id");
        params.put("expansions", "author_id");
        params.put("user.fields", "username,name");
        putIfPresent(params, "next_token", request.cursor());
        putIfPresent(params, "start_time", instantParam(request.requestedFrom()));
        putIfPresent(params, "end_time", instantParam(request.requestedUntil()));

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "GET",
                uri("https://api.x.com/2/tweets/search/recent", params),
                Map.of("Authorization", "Bearer " + token),
                ""));
        JsonNode root = json(response.body());
        Map<String, String> users = xUsers(root.path("includes").path("users"));
        List<MarketingMonitorPollItem> items = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (JsonNode node : iterable(root.path("data"))) {
            String id = text(node, "id");
            if (!hasText(id)) {
                continue;
            }
            String authorId = text(node, "author_id");
            items.add(new MarketingMonitorPollItem(
                    "x:" + id,
                    "https://x.com/i/web/status/" + id,
                    users.getOrDefault(authorId, authorId),
                    metadataText(request, "brandKey"),
                    text(node, "text"),
                    text(node, "lang"),
                    parseTime(text(node, "created_at")),
                    raw(node)));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorPollResponse(
                items,
                text(root.path("meta"), "next_token"),
                metadata(X_RECENT_SEARCH, response));
    }

    /**
     * 执行 fetchYouTubeSearch 流程，围绕 fetch you tube search 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 fetchYouTubeSearch 流程生成的业务结果。
     */
    private MarketingMonitorPollResponse fetchYouTubeSearch(MarketingMonitorPollRequest request) {
        String apiKey = apiKey(request);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("part", "snippet");
        params.put("type", "video");
        params.put("q", required(metadataText(request, "query"), "query"));
        params.put("maxResults", String.valueOf(bounded(request.maxItems(), 1, 50)));
        params.put("key", apiKey);
        putIfPresent(params, "pageToken", request.cursor());
        putIfPresent(params, "regionCode", metadataText(request, "regionCode"));
        putIfPresent(params, "publishedAfter", instantParam(request.requestedFrom()));
        putIfPresent(params, "publishedBefore", instantParam(request.requestedUntil()));

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "GET",
                uri("https://youtube.googleapis.com/youtube/v3/search", params),
                Map.of(),
                ""));
        JsonNode root = json(response.body());
        List<MarketingMonitorPollItem> items = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (JsonNode node : iterable(root.path("items"))) {
            JsonNode snippet = node.path("snippet");
            String videoId = text(node.path("id"), "videoId");
            if (!hasText(videoId)) {
                continue;
            }
            String title = text(snippet, "title");
            String description = text(snippet, "description");
            items.add(new MarketingMonitorPollItem(
                    "youtube:" + videoId,
                    "https://www.youtube.com/watch?v=" + videoId,
                    text(snippet, "channelTitle"),
                    metadataText(request, "brandKey"),
                    joinText(title, description),
                    null,
                    parseTime(text(snippet, "publishedAt")),
                    raw(node)));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorPollResponse(
                items,
                text(root, "nextPageToken"),
                metadata(YOUTUBE_SEARCH, response));
    }

    /**
     * 执行 fetchGoogleBusinessReviews 流程，围绕 fetch google business reviews 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 fetchGoogleBusinessReviews 流程生成的业务结果。
     */
    private MarketingMonitorPollResponse fetchGoogleBusinessReviews(MarketingMonitorPollRequest request) {
        String token = bearerToken(request);
        String accountId = resourcePart(required(metadataText(request, "accountId"), "accountId"), "accounts/");
        String locationId = resourcePart(required(metadataText(request, "locationId"), "locationId"), "locations/");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageSize", String.valueOf(bounded(request.maxItems(), 1, 50)));
        params.put("orderBy", "updateTime desc");
        putIfPresent(params, "pageToken", request.cursor());
        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "GET",
                uri("https://mybusiness.googleapis.com/v4/accounts/" + encodePath(accountId)
                        /**
                         * 执行 encodePath 流程，围绕 encode path 完成校验、计算或结果组装。
                         *
                         * @return 返回 encodePath 流程生成的业务结果。
                         */
                        + "/locations/" + encodePath(locationId) + "/reviews", params),
                Map.of("Authorization", "Bearer " + token),
                ""));
        JsonNode root = json(response.body());
        List<MarketingMonitorPollItem> items = new ArrayList<>();
        for (JsonNode node : iterable(root.path("reviews"))) {
            String reviewId = firstText(node, "reviewId", "name");
            if (!hasText(reviewId)) {
                continue;
            }
            items.add(new MarketingMonitorPollItem(
                    "google-review:" + reviewId,
                    null,
                    text(node.path("reviewer"), "displayName"),
                    metadataText(request, "brandKey"),
                    text(node, "comment"),
                    null,
                    parseTime(firstText(node, "updateTime", "createTime")),
                    raw(node)));
        }
        return new MarketingMonitorPollResponse(
                items,
                text(root, "nextPageToken"),
                metadata(GOOGLE_BUSINESS_REVIEWS, response));
    }

    /**
     * 执行 fetchTikTokResearchVideos 流程，围绕 fetch tik tok research videos 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 fetchTikTokResearchVideos 流程生成的业务结果。
     */
    private MarketingMonitorPollResponse fetchTikTokResearchVideos(MarketingMonitorPollRequest request) {
        String token = bearerToken(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", tiktokQuery(request));
        body.put("max_count", bounded(request.maxItems(), 1, 100));
        Map<String, Object> cursor = cursorMap(request.cursor());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (cursor.get("cursor") != null) {
            body.put("cursor", number(cursor.get("cursor")));
        }
        if (cursor.get("searchId") != null) {
            body.put("search_id", String.valueOf(cursor.get("searchId")));
        }
        putObjectIfPresent(body, "start_date", compactDate(request.requestedFrom()));
        putObjectIfPresent(body, "end_date", compactDate(request.requestedUntil()));

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create("https://open.tiktokapis.com/v2/research/video/query/"),
                Map.of(
                        "Authorization", "Bearer " + token,
                        "Content-Type", "application/json"),
                jsonString(body)));
        JsonNode root = json(response.body()).path("data");
        List<MarketingMonitorPollItem> items = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (JsonNode node : iterable(root.path("videos"))) {
            String id = firstText(node, "id", "video_id");
            if (!hasText(id)) {
                continue;
            }
            Map<String, Object> raw = raw(node);
            raw.put("regionCode", text(node, "region_code"));
            items.add(new MarketingMonitorPollItem(
                    "tiktok:" + id,
                    null,
                    firstText(node, "username", "user_name"),
                    metadataText(request, "brandKey"),
                    firstText(node, "video_description", "description"),
                    null,
                    parseEpochSeconds(node.path("create_time")),
                    raw));
        }
        return new MarketingMonitorPollResponse(
                items,
                tiktokNextCursor(root),
                metadata(TIKTOK_RESEARCH_VIDEO, response));
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    private MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request) {
        MarketingMonitorProviderHttpResponse response = transport.execute(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("monitoring provider HTTP error: " + response.statusCode());
        }
        return response;
    }

    /**
     * 执行 bearerToken 流程，围绕 bearer token 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 bearer token 生成的文本或业务键。
     */
    private String bearerToken(MarketingMonitorPollRequest request) {
        Map<String, Object> credentials = credentials(request.sourceMetadata());
        String mode = normalizeSourceType(string(credentials.get("mode")));
        String ref;
        String token;
        if ("BEARER_ENV".equals(mode)) {
            ref = required(string(credentials.get("tokenEnv")), "tokenEnv");
            token = credentialResolver.resolve(ref);
        // 根据前序判断结果进入后续条件分支。
        } else if ("BEARER_REF".equals(mode)) {
            ref = credentialReference(credentials, "access_token");
            token = credentialResolver.resolve(request.tenantId(), ref);
        } else {
            throw new IllegalArgumentException("credentials.mode must be BEARER_ENV or BEARER_REF");
        }
        if (!hasText(token)) {
            throw new IllegalArgumentException("credential value is not available: " + ref);
        }
        return token;
    }

    /**
     * 执行 apiKey 流程，围绕 api key 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 api key 生成的文本或业务键。
     */
    private String apiKey(MarketingMonitorPollRequest request) {
        Map<String, Object> credentials = credentials(request.sourceMetadata());
        String mode = normalizeSourceType(string(credentials.get("mode")));
        String ref;
        String key;
        if ("API_KEY_ENV".equals(mode)) {
            ref = required(string(credentials.get("apiKeyEnv")), "apiKeyEnv");
            key = credentialResolver.resolve(ref);
        // 根据前序判断结果进入后续条件分支。
        } else if ("API_KEY_REF".equals(mode)) {
            ref = credentialReference(credentials, "api_key");
            key = credentialResolver.resolve(request.tenantId(), ref);
        } else {
            throw new IllegalArgumentException("credentials.mode must be API_KEY_ENV or API_KEY_REF");
        }
        if (!hasText(key)) {
            throw new IllegalArgumentException("credential value is not available: " + ref);
        }
        return key;
    }

    /**
     * 执行 credentialReference 流程，围绕 credential reference 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 credentialReference 流程中的校验、计算或对象转换。
     * @param credentials credentials 参数，用于 credentialReference 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 credential reference 生成的文本或业务键。
     */
    private String credentialReference(Map<String, Object> credentials, String field) {
        String credentialKey = required(string(credentials.get("credentialKey")), "credentialKey");
        return "credential:" + credentialKey.trim().toLowerCase(Locale.ROOT) + ":" + field;
    }

    /**
     * 执行 credentials 流程，围绕 credentials 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 credentials 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 credentials 流程中的校验、计算或对象转换。
     * @return 返回 credentials 流程生成的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> credentials(Map<String, Object> metadata) {
        Object value = metadata == null ? null : metadata.get("credentials");
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("credentials are required");
    }

    /**
     * 执行 tiktokQuery 流程，围绕 tiktok query 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 tiktokQuery 流程生成的业务结果。
     */
    private Map<String, Object> tiktokQuery(MarketingMonitorPollRequest request) {
        Object queryJson = request.sourceMetadata().get("queryJson");
        if (queryJson instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, OBJECT_MAP);
        }
        String query = required(metadataText(request, "query"), "query");
        return Map.of("and", List.of(Map.of(
                "operation", "IN",
                "field_name", "keyword",
                "field_values", List.of(query))));
    }

    /**
     * 执行 cursorMap 流程，围绕 cursor map 完成校验、计算或结果组装。
     *
     * @param cursor cursor 参数，用于 cursorMap 流程中的校验、计算或对象转换。
     * @return 返回 cursorMap 流程生成的业务结果。
     */
    private Map<String, Object> cursorMap(String cursor) {
        if (!hasText(cursor) || !cursor.trim().startsWith("{")) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(cursor, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 执行 tiktokNextCursor 流程，围绕 tiktok next cursor 完成校验、计算或结果组装。
     *
     * @param data data 参数，用于 tiktokNextCursor 流程中的校验、计算或对象转换。
     * @return 返回 tiktok next cursor 生成的文本或业务键。
     */
    private String tiktokNextCursor(JsonNode data) {
        if (!data.path("has_more").asBoolean(false)) {
            return null;
        }
        Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put("cursor", data.path("cursor").asLong());
        String searchId = text(data, "search_id");
        if (hasText(searchId)) {
            cursor.put("searchId", searchId);
        }
        return jsonString(cursor);
    }

    /**
     * 执行 uri 流程，围绕 uri 完成校验、计算或结果组装。
     *
     * @param base base 参数，用于 uri 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 uri 流程中的校验、计算或对象转换。
     * @param params params 参数，用于 uri 流程中的校验、计算或对象转换。
     * @return 返回 uri 流程生成的业务结果。
     */
    private URI uri(String base, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(base);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!params.isEmpty()) {
            builder.append("?");
            boolean first = true;
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!hasText(entry.getValue())) {
                    continue;
                }
                if (!first) {
                    builder.append("&");
                }
                first = false;
                builder.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return URI.create(builder.toString());
    }

    /**
     * 执行 metadataText 流程，围绕 metadata text 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 metadata text 生成的文本或业务键。
     */
    private String metadataText(MarketingMonitorPollRequest request, String key) {
        return string(request.sourceMetadata().get(key));
    }

    /**
     * 执行 metadata 流程，围绕 metadata 完成校验、计算或结果组装。
     *
     * @param provider provider 参数，用于 metadata 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 metadata 流程中的校验、计算或对象转换。
     * @return 返回 metadata 流程生成的业务结果。
     */
    private Map<String, Object> metadata(String provider, MarketingMonitorProviderHttpResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", provider);
        metadata.put("httpStatus", response.statusCode());
        String requestId = response.headers().get("x-request-id");
        if (hasText(requestId)) {
            metadata.put("providerRequestId", requestId);
        }
        return metadata;
    }

    /**
     * 执行 xUsers 流程，围绕 x users 完成校验、计算或结果组装。
     *
     * @param users users 参数，用于 xUsers 流程中的校验、计算或对象转换。
     * @return 返回 x users 生成的文本或业务键。
     */
    private Map<String, String> xUsers(JsonNode users) {
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonNode user : iterable(users)) {
            String id = text(user, "id");
            String username = text(user, "username");
            if (hasText(id) && hasText(username)) {
                result.put(id, username);
            }
        }
        return result;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 json 流程生成的业务结果。
     */
    private JsonNode json(String body) {
        try {
            return objectMapper.readTree(hasText(body) ? body : "{}");
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("monitoring provider JSON parse failed", ex);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json string 生成的文本或业务键。
     */
    private String jsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("monitoring provider JSON serialization failed", ex);
        }
    }

    /**
     * 执行 raw 流程，围绕 raw 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 raw 流程中的校验、计算或对象转换。
     * @return 返回 raw 流程生成的业务结果。
     */
    private Map<String, Object> raw(JsonNode node) {
        return objectMapper.convertValue(node, OBJECT_MAP);
    }

    /**
     * 执行 iterable 流程，围绕 iterable 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 iterable 流程中的校验、计算或对象转换。
     * @return 返回 iterable 汇总后的集合、分页或映射视图。
     */
    private List<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        node.forEach(values::add);
        return values;
    }

    /**
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDateTime parseTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    }

    /**
     * 解析并校验输入数据。
     *
     * @param node node 参数，用于 parseEpochSeconds 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDateTime parseEpochSeconds(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(node.asLong()), ZoneOffset.UTC);
    }

    /**
     * 执行 instantParam 流程，围绕 instant param 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 instant param 生成的文本或业务键。
     */
    private String instantParam(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }

    /**
     * 执行 compactDate 流程，围绕 compact date 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 compact date 生成的文本或业务键。
     */
    private String compactDate(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (hasText(value)) {
            map.put(key, value.trim());
        }
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putObjectIfPresent 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 putObjectIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void putObjectIfPresent(Map<String, Object> map, String key, String value) {
        if (hasText(value)) {
            map.put(key, value.trim());
        }
    }

    /**
     * 执行 firstText 流程，围绕 first text 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @return 返回 first text 生成的文本或业务键。
     */
    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = text(node, key);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String key) {
        if (node == null || key == null) {
            return null;
        }
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    /**
     * 执行 joinText 流程，围绕 join text 完成校验、计算或结果组装。
     *
     * @param first first 参数，用于 joinText 流程中的校验、计算或对象转换。
     * @param second second 参数，用于 joinText 流程中的校验、计算或对象转换。
     * @return 返回 join text 生成的文本或业务键。
     */
    private String joinText(String first, String second) {
        if (!hasText(second)) {
            return first;
        }
        if (!hasText(first)) {
            return second;
        }
        return first + "\n" + second;
    }

    /**
     * 执行 resourcePart 流程，围绕 resource part 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param prefix prefix 参数，用于 resourcePart 流程中的校验、计算或对象转换。
     * @return 返回 resource part 生成的文本或业务键。
     */
    private String resourcePart(String value, String prefix) {
        String trimmed = value.trim();
        return trimmed.startsWith(prefix) ? trimmed.substring(prefix.length()) : trimmed;
    }

    /**
     * 执行 number 流程，围绕 number 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 number 计算得到的数量、金额或指标值。
     */
    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && hasText(text)) {
            return new BigDecimal(text).longValue();
        }
        return 0L;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param min min 参数，用于 bounded 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 bounded 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int bounded(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeSourceType(String sourceType) {
        return hasText(sourceType) ? sourceType.trim().toUpperCase(Locale.ROOT) : "";
    }

    /**
     * 执行 encodePath 流程，围绕 encode path 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 encode path 生成的文本或业务键。
     */
    private String encodePath(String value) {
        return urlEncode(value).replace("+", "%20");
    }

    /**
     * 执行 urlEncode 流程，围绕 url encode 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 url encode 生成的文本或业务键。
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
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
}
