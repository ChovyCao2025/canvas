package org.chovy.canvas.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineRequestAuthServiceTest {

    private static final String SECRET = "machine-secret-at-least-32-bytes-long";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-03T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void verifiesValidHmacSignature() {
        MachineRequestAuthService service = new MachineRequestAuthService(SECRET, CLOCK);
        String body = "{\"userId\":\"u1\"}";
        String timestamp = String.valueOf(CLOCK.millis());
        HttpHeaders headers = signedHeaders(service, timestamp, body);

        assertThatCode(() -> service.verify(headers, body)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingSignature() {
        MachineRequestAuthService service = new MachineRequestAuthService(SECRET, CLOCK);

        assertThatThrownBy(() -> service.verify(new HttpHeaders(), "{}"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void rejectsWeakSecret() {
        MachineRequestAuthService service = new MachineRequestAuthService("weak", CLOCK);

        assertThatThrownBy(() -> service.verify(signedHeaders(new MachineRequestAuthService(SECRET, CLOCK),
                        String.valueOf(CLOCK.millis()), "{}"), "{}"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    private HttpHeaders signedHeaders(MachineRequestAuthService service, String timestamp, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(MachineRequestAuthService.TIMESTAMP_HEADER, timestamp);
        headers.add(MachineRequestAuthService.SIGNATURE_HEADER, "sha256=" + service.signForTest(timestamp, body));
        return headers;
    }
}
