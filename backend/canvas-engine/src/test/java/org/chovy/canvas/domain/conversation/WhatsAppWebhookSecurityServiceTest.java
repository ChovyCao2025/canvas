package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppWebhookSecurityServiceTest {

    private static final String APP_SECRET = "whatsapp-app-secret";

    @Test
    void acceptsSubscribeChallengeWhenVerifyTokenMatches() {
        WhatsAppWebhookSecurityService service = new WhatsAppWebhookSecurityService("verify-token", APP_SECRET);

        String challenge = service.verifyChallenge("subscribe", "verify-token", "challenge-1");

        assertThat(challenge).isEqualTo("challenge-1");
    }

    @Test
    void rejectsSubscribeChallengeWhenVerifyTokenDoesNotMatch() {
        WhatsAppWebhookSecurityService service = new WhatsAppWebhookSecurityService("verify-token", APP_SECRET);

        assertThatThrownBy(() -> service.verifyChallenge("subscribe", "wrong-token", "challenge-1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void acceptsMetaSha256SignatureForExactRawBody() {
        WhatsAppWebhookSecurityService service = new WhatsAppWebhookSecurityService("verify-token", APP_SECRET);
        String rawBody = "{\"entry\":[{\"id\":\"entry-1\"}]}";

        assertThatCode(() -> service.verifySignature(rawBody, signature(rawBody)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSignatureWhenRawBodyChanges() {
        WhatsAppWebhookSecurityService service = new WhatsAppWebhookSecurityService("verify-token", APP_SECRET);
        String rawBody = "{\"entry\":[{\"id\":\"entry-1\"}]}";

        assertThatThrownBy(() -> service.verifySignature(rawBody + " ", signature(rawBody)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsSignatureWhenAppSecretIsMissing() {
        WhatsAppWebhookSecurityService service = new WhatsAppWebhookSecurityService("verify-token", "");

        assertThatThrownBy(() -> service.verifySignature("{}", signature("{}")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private String signature(String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
