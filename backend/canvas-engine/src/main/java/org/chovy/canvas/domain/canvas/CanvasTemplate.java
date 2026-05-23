package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("canvas_template")
public class CanvasTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String category;
    private String templateKey;
    private String companyType;
    private String marketingScenario;
    private String difficulty;
    private String coveredNodeTypes;
    private Integer sortOrder;
    private Integer enabled;
    private String graphJson;
    private String thumbnail;
    private Integer isOfficial;
    private Integer useCount;
    private String  createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
