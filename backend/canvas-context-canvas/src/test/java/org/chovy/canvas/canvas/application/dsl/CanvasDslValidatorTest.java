package org.chovy.canvas.canvas.application.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;
import org.junit.jupiter.api.Test;

/**
 * 封装CanvasDslValidatorTest相关的业务逻辑。
 */
class CanvasDslValidatorTest {

    /**
     * 处理rejectsUnsupportedVersionDuplicateNodesAndDanglingEdges。
     */
    @Test
    void rejectsUnsupportedVersionDuplicateNodesAndDanglingEdges() {
        CanvasDslValidator validator = new CanvasDslValidator();

        CanvasDslValidationResult result = validator.validate(new CanvasDslDocument(
                "canvas/v2",
                "Journey",
                new CanvasDslDocument.Metadata("new-user-welcome", "New user welcome"),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger("webhook", "user.registered"),
                        List.of(
                                new CanvasDslDocument.Node("segment", "condition", Map.of()),
                                new CanvasDslDocument.Node("segment", "message", Map.of())),
                        List.of(new CanvasDslDocument.Edge("segment", "missing")))));

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).extracting(CanvasDslValidationResult.Violation::code)
                .containsExactly("UNSUPPORTED_API_VERSION", "DUPLICATE_NODE_ID", "UNKNOWN_EDGE_TARGET");
    }

    /**
     * 处理acceptsSupportedGoldenPathNodeSet。
     */
    @Test
    void acceptsSupportedGoldenPathNodeSet() {
        CanvasDslValidator validator = new CanvasDslValidator();

        CanvasDslValidationResult result = validator.validate(goldenPath());

        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    /**
     * 处理rejectsWrongKindBlankMetadataBlankNodeIdAndCycles。
     */
    @Test
    void rejectsWrongKindBlankMetadataBlankNodeIdAndCycles() {
        CanvasDslValidator validator = new CanvasDslValidator();

        CanvasDslValidationResult result = validator.validate(new CanvasDslDocument(
                "canvas/v1",
                "Workflow",
                new CanvasDslDocument.Metadata(" ", "Bad"),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger("webhook", "user.registered"),
                        List.of(
                                new CanvasDslDocument.Node("start", "condition", Map.of()),
                                new CanvasDslDocument.Node(" ", "message", Map.of()),
                                new CanvasDslDocument.Node("end", "end", Map.of())),
                        List.of(
                                new CanvasDslDocument.Edge("start", "end"),
                                new CanvasDslDocument.Edge("end", "start")))));

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).extracting(CanvasDslValidationResult.Violation::code)
                .containsExactly("UNSUPPORTED_KIND", "MISSING_METADATA_NAME", "MISSING_NODE_ID", "GRAPH_CONTAINS_CYCLE");
    }

    /**
     * 处理goldenPath。
     */
    static CanvasDslDocument goldenPath() {
        return new CanvasDslDocument(
                "canvas/v1",
                "Journey",
                new CanvasDslDocument.Metadata("new-user-welcome", "New user welcome"),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger("webhook", "user.registered"),
                        List.of(
                                new CanvasDslDocument.Node("segment", "condition", Map.of("expression", "user.level == 'new'")),
                                new CanvasDslDocument.Node("send", "message", Map.of("channel", "sms")),
                                new CanvasDslDocument.Node("end", "end", Map.of())),
                        List.of(
                                new CanvasDslDocument.Edge("segment", "send"),
                                new CanvasDslDocument.Edge("send", "end"))));
    }
}
