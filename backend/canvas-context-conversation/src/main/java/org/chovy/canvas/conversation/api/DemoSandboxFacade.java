package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DemoSandboxFacade {

    SandboxView install(InstallCommand command, String actor);

    ResetResult reset(Long tenantId, String actor);

    List<SandboxView> expired();

    ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor);

    record InstallCommand(Long tenantId, String demoName, int ttlDays) {
    }

    record ConversationReplyCommand(
            Long canvasId,
            Long versionId,
            String executionId,
            String userId,
            String externalMessageId,
            String eventId,
            String text,
            String intent,
            Map<String, Object> attributes) {
    }

    record SandboxView(
            Long id,
            Long tenantId,
            String demoName,
            int ttlDays,
            String status,
            String installedBy,
            LocalDateTime installedAt,
            LocalDateTime expiresAt) {
    }

    record ResetResult(Long tenantId, String status, String resetBy, LocalDateTime resetAt) {
    }

    record ConversationReplyResult(
            Long id,
            Long tenantId,
            String channel,
            Long canvasId,
            Long versionId,
            String executionId,
            String userId,
            String externalMessageId,
            String eventId,
            String text,
            String intent,
            Map<String, Object> attributes,
            String createdBy,
            LocalDateTime createdAt) {
    }
}
