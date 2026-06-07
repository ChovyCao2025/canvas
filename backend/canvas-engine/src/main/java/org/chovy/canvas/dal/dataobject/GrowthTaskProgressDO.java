package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("growth_task_progress")
public class GrowthTaskProgressDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long activityId;

    private Long participantId;

    private Long taskId;

    private BigDecimal progressValue;

    private BigDecimal targetValue;

    private String status;

    private String lastEventKey;

    private String evidenceJson;

    private Long rewardGrantId;

    private String updatedBy;

    private LocalDateTime completedAt;

    private LocalDateTime updatedAt;
}
