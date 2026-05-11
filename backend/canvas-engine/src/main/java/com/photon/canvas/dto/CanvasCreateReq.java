package com.photon.canvas.dto;

import lombok.Data;

@Data
public class CanvasCreateReq {
    private String name;
    private String description;
    /** 初始 graph JSON（可为空） */
    private String graphJson;
    private String createdBy;
}
