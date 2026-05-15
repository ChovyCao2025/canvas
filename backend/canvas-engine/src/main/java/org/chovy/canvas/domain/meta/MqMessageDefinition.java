package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mq_message_definition")
public class MqMessageDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String messageCode;
    private String topic;
    private String requestSchema;
    private String description;
    private Integer enabled;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
