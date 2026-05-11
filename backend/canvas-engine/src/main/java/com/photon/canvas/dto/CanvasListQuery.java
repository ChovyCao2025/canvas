package com.photon.canvas.dto;

import lombok.Data;

@Data
public class CanvasListQuery {
    private int page = 1;
    private int size = 20;
    /** 0草稿 1已发布 2已下线，null=不过滤 */
    private Integer status;
    private String name;
}
