package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tag_definition")
public class TagDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String tagCode;
    private String tagType;   // offline / realtime
    private String description;
    private Integer enabled;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
