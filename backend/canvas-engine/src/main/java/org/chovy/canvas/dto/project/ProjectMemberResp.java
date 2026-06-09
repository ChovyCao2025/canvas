package org.chovy.canvas.dto.project;

/**
 * ProjectMemberResp 承载 dto.project 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param projectId projectId 字段。
 * @param userId userId 字段。
 * @param username username 字段。
 * @param role role 字段。
 * @param source source 字段。
 */
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
