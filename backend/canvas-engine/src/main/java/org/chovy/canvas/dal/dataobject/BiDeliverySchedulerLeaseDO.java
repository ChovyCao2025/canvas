package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_delivery_scheduler_lease")
public class BiDeliverySchedulerLeaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String leaseKey;

    private String ownerId;

    private LocalDateTime leaseUntil;

    private LocalDateTime lastAcquiredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
