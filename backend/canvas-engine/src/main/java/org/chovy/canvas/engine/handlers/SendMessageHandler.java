package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.channel.ChannelDedupeService;
import org.chovy.canvas.engine.channel.ChannelFallbackService;
import org.chovy.canvas.engine.channel.ProviderBackpressureService;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
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

    SendMessageHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    @Autowired
    public SendMessageHandler(ReachDeliveryService deliveryService,
                              ChannelConnectorRegistry connectorRegistry,
                              ProviderBackpressureService backpressureService,
                              ChannelFallbackService fallbackService,
                              ChannelDedupeService dedupeService,
                              ObjectProvider<MarketingContentReleaseService> contentReleaseServiceProvider) {
        super(deliveryService,
                connectorRegistry,
                backpressureService,
                fallbackService,
                dedupeService,
                contentReleaseServiceProvider == null ? null : contentReleaseServiceProvider.getIfAvailable());
    }

    SendMessageHandler(ReachDeliveryService deliveryService,
                       ChannelConnectorRegistry connectorRegistry,
                       ProviderBackpressureService backpressureService,
                       ChannelFallbackService fallbackService,
                       ChannelDedupeService dedupeService) {
        super(deliveryService, connectorRegistry, backpressureService, fallbackService, dedupeService);
    }

    SendMessageHandler(ReachDeliveryService deliveryService, ChannelConnectorRegistry connectorRegistry) {
        super(deliveryService, connectorRegistry);
    }

    SendMessageHandler(ReachDeliveryService deliveryService,
                       MarketingContentReleaseService contentReleaseService) {
        super(deliveryService, contentReleaseService);
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
