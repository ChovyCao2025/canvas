package org.chovy.canvas.domain.conversation;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ConversationReplyAdapterSupport 承载对应领域的业务规则、流程编排和结果转换。
 */
final class ConversationReplyAdapterSupport {

    /**
     * 初始化 ConversationReplyAdapterSupport 实例。
     */
    private ConversationReplyAdapterSupport() {
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param provider provider 参数，用于 normalizeProvider 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeProvider(String provider) {
        if (isBlank(provider)) {
            return "DEFAULT";
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param text text 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @param fallback fallback 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @return 返回 first text 生成的文本或业务键。
     */
    static String firstText(String text, String fallback) {
        if (!isBlank(text)) {
            return text.trim();
        }
        return isBlank(fallback) ? null : fallback.trim();
    }

    @SafeVarargs
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param adapterKey 业务键，用于在同一租户下定位资源。
     * @param sourceAttributes source attributes 参数，用于 adapterAttributes 流程中的校验、计算或对象转换。
     * @param optionalTextAttributes optional text attributes 参数，用于 adapterAttributes 流程中的校验、计算或对象转换。
     * @return 返回 adapterAttributes 流程生成的业务结果。
     */
    static Map<String, Object> adapterAttributes(
            String adapterKey,
            Map<String, Object> sourceAttributes,
            Map.Entry<String, String>... optionalTextAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sourceAttributes != null) {
            attributes.putAll(sourceAttributes);
        }
        attributes.put("adapter", adapterKey);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map.Entry<String, String> attribute : optionalTextAttributes) {
            if (attribute != null && !isBlank(attribute.getValue())) {
                attributes.put(attribute.getKey(), attribute.getValue().trim());
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return attributes;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 text attribute 生成的文本或业务键。
     */
    static Map.Entry<String, String> textAttribute(String key, String value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param values values 参数，用于 hasAnyText 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    static boolean hasAnyText(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param channel channel 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @param interactive interactive 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @param fallbackText fallback text 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @param attributes attributes 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @return 返回 providerIngress 流程生成的业务结果。
     */
    static ConversationIngressReq providerIngress(ProviderConversationReplyPayload payload,
                                                  String channel,
                                                  boolean interactive,
                                                  String fallbackText,
                                                  Map<String, ?> attributes) {
        return new ConversationIngressReq(
                payload.canvasId(),
                payload.versionId(),
                payload.executionId(),
                payload.userId(),
                channel,
                normalizeProvider(payload.provider()),
                payload.externalMessageId(),
                payload.eventId(),
                interactive ? "INTERACTIVE" : "TEXT",
                firstText(payload.text(), fallbackText),
                payload.intent(),
                attributes == null ? Map.of() : new LinkedHashMap<>(attributes),
                payload.occurredAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param channel channel 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @param fallbackText fallback text 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @param attributes attributes 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @param interactiveMarkers interactive markers 参数，用于 providerIngress 流程中的校验、计算或对象转换。
     * @return 返回 providerIngress 流程生成的业务结果。
     */
    static ConversationIngressReq providerIngress(ProviderConversationReplyPayload payload,
                                                  String channel,
                                                  String fallbackText,
                                                  Map<String, ?> attributes,
                                                  String... interactiveMarkers) {
        return providerIngress(
                payload,
                channel,
                hasAnyText(interactiveMarkers),
                fallbackText,
                attributes);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
