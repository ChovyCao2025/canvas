package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

public interface ProviderConversationReplyPayload {

    Long canvasId();

    Long versionId();

    String executionId();

    String userId();

    String provider();

    String externalMessageId();

    String eventId();

    String text();

    String intent();

    Map<String, Object> attributes();

    LocalDateTime occurredAt();
}
