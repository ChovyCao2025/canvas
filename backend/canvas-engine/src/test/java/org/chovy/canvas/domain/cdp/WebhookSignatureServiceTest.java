package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureServiceTest {

    @Test
    void signUsesTimestampNewlinePayloadCanonicalString() {
        WebhookSignatureService service = new WebhookSignatureService();

        String signature = service.sign("secret-123", "1717200000000", "{\"event\":\"x\"}");

        assertThat(signature).startsWith("sha256=");
        assertThat(service.verify("secret-123", "1717200000000", "{\"event\":\"x\"}", signature)).isTrue();
        assertThat(service.verify("secret-123", "1717200000000", "{\"event\":\"y\"}", signature)).isFalse();
    }

    @Test
    void verifyRejectsMissingSignature() {
        WebhookSignatureService service = new WebhookSignatureService();

        assertThat(service.verify("secret-123", "1717200000000", "{\"event\":\"x\"}", null)).isFalse();
    }
}
