package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bi_delivery_log")
public class BiDeliveryLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String jobType;

    private Long jobId;

    private String jobKey;

    private String resourceType;

    private Long resourceId;

    private String channel;

    private String receiverJson;

    private String payloadJson;

    private BigDecimal metricValue;

    private String status;

    private String message;

    private String errorMessage;

    private Integer retryCount;

    private Integer maxRetryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime lastRetryAt;

    private LocalDateTime retryExhaustedAt;

    private String triggeredBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
