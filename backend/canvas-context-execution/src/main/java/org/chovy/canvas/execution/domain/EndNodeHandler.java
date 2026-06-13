package org.chovy.canvas.execution.domain;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@NodeHandlerType("END")
public class EndNodeHandler implements NodeHandler {

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        return NodeExecutionResult.success(Map.of("ended", true));
    }
}
