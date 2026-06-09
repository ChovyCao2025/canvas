package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * AbstractProviderConversationReplyAdapter 业务组件。
 */
abstract class AbstractProviderConversationReplyAdapter<T extends ProviderConversationReplyPayload>
        implements ConversationReplyAdapter<T> {

    private final String adapterKey;
    private final Class<T> payloadType;
    private final String missingPayloadMessage;
    private final List<ProviderAttribute<T>> providerAttributes;
    private final Function<T, String> fallbackText;
    private final List<Function<T, String>> interactiveMarkers;

    /**
     * 执行 AbstractProviderConversationReplyAdapter 流程，围绕 abstract provider conversation reply adapter 完成校验、计算或结果组装。
     *
     * @param adapterKey 业务键，用于在同一租户下定位资源。
     * @param payloadType 类型标识，用于选择对应处理分支。
     * @param missingPayloadMessage missing payload message 参数，用于 AbstractProviderConversationReplyAdapter 流程中的校验、计算或对象转换。
     */
    AbstractProviderConversationReplyAdapter(String adapterKey,
                                             Class<T> payloadType,
                                             String missingPayloadMessage) {
        this(adapterKey, payloadType, missingPayloadMessage, List.of(), payload -> null, List.of());
    }

    /**
     * 执行 AbstractProviderConversationReplyAdapter 流程，围绕 abstract provider conversation reply adapter 完成校验、计算或结果组装。
     *
     * @param adapterKey 业务键，用于在同一租户下定位资源。
     * @param payloadType 类型标识，用于选择对应处理分支。
     * @param missingPayloadMessage missing payload message 参数，用于 AbstractProviderConversationReplyAdapter 流程中的校验、计算或对象转换。
     * @param providerAttributes provider attributes 参数，用于 AbstractProviderConversationReplyAdapter 流程中的校验、计算或对象转换。
     * @param fallbackText fallback text 参数，用于 AbstractProviderConversationReplyAdapter 流程中的校验、计算或对象转换。
     * @param interactiveMarkers interactive markers 参数，用于 AbstractProviderConversationReplyAdapter 流程中的校验、计算或对象转换。
     */
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

    /**
     * adapterKey 处理 domain.conversation 场景的业务逻辑。
     * @return 返回 adapter key 生成的文本或业务键。
     */
    @Override
    public final String adapterKey() {
        return adapterKey;
    }

    /**
     * payloadType 处理 domain.conversation 场景的业务逻辑。
     * @return 返回 payloadType 流程生成的业务结果。
     */
    @Override
    public final Class<?> payloadType() {
        return payloadType;
    }

    /**
     * toIngress 校验或转换 domain.conversation 场景的数据。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回组装或转换后的结果对象。
     */
    @Override
    public final ConversationIngressReq toIngress(T payload, ConversationAdapterContext context) {
        if (payload == null) {
            throw new IllegalArgumentException(missingPayloadMessage);
        }
        return toProviderIngress(payload);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 执行 providerAttributes 流程，围绕 provider attributes 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 provider attributes 生成的文本或业务键。
     */
    private List<Map.Entry<String, String>> providerAttributes(T payload) {
        return providerAttributes.stream()
                .map(attribute -> ConversationReplyAdapterSupport.textAttribute(
                        attribute.name(),
                        attribute.value().apply(payload)))
                .toList();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 fallback text 生成的文本或业务键。
     */
    private String fallbackText(T payload) {
        return fallbackText.apply(payload);
    }

    /**
     * 执行 interactiveMarkers 流程，围绕 interactive markers 完成校验、计算或结果组装。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 interactive markers 汇总后的集合、分页或映射视图。
     */
    private List<String> interactiveMarkers(T payload) {
        return interactiveMarkers.stream()
                .map(marker -> marker.apply(payload))
                .toList();
    }

    /**
     * providerAttribute 处理 domain.conversation 场景的业务逻辑。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 providerAttribute 流程生成的业务结果。
     */
    protected static <T extends ProviderConversationReplyPayload> ProviderAttribute<T> providerAttribute(
            String name,
            Function<T, String> value) {
        return new ProviderAttribute<>(name, value);
    }

    /**
     * ProviderAttribute 数据记录。
     */
    protected record ProviderAttribute<T extends ProviderConversationReplyPayload>(
            String name,
            Function<T, String> value) {
    }
}
