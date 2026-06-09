package org.chovy.canvas.domain.monitoring;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JavaMarketingMonitorProviderHttpTransport 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class JavaMarketingMonitorProviderHttpTransport implements MarketingMonitorProviderHttpTransport {

    private final HttpClient httpClient;

    /**
     * 创建 JavaMarketingMonitorProviderHttpTransport 实例并注入 domain.monitoring 场景依赖。
     */
    public JavaMarketingMonitorProviderHttpTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    /**
     * 执行 JavaMarketingMonitorProviderHttpTransport 流程，围绕 java marketing monitor provider http transport 完成校验、计算或结果组装。
     *
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     */
    JavaMarketingMonitorProviderHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 使用 JDK HttpClient 执行监控提供方 HTTP 请求。
     *
     * <p>方法支持 GET 和 POST，透传请求头与请求体，并把状态码、响应体和首个响应头值封装为统一响应对象。
     * 网络失败或线程中断会转为运行时异常，供上层轮询逻辑记录失败和重试。</p>
     *
     * @param request 已组装好的监控提供方 HTTP 请求
     * @return HTTP 状态码、响应体和响应头摘要
     */
    @Override
    public MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(Duration.ofSeconds(30));
        request.headers().forEach(builder::header);
        if ("POST".equals(request.method())) {
            builder.POST(HttpRequest.BodyPublishers.ofString(request.body()));
        } else {
            builder.GET();
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            });
            return new MarketingMonitorProviderHttpResponse(response.statusCode(), response.body(), headers);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException ex) {
            throw new IllegalStateException("monitoring provider HTTP request failed", ex);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("monitoring provider HTTP request interrupted", ex);
        }
    }
}
