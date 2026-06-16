package org.chovy.canvas.execution.domain;

import java.util.Map;

@FunctionalInterface
public interface NodeConfigParser {

    Map<String, Object> parse(String configJson, String nodeId);

    static NodeConfigParser empty() {
        return (configJson, nodeId) -> Map.of();
    }
}
