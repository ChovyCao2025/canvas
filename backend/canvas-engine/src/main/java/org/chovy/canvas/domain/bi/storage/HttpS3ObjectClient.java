package org.chovy.canvas.domain.bi.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HttpS3ObjectClient implements S3ObjectClient {

    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final HexFormat HEX = HexFormat.of();

    private final S3CompatibleBiStorageProperties properties;
    private final HttpClient httpClient;
    private final Clock clock;

    public HttpS3ObjectClient(S3CompatibleBiStorageProperties properties, HttpClient httpClient) {
        this(properties, httpClient, Clock.systemUTC());
    }

    HttpS3ObjectClient(S3CompatibleBiStorageProperties properties, HttpClient httpClient, Clock clock) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    @Override
    public void putObject(S3ObjectRequest request, byte[] bytes) {
        byte[] payload = bytes == null ? new byte[0] : bytes;
        HttpResponse<byte[]> response = exchange("PUT", request, payload);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 putObject failed with status " + status + " for " + request.objectKey());
        }
    }

    @Override
    public byte[] getObject(S3ObjectRequest request) {
        HttpResponse<byte[]> response = exchange("GET", request, new byte[0]);
        int status = response.statusCode();
        if (status == 404) {
            return null;
        }
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 getObject failed with status " + status + " for " + request.objectKey());
        }
        return response.body();
    }

    @Override
    public boolean deleteObject(S3ObjectRequest request) {
        HttpResponse<byte[]> response = exchange("DELETE", request, new byte[0]);
        int status = response.statusCode();
        if (status == 404) {
            return false;
        }
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 deleteObject failed with status " + status + " for " + request.objectKey());
        }
        return true;
    }

    @Override
    public void putBucketLifecycle(S3BucketLifecycleRequest request, String lifecycleXml) {
        byte[] payload = (lifecycleXml == null ? "" : lifecycleXml).getBytes(StandardCharsets.UTF_8);
        HttpResponse<byte[]> response = exchange("PUT", bucketLifecycleUri(request), request.bucket(), payload);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 putBucketLifecycle failed with status " + status
                    + " for bucket " + request.bucket());
        }
    }

    private HttpResponse<byte[]> exchange(String method, S3ObjectRequest request, byte[] payload) {
        return exchange(method, objectUri(request), request.objectKey(), payload);
    }

    private HttpResponse<byte[]> exchange(String method, URI uri, String target, byte[] payload) {
        try {
            String payloadHash = sha256Hex(payload);
            Instant now = clock.instant();
            String amzDate = AMZ_DATE.format(now);
            String dateStamp = DATE_STAMP.format(now);
            String host = hostHeader(uri);
            String authorization = authorization(method, uri, host, payloadHash, amzDate, dateStamp);

            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(properties.requestTimeoutMs()))
                    .header("Authorization", authorization)
                    .header("x-amz-content-sha256", payloadHash)
                    .header("x-amz-date", amzDate);
            HttpRequest httpRequest = switch (method) {
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofByteArray(payload)).build();
                case "GET" -> builder.GET().build();
                case "DELETE" -> builder.DELETE().build();
                default -> throw new IllegalArgumentException("unsupported S3 method: " + method);
            };
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("S3 request failed for " + target, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("S3 request interrupted for " + target, e);
        }
    }

    private URI objectUri(S3ObjectRequest request) {
        URI endpoint = URI.create(properties.endpoint());
        String objectPath = encodeObjectKey(request.objectKey());
        String basePath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        try {
            if (properties.pathStyle()) {
                String path = basePath + "/" + encodeSegment(request.bucket()) + "/" + objectPath;
                return new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(), path, null, null);
            }
            String host = request.bucket() + "." + endpoint.getHost();
            String path = basePath + "/" + objectPath;
            return new URI(endpoint.getScheme(), null, host, endpoint.getPort(), path, null, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid S3 endpoint or object key", e);
        }
    }

    private URI bucketLifecycleUri(S3BucketLifecycleRequest request) {
        URI endpoint = URI.create(properties.endpoint());
        String basePath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        try {
            if (properties.pathStyle()) {
                String path = basePath + "/" + encodeSegment(request.bucket());
                return new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(), path, "lifecycle", null);
            }
            String host = request.bucket() + "." + endpoint.getHost();
            String path = basePath.isBlank() ? "/" : basePath + "/";
            return new URI(endpoint.getScheme(), null, host, endpoint.getPort(), path, "lifecycle", null);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid S3 endpoint or bucket lifecycle request", e);
        }
    }

    private String authorization(String method,
                                 URI uri,
                                 String host,
                                 String payloadHash,
                                 String amzDate,
                                 String dateStamp) {
        Map<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-content-sha256", payloadHash);
        headers.put("x-amz-date", amzDate);
        StringBuilder canonicalHeaders = new StringBuilder();
        headers.forEach((key, value) -> canonicalHeaders.append(key).append(':').append(value).append('\n'));
        String signedHeaders = String.join(";", headers.keySet());
        String canonicalRequest = method + "\n"
                + canonicalPath(uri) + "\n"
                + canonicalQuery(uri) + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;
        String scope = dateStamp + "/" + properties.region() + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + scope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String signature = HEX.formatHex(hmac(signingKey(dateStamp), stringToSign));
        return "AWS4-HMAC-SHA256 Credential=" + properties.accessKey() + "/" + scope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    private byte[] signingKey(String dateStamp) {
        byte[] dateKey = hmac(("AWS4" + properties.secretKey()).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] regionKey = hmac(dateKey, properties.region());
        byte[] serviceKey = hmac(regionKey, "s3");
        return hmac(serviceKey, "aws4_request");
    }

    private static String canonicalPath(URI uri) {
        String rawPath = uri.getRawPath();
        return rawPath == null || rawPath.isBlank() ? "/" : rawPath;
    }

    private static String canonicalQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        if ("lifecycle".equals(rawQuery)) {
            return "lifecycle=";
        }
        return rawQuery;
    }

    private static String hostHeader(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost();
        if (port < 0 || (port == 443 && "https".equalsIgnoreCase(uri.getScheme()))
                || (port == 80 && "http".equalsIgnoreCase(uri.getScheme()))) {
            return host;
        }
        return host + ":" + port;
    }

    private static String encodeObjectKey(String objectKey) {
        String[] segments = objectKey.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodeSegment(segments[i]));
        }
        return encoded.toString();
    }

    private static String encodeSegment(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(String.format(Locale.ROOT, "%02X", c));
            }
        }
        return encoded.toString();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static byte[] hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("S3 signature failed", e);
        }
    }
}
