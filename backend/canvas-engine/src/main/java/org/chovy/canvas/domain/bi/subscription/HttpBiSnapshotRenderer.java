package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
/**
 * HttpBiSnapshotRenderer 承载对应领域的业务规则、流程编排和结果转换。
 */
public class HttpBiSnapshotRenderer implements BiSnapshotRenderer {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final List<String> endpointUrls;
    private final int timeoutMs;
    private final AtomicInteger nextEndpointIndex = new AtomicInteger();

    @Autowired
    /**
     * 初始化 HttpBiSnapshotRenderer 实例。
     *
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 HttpBiSnapshotRenderer 流程中的校验、计算或对象转换。
     * @param endpointUrl endpoint url 参数，用于 HttpBiSnapshotRenderer 流程中的校验、计算或对象转换。
     * @param endpointUrls endpoint urls 参数，用于 HttpBiSnapshotRenderer 流程中的校验、计算或对象转换。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    public HttpBiSnapshotRenderer(WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.enabled:false}") boolean enabled,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.url:}") String endpointUrl,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.urls:}") String endpointUrls,
                                  @Value("${canvas.bi.delivery.snapshot.renderer.timeout-ms:15000}") int timeoutMs) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.endpointUrls = endpointUrls(endpointUrl, endpointUrls);
        this.timeoutMs = Math.max(1000, timeoutMs);
    }

    /**
     * 初始化 HttpBiSnapshotRenderer 实例。
     *
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 HttpBiSnapshotRenderer 流程中的校验、计算或对象转换。
     * @param endpointUrl endpoint url 参数，用于 HttpBiSnapshotRenderer 流程中的校验、计算或对象转换。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    public HttpBiSnapshotRenderer(WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper,
                                  boolean enabled,
                                  String endpointUrl,
                                  int timeoutMs) {
        this(webClientBuilder, objectMapper, enabled, endpointUrl, "", timeoutMs);
    }

    @Override
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 configured 的布尔判断结果。
     */
    public boolean configured() {
        return enabled && !endpointUrls.isEmpty();
    }

    @Override
    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    public BiSnapshotRenderResult render(BiSnapshotRenderRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!configured()) {
            throw new IllegalStateException("BI snapshot renderer is not configured");
        }
        RuntimeException lastFailure = null;
        int startIndex = Math.floorMod(nextEndpointIndex.getAndIncrement(), endpointUrls.size());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int attempt = 0; attempt < endpointUrls.size(); attempt++) {
            String endpointUrl = endpointUrls.get((startIndex + attempt) % endpointUrls.size());
            try {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return render(endpointUrl, request);
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        if (endpointUrls.size() == 1 && lastFailure != null) {
            throw lastFailure;
        }
        throw new IllegalStateException("BI snapshot renderer cluster failed across "
                + endpointUrls.size() + " endpoint(s)", lastFailure);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param endpointUrl endpoint url 参数，用于 render 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private BiSnapshotRenderResult render(String endpointUrl, BiSnapshotRenderRequest request) {
        Map<String, Object> response = postRenderRequest(endpointUrl, requestBody(request));
        String contentType = stringValue(response.get("contentType"));
        String format = normalizeFormat(stringValue(response.getOrDefault("format", request.format())));
        if (!hasText(contentType)) {
            contentType = contentType(format);
        }
        String base64 = stringValue(firstValue(response, "base64", "bytes", "data"));
        if (!hasText(base64)) {
            throw new IllegalStateException("BI snapshot renderer response did not include base64 image data");
        }
        return new BiSnapshotRenderResult(format, contentType, Base64.getDecoder().decode(stripDataUrl(base64)));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param primaryEndpointUrl primary endpoint url 参数，用于 endpointUrls 流程中的校验、计算或对象转换。
     * @param clusterEndpointUrls cluster endpoint urls 参数，用于 endpointUrls 流程中的校验、计算或对象转换。
     * @return 返回 endpoint urls 汇总后的集合、分页或映射视图。
     */
    private List<String> endpointUrls(String primaryEndpointUrl, String clusterEndpointUrls) {
        List<String> urls = new ArrayList<>();
        addEndpointUrl(urls, primaryEndpointUrl);
        if (hasText(clusterEndpointUrls)) {
            for (String endpointUrl : clusterEndpointUrls.split(",")) {
                addEndpointUrl(urls, endpointUrl);
            }
        }
        return List.copyOf(urls);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param urls urls 参数，用于 addEndpointUrl 流程中的校验、计算或对象转换。
     * @param endpointUrl endpoint url 参数，用于 addEndpointUrl 流程中的校验、计算或对象转换。
     */
    private void addEndpointUrl(List<String> urls, String endpointUrl) {
        if (!hasText(endpointUrl)) {
            return;
        }
        String normalized = endpointUrl.trim();
        if (!urls.contains(normalized)) {
            urls.add(normalized);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param url url 参数，用于 postRenderRequest 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 postRenderRequest 流程中的校验、计算或对象转换。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 postRenderRequest 流程生成的业务结果。
     */
    protected Map<String, Object> postRenderRequest(String url, Map<String, Object> body) {
        String json = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMillis(timeoutMs));
        if (!hasText(json)) {
            throw new IllegalStateException("BI snapshot renderer returned empty response");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("BI snapshot renderer returned invalid JSON", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 requestBody 流程生成的业务结果。
     */
    private Map<String, Object> requestBody(BiSnapshotRenderRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("html", request.html());
        body.put("resourceUrl", request.resourceUrl());
        body.put("format", normalizeFormat(request.format()));
        body.put("width", request.width());
        body.put("height", request.height());
        body.put("scale", request.scale());
        body.put("metadata", request.metadata());
        return body;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @return 返回 firstValue 流程生成的业务结果。
     */
    private Object firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 strip data url 生成的文本或业务键。
     */
    private String stripDataUrl(String value) {
        int comma = value.indexOf(',');
        if (value.startsWith("data:") && comma >= 0) {
            return value.substring(comma + 1);
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeFormat(String value) {
        String format = value == null || value.isBlank() ? "PNG" : value.trim().toUpperCase(Locale.ROOT);
        return "JPG".equals(format) ? "JPEG" : format;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param format 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 content type 生成的文本或业务键。
     */
    private String contentType(String format) {
        return "JPEG".equals(normalizeFormat(format)) ? "image/jpeg" : "image/png";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
