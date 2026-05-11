package com.photon.canvas.auth.dto;

import lombok.Data;

@Data
public class UserUpdateReq {
    private String displayName;
    private String password;
    private String role;
}
