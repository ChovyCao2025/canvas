package org.chovy.canvas.domain.conversation;

public interface ConversationAiReplyGenerator {

    ConversationAiReplyGenerationResult generate(ConversationAiReplyGenerationContext context);
}
