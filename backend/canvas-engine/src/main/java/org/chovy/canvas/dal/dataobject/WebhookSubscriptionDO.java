package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("webhook_subscription")
public class WebhookSubscriptionDO {
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String DISABLED = "DISABLED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String name;
    private String callbackUrl;
    private String secretPrefix;
    private String secretHash;
    private String secretCiphertext;
    private String eventTypes;
    private String status;
    private Integer maxAttempts;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
