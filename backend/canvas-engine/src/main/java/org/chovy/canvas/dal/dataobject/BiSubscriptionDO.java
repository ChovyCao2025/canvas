package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiSubscriptionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_subscription")
public class BiSubscriptionDO {

    /** BI订阅主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI订阅订阅业务键 */
    private String subscriptionKey;

    /** BI订阅名称 */
    private String name;

    /** BI订阅资源类型 */
    private String resourceType;

    /** BI订阅资源 ID */
    private Long resourceId;

    /** BI订阅调度配置 JSON */
    private String scheduleJson;

    /** BI订阅接收方配置 JSON */
    private String receiverJson;

    /** BI订阅投递明细 JSON */
    private String deliveryJson;

    /** BI订阅是否启用 */
    private Boolean enabled;

    /** BI订阅创建人 */
    private String createdBy;

    /** BI订阅创建时间 */
    private LocalDateTime createdAt;

    /** BI订阅最后更新时间 */
    private LocalDateTime updatedAt;
}
