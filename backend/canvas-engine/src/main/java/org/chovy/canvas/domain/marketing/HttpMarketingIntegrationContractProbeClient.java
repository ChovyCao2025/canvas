package org.chovy.canvas.domain.marketing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HttpMarketingIntegrationContractProbeClient 编排 domain.marketing 场景的领域业务规则。
 */
@Component
public class HttpMarketingIntegrationContractProbeClient implements MarketingIntegrationContractProbeClient {

    static final String PROBLEM_TYPE_URI = "urn:canvas:marketing-integration:http-probe";

    private final HttpClient httpClient;
    private final String baseUrl;

    /**
     * 创建 HttpMarketingIntegrationContractProbeClient 实例并注入 domain.marketing 场景依赖。
     * @param baseUrl base url 参数，用于 HttpMarketingIntegrationContractProbeClient 流程中的校验、计算或对象转换。
     */
    @Autowired
    public HttpMarketingIntegrationContractProbeClient(
            @Value("${canvas.marketing-integrations.probe.base-url:}") String baseUrl) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), baseUrl);
    }

    /**
     * 执行 HttpMarketingIntegrationContractProbeClient 流程，围绕 http marketing integration contract probe client 完成校验、计算或结果组装。
     *
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param baseUrl base url 参数，用于 HttpMarketingIntegrationContractProbeClient 流程中的校验、计算或对象转换。
     */
    HttpMarketingIntegrationContractProbeClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    /**
     * probe 处理 domain.marketing 场景的业务逻辑。
     * @param target target 参数，用于 probe 流程中的校验、计算或对象转换。
     * @return 返回 probe 流程生成的业务结果。
     */
    @Override
    public ProbeResult probe(ProbeTarget target) {
        URI uri = probeUri(target);
        String method = stringMetadata(target, "probeMethod", "GET").toUpperCase(Locale.ROOT);
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            throw new IllegalArgumentException("unsupported integration probe method: " + method);
        }
        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(timeoutMs(target.timeoutMs())))
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "canvas-marketing-integration-probe/1.0")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = Math.max(0L, Duration.ofNanos(System.nanoTime() - started).toMillis());
            String status = statusFor(response.statusCode());
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("transport", "http");
            evidence.put("method", method);
            evidence.put("uriScheme", uri.getScheme());
            evidence.put("uriHost", uri.getHost());
            evidence.put("uriPath", uri.getPath());
            evidence.put("responseBytes", response.body() == null ? 0 : response.body().length());
            return new ProbeResult(
                    status,
                    response.statusCode(),
                    latencyMs,
                    PROBLEM_TYPE_URI,
                    "PASS".equals(status) ? null : "provider probe returned HTTP " + response.statusCode(),
                    "PASS".equals(status) ? "Provider health endpoint passed" : "Provider health endpoint did not pass",
                    evidence);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("integration contract probe interrupted", e);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            throw e;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e);
        }
    }

    /**
     * 执行 probeUri 流程，围绕 probe uri 完成校验、计算或结果组装。
     *
     * @param target target 参数，用于 probeUri 流程中的校验、计算或对象转换。
     * @return 返回 probeUri 流程生成的业务结果。
     */
    private URI probeUri(ProbeTarget target) {
        // 准备本次处理所需的上下文和中间变量。
        String override = stringMetadata(target, "probeUrl", null);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (override != null) {
            return absoluteHttpUri(override);
        }
        String apiRoot = required(target.apiRoot(), "apiRoot");
        URI root = URI.create(apiRoot);
        if (!isAbsoluteHttp(root)) {
            if (baseUrl.isBlank()) {
                throw new IllegalArgumentException("relative integration apiRoot requires canvas.marketing-integrations.probe.base-url");
            }
            root = URI.create(baseUrl).resolve(apiRoot.startsWith("/") ? apiRoot.substring(1) : apiRoot);
        }
        String probePath = stringMetadata(target, "probePath", null);
        URI uri = probePath == null ? root : root.resolve(probePath);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return absoluteHttpUri(uri.toString());
    }

    /**
     * 执行 absoluteHttpUri 流程，围绕 absolute http uri 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 absoluteHttpUri 流程生成的业务结果。
     */
    private URI absoluteHttpUri(String value) {
        URI uri = URI.create(required(value, "probeUrl"));
        if (!isAbsoluteHttp(uri)) {
            throw new IllegalArgumentException("integration probe URL must be absolute HTTP(S)");
        }
        return uri;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param uri uri 参数，用于 isAbsoluteHttp 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isAbsoluteHttp(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    /**
     * 执行 statusFor 流程，围绕 status for 完成校验、计算或结果组装。
     *
     * @param httpStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回 status for 生成的文本或业务键。
     */
    private static String statusFor(int httpStatus) {
        if (httpStatus >= 200 && httpStatus <= 299) {
            return "PASS";
        }
        if (httpStatus >= 300 && httpStatus <= 399) {
            return "WARN";
        }
        return "FAIL";
    }

    /**
     * 执行 timeoutMs 流程，围绕 timeout ms 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 timeout ms 计算得到的数量、金额或指标值。
     */
    private static int timeoutMs(Integer value) {
        if (value == null || value <= 0) {
            return 5000;
        }
        return Math.max(1000, Math.min(value, 60000));
    }

    /**
     * 执行 stringMetadata 流程，围绕 string metadata 完成校验、计算或结果组装。
     *
     * @param target target 参数，用于 stringMetadata 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param fallback fallback 参数，用于 stringMetadata 流程中的校验、计算或对象转换。
     * @return 返回 string metadata 生成的文本或业务键。
     */
    private static String stringMetadata(ProbeTarget target, String key, String fallback) {
        if (target == null || target.metadata() == null) {
            return fallback;
        }
        Object value = target.metadata().get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
