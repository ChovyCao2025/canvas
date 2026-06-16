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
import java.util.Objects;

/**
 * 负责把 Flink 作业启动和运行期 checkpoint 状态同步上报给 Canvas 后端。
 *
 * <p>上报器只负责同步 HTTP 投递和状态码校验，不在本地做重试或持久化，避免 Flink 作业入口误判平台侧已收到状态。
 */
public class CanvasFlinkCheckpointReporter {

    /**
     * checkpoint 上报接口地址。
     */
    private final URI endpoint;

    /**
     * HTTP 连接和请求超时时间。
     */
    private final Duration timeout;

    /**
     * 内部接口认证 token，非空时写入 X-Canvas-Internal-Token 请求头。
     */
    private final String internalApiToken;

    /**
     * Java 标准 HTTP 客户端，用于同步发送 checkpoint 请求。
     */
    private final HttpClient httpClient;

    /**
     * checkpoint payload JSON 序列化器。
     */
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
                // 非成功响应必须暴露给作业入口，否则平台侧可能缺失关键运行证据。
                throw new IllegalStateException("checkpoint report failed with HTTP "
                        + response.statusCode() + ": " + response.body());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("checkpoint report request failed", ex);
        } catch (InterruptedException ex) {
            // 恢复中断标记，让外层调度或 Flink 运行时仍能感知线程中断。
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
     * checkpoint 上报请求体。
     *
     * <p>该类型保留 record 风格访问器，供现有调用方和测试继续按不可变快照读取字段。
     */
    public static final class CheckpointPayload {

        /**
         * 作业管道标识，对应 registry 中的 pipeline key。
         */
        private final String pipelineKey;

        /**
         * checkpoint 或启动事件唯一标识。
         */
        private final String checkpointId;

        /**
         * 源端分区、binlog 分片或启动事件分区标识。
         */
        private final String sourcePartition;

        /**
         * 源端当前消费 offset。
         */
        private final String sourceOffset;

        /**
         * 已确认提交到目标端的 offset。
         */
        private final String committedOffset;

        /**
         * Flink watermark 时间或启动事件时间。
         */
        private final String watermarkTime;

        /**
         * checkpoint 产生或事件上报时间。
         */
        private final String checkpointTime;

        /**
         * 源端到目标端的估算延迟，单位为毫秒。
         */
        private final Long lagMs;

        /**
         * 本次 checkpoint 覆盖或提交的行数。
         */
        private final Long rowCount;

        /**
         * 上报状态，例如 WARN、FAIL 或运行期成功状态。
         */
        private final String status;

        /**
         * 失败原因或启动事件说明，成功状态可为空。
         */
        private final String errorMessage;

        /**
         * 上报方标识。
         */
        private final String reportedBy;

        /**
         * 源端 schema 版本。
         */
        private final String sourceSchemaVersion;

        /**
         * 目标端 schema 版本。
         */
        private final String sinkSchemaVersion;

        /**
         * 创建 checkpoint 上报请求体。
         *
         * @param pipelineKey 作业管道标识
         * @param checkpointId checkpoint 或启动事件唯一标识
         * @param sourcePartition 源端分区或启动事件分区标识
         * @param sourceOffset 源端当前消费 offset
         * @param committedOffset 已确认提交到目标端的 offset
         * @param watermarkTime Flink watermark 时间或启动事件时间
         * @param checkpointTime checkpoint 产生或事件上报时间
         * @param lagMs 源端到目标端的估算延迟毫秒数
         * @param rowCount 本次 checkpoint 覆盖或提交的行数
         * @param status 上报状态
         * @param errorMessage 失败原因或启动事件说明
         * @param reportedBy 上报方标识
         * @param sourceSchemaVersion 源端 schema 版本
         * @param sinkSchemaVersion 目标端 schema 版本
         */
        public CheckpointPayload(String pipelineKey,
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
            this.pipelineKey = pipelineKey;
            this.checkpointId = checkpointId;
            this.sourcePartition = sourcePartition;
            this.sourceOffset = sourceOffset;
            this.committedOffset = committedOffset;
            this.watermarkTime = watermarkTime;
            this.checkpointTime = checkpointTime;
            this.lagMs = lagMs;
            this.rowCount = rowCount;
            this.status = status;
            this.errorMessage = errorMessage;
            this.reportedBy = reportedBy;
            this.sourceSchemaVersion = sourceSchemaVersion;
            this.sinkSchemaVersion = sinkSchemaVersion;
        }

        /**
         * 返回作业管道标识。
         *
         * @return 作业管道标识
         */
        public String pipelineKey() {
            return pipelineKey;
        }

        /**
         * 返回 checkpoint 或启动事件唯一标识。
         *
         * @return checkpoint 或启动事件唯一标识
         */
        public String checkpointId() {
            return checkpointId;
        }

        /**
         * 返回源端分区、binlog 分片或启动事件分区标识。
         *
         * @return 源端分区标识
         */
        public String sourcePartition() {
            return sourcePartition;
        }

        /**
         * 返回源端当前消费 offset。
         *
         * @return 源端当前消费 offset
         */
        public String sourceOffset() {
            return sourceOffset;
        }

        /**
         * 返回已确认提交到目标端的 offset。
         *
         * @return 已确认提交到目标端的 offset
         */
        public String committedOffset() {
            return committedOffset;
        }

        /**
         * 返回 Flink watermark 时间或启动事件时间。
         *
         * @return watermark 时间
         */
        public String watermarkTime() {
            return watermarkTime;
        }

        /**
         * 返回 checkpoint 产生或事件上报时间。
         *
         * @return checkpoint 时间
         */
        public String checkpointTime() {
            return checkpointTime;
        }

        /**
         * 返回源端到目标端的估算延迟。
         *
         * @return 延迟毫秒数
         */
        public Long lagMs() {
            return lagMs;
        }

        /**
         * 返回本次 checkpoint 覆盖或提交的行数。
         *
         * @return 行数
         */
        public Long rowCount() {
            return rowCount;
        }

        /**
         * 返回上报状态。
         *
         * @return 上报状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回失败原因或启动事件说明。
         *
         * @return 失败原因或启动说明
         */
        public String errorMessage() {
            return errorMessage;
        }

        /**
         * 返回上报方标识。
         *
         * @return 上报方标识
         */
        public String reportedBy() {
            return reportedBy;
        }

        /**
         * 返回源端 schema 版本。
         *
         * @return 源端 schema 版本
         */
        public String sourceSchemaVersion() {
            return sourceSchemaVersion;
        }

        /**
         * 返回目标端 schema 版本。
         *
         * @return 目标端 schema 版本
         */
        public String sinkSchemaVersion() {
            return sinkSchemaVersion;
        }

        /**
         * 返回 Jackson 兼容的作业管道标识 getter。
         *
         * @return 作业管道标识
         */
        public String getPipelineKey() {
            return pipelineKey;
        }

        /**
         * 返回 Jackson 兼容的 checkpoint 标识 getter。
         *
         * @return checkpoint 或启动事件唯一标识
         */
        public String getCheckpointId() {
            return checkpointId;
        }

        /**
         * 返回 Jackson 兼容的源端分区 getter。
         *
         * @return 源端分区标识
         */
        public String getSourcePartition() {
            return sourcePartition;
        }

        /**
         * 返回 Jackson 兼容的源端 offset getter。
         *
         * @return 源端当前消费 offset
         */
        public String getSourceOffset() {
            return sourceOffset;
        }

        /**
         * 返回 Jackson 兼容的提交 offset getter。
         *
         * @return 已确认提交到目标端的 offset
         */
        public String getCommittedOffset() {
            return committedOffset;
        }

        /**
         * 返回 Jackson 兼容的 watermark 时间 getter。
         *
         * @return watermark 时间
         */
        public String getWatermarkTime() {
            return watermarkTime;
        }

        /**
         * 返回 Jackson 兼容的 checkpoint 时间 getter。
         *
         * @return checkpoint 时间
         */
        public String getCheckpointTime() {
            return checkpointTime;
        }

        /**
         * 返回 Jackson 兼容的延迟 getter。
         *
         * @return 延迟毫秒数
         */
        public Long getLagMs() {
            return lagMs;
        }

        /**
         * 返回 Jackson 兼容的行数 getter。
         *
         * @return 行数
         */
        public Long getRowCount() {
            return rowCount;
        }

        /**
         * 返回 Jackson 兼容的状态 getter。
         *
         * @return 上报状态
         */
        public String getStatus() {
            return status;
        }

        /**
         * 返回 Jackson 兼容的错误信息 getter。
         *
         * @return 失败原因或启动说明
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * 返回 Jackson 兼容的上报方 getter。
         *
         * @return 上报方标识
         */
        public String getReportedBy() {
            return reportedBy;
        }

        /**
         * 返回 Jackson 兼容的源端 schema 版本 getter。
         *
         * @return 源端 schema 版本
         */
        public String getSourceSchemaVersion() {
            return sourceSchemaVersion;
        }

        /**
         * 返回 Jackson 兼容的目标端 schema 版本 getter。
         *
         * @return 目标端 schema 版本
         */
        public String getSinkSchemaVersion() {
            return sinkSchemaVersion;
        }

        /**
         * 按字段值判断两个 checkpoint payload 是否相同。
         *
         * @param o 待比较对象
         * @return true 表示所有字段相同
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CheckpointPayload that)) {
                return false;
            }
            return Objects.equals(pipelineKey, that.pipelineKey)
                    && Objects.equals(checkpointId, that.checkpointId)
                    && Objects.equals(sourcePartition, that.sourcePartition)
                    && Objects.equals(sourceOffset, that.sourceOffset)
                    && Objects.equals(committedOffset, that.committedOffset)
                    && Objects.equals(watermarkTime, that.watermarkTime)
                    && Objects.equals(checkpointTime, that.checkpointTime)
                    && Objects.equals(lagMs, that.lagMs)
                    && Objects.equals(rowCount, that.rowCount)
                    && Objects.equals(status, that.status)
                    && Objects.equals(errorMessage, that.errorMessage)
                    && Objects.equals(reportedBy, that.reportedBy)
                    && Objects.equals(sourceSchemaVersion, that.sourceSchemaVersion)
                    && Objects.equals(sinkSchemaVersion, that.sinkSchemaVersion);
        }

        /**
         * 基于所有字段生成 hashCode，保持不可变值对象语义。
         *
         * @return 字段组合哈希值
         */
        @Override
        public int hashCode() {
            int result = Objects.hashCode(pipelineKey);
            result = 31 * result + Objects.hashCode(checkpointId);
            result = 31 * result + Objects.hashCode(sourcePartition);
            result = 31 * result + Objects.hashCode(sourceOffset);
            result = 31 * result + Objects.hashCode(committedOffset);
            result = 31 * result + Objects.hashCode(watermarkTime);
            result = 31 * result + Objects.hashCode(checkpointTime);
            result = 31 * result + Objects.hashCode(lagMs);
            result = 31 * result + Objects.hashCode(rowCount);
            result = 31 * result + Objects.hashCode(status);
            result = 31 * result + Objects.hashCode(errorMessage);
            result = 31 * result + Objects.hashCode(reportedBy);
            result = 31 * result + Objects.hashCode(sourceSchemaVersion);
            result = 31 * result + Objects.hashCode(sinkSchemaVersion);
            return result;
        }

        /**
         * 返回与原 record 形式一致的调试字符串。
         *
         * @return 字段名和值组成的字符串
         */
        @Override
        public String toString() {
            return "CheckpointPayload["
                    + "pipelineKey=" + pipelineKey
                    + ", checkpointId=" + checkpointId
                    + ", sourcePartition=" + sourcePartition
                    + ", sourceOffset=" + sourceOffset
                    + ", committedOffset=" + committedOffset
                    + ", watermarkTime=" + watermarkTime
                    + ", checkpointTime=" + checkpointTime
                    + ", lagMs=" + lagMs
                    + ", rowCount=" + rowCount
                    + ", status=" + status
                    + ", errorMessage=" + errorMessage
                    + ", reportedBy=" + reportedBy
                    + ", sourceSchemaVersion=" + sourceSchemaVersion
                    + ", sinkSchemaVersion=" + sinkSchemaVersion
                    + ']';
        }
    }
}
