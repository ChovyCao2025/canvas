package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_alert_delivery")
public class MarketingMonitorAlertDeliveryDO {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
    public static final String DEAD = "DEAD";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long alertId;

    private Long channelId;

    private String channelKey;

    private String channelType;

    private String deliveryId;

    private Integer attempt;

    private Integer httpStatus;

    private String requestPayload;

    private String responseBody;

    private String status;

    private LocalDateTime nextRetryAt;

    private String errorMessage;

    private String terminalReason;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
