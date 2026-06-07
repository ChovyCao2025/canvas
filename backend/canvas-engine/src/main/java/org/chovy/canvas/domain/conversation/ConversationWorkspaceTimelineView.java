package org.chovy.canvas.domain.conversation;

import java.util.List;

public record ConversationWorkspaceTimelineView(
        ConversationWorkItemView workItem,
        ConversationContactProfileView contactProfile,
        ConversationSessionView session,
        List<ConversationMessageView> messages,
        List<ConversationSopTaskView> tasks,
        List<ConversationWorkItemAuditView> audits) {

    public ConversationWorkspaceTimelineView {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        audits = audits == null ? List.of() : List.copyOf(audits);
    }
}
