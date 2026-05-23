package org.chovy.canvas.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CanvasUpdateReq {
    private String name;
    private String description;
    /** 最新草稿 graph JSON */
    private String graphJson;
    private String updatedBy;
    /** REALTIME | SCHEDULED */
    private String triggerType;
    private String cronExpression;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer maxTotalExecutions;
    private Integer perUserDailyLimit;
    private Integer perUserTotalLimit;
    private Integer cooldownSeconds;
}
