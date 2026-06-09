package org.chovy.canvas.domain.conversation;

import java.util.List;

/**
 * ConversationWorkspaceTimelineView 承载 domain.conversation 场景中的不可变数据快照。
 * @param workItem workItem 字段。
 * @param contactProfile contactProfile 字段。
 * @param session session 字段。
 * @param messages messages 字段。
 * @param tasks tasks 字段。
 * @param audits audits 字段。
 */
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
