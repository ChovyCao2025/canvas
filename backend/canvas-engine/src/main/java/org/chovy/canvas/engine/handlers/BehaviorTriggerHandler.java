package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@NodeHandlerType("BEHAVIOR_TRIGGER")
public class BehaviorTriggerHandler implements NodeHandler {

    private final BehaviorInAppHandler inappHandler;
    private final DirectCallHandler    directHandler;

    @Autowired
    public BehaviorTriggerHandler(BehaviorInAppHandler inappHandler,
                                   DirectCallHandler directHandler) {
        this.inappHandler  = inappHandler;
        this.directHandler = directHandler;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String triggerType = (String) config.getOrDefault("triggerType", "inapp");
        if ("direct".equals(triggerType)) {
            return directHandler.executeAsync(config, ctx);
        }
        return inappHandler.executeAsync(config, ctx);
    }
}
