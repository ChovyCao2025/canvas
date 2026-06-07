package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ConversationAdapterCatalog {

    private final Map<String, ConversationReplyAdapter<?>> adapters;

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
    public ConversationReplyAdapter<Object> require(String key) {
        String normalized = normalize(key);
        ConversationReplyAdapter<?> adapter = adapters.get(normalized);
        if (adapter == null) {
            throw new IllegalArgumentException("Conversation adapter not registered: " + normalized);
        }
        return (ConversationReplyAdapter<Object>) adapter;
    }

    public List<String> keys() {
        return List.copyOf(adapters.keySet());
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("conversation adapter key is required");
        }
        return key.trim().toUpperCase(Locale.ROOT);
    }
}
