package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("event_log")
public class EventLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventCode;
    private String userId;
    private String attributes;
    private Integer canvasTriggered;
    private Integer canvasCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
