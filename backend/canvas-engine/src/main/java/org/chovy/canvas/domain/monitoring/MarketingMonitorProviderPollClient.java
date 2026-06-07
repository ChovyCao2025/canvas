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

    public MarketingMonitorProviderPollClient(MarketingMonitorProviderHttpTransport transport,
                                              MarketingMonitorProviderCredentialResolver credentialResolver,
                                              ObjectMapper objectMapper) {
        this.transport = transport;
        this.credentialResolver = credentialResolver;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public boolean supports(String sourceType) {
        String normalized = normalizeSourceType(sourceType);
        return X_RECENT_SEARCH.equals(normalized)
                || YOUTUBE_SEARCH.equals(normalized)
                || GOOGLE_BUSINESS_REVIEWS.equals(normalized)
                || TIKTOK_RESEARCH_VIDEO.equals(normalized);
    }

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

        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "GET",
                uri("https://api.x.com/2/tweets/search/recent", params),
                Map.of("Authorization", "Bearer " + token),
                ""));
        JsonNode root = json(response.body());
        Map<String, String> users = xUsers(root.path("includes").path("users"));
        List<MarketingMonitorPollItem> items = new ArrayList<>();
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
        return new MarketingMonitorPollResponse(
                items,
                text(root.path("meta"), "next_token"),
                metadata(X_RECENT_SEARCH, response));
    }

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

        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "GET",
                uri("https://youtube.googleapis.com/youtube/v3/search", params),
                Map.of(),
                ""));
        JsonNode root = json(response.body());
        List<MarketingMonitorPollItem> items = new ArrayList<>();
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
        return new MarketingMonitorPollResponse(
                items,
                text(root, "nextPageToken"),
                metadata(YOUTUBE_SEARCH, response));
    }

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

    private MarketingMonitorPollResponse fetchTikTokResearchVideos(MarketingMonitorPollRequest request) {
        String token = bearerToken(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", tiktokQuery(request));
        body.put("max_count", bounded(request.maxItems(), 1, 100));
        Map<String, Object> cursor = cursorMap(request.cursor());
        if (cursor.get("cursor") != null) {
            body.put("cursor", number(cursor.get("cursor")));
        }
        if (cursor.get("searchId") != null) {
            body.put("search_id", String.valueOf(cursor.get("searchId")));
        }
        putObjectIfPresent(body, "start_date", compactDate(request.requestedFrom()));
        putObjectIfPresent(body, "end_date", compactDate(request.requestedUntil()));

        MarketingMonitorProviderHttpResponse response = execute(new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create("https://open.tiktokapis.com/v2/research/video/query/"),
                Map.of(
                        "Authorization", "Bearer " + token,
                        "Content-Type", "application/json"),
                jsonString(body)));
        JsonNode root = json(response.body()).path("data");
        List<MarketingMonitorPollItem> items = new ArrayList<>();
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

    private MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request) {
        MarketingMonitorProviderHttpResponse response = transport.execute(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("monitoring provider HTTP error: " + response.statusCode());
        }
        return response;
    }

    private String bearerToken(MarketingMonitorPollRequest request) {
        Map<String, Object> credentials = credentials(request.sourceMetadata());
        String mode = normalizeSourceType(string(credentials.get("mode")));
        String ref;
        String token;
        if ("BEARER_ENV".equals(mode)) {
            ref = required(string(credentials.get("tokenEnv")), "tokenEnv");
            token = credentialResolver.resolve(ref);
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

    private String apiKey(MarketingMonitorPollRequest request) {
        Map<String, Object> credentials = credentials(request.sourceMetadata());
        String mode = normalizeSourceType(string(credentials.get("mode")));
        String ref;
        String key;
        if ("API_KEY_ENV".equals(mode)) {
            ref = required(string(credentials.get("apiKeyEnv")), "apiKeyEnv");
            key = credentialResolver.resolve(ref);
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

    private String credentialReference(Map<String, Object> credentials, String field) {
        String credentialKey = required(string(credentials.get("credentialKey")), "credentialKey");
        return "credential:" + credentialKey.trim().toLowerCase(Locale.ROOT) + ":" + field;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> credentials(Map<String, Object> metadata) {
        Object value = metadata == null ? null : metadata.get("credentials");
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("credentials are required");
    }

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

    private Map<String, Object> cursorMap(String cursor) {
        if (!hasText(cursor) || !cursor.trim().startsWith("{")) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(cursor, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

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

    private URI uri(String base, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(base);
        if (!params.isEmpty()) {
            builder.append("?");
            boolean first = true;
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
        return URI.create(builder.toString());
    }

    private String metadataText(MarketingMonitorPollRequest request, String key) {
        return string(request.sourceMetadata().get(key));
    }

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

    private JsonNode json(String body) {
        try {
            return objectMapper.readTree(hasText(body) ? body : "{}");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("monitoring provider JSON parse failed", ex);
        }
    }

    private String jsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("monitoring provider JSON serialization failed", ex);
        }
    }

    private Map<String, Object> raw(JsonNode node) {
        return objectMapper.convertValue(node, OBJECT_MAP);
    }

    private List<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        node.forEach(values::add);
        return values;
    }

    private LocalDateTime parseTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    }

    private LocalDateTime parseEpochSeconds(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(node.asLong()), ZoneOffset.UTC);
    }

    private String instantParam(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }

    private String compactDate(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (hasText(value)) {
            map.put(key, value.trim());
        }
    }

    private void putObjectIfPresent(Map<String, Object> map, String key, String value) {
        if (hasText(value)) {
            map.put(key, value.trim());
        }
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = text(node, key);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String key) {
        if (node == null || key == null) {
            return null;
        }
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String joinText(String first, String second) {
        if (!hasText(second)) {
            return first;
        }
        if (!hasText(first)) {
            return second;
        }
        return first + "\n" + second;
    }

    private String resourcePart(String value, String prefix) {
        String trimmed = value.trim();
        return trimmed.startsWith(prefix) ? trimmed.substring(prefix.length()) : trimmed;
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && hasText(text)) {
            return new BigDecimal(text).longValue();
        }
        return 0L;
    }

    private int bounded(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeSourceType(String sourceType) {
        return hasText(sourceType) ? sourceType.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String encodePath(String value) {
        return urlEncode(value).replace("+", "%20");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
