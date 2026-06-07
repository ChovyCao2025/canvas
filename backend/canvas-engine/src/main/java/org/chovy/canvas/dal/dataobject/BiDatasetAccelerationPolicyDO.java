package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dataset_acceleration_policy")
public class BiDatasetAccelerationPolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String datasetKey;
    private Boolean enabled;
    private String accelerationMode;
    private String refreshMode;
    private Long refreshIntervalMinutes;
    private Long ttlSeconds;
    private Long maxRows;
    private String cronExpression;
    private String materializedTable;
    private String lastStatus;
    private Long lastRunId;
    private LocalDateTime lastRefreshedAt;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
