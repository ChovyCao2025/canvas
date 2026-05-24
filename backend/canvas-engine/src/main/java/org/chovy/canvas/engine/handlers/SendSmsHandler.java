package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;

@Component
@NodeHandlerType(NodeType.SEND_SMS)
public class SendSmsHandler extends AbstractSendMessageHandler {
    public SendSmsHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    @Override
    protected String channel() {
        return "SMS";
    }
}
