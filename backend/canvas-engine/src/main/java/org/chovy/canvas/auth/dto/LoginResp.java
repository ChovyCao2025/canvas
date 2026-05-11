package org.chovy.canvas.auth.dto;

import lombok.Data;

@Data
public class LoginResp {
    private String token;
    private Long userId;
    private String username;
    private String displayName;
    private String role;
}
