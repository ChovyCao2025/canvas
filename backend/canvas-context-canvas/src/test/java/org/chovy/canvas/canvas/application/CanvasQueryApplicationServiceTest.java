package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRuntimeOptions;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.junit.jupiter.api.Test;

/**
 * 封装CanvasQueryApplicationServiceTest相关的业务逻辑。
 */
class CanvasQueryApplicationServiceTest {

    /**
     * 获取PublishedIncludesRuntimeOptionsAndParsedGraphDefinition。
     */
    @Test
    void getPublishedIncludesRuntimeOptionsAndParsedGraphDefinition() {
        CanvasDraftApplicationServiceTest.InMemoryCanvasRepository canvases =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasRepository();
        CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository versions =
                new CanvasDraftApplicationServiceTest.InMemoryCanvasVersionRepository();
        Canvas canvas = canvases.save(Canvas.createDraft(10L, 9L, "Welcome", "desc", "creator")
                .withRuntimeOptions(new CanvasRuntimeOptions(
                        "EVENT",
                        null,
                        null,
                        null,
                        null,
                        5,
                        null,
                        30,
                        null,
                        null,
                        "ORDER_PAID",
                        7,
                        "FIRST_TOUCH"))
                .publish(20L));
        versions.save(CanvasVersion.published(20L, canvas.id(), 9L, 2, """
                {"nodes":[{"id":"message","type":"message","displayName":"Send","config":{"template":"welcome"}}],
                 "edges":[{"source":"message","target":"end"}]}
                """, "publisher"));
        CanvasQueryApplicationService service = new CanvasQueryApplicationService(canvases, versions);

        PublishedCanvasDefinition definition = service.getPublished(9L, canvas.id());

        assertThat(definition.executionOptions())
                .containsEntry("triggerType", "EVENT")
                .containsEntry("perUserDailyLimit", 5)
                .containsEntry("cooldownSeconds", 30)
                .containsEntry("conversionEventCode", "ORDER_PAID")
                .containsEntry("attributionWindowDays", 7)
                .containsEntry("attributionModel", "FIRST_TOUCH");
        assertThat(definition.nodes()).hasSize(1);
        assertThat(definition.nodes().get(0).displayName()).isEqualTo("Send");
        assertThat(definition.nodes().get(0).configJson()).contains("\"template\":\"welcome\"");
        assertThat(definition.edges()).hasSize(1);
        assertThat(definition.edges().get(0).edgeId()).isEqualTo("message->end");
    }
}
