package org.chovy.canvas.dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * ProjectMemberUpdateReq 承载 dto.project 场景中的不可变数据快照。
 * @param username username 字段。
 * @param role role 字段。
 */
public record ProjectMemberUpdateReq(
        @NotBlank String username,
        @NotBlank String role
) {
}
