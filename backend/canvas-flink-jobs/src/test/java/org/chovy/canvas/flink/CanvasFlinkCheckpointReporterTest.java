package org.chovy.canvas.flink;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 checkpoint 上报器的 HTTP 投递契约。
 */
class CanvasFlinkCheckpointReporterTest {

    /**
     * 每个用例独占的本地 HTTP 服务。
     */
    private HttpServer server;

    /**
     * 用例结束后关闭本地 HTTP 服务。
     */
    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * 应当按后端接口兼容的 JSON 结构发送 checkpoint payload。
     *
     * @throws Exception 本地 HTTP 服务启动或请求处理失败时抛出
     */
    @Test
    void postsControllerCompatibleCheckpointPayload() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> internalToken = new AtomicReference<>();
        server = server(200, method, path, body, internalToken);

        CanvasFlinkCheckpointReporter reporter = new CanvasFlinkCheckpointReporter(
                endpoint(), Duration.ofSeconds(2));

        reporter.report(payload("PASS"));

        assertThat(method).hasValue("POST");
        assertThat(path).hasValue("/warehouse/realtime/pipelines/checkpoints");
        assertThat(internalToken.get()).isNull();
        assertThat(body.get())
                .contains("\"pipelineKey\":\"mysql_cdp_event_log_to_doris_ods\"")
                .contains("\"checkpointId\":\"checkpoint-42\"")
                .contains("\"sourcePartition\":\"mysql-binlog\"")
                .contains("\"sourceOffset\":\"binlog.000001:456\"")
                .contains("\"committedOffset\":\"binlog.000001:456\"")
                .contains("\"watermarkTime\":\"2026-06-06T09:59:58\"")
                .contains("\"checkpointTime\":\"2026-06-06T10:00:00\"")
                .contains("\"lagMs\":1200")
                .contains("\"rowCount\":99")
                .contains("\"status\":\"PASS\"")
                .contains("\"reportedBy\":\"canvas-flink-jobs\"")
                .contains("\"sourceSchemaVersion\":\"v1\"")
                .contains("\"sinkSchemaVersion\":\"v1\"");
    }

    /**
     * 配置内部 token 时应写入内部认证请求头。
     *
     * @throws Exception 本地 HTTP 服务启动或请求处理失败时抛出
     */
    @Test
    void sendsInternalApiTokenWhenConfigured() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> internalToken = new AtomicReference<>();
        server = server(200, method, path, body, internalToken);

        CanvasFlinkCheckpointReporter reporter = new CanvasFlinkCheckpointReporter(
                endpoint(), Duration.ofSeconds(2), "internal-secret");

        reporter.report(payload("PASS"));

        assertThat(internalToken).hasValue("internal-secret");
    }

    /**
     * 后端返回非 2xx 时应让调用方感知上报失败。
     *
     * @throws Exception 本地 HTTP 服务启动或请求处理失败时抛出
     */
    @Test
    void failsOnNonSuccessfulHttpStatus() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/warehouse/realtime/pipelines/checkpoints", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 503, "{\"message\":\"unavailable\"}");
        });
        server.start();

        CanvasFlinkCheckpointReporter reporter = new CanvasFlinkCheckpointReporter(
                endpoint(), Duration.ofSeconds(2));

        assertThatThrownBy(() -> reporter.report(payload("FAIL")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("503");
        assertThat(requests).hasValue(1);
    }

    /**
     * 创建测试用 checkpoint payload。
     *
     * @param status checkpoint 状态
     * @return 测试 payload
     */
    private CanvasFlinkCheckpointReporter.CheckpointPayload payload(String status) {
        return new CanvasFlinkCheckpointReporter.CheckpointPayload(
                "mysql_cdp_event_log_to_doris_ods",
                "checkpoint-42",
                "mysql-binlog",
                "binlog.000001:456",
                "binlog.000001:456",
                "2026-06-06T09:59:58",
                "2026-06-06T10:00:00",
                1200L,
                99L,
                status,
                null,
                "canvas-flink-jobs",
                "v1",
                "v1");
    }

    /**
     * 创建不检查内部 token 的本地 HTTP 服务。
     *
     * @param status 响应状态码
     * @param method 接收到的请求方法
     * @param path 接收到的请求路径
     * @param body 接收到的请求体
     * @return 已启动的 HTTP 服务
     * @throws IOException 服务启动失败时抛出
     */
    private HttpServer server(int status,
                              AtomicReference<String> method,
                              AtomicReference<String> path,
                              AtomicReference<String> body) throws IOException {
        return server(status, method, path, body, null);
    }

    /**
     * 创建会记录请求明细的本地 HTTP 服务。
     *
     * @param status 响应状态码
     * @param method 接收到的请求方法
     * @param path 接收到的请求路径
     * @param body 接收到的请求体
     * @param internalToken 接收到的内部认证 token
     * @return 已启动的 HTTP 服务
     * @throws IOException 服务启动失败时抛出
     */
    private HttpServer server(int status,
                              AtomicReference<String> method,
                              AtomicReference<String> path,
                              AtomicReference<String> body,
                              AtomicReference<String> internalToken) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/warehouse/realtime/pipelines/checkpoints", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes()));
            if (internalToken != null) {
                internalToken.set(exchange.getRequestHeaders().getFirst("X-Canvas-Internal-Token"));
            }
            respond(exchange, status, "{\"code\":0}");
        });
        httpServer.start();
        return httpServer;
    }

    /**
     * 返回当前本地 HTTP 服务的 checkpoint 接口地址。
     *
     * @return checkpoint 接口地址
     */
    private String endpoint() {
        return "http://localhost:" + server.getAddress().getPort()
                + "/warehouse/realtime/pipelines/checkpoints";
    }

    /**
     * 向测试请求写入固定响应。
     *
     * @param exchange HTTP 请求交换对象
     * @param status 响应状态码
     * @param response 响应体
     * @throws IOException 响应写入失败时抛出
     */
    private static void respond(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
