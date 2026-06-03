package org.chovy.canvas.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

public class CanvasHmacVerifier {

    public static final String TIMESTAMP_HEADER = "X-Canvas-Timestamp";
    public static final String SIGNATURE_HEADER = "X-Canvas-Signature";

    private static final Duration MAX_SKEW = Duration.ofMinutes(5);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final Clock clock;

    public CanvasHmacVerifier(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }

    public void verify(HttpHeaders headers, String rawBody, String subject) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw unauthorized(subject + "签名密钥未配置");
        }
        String timestamp = headers.getFirst(TIMESTAMP_HEADER);
        String signature = headers.getFirst(SIGNATURE_HEADER);
        if (timestamp == null || timestamp.isBlank() || signature == null || signature.isBlank()) {
            throw unauthorized(subject + "缺少签名头");
        }
        long timestampMs = parseTimestampMillis(timestamp, subject);
        long now = clock.millis();
        if (Math.abs(now - timestampMs) > MAX_SKEW.toMillis()) {
            throw unauthorized(subject + "签名已过期");
        }
        String expected = sign(timestamp.trim(), rawBody == null ? "" : rawBody, subject);
        String supplied = normalizeSignature(signature);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw unauthorized(subject + "签名无效");
        }
    }

    private long parseTimestampMillis(String timestamp, String subject) {
        try {
            long value = Long.parseLong(timestamp.trim());
            return value < 10_000_000_000L ? value * 1000 : value;
        } catch (NumberFormatException e) {
            throw unauthorized(subject + "时间戳不合法");
        }
    }

    private String sign(String timestamp, String rawBody, String subject) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(
                    mac.doFinal((timestamp + "\n" + rawBody).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw unauthorized(subject + "签名校验失败");
        }
    }

    private String normalizeSignature(String signature) {
        String trimmed = signature.trim();
        return trimmed.startsWith("sha256=") ? trimmed.substring("sha256=".length()) : trimmed;
    }

    private ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }
}
