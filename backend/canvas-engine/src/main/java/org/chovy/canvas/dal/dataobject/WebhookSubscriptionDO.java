package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebhookSubscriptionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("webhook_subscription")
public class WebhookSubscriptionDO {
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String DISABLED = "DISABLED";

    /** Webhook订阅主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** Webhook订阅名称 */
    private String name;
    /** Webhook订阅回调URL */
    private String callbackUrl;
    /** Webhook订阅密钥前缀 */
    private String secretPrefix;
    /** Webhook订阅密钥哈希 */
    private String secretHash;
    /** Webhook订阅密钥密文 */
    private String secretCiphertext;
    /** Webhook订阅事件类型 */
    private String eventTypes;
    /** Webhook订阅当前状态 */
    private String status;
    /** Webhook订阅最大尝试 */
    private Integer maxAttempts;
    /** Webhook订阅创建人 */
    private String createdBy;
    /** Webhook订阅创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** Webhook订阅最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
