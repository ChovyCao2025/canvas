package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_decision_run")
public class AiDecisionRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String modelKey;
    private String modelVersion;
    private String decisionScope;
    private LocalDate runDate;
    private String status;
    private Integer requestedCount;
    private Integer processedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private String metadataJson;
    private String createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
