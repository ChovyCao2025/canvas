package org.chovy.canvas.domain.marketing;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpMarketingIntegrationContractProbeClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void probeRecordsPassForSuccessAndFailForProviderErrors() throws IOException {
        startServer();
        server.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.createContext("/failed", exchange -> {
            byte[] body = "provider unavailable".getBytes();
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        HttpMarketingIntegrationContractProbeClient client = client("");

        MarketingIntegrationContractProbeClient.ProbeResult pass =
                client.probe(target(baseUrl() + "/health", Map.of()));
        MarketingIntegrationContractProbeClient.ProbeResult fail =
                client.probe(target(baseUrl() + "/failed", Map.of()));

        assertThat(pass.status()).isEqualTo("PASS");
        assertThat(pass.httpStatusCode()).isEqualTo(204);
        assertThat(pass.latencyMs()).isNotNegative();
        assertThat(pass.evidence()).containsEntry("transport", "http");
        assertThat(fail.status()).isEqualTo("FAIL");
        assertThat(fail.httpStatusCode()).isEqualTo(503);
        assertThat(fail.errorMessage()).contains("503");
    }

    @Test
    void probeSupportsConfiguredBaseUrlForRelativeRootsAndRejectsUnsafeMethods() throws IOException {
        startServer();
        server.createContext("/canvas/search-marketing/mutations", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        HttpMarketingIntegrationContractProbeClient client = client(baseUrl());

        MarketingIntegrationContractProbeClient.ProbeResult pass =
                client.probe(target("/canvas/search-marketing/mutations", Map.of()));

        assertThat(pass.status()).isEqualTo("PASS");
        assertThatThrownBy(() -> client.probe(target(baseUrl() + "/health", Map.of("probeMethod", "POST"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration probe method");
    }

    @Test
    void probeRequiresBaseUrlForRelativeRoots() {
        HttpMarketingIntegrationContractProbeClient client = client("");

        assertThatThrownBy(() -> client.probe(target("/canvas/search-marketing/mutations", Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relative integration apiRoot requires");
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private HttpMarketingIntegrationContractProbeClient client(String baseUrl) {
        return new HttpMarketingIntegrationContractProbeClient(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                baseUrl);
    }

    private MarketingIntegrationContractProbeClient.ProbeTarget target(
            String apiRoot,
            Map<String, Object> metadata) {
        return new MarketingIntegrationContractProbeClient.ProbeTarget(
                10L,
                7L,
                "google-ads-keyword-write",
                "Google Ads keyword write",
                "SEM",
                apiRoot,
                "OAUTH",
                3000,
                Map.of(),
                metadata);
    }
}
