package org.chovy.canvas.domain.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class MarketingAssetUploadWebhookSignatureService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;

    private final String signingSecret;
    private final int toleranceSeconds;
    private final Clock clock;

    @Autowired
    public MarketingAssetUploadWebhookSignatureService(
            @Value("${canvas.marketing.content.asset-upload.webhook-secret:}") String signingSecret,
            @Value("${canvas.marketing.content.asset-upload.webhook-tolerance-seconds:300}") Integer toleranceSeconds) {
        this(signingSecret, toleranceSeconds, Clock.systemUTC());
    }

    MarketingAssetUploadWebhookSignatureService(String signingSecret, Integer toleranceSeconds, Clock clock) {
        this.signingSecret = signingSecret == null ? "" : signingSecret.trim();
        this.toleranceSeconds = toleranceSeconds == null || toleranceSeconds <= 0
                ? DEFAULT_TOLERANCE_SECONDS
                : toleranceSeconds;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public String sign(String timestamp, String rawBody) {
        if (signingSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "asset upload webhook secret is not configured");
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing asset upload webhook timestamp");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(message(timestamp, rawBody).getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(digest);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "asset upload webhook signature verification failed",
                    ex);
        }
    }

    public void verifyOrThrow(String timestamp, String rawBody, String suppliedSignature) {
        long eventEpochSeconds = parseTimestamp(timestamp);
        long now = clock.instant().getEpochSecond();
        if (Math.abs(now - eventEpochSeconds) > toleranceSeconds) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "stale asset upload webhook timestamp");
        }
        if (suppliedSignature == null || suppliedSignature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing asset upload webhook signature");
        }
        String expected = sign(timestamp.trim(), rawBody);
        String supplied = suppliedSignature.trim().toLowerCase(Locale.ROOT);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid asset upload webhook signature");
        }
    }

    private long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing asset upload webhook timestamp");
        }
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid asset upload webhook timestamp");
        }
    }

    private String message(String timestamp, String rawBody) {
        return timestamp.trim() + "\n" + (rawBody == null ? "" : rawBody);
    }
}
