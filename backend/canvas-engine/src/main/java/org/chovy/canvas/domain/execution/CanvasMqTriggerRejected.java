package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_mq_trigger_rejected")
public class CanvasMqTriggerRejected {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String tag;
    private String reason;
    private String errorMsg;
    private String body;
    private LocalDateTime createdAt;
}
