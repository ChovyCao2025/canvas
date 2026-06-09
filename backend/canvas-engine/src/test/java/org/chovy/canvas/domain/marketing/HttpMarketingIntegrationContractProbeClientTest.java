package org.chovy.canvas.domain.marketing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpMarketingIntegrationContractProbeClientTest {

    @Test
    void probeRecordsPassForSuccessAndFailForProviderErrors() {
        StubHttpClient httpClient = new StubHttpClient(Map.of(
                "/health", response(204, ""),
                "/failed", response(503, "provider unavailable")));
        HttpMarketingIntegrationContractProbeClient client = client(httpClient, "");

        MarketingIntegrationContractProbeClient.ProbeResult pass =
                client.probe(target("http://provider.test/health", Map.of()));
        MarketingIntegrationContractProbeClient.ProbeResult fail =
                client.probe(target("http://provider.test/failed", Map.of()));

        assertThat(pass.status()).isEqualTo("PASS");
        assertThat(pass.httpStatusCode()).isEqualTo(204);
        assertThat(pass.latencyMs()).isNotNegative();
        assertThat(pass.evidence()).containsEntry("transport", "http");
        assertThat(fail.status()).isEqualTo("FAIL");
        assertThat(fail.httpStatusCode()).isEqualTo(503);
        assertThat(fail.errorMessage()).contains("503");
        assertThat(httpClient.requests()).extracting(request -> request.uri().getPath())
                .containsExactly("/health", "/failed");
    }

    @Test
    void probeSupportsConfiguredBaseUrlForRelativeRootsAndRejectsUnsafeMethods() {
        StubHttpClient httpClient = new StubHttpClient(Map.of(
                "/canvas/search-marketing/mutations", response(200, "")));
        HttpMarketingIntegrationContractProbeClient client = client(httpClient, "http://control-plane.test/");

        MarketingIntegrationContractProbeClient.ProbeResult pass =
                client.probe(target("/canvas/search-marketing/mutations", Map.of()));

        assertThat(pass.status()).isEqualTo("PASS");
        assertThat(httpClient.requests()).singleElement()
                .satisfies(request -> assertThat(request.uri().toString())
                        .isEqualTo("http://control-plane.test/canvas/search-marketing/mutations"));
        assertThatThrownBy(() -> client.probe(target("http://provider.test/health", Map.of("probeMethod", "POST"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration probe method");
    }

    @Test
    void probeRequiresBaseUrlForRelativeRoots() {
        HttpMarketingIntegrationContractProbeClient client = client(new StubHttpClient(Map.of()), "");

        assertThatThrownBy(() -> client.probe(target("/canvas/search-marketing/mutations", Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relative integration apiRoot requires");
    }

    private HttpMarketingIntegrationContractProbeClient client(HttpClient httpClient, String baseUrl) {
        return new HttpMarketingIntegrationContractProbeClient(httpClient, baseUrl);
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

    private static StubResponse response(int status, String body) {
        return new StubResponse(status, body);
    }

    private record StubResponse(int statusCode, String body) {
    }

    private static final class StubHttpClient extends HttpClient {
        private final Map<String, StubResponse> responses;
        private final List<HttpRequest> requests = new java.util.ArrayList<>();

        private StubHttpClient(Map<String, StubResponse> responses) {
            this.responses = responses;
        }

        private List<HttpRequest> requests() {
            return List.copyOf(requests);
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.of(Duration.ofSeconds(2));
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
            requests.add(request);
            StubResponse response = responses.get(request.uri().getPath());
            if (response == null) {
                throw new IOException("unexpected probe path: " + request.uri().getPath());
            }
            return new StubHttpResponse<>(request, response.statusCode(), body(response.body()));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("sendAsync is not used by probe tests");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("sendAsync is not used by probe tests");
        }

        @Override
        public WebSocket.Builder newWebSocketBuilder() {
            throw new UnsupportedOperationException("websocket is not used by probe tests");
        }

        @SuppressWarnings("unchecked")
        private static <T> T body(String body) {
            return (T) body;
        }
    }

    private record StubHttpResponse<T>(HttpRequest request, int statusCode, T body) implements HttpResponse<T> {
        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

    }
}
