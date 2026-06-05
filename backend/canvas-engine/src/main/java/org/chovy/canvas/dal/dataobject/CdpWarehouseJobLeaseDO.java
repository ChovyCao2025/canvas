package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_job_lease")
public class CdpWarehouseJobLeaseDO {

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
