package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dto.canvas.MessagePreviewReq;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasMessagePreviewServiceTest {

    @Test
    void previewMasksSensitiveFieldsAndResolvesVariables() {
        CanvasMessagePreviewService service = new CanvasMessagePreviewService(new ObjectMapper());
        String graphJson = """
                {"nodes":[{"id":"send","type":"SEND_MESSAGE","config":{
                  "channel":"sms",
                  "templateId":"tpl-1",
                  "title":"Hi $name",
                  "body":"Phone $phone and city {{profile.city}}",
                  "variables":{"name":"$name","phone":"$phone","token":"$token"}
                }}]}
                """;

        var resp = service.preview(new MessagePreviewReq(
                62L,
                "send",
                "u1",
                graphJson,
                Map.of(
                        "name", "Alice",
                        "phone", "13812345678",
                        "token", "secret-token",
                        "profile", Map.of("city", "Shanghai"))));

        assertThat(resp.channel()).isEqualTo("SMS");
        assertThat(resp.templateId()).isEqualTo("tpl-1");
        assertThat(resp.content()).containsEntry("title", "Hi Alice");
        assertThat(String.valueOf(resp.content().get("body"))).contains("138****5678", "Shanghai");
        assertThat(resp.variables()).containsEntry("name", "Alice");
        assertThat(resp.variables()).containsEntry("phone", "******");
        assertThat(resp.variables()).containsEntry("token", "******");
        assertThat(resp.warnings()).containsExactly("PREVIEW_ONLY_NO_SEND");
    }

    @Test
    void previewRejectsNonSendMessageNode() {
        CanvasMessagePreviewService service = new CanvasMessagePreviewService(new ObjectMapper());
        String graphJson = "{\"nodes\":[{\"id\":\"tag\",\"type\":\"TAGGER\",\"config\":{}}]}";

        assertThatThrownBy(() -> service.preview(new MessagePreviewReq(
                62L, "tag", "u1", graphJson, Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SEND_MESSAGE");
    }
}
