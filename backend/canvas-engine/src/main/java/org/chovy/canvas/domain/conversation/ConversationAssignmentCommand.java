package org.chovy.canvas.domain.conversation;

public record ConversationAssignmentCommand(
        String assignedTo,
        String assignedTeam,
        String note) {
}
