package org.chovy.canvas.domain.bi.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class HttpS3ObjectClientTest {

    @Test
    void putObjectUsesPathStyleObjectUrlAndAwsV4Headers() throws Exception {
        AtomicReference<RecordedRequest> recorded = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            recorded.set(record(exchange));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            S3CompatibleBiStorageProperties properties = new S3CompatibleBiStorageProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "us-east-1",
                    "canvas-bi",
                    "access",
                    "secret",
                    "",
                    true,
                    "",
                    1000,
                    3000);
            HttpS3ObjectClient client = new HttpS3ObjectClient(
                    properties,
                    HttpClient.newHttpClient(),
                    Clock.fixed(Instant.parse("2026-06-05T09:00:00Z"), ZoneOffset.UTC));

            client.putObject(new S3ObjectRequest("canvas-bi", "bi-artifacts/exports/report.csv"),
                    "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));

            RecordedRequest request = recorded.get();
            assertThat(request.method()).isEqualTo("PUT");
            assertThat(request.path()).isEqualTo("/canvas-bi/bi-artifacts/exports/report.csv");
            assertThat(request.body()).isEqualTo("a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));
            assertThat(request.amzDate()).isEqualTo("20260605T090000Z");
            assertThat(request.payloadHash()).isEqualTo(sha256("a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)));
            assertThat(request.authorization())
                    .startsWith("AWS4-HMAC-SHA256 Credential=access/20260605/us-east-1/s3/aws4_request")
                    .contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date")
                    .contains("Signature=");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void putBucketLifecycleUsesLifecycleSubresourceAndAwsV4Headers() throws Exception {
        AtomicReference<RecordedRequest> recorded = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            recorded.set(record(exchange));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            S3CompatibleBiStorageProperties properties = new S3CompatibleBiStorageProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "us-east-1",
                    "canvas-bi",
                    "access",
                    "secret",
                    "",
                    true,
                    "",
                    1000,
                    3000);
            HttpS3ObjectClient client = new HttpS3ObjectClient(
                    properties,
                    HttpClient.newHttpClient(),
                    Clock.fixed(Instant.parse("2026-06-05T09:00:00Z"), ZoneOffset.UTC));
            String lifecycleXml = "<LifecycleConfiguration><Rule><ID>exports</ID></Rule></LifecycleConfiguration>";

            client.putBucketLifecycle(new S3BucketLifecycleRequest("canvas-bi"), lifecycleXml);

            RecordedRequest request = recorded.get();
            assertThat(request.method()).isEqualTo("PUT");
            assertThat(request.path()).isEqualTo("/canvas-bi");
            assertThat(request.query()).isEqualTo("lifecycle");
            assertThat(request.body()).isEqualTo(lifecycleXml.getBytes(StandardCharsets.UTF_8));
            assertThat(request.amzDate()).isEqualTo("20260605T090000Z");
            assertThat(request.payloadHash()).isEqualTo(sha256(lifecycleXml.getBytes(StandardCharsets.UTF_8)));
            assertThat(request.authorization())
                    .startsWith("AWS4-HMAC-SHA256 Credential=access/20260605/us-east-1/s3/aws4_request")
                    .contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date")
                    .contains("Signature=");
        } finally {
            server.stop(0);
        }
    }

    private static RecordedRequest record(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        return new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("x-amz-date"),
                exchange.getRequestHeaders().getFirst("x-amz-content-sha256"),
                body);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record RecordedRequest(
            String method,
            String path,
            String query,
            String authorization,
            String amzDate,
            String payloadHash,
            byte[] body) {
    }
}
