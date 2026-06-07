package org.chovy.canvas.domain.content;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MarketingAssetS3Presigner {

    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final HexFormat HEX = HexFormat.of();
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private final MarketingAssetS3PresignProperties properties;
    private final Clock clock;

    public MarketingAssetS3Presigner(MarketingAssetS3PresignProperties properties) {
        this(properties, Clock.systemUTC());
    }

    MarketingAssetS3Presigner(MarketingAssetS3PresignProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public PresignedPut presignPut(String relativeObjectKey,
                                   String mimeType,
                                   Map<String, String> requiredHeaders,
                                   Duration ttl) {
        String objectKey = properties.objectKey(relativeObjectKey);
        URI uri = objectUri(objectKey);
        Instant now = clock.instant();
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = DATE_STAMP.format(now);
        String scope = dateStamp + "/" + properties.region() + "/s3/aws4_request";
        long expires = Math.min(Math.max(ttl == null ? 3600 : ttl.toSeconds(), 60), 3600);
        Map<String, String> headers = signedHeaders(uri, mimeType, requiredHeaders);
        String signedHeaders = String.join(";", headers.keySet());
        Map<String, String> query = new TreeMap<>();
        query.put("X-Amz-Algorithm", ALGORITHM);
        query.put("X-Amz-Credential", properties.accessKey() + "/" + scope);
        query.put("X-Amz-Date", amzDate);
        query.put("X-Amz-Expires", String.valueOf(expires));
        query.put("X-Amz-SignedHeaders", signedHeaders);

        String canonicalRequest = "PUT\n"
                + canonicalPath(uri) + "\n"
                + canonicalQuery(query) + "\n"
                + canonicalHeaders(headers) + "\n"
                + signedHeaders + "\n"
                + UNSIGNED_PAYLOAD;
        String stringToSign = ALGORITHM + "\n"
                + amzDate + "\n"
                + scope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        query.put("X-Amz-Signature", HEX.formatHex(hmac(signingKey(dateStamp), stringToSign)));
        return new PresignedPut(uriWithQuery(uri, query).toString(), objectKey, properties.publicUrl(relativeObjectKey),
                clientHeaders(headers), expires, amzDate);
    }

    private Map<String, String> signedHeaders(URI uri, String mimeType, Map<String, String> requiredHeaders) {
        Map<String, String> headers = new TreeMap<>();
        headers.put("host", hostHeader(uri));
        if (mimeType != null && !mimeType.isBlank()) {
            headers.put("content-type", mimeType.trim().toLowerCase(Locale.ROOT));
        }
        if (requiredHeaders != null) {
            requiredHeaders.forEach((key, value) -> {
                if (key != null && value != null && !key.isBlank() && !value.isBlank()) {
                    headers.put(key.trim().toLowerCase(Locale.ROOT), value.trim());
                }
            });
        }
        return headers;
    }

    private Map<String, String> clientHeaders(Map<String, String> signedHeaders) {
        Map<String, String> headers = new LinkedHashMap<>();
        signedHeaders.forEach((key, value) -> {
            if (!"host".equals(key)) {
                headers.put(key, value);
            }
        });
        return headers;
    }

    private URI objectUri(String objectKey) {
        URI endpoint = URI.create(properties.endpoint());
        String objectPath = encodeObjectKey(objectKey);
        String basePath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        try {
            if (properties.pathStyle()) {
                String path = basePath + "/" + encodeSegment(properties.bucket()) + "/" + objectPath;
                return new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(), path, null, null);
            }
            String host = properties.bucket() + "." + endpoint.getHost();
            String path = basePath + "/" + objectPath;
            return new URI(endpoint.getScheme(), null, host, endpoint.getPort(), path, null, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid S3 endpoint or object key", e);
        }
    }

    private URI uriWithQuery(URI uri, Map<String, String> query) {
        return URI.create(uri.toString() + "?" + canonicalQuery(query));
    }

    private static String canonicalPath(URI uri) {
        String rawPath = uri.getRawPath();
        return rawPath == null || rawPath.isBlank() ? "/" : rawPath;
    }

    private static String canonicalQuery(Map<String, String> query) {
        StringBuilder builder = new StringBuilder();
        query.forEach((key, value) -> {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(encodeSegment(key)).append('=').append(encodeSegment(value));
        });
        return builder.toString();
    }

    private static String canonicalHeaders(Map<String, String> headers) {
        StringBuilder builder = new StringBuilder();
        headers.forEach((key, value) -> builder.append(key).append(':').append(value.trim()).append('\n'));
        return builder.toString();
    }

    private byte[] signingKey(String dateStamp) {
        byte[] dateKey = hmac(("AWS4" + properties.secretKey()).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] regionKey = hmac(dateKey, properties.region());
        byte[] serviceKey = hmac(regionKey, "s3");
        return hmac(serviceKey, "aws4_request");
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

    public record PresignedPut(
            String uploadUrl,
            String objectKey,
            String storageUrl,
            Map<String, String> requiredHeaders,
            long expiresInSeconds,
            String signedAt) {
    }
}
