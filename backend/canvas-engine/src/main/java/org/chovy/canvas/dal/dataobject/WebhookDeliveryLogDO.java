package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebhookDeliveryLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("webhook_delivery_log")
public class WebhookDeliveryLogDO {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
    public static final String DEAD = "DEAD";

    /** Webhook投递日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的订阅 ID */
    private Long subscriptionId;
    /** 关联的投递 ID */
    private String deliveryId;
    /** Webhook投递日志事件类型 */
    private String eventType;
    /** Webhook投递日志载荷 */
    private String payload;
    /** Webhook投递日志尝试 */
    private Integer attempt;
    /** Webhook投递日志HTTP状态 */
    private Integer httpStatus;
    /** Webhook投递日志响应正文 */
    private String responseBody;
    /** Webhook投递日志当前状态 */
    private String status;
    /** Webhook投递日志下次重试时间 */
    private LocalDateTime nextRetryAt;
    /** Webhook投递日志错误信息 */
    private String errorMessage;
    /** Webhook投递日志终端原因 */
    private String terminalReason;
    /** Webhook投递日志创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** Webhook投递日志最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
