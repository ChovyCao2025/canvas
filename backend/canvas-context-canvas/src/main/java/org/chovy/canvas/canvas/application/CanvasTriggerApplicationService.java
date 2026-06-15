package org.chovy.canvas.canvas.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasTriggerFacade;
import org.springframework.stereotype.Service;

@Service
public class CanvasTriggerApplicationService implements CanvasTriggerFacade {

    @Override
    public BehaviorTriggerResult triggerBehavior(BehaviorTriggerCommand command) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accepted", true);
        data.put("canvasId", command.canvasId());
        data.put("userId", command.userId());
        data.put("eventCode", command.eventCode());
        data.put("eventId", command.eventId());
        data.put("behaviorData", command.behaviorData());
        return new BehaviorTriggerResult(data);
    }
}
