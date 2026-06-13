package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_mq_trigger_rejected")
public class CanvasMqTriggerRejectedDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    public String msgId;
    public String tag;
    public String reason;
    public String errorMsg;
    public String body;
    public LocalDateTime createdAt;
}
