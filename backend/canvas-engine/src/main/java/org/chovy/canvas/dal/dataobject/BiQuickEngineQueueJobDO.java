package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_quick_engine_queue_job")
public class BiQuickEngineQueueJobDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String poolKey;

    private String sqlHash;

    private String datasetKey;

    private String requestedBy;

    private String status;

    private Integer attemptCount;

    private LocalDateTime queuedAt;

    private LocalDateTime expiresAt;

    private String claimedBy;

    private LocalDateTime claimedAt;

    private LocalDateTime completedAt;

    private String blockedReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
