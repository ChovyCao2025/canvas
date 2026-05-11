package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("canvas_version")
public class CanvasVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long canvasId;

    /** 版本号，从1递增 */
    private Integer version;

    /** 完整画布 JSON */
    private String graphJson;

    /** 0草稿 1已发布 */
    private Integer status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
