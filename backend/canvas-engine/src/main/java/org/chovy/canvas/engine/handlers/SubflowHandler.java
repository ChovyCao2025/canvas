package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.SUBFLOW)
public class SubflowHandler implements NodeHandler {
    private final SubFlowRefHandler delegate;

    public SubflowHandler(SubFlowRefHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> mapped = new HashMap<>(config);
        if (mapped.containsKey("subflowId") && !mapped.containsKey("subFlowId")) {
            mapped.put("subFlowId", mapped.get("subflowId"));
        }
        return delegate.executeAsync(mapped, ctx);
    }
}
