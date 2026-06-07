package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_activity")
public class GrowthActivityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String activityKey;

    private String activityName;

    private String activityType;

    private String status;

    private Long campaignId;

    private String objective;

    private String ownerTeam;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String channelScope;

    private String audienceRefsJson;

    private String riskPolicyRef;

    private String experimentRef;

    private String dashboardRef;

    private String metadataJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
