package org.chovy.canvas.common.enums;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
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
                "DIRECT_CALL", "EVENT_TRIGGER", "MQ_TRIGGER", "SCHEDULED_TRIGGER",
                "IF_CONDITION", "SPLIT",
                "WAIT", "HUB", "AGGREGATE", "THRESHOLD",
                "API_CALL", "SEND_MQ", "GROOVY",
                "SEND_MESSAGE",
                "TAGGER", "COMMIT_ACTION",
                "SUB_FLOW_REF"
        );
    }
}
