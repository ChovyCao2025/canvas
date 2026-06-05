package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("webhook_delivery_log")
public class WebhookDeliveryLogDO {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
    public static final String DEAD = "DEAD";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long subscriptionId;
    private String deliveryId;
    private String eventType;
    private String payload;
    private Integer attempt;
    private Integer httpStatus;
    private String responseBody;
    private String status;
    private LocalDateTime nextRetryAt;
    private String errorMessage;
    private String terminalReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
