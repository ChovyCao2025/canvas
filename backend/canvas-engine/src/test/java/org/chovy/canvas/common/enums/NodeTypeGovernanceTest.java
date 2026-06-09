package org.chovy.canvas.common.enums;

import org.junit.jupiter.api.Test;
import org.chovy.canvas.engine.handlers.AiLlmHandler;
import org.chovy.canvas.engine.llm.AiLlmGateway;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NodeTypeGovernanceTest {

    @Test
    void exposesOnlyGovernedProductNodeTypes() {
        Set<String> actual = Arrays.stream(NodeType.class.getDeclaredFields())
                .filter(field -> String.class.equals(field.getType()))
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(actual).containsExactlyInAnyOrder(
                "START", "END", "DIRECT_RETURN",
                "LOOP",
                "DIRECT_CALL", "EVENT_TRIGGER", "MQ_TRIGGER", "SCHEDULED_TRIGGER",
                "IF_CONDITION", "LOGIC_RELATION", "SPLIT",
                "WAIT", "USER_INPUT", "MANUAL_APPROVAL", "HUB", "AGGREGATE", "THRESHOLD",
                "API_CALL", "SEND_MQ", "GROOVY",
                "SEND_MESSAGE",
                "AI_LLM",
                "TAGGER", "COMMIT_ACTION",
                "SUB_FLOW_REF", "TRANSFER_JOURNEY"
        );
    }

    @Test
    void futureStubNodesAreNotGenerallyAvailable() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V90__register_commit_action_node.sql"));

        assertThat(sql).doesNotContain("'AI_NEXT_BEST_ACTION'");
        assertThat(sql).doesNotContain("'RECOMMENDATION'");
        assertThat(sql).doesNotContain("'IN_APP_NOTIFY'");
    }

    @Test
    void aiCapabilityConstantsFollowVisibilityPolicy() throws Exception {
        Set<String> values = Arrays.stream(NodeType.class.getDeclaredFields())
                .filter(field -> String.class.equals(field.getType()))
                .map(field -> {
                    try {
                        return (String) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                })
                .collect(Collectors.toSet());

        assertThat(values).doesNotContain("AI_NEXT_BEST_ACTION");
        if (values.contains("AI_LLM")) {
            assertThat(AiLlmHandler.class.getName())
                    .isEqualTo("org.chovy.canvas.engine.handlers.AiLlmHandler");
            assertThat(AiLlmGateway.class.getName())
                    .isEqualTo("org.chovy.canvas.engine.llm.AiLlmGateway");
            assertThat(Files.readString(aiPolicyPath()))
                    .contains("P2-019")
                    .contains("AI_LLM")
                    .contains("AI_NEXT_BEST_ACTION");
        }
    }

    private Path aiPolicyPath() {
        for (Path candidate : List.of(
                Path.of("../../docs/product-evolution/AI_CAPABILITY_POLICY.md"),
                Path.of("../docs/product-evolution/AI_CAPABILITY_POLICY.md"),
                Path.of("docs/product-evolution/AI_CAPABILITY_POLICY.md"))) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return Path.of("docs/product-evolution/AI_CAPABILITY_POLICY.md");
    }
}
