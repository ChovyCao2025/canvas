package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpEventLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_event_log")
public class CdpEventLogDO {
    public static final String ACCEPTED = "ACCEPTED";

    /** CDP事件日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的写入键 ID */
    private Long writeKeyId;
    /** 关联的消息 ID */
    private String messageId;
    /** CDP事件日志事件类型 */
    private String eventType;
    /** CDP事件日志事件编码 */
    private String eventCode;
    /** 关联的用户 ID */
    private String userId;
    /** 关联的匿名 ID */
    private String anonymousId;
    /** 关联的会话 ID */
    private String sessionId;
    /** 关联的设备 ID */
    private String deviceId;
    /** CDP事件日志平台 */
    private String platform;
    /** CDP事件日志SDK上下文 */
    private String sdkContext;
    /** CDP事件日志属性 */
    private String properties;
    /** CDP事件日志幂等键 */
    private String idempotencyKey;
    /** CDP事件日志事件发生时间 */
    private LocalDateTime eventTime;
    /** CDP事件日志发送时间 */
    private LocalDateTime sentAt;
    /** CDP事件日志接收时间 */
    private LocalDateTime receivedAt;
    /** CDP事件日志当前状态 */
    private String status;
    /** CDP事件日志错误信息 */
    private String errorMessage;
    /** CDP事件日志创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
