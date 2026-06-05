package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_alert_rule")
public class BiAlertRuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String alertKey;

    private String name;

    private Long datasetId;

    private String metricKey;

    private String conditionJson;

    private String receiverJson;

    private Boolean enabled;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
