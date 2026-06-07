package org.chovy.canvas.domain.conversation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class WhatsAppWebhookSecurityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String verifyToken;
    private final String appSecret;

    public WhatsAppWebhookSecurityService(
            @Value("${canvas.conversation.whatsapp.webhook.verify-token:${canvas.conversation.whatsapp.verify-token:}}")
            String verifyToken,
            @Value("${canvas.conversation.whatsapp.webhook.app-secret:${canvas.conversation.whatsapp.app-secret:}}")
            String appSecret) {
        this.verifyToken = verifyToken == null ? "" : verifyToken;
        this.appSecret = appSecret == null ? "" : appSecret;
    }

    public String verifyChallenge(String mode, String token, String challenge) {
        if (verifyToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WhatsApp webhook verify token is not configured");
        }
        if (!"subscribe".equalsIgnoreCase(text(mode)) || isBlank(challenge)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid WhatsApp webhook challenge");
        }
        if (!constantTimeEquals(verifyToken.trim(), text(token))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid WhatsApp webhook verify token");
        }
        return challenge.trim();
    }

    public void verifySignature(String rawBody, String signatureHeader) {
        if (appSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WhatsApp webhook app secret is not configured");
        }
        if (isBlank(signatureHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing WhatsApp webhook signature");
        }
        String expected = SIGNATURE_PREFIX + hmacSha256(rawBody == null ? "" : rawBody);
        if (!constantTimeEquals(expected, signatureHeader.trim().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid WhatsApp webhook signature");
        }
    }

    private String hmacSha256(String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "WhatsApp webhook signature verification failed",
                    ex);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                (actual == null ? "" : actual).getBytes(StandardCharsets.UTF_8));
    }

    private static String text(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
