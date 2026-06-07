package org.chovy.canvas.domain.monitoring;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class MarketingMonitorWebhookSignatureService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;

    private final Clock clock;

    public MarketingMonitorWebhookSignatureService() {
        this(Clock.systemUTC());
    }

    MarketingMonitorWebhookSignatureService(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public String sign(String secret, String timestamp, String rawBody) {
        if (isBlank(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "monitoring webhook secret is not configured");
        }
        if (isBlank(timestamp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing monitoring webhook timestamp");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(message(timestamp, rawBody).getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(digest);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "monitoring webhook signature verification failed",
                    ex);
        }
    }

    public void verifyOrThrow(String secret,
                              String timestamp,
                              String rawBody,
                              String suppliedSignature,
                              Integer toleranceSeconds) {
        long eventEpochSeconds = parseTimestamp(timestamp);
        int tolerance = toleranceSeconds == null || toleranceSeconds <= 0
                ? DEFAULT_TOLERANCE_SECONDS
                : toleranceSeconds;
        long now = clock.instant().getEpochSecond();
        if (Math.abs(now - eventEpochSeconds) > tolerance) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "stale monitoring webhook timestamp");
        }
        if (isBlank(suppliedSignature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing monitoring webhook signature");
        }
        String expected = sign(secret, timestamp.trim(), rawBody);
        String supplied = suppliedSignature.trim().toLowerCase(Locale.ROOT);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid monitoring webhook signature");
        }
    }

    private long parseTimestamp(String timestamp) {
        if (isBlank(timestamp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing monitoring webhook timestamp");
        }
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid monitoring webhook timestamp");
        }
    }

    private String message(String timestamp, String rawBody) {
        return timestamp.trim() + "\n" + (rawBody == null ? "" : rawBody);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
