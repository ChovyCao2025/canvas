package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractProviderConversationReplyAdapter<T extends ProviderConversationReplyPayload>
        implements ConversationReplyAdapter<T> {

    private final String adapterKey;
    private final Class<T> payloadType;
    private final String missingPayloadMessage;
    private final List<ProviderAttribute<T>> providerAttributes;
    private final Function<T, String> fallbackText;
    private final List<Function<T, String>> interactiveMarkers;

    AbstractProviderConversationReplyAdapter(String adapterKey,
                                             Class<T> payloadType,
                                             String missingPayloadMessage) {
        this(adapterKey, payloadType, missingPayloadMessage, List.of(), payload -> null, List.of());
    }

    AbstractProviderConversationReplyAdapter(String adapterKey,
                                             Class<T> payloadType,
                                             String missingPayloadMessage,
                                             List<ProviderAttribute<T>> providerAttributes,
                                             Function<T, String> fallbackText,
                                             List<Function<T, String>> interactiveMarkers) {
        this.adapterKey = adapterKey;
        this.payloadType = payloadType;
        this.missingPayloadMessage = missingPayloadMessage;
        this.providerAttributes = providerAttributes == null ? List.of() : List.copyOf(providerAttributes);
        this.fallbackText = fallbackText == null ? payload -> null : fallbackText;
        this.interactiveMarkers = interactiveMarkers == null ? List.of() : List.copyOf(interactiveMarkers);
    }

    @Override
    public final String adapterKey() {
        return adapterKey;
    }

    @Override
    public final Class<?> payloadType() {
        return payloadType;
    }

    @Override
    public final ConversationIngressReq toIngress(T payload, ConversationAdapterContext context) {
        if (payload == null) {
            throw new IllegalArgumentException(missingPayloadMessage);
        }
        return toProviderIngress(payload);
    }

    private ConversationIngressReq toProviderIngress(T payload) {
        Map<String, Object> attributes = ConversationReplyAdapterSupport.adapterAttributes(
                adapterKey(),
                payload.attributes());
        for (Map.Entry<String, String> attribute : providerAttributes(payload)) {
            if (attribute != null && attribute.getValue() != null && !attribute.getValue().isBlank()) {
                attributes.put(attribute.getKey(), attribute.getValue().trim());
            }
        }
        return ConversationReplyAdapterSupport.providerIngress(
                payload,
                adapterKey(),
                fallbackText(payload),
                attributes,
                interactiveMarkers(payload).toArray(String[]::new));
    }

    private List<Map.Entry<String, String>> providerAttributes(T payload) {
        return providerAttributes.stream()
                .map(attribute -> ConversationReplyAdapterSupport.textAttribute(
                        attribute.name(),
                        attribute.value().apply(payload)))
                .toList();
    }

    private String fallbackText(T payload) {
        return fallbackText.apply(payload);
    }

    private List<String> interactiveMarkers(T payload) {
        return interactiveMarkers.stream()
                .map(marker -> marker.apply(payload))
                .toList();
    }

    protected static <T extends ProviderConversationReplyPayload> ProviderAttribute<T> providerAttribute(
            String name,
            Function<T, String> value) {
        return new ProviderAttribute<>(name, value);
    }

    protected record ProviderAttribute<T extends ProviderConversationReplyPayload>(
            String name,
            Function<T, String> value) {
    }
}
