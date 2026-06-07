package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_quick_engine_capacity_policy")
public class BiQuickEngineCapacityPolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Boolean enabled;
    private Long capacityLimitRows;
    private Integer warningThresholdPercent;
    private Integer criticalThresholdPercent;
    private String notificationChannels;
    private String notificationReceivers;
    private String poolKey;
    private Integer maxConcurrentQueries;
    private Integer queueLimit;
    private Integer queueTimeoutSeconds;
    private Integer poolWeight;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
