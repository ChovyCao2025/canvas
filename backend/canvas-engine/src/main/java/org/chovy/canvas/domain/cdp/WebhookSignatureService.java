package org.chovy.canvas.domain.cdp;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class WebhookSignatureService {
    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String secret, String timestamp, String rawPayload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "\n" + rawPayload).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("webhook signature failed", e);
        }
    }

    public boolean verify(String secret, String timestamp, String rawPayload, String supplied) {
        if (supplied == null) {
            return false;
        }
        String expected = sign(secret, timestamp, rawPayload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
