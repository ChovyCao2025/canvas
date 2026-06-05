package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_subscription")
public class BiSubscriptionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String subscriptionKey;

    private String name;

    private String resourceType;

    private Long resourceId;

    private String scheduleJson;

    private String receiverJson;

    private String deliveryJson;

    private Boolean enabled;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
