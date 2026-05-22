package org.chovy.canvas.domain.audience;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_definition")
public class AudienceDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String ruleJson;
    private String engineType;
    private String dataSourceType;
    private String dataSourceConfig;
    private String evaluationStrategy;
    private String cronExpression;
    private Integer enabled;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
