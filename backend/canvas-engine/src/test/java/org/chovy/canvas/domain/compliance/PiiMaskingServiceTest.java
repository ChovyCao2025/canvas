package org.chovy.canvas.domain.compliance;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskingServiceTest {

    private final PiiMaskingService service = new PiiMaskingService();

    @Test
    void masksCommonPiiAndSecrets() {
        assertThat(service.maskPhone("13812345678")).isEqualTo("138****5678");
        assertThat(service.maskEmail("alice@example.com")).isEqualTo("a***e@example.com");
        assertThat(service.maskOpenId("wx_open_id_abcdef")).isEqualTo("wx_o**********cdef");
        assertThat(service.maskSecret("secret-token-123456")).isEqualTo("****3456");
    }

    @Test
    @SuppressWarnings("unchecked")
    void masksNestedMetadataValuesBySensitiveKeys() {
        Map<String, Object> masked = service.maskMetadata(Map.of(
                "phone", "13812345678",
                "email", "alice@example.com",
                "nested", Map.of("apiKey", "secret-token-123456"),
                "safe", "visible"));

        assertThat(masked).containsEntry("phone", "138****5678");
        assertThat(masked).containsEntry("email", "a***e@example.com");
        assertThat(masked).containsEntry("safe", "visible");
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) masked.get("nested");
        assertThat(nested).containsEntry("apiKey", "****3456");
    }
}
