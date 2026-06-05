package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.canvas.ConnectedContentGateway;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectedContentHandlerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-04T01:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void migrationCreatesConnectedContentCacheTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V130__connected_content_cache.sql"));

        assertThat(sql)
                .contains("CREATE TABLE `connected_content_cache`")
                .contains("`cache_key`")
                .contains("`response_json`")
                .contains("UNIQUE KEY `uk_connected_content_cache_key`")
                .contains("'CONNECTED_CONTENT'");
    }

    @Test
    void rejectsUnsafeUrlBeforeFetch() {
        FakeGateway gateway = new FakeGateway();
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of("url", "http://127.0.0.1/internal"), ctx()).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("内网");
        assertThat(gateway.fetches).isEqualTo(0);
    }

    @Test
    void returnsCacheHitWithoutFetching() {
        FakeGateway gateway = new FakeGateway();
        gateway.cachedBody = "{\"title\":\"cached\"}";
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of(
                "url", "http://93.184.216.34/content",
                "nextNodeId", "next-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "next-1");
        assertThat(result.output()).containsEntry("connectedContentCacheHit", true);
        assertThat(gateway.fetches).isEqualTo(0);
    }

    @Test
    void fetchesAndCachesExpiredMiss() {
        FakeGateway gateway = new FakeGateway();
        gateway.fetchBody = "{\"title\":\"fresh\"}";
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of(
                "url", "http://93.184.216.34/content",
                "cacheTtlSeconds", 60,
                "nextNodeId", "next-1"
        ), ctx()).block();

        assertThat(result.output()).containsEntry("connectedContentCacheHit", false);
        assertThat(gateway.fetches).isEqualTo(1);
        assertThat(gateway.savedBodies).containsExactly("{\"title\":\"fresh\"}");
        assertThat(gateway.savedExpiresAt.get(0)).isEqualTo(LocalDateTime.of(2026, 6, 4, 9, 1));
    }

    @Test
    void extractsJsonPathMappingsIntoOutputPrefix() {
        FakeGateway gateway = new FakeGateway();
        gateway.fetchBody = "{\"user\":{\"name\":\"Ada\"},\"score\":42}";
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of(
                "url", "http://93.184.216.34/content",
                "outputPrefix", "cc",
                "jsonPathMappings", List.of(Map.of("path", "$.user.name", "outputKey", "userName")),
                "nextNodeId", "next-1"
        ), ctx()).block();

        assertThat(result.output()).containsEntry("cc.userName", "Ada");
        assertThat(result.output()).containsKeys("cc.connectedContentBody");
    }

    @Test
    void routesToFailWhenPayloadExceedsMaxBytes() {
        FakeGateway gateway = new FakeGateway();
        gateway.fetchBody = "{\"body\":\"too-large\"}";
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of(
                "url", "http://93.184.216.34/content",
                "maxBytes", 5,
                "failNodeId", "fail-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.FAIL);
        assertThat(result.routes()).containsEntry("fail", "fail-1");
        assertThat(result.output()).containsEntry("connectedContentStatus", "CONNECTED_CONTENT_PAYLOAD_TOO_LARGE");
    }

    @Test
    void routesToTimeoutBranchWhenProviderDoesNotRespond() {
        FakeGateway gateway = new FakeGateway();
        gateway.never = true;
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of(
                "url", "http://93.184.216.34/content",
                "timeoutMs", 10,
                "timeoutNodeId", "timeout-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
        assertThat(result.routes()).containsEntry("timeout", "timeout-1");
        assertThat(result.output()).containsEntry("connectedContentStatus", "TIMEOUT");
    }

    @Test
    void providerErrorKeepsTraceOutputAndRoutesToFailBranch() {
        FakeGateway gateway = new FakeGateway();
        gateway.error = new IllegalStateException("provider down");
        ConnectedContentHandler handler = handler(gateway);

        NodeResult result = handler.executeAsync(Map.of(
                "url", "http://93.184.216.34/content",
                "failNodeId", "fail-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.FAIL);
        assertThat(result.routes()).containsEntry("fail", "fail-1");
        assertThat(result.output()).containsEntry("connectedContentStatus", "CONNECTED_CONTENT_PROVIDER_ERROR");
        assertThat(result.output().get("connectedContentError")).asString().contains("provider down");
    }

    private static ConnectedContentHandler handler(FakeGateway gateway) {
        return new ConnectedContentHandler(gateway, new ObjectMapper(), CLOCK);
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(7L);
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(20L);
        ctx.setUserId("user-1");
        return ctx;
    }

    private static class FakeGateway implements ConnectedContentGateway {
        private String cachedBody;
        private String fetchBody = "{}";
        private Throwable error;
        private boolean never;
        private int fetches;
        private final List<String> savedBodies = new ArrayList<>();
        private final List<LocalDateTime> savedExpiresAt = new ArrayList<>();

        @Override
        public Optional<ConnectedContentGateway.CachedContent> findFresh(Long tenantId, String cacheKey, LocalDateTime now) {
            return cachedBody == null
                    ? Optional.empty()
                    : Optional.of(new ConnectedContentGateway.CachedContent(cachedBody));
        }

        @Override
        public void save(Long tenantId,
                         String cacheKey,
                         String urlHash,
                         String requestHash,
                         String body,
                         LocalDateTime expiresAt) {
            savedBodies.add(body);
            savedExpiresAt.add(expiresAt);
        }

        @Override
        public Mono<String> fetch(ConnectedContentGateway.HttpRequest request) {
            fetches++;
            if (never) {
                return Mono.never();
            }
            if (error != null) {
                return Mono.error(error);
            }
            return Mono.just(fetchBody);
        }
    }
}
