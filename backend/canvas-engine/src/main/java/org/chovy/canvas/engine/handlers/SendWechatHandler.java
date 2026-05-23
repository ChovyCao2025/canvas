package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;

@Component
@NodeHandlerType(NodeType.SEND_WECHAT)
public class SendWechatHandler extends AbstractSendMessageHandler {
    public SendWechatHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    @Override
    protected String channel() {
        return "WECHAT";
    }
}
