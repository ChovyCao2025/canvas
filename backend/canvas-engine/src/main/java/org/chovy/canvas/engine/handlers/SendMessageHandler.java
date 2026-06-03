package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Generic product-facing message send node.
 *
 * <p>Channel-specific nodes were removed from the public catalog. Channel is a
 * configuration value so the journey builder sees one message action while the
 * delivery layer still receives explicit channel information.
 */
@Component
@NodeHandlerType(NodeType.SEND_MESSAGE)
public class SendMessageHandler extends AbstractSendMessageHandler {

    public SendMessageHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    @Override
    protected String channel() {
        return "EMAIL";
    }

    @Override
    protected String channel(Map<String, Object> config) {
        Object value = config.get("channel");
        return value == null || value.toString().isBlank()
                ? channel()
                : value.toString().trim().toUpperCase();
    }
}
