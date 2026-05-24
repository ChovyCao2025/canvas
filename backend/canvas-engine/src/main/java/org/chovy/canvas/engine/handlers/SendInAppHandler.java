package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;

@Component
@NodeHandlerType(NodeType.SEND_IN_APP)
public class SendInAppHandler extends AbstractSendMessageHandler {
    public SendInAppHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    @Override
    protected String channel() {
        return "IN_APP";
    }
}
