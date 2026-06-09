package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseJobLeaseDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_job_lease")
public class CdpWarehouseJobLeaseDO {

    /** CDP数仓任务租约主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓任务租约租约业务键 */
    private String leaseKey;

    /** CDP数仓任务租约负责人 ID */
    private String ownerId;

    /** CDP数仓任务租约租约截止 */
    private LocalDateTime leaseUntil;

    /** CDP数仓任务租约最近获取时间 */
    private LocalDateTime lastAcquiredAt;

    /** CDP数仓任务租约创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓任务租约最后更新时间 */
    private LocalDateTime updatedAt;
}
