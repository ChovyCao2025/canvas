package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasMqTriggerRejectedDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_mq_trigger_rejected")
public class CanvasMqTriggerRejectedDO {

    /**
     * 保存 id 对应的状态或配置。
     */
    @TableId(type = IdType.AUTO)
    public Long id;

    /**
     * 保存 msgId 对应的状态或配置。
     */
    public String msgId;

    /**
     * 保存 tag 对应的状态或配置。
     */
    public String tag;

    /**
     * 保存 reason 对应的状态或配置。
     */
    public String reason;

    /**
     * 保存 errorMsg 对应的状态或配置。
     */
    public String errorMsg;

    /**
     * 保存 body 对应的状态或配置。
     */
    public String body;

    /**
     * 保存 createdAt 对应的状态或配置。
     */
    public LocalDateTime createdAt;
}
