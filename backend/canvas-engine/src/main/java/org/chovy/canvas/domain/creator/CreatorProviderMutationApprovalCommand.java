package org.chovy.canvas.domain.creator;

public record CreatorProviderMutationApprovalCommand(
        String decision,
        String reason
) {
}
