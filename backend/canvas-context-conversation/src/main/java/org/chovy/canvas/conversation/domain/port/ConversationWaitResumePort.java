package org.chovy.canvas.conversation.domain.port;

import java.util.Map;

public interface ConversationWaitResumePort {

    int resumeEventWaits(String eventCode, String subject, Map<String, Object> attributes, String eventId);
}
