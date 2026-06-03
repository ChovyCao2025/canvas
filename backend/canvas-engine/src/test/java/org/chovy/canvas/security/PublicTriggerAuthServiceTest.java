package org.chovy.canvas.security;

import org.chovy.canvas.web.EventReportAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicTriggerAuthServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsMissingSignature() {
        PublicTriggerAuthService service = new PublicTriggerAuthService(SECRET, CLOCK);

        assertThatThrownBy(() -> service.verify(new HttpHeaders(), "{}"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void acceptsValidSignatureWithSharedHeaderContract() throws Exception {
        PublicTriggerAuthService service = new PublicTriggerAuthService(SECRET, CLOCK);
        String body = "{\"userId\":\"user-1\"}";
        String timestamp = String.valueOf(CLOCK.millis());
        HttpHeaders headers = new HttpHeaders();
        headers.add(EventReportAuthService.TIMESTAMP_HEADER, timestamp);
        headers.add(EventReportAuthService.SIGNATURE_HEADER, "sha256=" + hmac(timestamp + "\n" + body));

        assertThatCode(() -> service.verify(headers, body)).doesNotThrowAnyException();
    }

    @Test
    void rejectsWeakSecret() throws Exception {
        PublicTriggerAuthService service = new PublicTriggerAuthService("weak", CLOCK);
        String body = "{}";
        String timestamp = String.valueOf(CLOCK.millis());
        HttpHeaders headers = new HttpHeaders();
        headers.add(EventReportAuthService.TIMESTAMP_HEADER, timestamp);
        headers.add(EventReportAuthService.SIGNATURE_HEADER, "sha256=" + hmac(timestamp + "\n" + body));

        assertThatThrownBy(() -> service.verify(headers, body))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private static String hmac(String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }
}
