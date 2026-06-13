package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_template")
public class CanvasTemplateDO {
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
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
