package org.chovy.canvas.domain.approval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpLarkApprovalClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createInstanceSendsIdempotencyUuidAndApprovalPayload() throws Exception {
        AtomicReference<Map<String, Object>> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedAuthorization = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            capturedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] request = exchange.getRequestBody().readAllBytes();
            capturedBody.set(objectMapper.readValue(request, new TypeReference<>() {}));
            respond(exchange, "{\"code\":0,\"data\":{\"instance_code\":\"lark-instance-101\"}}");
        });
        server.start();
        try {
            HttpLarkApprovalClient client = client(server);

            String instanceCode = client.createInstance(new LarkApprovalCreateInstanceRequest(
                    7L,
                    "approval-code",
                    "canvas-approval-101",
                    "ou_submitter",
                    null,
                    "od_growth",
                    "[]"));

            assertThat(instanceCode).isEqualTo("lark-instance-101");
            assertThat(capturedBody.get())
                    .containsEntry("approval_code", "approval-code")
                    .containsEntry("uuid", "canvas-approval-101")
                    .containsEntry("open_id", "ou_submitter")
                    .containsEntry("department_id", "od_growth")
                    .containsEntry("form", "[]");
            assertThat(capturedAuthorization.get()).isEqualTo("Bearer tenant-token");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getInstanceReadsCompatibleTaskListFields() throws Exception {
        AtomicReference<String> capturedAuthorization = new AtomicReference<>();
        AtomicReference<String> capturedQuery = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            capturedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedQuery.set(exchange.getRequestURI().getQuery());
            respond(exchange, """
                {
                  "code": 0,
                  "data": {
                    "instance_code": "lark-instance-101",
                    "status": "PENDING",
                    "task_list": [
                      {
                        "task_id": "lark-task-201",
                        "status": "PENDING",
                        "open_id": "ou_bob"
                      }
                    ]
                  }
                }
                """);
        });
        server.start();
        try {
            HttpLarkApprovalClient client = client(server);

            LarkApprovalInstanceSnapshot snapshot = client.getInstance(7L, "lark-instance-101");

            assertThat(snapshot.instanceCode()).isEqualTo("lark-instance-101");
            assertThat(snapshot.status()).isEqualTo("PENDING");
            assertThat(snapshot.tasks())
                    .containsExactly(new LarkApprovalTaskSnapshot("lark-task-201", "PENDING", "ou_bob"));
            assertThat(capturedAuthorization.get()).isEqualTo("Bearer user-token");
            assertThat(capturedQuery.get()).contains("user_id_type=open_id");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void approveTaskSendsLarkTaskActionPayload() throws Exception {
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedAuthorization = new AtomicReference<>();
        AtomicReference<Map<String, Object>> capturedBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] request = exchange.getRequestBody().readAllBytes();
            capturedBody.set(objectMapper.readValue(request, new TypeReference<>() {}));
            respond(exchange, "{\"code\":0,\"data\":{}}");
        });
        server.start();
        try {
            HttpLarkApprovalClient client = client(server);

            client.approveTask(new LarkApprovalTaskActionRequest(
                    7L,
                    "lark-instance-101",
                    "lark-task-201",
                    "bob",
                    "检查通过"));

            assertThat(capturedPath.get()).isEqualTo("/open-apis/approval/v4/tasks/approve");
            assertThat(capturedBody.get())
                    .containsEntry("instance_code", "lark-instance-101")
                    .containsEntry("task_id", "lark-task-201")
                    .containsEntry("comment", "检查通过");
            assertThat(capturedAuthorization.get()).isEqualTo("Bearer user-token");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectTaskSendsLarkTaskActionPayload() throws Exception {
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedAuthorization = new AtomicReference<>();
        AtomicReference<Map<String, Object>> capturedBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] request = exchange.getRequestBody().readAllBytes();
            capturedBody.set(objectMapper.readValue(request, new TypeReference<>() {}));
            respond(exchange, "{\"code\":0,\"data\":{}}");
        });
        server.start();
        try {
            HttpLarkApprovalClient client = client(server);

            client.rejectTask(new LarkApprovalTaskActionRequest(
                    7L,
                    "lark-instance-101",
                    "lark-task-201",
                    "bob",
                    "风险未说明"));

            assertThat(capturedPath.get()).isEqualTo("/open-apis/approval/v4/tasks/reject");
            assertThat(capturedBody.get())
                    .containsEntry("instance_code", "lark-instance-101")
                    .containsEntry("task_id", "lark-task-201")
                    .containsEntry("comment", "风险未说明");
            assertThat(capturedAuthorization.get()).isEqualTo("Bearer user-token");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createInstanceIncludesLarkErrorMessageWhenResponseCodeFails() throws Exception {
        HttpServer server = server(exchange -> respond(exchange, "{\"code\":999,\"msg\":\"approval code invalid\"}"));
        server.start();
        try {
            HttpLarkApprovalClient client = client(server);

            assertThatThrownBy(() -> client.createInstance(new LarkApprovalCreateInstanceRequest(
                    7L,
                    "bad-approval-code",
                    "canvas-approval-101",
                    "ou_submitter",
                    null,
                    null,
                    "[]")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Lark approval instance create failed")
                    .hasMessageContaining("approval code invalid");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void approveTaskIncludesLarkErrorMessageWhenResponseCodeFails() throws Exception {
        HttpServer server = server(exchange -> respond(exchange, "{\"code\":999,\"msg\":\"task already finished\"}"));
        server.start();
        try {
            HttpLarkApprovalClient client = client(server);

            assertThatThrownBy(() -> client.approveTask(new LarkApprovalTaskActionRequest(
                    7L,
                    "lark-instance-101",
                    "lark-task-201",
                    "bob",
                    "检查通过")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Lark approval task action failed")
                    .hasMessageContaining("task already finished");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createInstanceRejectsMissingTenantTokenReference() {
        HttpLarkApprovalClient client = new HttpLarkApprovalClient(
                WebClient.builder(),
                reference -> reference + "-token",
                "http://127.0.0.1:1",
                "user",
                "");

        assertThatThrownBy(() -> client.createInstance(new LarkApprovalCreateInstanceRequest(
                7L,
                "approval-code",
                "canvas-approval-101",
                "ou_submitter",
                null,
                null,
                "[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("canvas.approval.lark.tenant-token-reference is required");
    }

    @Test
    void getInstanceRejectsMissingUserTokenReference() {
        HttpLarkApprovalClient client = new HttpLarkApprovalClient(
                WebClient.builder(),
                reference -> reference + "-token",
                "http://127.0.0.1:1",
                "",
                "tenant");

        assertThatThrownBy(() -> client.getInstance(7L, "lark-instance-101"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("canvas.approval.lark.user-token-reference is required");
    }

    @Test
    void approveTaskRejectsUnresolvedUserToken() {
        HttpLarkApprovalClient client = new HttpLarkApprovalClient(
                WebClient.builder(),
                reference -> null,
                "http://127.0.0.1:1",
                "user",
                "tenant");

        assertThatThrownBy(() -> client.approveTask(new LarkApprovalTaskActionRequest(
                7L,
                "lark-instance-101",
                "lark-task-201",
                "bob",
                "检查通过")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lark approval user token is not configured");
    }

    @Test
    void createInstanceRejectsUnresolvedTenantToken() {
        HttpLarkApprovalClient client = new HttpLarkApprovalClient(
                WebClient.builder(),
                reference -> null,
                "http://127.0.0.1:1",
                "user",
                "tenant");

        assertThatThrownBy(() -> client.createInstance(new LarkApprovalCreateInstanceRequest(
                7L,
                "approval-code",
                "canvas-approval-101",
                "ou_submitter",
                null,
                null,
                "[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lark approval tenant token is not configured");
    }

    private HttpLarkApprovalClient client(HttpServer server) {
        return new HttpLarkApprovalClient(
                WebClient.builder(),
                reference -> reference + "-token",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "user",
                "tenant");
    }

    private HttpServer server(ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        return server;
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String responseBody) throws IOException {
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
