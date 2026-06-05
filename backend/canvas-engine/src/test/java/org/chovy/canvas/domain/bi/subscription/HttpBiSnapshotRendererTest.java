package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpBiSnapshotRendererTest {

    @Test
    void configuredRequiresEnabledAndEndpoint() {
        assertThat(renderer(false, "https://renderer.example.test").configured()).isFalse();
        assertThat(renderer(true, "").configured()).isFalse();
        assertThat(renderer(true, "https://renderer.example.test").configured()).isTrue();
    }

    @Test
    void renderPostsSnapshotRequestAndDecodesBase64Image() {
        TestHttpBiSnapshotRenderer renderer = new TestHttpBiSnapshotRenderer(
                Map.of(
                        "format", "PNG",
                        "contentType", "image/png",
                        "base64", Base64.getEncoder().encodeToString("PNG_BYTES".getBytes(StandardCharsets.UTF_8))));

        BiSnapshotRenderResult result = renderer.render(new BiSnapshotRenderRequest(
                "<html>snapshot</html>",
                "/bi?resourceType=DASHBOARD&resourceId=21",
                "PNG",
                1280,
                720,
                2,
                Map.of("jobKey", "canvas-daily")));

        assertThat(result.format()).isEqualTo("PNG");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(new String(result.bytes(), StandardCharsets.UTF_8)).isEqualTo("PNG_BYTES");
        assertThat(renderer.url).isEqualTo("https://renderer.example.test/render");
        assertThat(renderer.body)
                .containsEntry("format", "PNG")
                .containsEntry("width", 1280)
                .containsEntry("height", 720)
                .containsEntry("scale", 2.0);
        Map<?, ?> metadata = (Map<?, ?>) renderer.body.get("metadata");
        assertThat(metadata.get("jobKey")).isEqualTo("canvas-daily");
    }

    @Test
    void renderRejectsMissingImageData() {
        TestHttpBiSnapshotRenderer renderer = new TestHttpBiSnapshotRenderer(Map.of("format", "PNG"));

        assertThatThrownBy(() -> renderer.render(new BiSnapshotRenderRequest(
                "<html></html>",
                "/bi",
                "PNG",
                1280,
                720,
                1,
                Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base64");
    }

    private HttpBiSnapshotRenderer renderer(boolean enabled, String endpoint) {
        return new HttpBiSnapshotRenderer(WebClient.builder(), new ObjectMapper(), enabled, endpoint, 1000);
    }

    private static final class TestHttpBiSnapshotRenderer extends HttpBiSnapshotRenderer {
        private final Map<String, Object> response;
        private String url;
        private Map<String, Object> body;

        private TestHttpBiSnapshotRenderer(Map<String, Object> response) {
            super(WebClient.builder(), new ObjectMapper(), true, "https://renderer.example.test/render", 1000);
            this.response = response;
        }

        @Override
        protected Map<String, Object> postRenderRequest(String url, Map<String, Object> body) {
            this.url = url;
            this.body = body;
            return response;
        }
    }
}
