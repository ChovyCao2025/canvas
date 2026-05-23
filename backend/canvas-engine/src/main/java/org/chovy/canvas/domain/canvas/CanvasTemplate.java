package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 画布模板（canvas_template）。
 */
@Data
@TableName("canvas_template")
public class CanvasTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称。 */
    private String name;

    /** 模板描述。 */
    private String description;

    /** 模板分类。 */
    private String category;

    /** 模板图结构 JSON。 */
    private String graphJson;

    /** 模板缩略图（URL 或 Base64，按实现约定）。 */
    private String thumbnail;

    /** 官方模板标记：1=官方，0=自定义。 */
    private Integer isOfficial;

    /** 模板使用次数。 */
    private Integer useCount;

    /** 创建人。 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
