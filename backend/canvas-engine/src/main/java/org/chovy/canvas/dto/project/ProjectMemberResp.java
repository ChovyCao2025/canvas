package org.chovy.canvas.dto.project;

public record ProjectMemberResp(
        Long id,
        Long tenantId,
        Long projectId,
        Long userId,
        String username,
        String role,
        String source
) {
}
