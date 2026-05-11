package org.chovy.canvas.auth.dto;

import lombok.Data;

@Data
public class UserCreateReq {
    private String username;
    private String password;
    private String displayName;
    /** ADMIN / OPERATOR */
    private String role;
}
