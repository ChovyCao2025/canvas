package com.photon.canvas.domain.canvas;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("canvas")
public class Canvas {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;

    /** 0草稿 1已发布 2已下线 */
    private Integer status;

    private Long publishedVersionId;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
