package org.chovy.canvas.canvas.application.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;
import org.junit.jupiter.api.Test;

/**
 * 封装CanvasDslMapperTest相关的业务逻辑。
 */
class CanvasDslMapperTest {

    /**
     * 处理mapsDslJourneyToGraphJsonWithoutReplacingFullGraphSemantics。
     */
    @Test
    void mapsDslJourneyToGraphJsonWithoutReplacingFullGraphSemantics() {
        CanvasDslMapper mapper = new CanvasDslMapper();

        CanvasDslMappingService.MappingResult result = mapper.toGraphJson(CanvasDslValidatorTest.goldenPath());

        assertThat(result.templateKey()).isEqualTo("new-user-welcome");
        assertThat(result.graphJson()).contains("\"dslVersion\":\"canvas/v1\"");
        assertThat(result.graphJson()).contains("\"type\":\"condition\"");
        assertThat(result.graphJson()).contains("\"from\":\"segment\"");
    }

    /**
     * 处理mapsGraphJsonBackToStableDslProjection。
     */
    @Test
    void mapsGraphJsonBackToStableDslProjection() {
        CanvasDslMapper mapper = new CanvasDslMapper();
        CanvasDslMappingService.MappingResult mapped = mapper.toGraphJson(CanvasDslValidatorTest.goldenPath());

        CanvasDslDocument document = mapper.fromGraphJson(mapped.graphJson());

        assertThat(document.apiVersion()).isEqualTo("canvas/v1");
        assertThat(document.kind()).isEqualTo("Journey");
        assertThat(document.metadata().name()).isEqualTo("new-user-welcome");
        assertThat(document.spec().nodes()).extracting(CanvasDslDocument.Node::id)
                .containsExactly("segment", "send", "end");
    }

    /**
     * 处理exportsDslProjectionFromCurrentGraphJsonShape。
     */
    @Test
    void exportsDslProjectionFromCurrentGraphJsonShape() {
        CanvasDslMapper mapper = new CanvasDslMapper();

        CanvasDslDocument document = mapper.fromGraphJson("""
                {
                  "metadata": {"name": "current-shape", "title": "Current Shape"},
                  "trigger": {"type": "webhook", "event": "lead.created"},
                  "nodes": [
                    {"id": "send", "type": "message", "data": {"config": {"channel": "email"}}},
                    {"id": "end", "type": "end", "position": {"x": 160, "y": 0}}
                  ],
                  "edges": [{"source": "send", "target": "end"}]
                }
                """);

        assertThat(document.apiVersion()).isEqualTo("canvas/v1");
        assertThat(document.kind()).isEqualTo("Journey");
        assertThat(document.metadata().name()).isEqualTo("current-shape");
        assertThat(document.metadata().title()).isEqualTo("Current Shape");
        assertThat(document.spec().nodes()).extracting(CanvasDslDocument.Node::id)
                .containsExactly("send", "end");
        assertThat(document.spec().nodes().get(0).config()).containsEntry("channel", "email");
        assertThat(document.spec().edges()).containsExactly(new CanvasDslDocument.Edge("send", "end"));
    }

    /**
     * 处理preservesMetadataTitleAsPublicDslFieldWhenMapping。
     */
    @Test
    void preservesMetadataTitleAsPublicDslFieldWhenMapping() {
        CanvasDslMapper mapper = new CanvasDslMapper();

        CanvasDslMappingService.MappingResult result = mapper.toGraphJson(CanvasDslValidatorTest.goldenPath());
        CanvasDslDocument exported = mapper.fromGraphJson(result.graphJson());

        assertThat(result.graphJson()).contains("\"title\":\"New user welcome\"");
        assertThat(result.graphJson()).doesNotContain("displayName");
        assertThat(exported.metadata().title()).isEqualTo("New user welcome");
    }

    /**
     * 处理reportsStableDiffBetweenDslDocuments。
     */
    @Test
    void reportsStableDiffBetweenDslDocuments() {
        CanvasDslMapper mapper = new CanvasDslMapper();
        CanvasDslDocument source = CanvasDslValidatorTest.goldenPath();
        CanvasDslDocument target = new CanvasDslDocument(
                "canvas/v1",
                "Journey",
                new CanvasDslDocument.Metadata("new-user-welcome", "New user welcome"),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger("webhook", "user.registered"),
                        java.util.List.of(
                                new CanvasDslDocument.Node("segment", "condition", java.util.Map.of("expression", "user.level == 'vip'")),
                                new CanvasDslDocument.Node("coupon", "coupon", java.util.Map.of("code", "WELCOME10")),
                                new CanvasDslDocument.Node("end", "end", java.util.Map.of())),
                        java.util.List.of(
                                new CanvasDslDocument.Edge("segment", "coupon"),
                                new CanvasDslDocument.Edge("coupon", "end"))));

        CanvasDslMappingService.DiffResult result = mapper.diff(source, target);

        assertThat(result.changed()).isTrue();
        assertThat(result.changes()).extracting(CanvasDslMappingService.DiffChange::code)
                .containsExactly(
                        "NODE_CONFIG_CHANGED",
                        "NODE_REMOVED",
                        "NODE_ADDED",
                        "EDGE_REMOVED",
                        "EDGE_REMOVED",
                        "EDGE_ADDED",
                        "EDGE_ADDED");
    }
}
