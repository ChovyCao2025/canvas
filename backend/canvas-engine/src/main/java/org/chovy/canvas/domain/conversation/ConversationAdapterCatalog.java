package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
/**
 * ConversationAdapterCatalog 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ConversationAdapterCatalog {

    private final Map<String, ConversationReplyAdapter<?>> adapters;

    /**
     * 初始化 ConversationAdapterCatalog 实例。
     *
     * @param adapters adapters 参数，用于 ConversationAdapterCatalog 流程中的校验、计算或对象转换。
     */
    public ConversationAdapterCatalog(List<ConversationReplyAdapter<?>> adapters) {
        Map<String, ConversationReplyAdapter<?>> indexed = new LinkedHashMap<>();
        for (ConversationReplyAdapter<?> adapter : adapters == null ? List.<ConversationReplyAdapter<?>>of() : adapters) {
            String key = normalize(adapter.adapterKey());
            if (indexed.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate conversation adapter key: " + key);
            }
            indexed.put(key, adapter);
        }
        this.adapters = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    @SuppressWarnings("unchecked")
    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 require 流程生成的业务结果。
     */
    public ConversationReplyAdapter<Object> require(String key) {
        String normalized = normalize(key);
        ConversationReplyAdapter<?> adapter = adapters.get(normalized);
        if (adapter == null) {
            throw new IllegalArgumentException("Conversation adapter not registered: " + normalized);
        }
        return (ConversationReplyAdapter<Object>) adapter;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 keys 汇总后的集合、分页或映射视图。
     */
    public List<String> keys() {
        return List.copyOf(adapters.keySet());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("conversation adapter key is required");
        }
        return key.trim().toUpperCase(Locale.ROOT);
    }
}
