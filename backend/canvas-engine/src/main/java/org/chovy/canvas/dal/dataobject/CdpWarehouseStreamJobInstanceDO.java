package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseStreamJobInstanceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_stream_job_instance")
public class CdpWarehouseStreamJobInstanceDO {

    /** CDP数仓流任务实例主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓流任务实例管道业务键 */
    private String pipelineKey;

    /** CDP数仓流任务实例任务业务键 */
    private String jobKey;

    /** CDP数仓流任务实例引擎类型 */
    private String engineType;

    /** 关联的引擎任务 ID */
    private String engineJobId;

    /** CDP数仓流任务实例部署引用 */
    private String deploymentRef;

    /** CDP数仓流任务实例运行时状态 */
    private String runtimeStatus;

    /** CDP数仓流任务实例期望状态 */
    private String desiredStatus;

    /** CDP数仓流任务实例最近心跳时间 */
    private LocalDateTime lastHeartbeatAt;

    /** CDP数仓流任务实例心跳载荷明细 JSON */
    private String heartbeatPayloadJson;

    /** CDP数仓流任务实例最近错误消息 */
    private String lastErrorMessage;

    /** CDP数仓流任务实例负责人姓名 */
    private String ownerName;

    /** CDP数仓流任务实例创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓流任务实例最后更新时间 */
    private LocalDateTime updatedAt;
}
