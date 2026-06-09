package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseSyncRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_sync_run")
public class CdpWarehouseSyncRunDO {

    /** CDP数仓同步运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓同步运行任务类型 */
    private String jobType;

    /** CDP数仓同步运行来源表 */
    private String sourceTable;

    /** 关联的来源开始 ID */
    private Long sourceStartId;

    /** 关联的来源结束 ID */
    private Long sourceEndId;

    /** CDP数仓同步运行窗口开始时间 */
    private LocalDateTime windowStart;

    /** CDP数仓同步运行窗口结束时间 */
    private LocalDateTime windowEnd;

    /** CDP数仓同步运行当前状态 */
    private String status;

    /** CDP数仓同步运行加载行数 */
    private Long loadedRows;

    /** CDP数仓同步运行失败行数 */
    private Long failedRows;

    /** CDP数仓同步运行错误信息 */
    private String errorMessage;

    /** CDP数仓同步运行开始时间 */
    private LocalDateTime startedAt;

    /** CDP数仓同步运行结束时间 */
    private LocalDateTime finishedAt;

    /** CDP数仓同步运行创建人 */
    private String createdBy;
}
