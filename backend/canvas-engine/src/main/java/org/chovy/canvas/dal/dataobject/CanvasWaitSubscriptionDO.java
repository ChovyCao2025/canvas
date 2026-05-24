package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 等待订阅记录。
 *
 * <p>WAIT_UNTIL_EVENT 和 GOAL_CHECK 异步监听都会写入该表，事件上报或超时扫描再恢复对应执行。
 */
@Data
@TableName("canvas_wait_subscription")
public class CanvasWaitSubscriptionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private Long canvasId;
    private Long versionId;
    private String userId;
    private String nodeId;
    private String waitType;
    private String eventCode;
    private String eventFilters;
    private String resumePayload;
    private LocalDateTime expiresAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
