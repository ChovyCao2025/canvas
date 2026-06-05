package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analytics_retention_policy")
public class AnalyticsRetentionPolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String recordKind;

    private Integer retentionDays;

    private String action;

    private Integer maxBatchSize;

    private String legalHoldBehavior;

    private Boolean enabled;

    private String updatedBy;

    private LocalDateTime updatedAt;
}
