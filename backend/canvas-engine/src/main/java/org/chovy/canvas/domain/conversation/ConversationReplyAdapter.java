package org.chovy.canvas.domain.conversation;

public interface ConversationReplyAdapter<T> {

    default String adapterKey() {
        return getClass().getSimpleName()
                .replace("ConversationReplyAdapter", "")
                .replace("ConversationAdapter", "")
                .trim()
                .toUpperCase();
    }

    default Class<?> payloadType() {
        return Object.class;
    }

    ConversationIngressReq toIngress(T payload, ConversationAdapterContext context);
}
