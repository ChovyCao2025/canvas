package org.chovy.canvas.flink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * CanvasFlinkCheckpointReporter 支撑 flink 场景的后端处理。
 */
public class CanvasFlinkCheckpointReporter {

    /** checkpoint 上报接口地址。 */
    private final URI endpoint;
    /** HTTP 连接和请求超时时间。 */
    private final Duration timeout;
    /** 内部接口认证 token，非空时写入 X-Canvas-Internal-Token 请求头。 */
    private final String internalApiToken;
    /** Java 标准 HTTP 客户端，用于同步发送 checkpoint 请求。 */
    private final HttpClient httpClient;
    /** checkpoint payload JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建不带内部认证 token 的 checkpoint 上报器。
     *
     * <p>适用于本地测试或网关已经完成认证的部署；请求仍会同步发送并校验 HTTP 状态码。
     *
     * @param endpoint checkpoint 上报接口地址
     * @param timeout 请求超时时间，传 null 时默认 5 秒
     */
    public CanvasFlinkCheckpointReporter(String endpoint, Duration timeout) {
        this(endpoint, timeout, "");
    }

    /**
     * 创建带内部认证 token 的 checkpoint 上报器。
     *
     * <p>上报采用同步 HTTP POST；非 2xx、序列化失败、IO 失败或线程中断都会转换为运行时异常交给作业入口处理。
     *
     * @param endpoint checkpoint 上报接口地址
     * @param timeout 请求超时时间，传 null 时默认 5 秒
     * @param internalApiToken 内部接口认证 token，可为空
     */
    public CanvasFlinkCheckpointReporter(String endpoint, Duration timeout, String internalApiToken) {
        this(URI.create(required(endpoint, "checkpoint endpoint")),
                timeout == null ? Duration.ofSeconds(5) : timeout,
                internalApiToken,
                HttpClient.newBuilder()
                        .connectTimeout(timeout == null ? Duration.ofSeconds(5) : timeout)
                        .build(),
                new ObjectMapper());
    }

    /**
     * 使用可替换依赖创建 checkpoint 上报器，便于单元测试注入 HTTP 客户端和序列化器。
     *
     * @param endpoint checkpoint 上报接口地址
     * @param timeout 请求超时时间
     * @param internalApiToken 内部接口认证 token
     * @param httpClient HTTP 客户端
     * @param objectMapper JSON 序列化器
     */
    CanvasFlinkCheckpointReporter(URI endpoint,
                                  Duration timeout,
                                  String internalApiToken,
                                  HttpClient httpClient,
                                  ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.internalApiToken = internalApiToken == null ? "" : internalApiToken.trim();
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步上报一条 checkpoint 语义事件。
     *
     * <p>当前作业入口主要用于上报启动提交 WARN 和提交失败 FAIL；运行期真实 checkpoint 可复用同一 payload 结构。
     * <p>接口返回非 2xx 会抛出异常，避免调用方误以为作业状态已经被平台记录。
     *
     * @param payload checkpoint 上报内容
     */
    public void report(CheckpointPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("checkpoint payload is required");
        }
        String body = toJson(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (!internalApiToken.isBlank()) {
            builder.header("X-Canvas-Internal-Token", internalApiToken);
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("checkpoint report failed with HTTP "
                        + response.statusCode() + ": " + response.body());
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException ex) {
            throw new IllegalStateException("checkpoint report request failed", ex);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("checkpoint report request interrupted", ex);
        }
    }

    /**
     * 将 checkpoint payload 序列化为 JSON 请求体。
     *
     * @param payload 待上报的 checkpoint 内容
     * @return JSON 字符串
     */
    private String toJson(CheckpointPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("checkpoint payload cannot be serialized", ex);
        }
    }

    /**
     * 校验必填字符串并返回去除首尾空白后的值。
     *
     * @param value 待校验字符串
     * @param fieldName 字段名，用于错误信息
     * @return 非空字符串
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * checkpoint 上报请求体记录。
     *
     * @param pipelineKey 作业管道标识，对应 registry 中的 pipeline key.
     * @param checkpointId checkpoint 或启动事件唯一标识.
     * @param sourcePartition 源端分区、binlog 分片或启动事件分区标识.
     * @param sourceOffset 源端当前消费 offset.
     * @param committedOffset 已确认提交到目标端的 offset.
     * @param watermarkTime Flink watermark 时间或启动事件时间.
     * @param checkpointTime checkpoint 产生或事件上报时间.
     * @param lagMs 源端到目标端的估算延迟毫秒数.
     * @param rowCount 本次 checkpoint 覆盖或提交的行数.
     * @param status 上报状态，例如 WARN、FAIL 或运行期成功状态.
     * @param errorMessage 失败原因或启动事件说明，成功状态可为空.
     * @param reportedBy 上报方标识.
     * @param sourceSchemaVersion 源端 schema 版本.
     * @param sinkSchemaVersion 目标端 schema 版本.
     */
    public record CheckpointPayload(
        String pipelineKey,
        String checkpointId,
        String sourcePartition,
        String sourceOffset,
        String committedOffset,
        String watermarkTime,
        String checkpointTime,
        Long lagMs,
        Long rowCount,
        String status,
        String errorMessage,
        String reportedBy,
        String sourceSchemaVersion,
        String sinkSchemaVersion) {
    }
}
