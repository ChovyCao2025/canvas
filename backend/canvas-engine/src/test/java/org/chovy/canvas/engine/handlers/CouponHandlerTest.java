package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CouponHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void usesNodeScopedIdempotencyKeyByDefault() throws Exception {
        AtomicReference<Map<String, Object>> capturedBody = new AtomicReference<>();
        HttpServer server = couponServer(capturedBody);
        server.start();
        try {
            CouponHandler handler = new CouponHandler("http://127.0.0.1:" + server.getAddress().getPort());
            ExecutionContext ctx = new ExecutionContext();
            ctx.setExecutionId("exec-1");
            ctx.setUserId("user-1");

            NodeResult result = handler.executeAsync(Map.of(
                    MapFieldKeys.COUPON_TYPE_KEY, "coupon-a",
                    MapFieldKeys.NODE_ID_INTERNAL, "node-a"
            ), ctx).block();

            assertThat(result.success()).isTrue();
            assertThat(capturedBody.get())
                    .containsEntry(MapFieldKeys.IDEMPOTENCY_KEY, "exec-1:node-a")
                    .containsEntry(MapFieldKeys.USER_ID, "user-1");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer couponServer(AtomicReference<Map<String, Object>> capturedBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/issue", exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            capturedBody.set(objectMapper.readValue(request, new TypeReference<>() {}));
            byte[] response = "{\"status\":\"SUCCESS\",\"couponId\":\"coupon-1\",\"couponAmount\":10}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        return server;
    }
}
