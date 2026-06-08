package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDeliverySchedulerLeaseDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_delivery_scheduler_lease")
public class BiDeliverySchedulerLeaseDO {

    /** BI投递调度租约主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** BI投递调度租约租约键 */
    private String leaseKey;

    /** BI投递调度租约负责人 ID */
    private String ownerId;

    /** BI投递调度租约租约有效截止时间 */
    private LocalDateTime leaseUntil;

    /** BI投递调度租约最近获取租约时间 */
    private LocalDateTime lastAcquiredAt;

    /** BI投递调度租约创建时间 */
    private LocalDateTime createdAt;

    /** BI投递调度租约最后更新时间 */
    private LocalDateTime updatedAt;
}
