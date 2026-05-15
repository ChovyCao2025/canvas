package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("event_definition")
public class EventDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String eventCode;
    private String attributes;   // JSON: [{name,displayName,type,required}]
    private String description;
    private Integer enabled;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
