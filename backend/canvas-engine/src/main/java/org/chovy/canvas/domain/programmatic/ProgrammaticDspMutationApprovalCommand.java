package org.chovy.canvas.domain.programmatic;

public record ProgrammaticDspMutationApprovalCommand(
        String decision,
        String reason
) {
}
