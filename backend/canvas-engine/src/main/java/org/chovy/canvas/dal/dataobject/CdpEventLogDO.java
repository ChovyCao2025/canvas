package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_event_log")
public class CdpEventLogDO {
    public static final String ACCEPTED = "ACCEPTED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long writeKeyId;
    private String messageId;
    private String eventType;
    private String eventCode;
    private String userId;
    private String anonymousId;
    private String sessionId;
    private String deviceId;
    private String platform;
    private String sdkContext;
    private String properties;
    private String idempotencyKey;
    private LocalDateTime eventTime;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;
    private String status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
