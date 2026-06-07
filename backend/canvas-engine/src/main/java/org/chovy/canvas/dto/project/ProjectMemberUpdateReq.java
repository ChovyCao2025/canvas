package org.chovy.canvas.dto.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectMemberUpdateReq(
        @NotBlank String username,
        @NotBlank String role
) {
}
