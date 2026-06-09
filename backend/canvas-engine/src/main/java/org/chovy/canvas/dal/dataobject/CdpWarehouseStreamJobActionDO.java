package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseStreamJobActionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_stream_job_action")
public class CdpWarehouseStreamJobActionDO {

    /** CDP数仓流任务动作主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓流任务动作管道业务键 */
    private String pipelineKey;

    /** CDP数仓流任务动作任务业务键 */
    private String jobKey;

    /** CDP数仓流任务动作 */
    private String action;

    /** CDP数仓流任务动作当前状态 */
    private String status;

    /** CDP数仓流任务动作请求人 */
    private String requestedBy;

    /** CDP数仓流任务动作原因说明 */
    private String reason;

    /** CDP数仓流任务动作请求时间 */
    private LocalDateTime requestedAt;

    /** CDP数仓流任务动作确认时间 */
    private LocalDateTime acknowledgedAt;

    /** CDP数仓流任务动作完成时间 */
    private LocalDateTime completedAt;

    /** CDP数仓流任务动作结果消息 */
    private String resultMessage;

    /** CDP数仓流任务动作创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓流任务动作最后更新时间 */
    private LocalDateTime updatedAt;
}
