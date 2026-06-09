package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseRealtimeCheckpointDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_realtime_checkpoint")
public class CdpWarehouseRealtimeCheckpointDO {

    /** CDP数仓实时检查点主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓实时检查点流业务键 */
    private String streamKey;

    /** 关联的最近事件日志 ID */
    private Long lastEventLogId;

    /** 关联的最近消息 ID */
    private String lastMessageId;

    /** CDP数仓实时检查点最近事件编码 */
    private String lastEventCode;

    /** CDP数仓实时检查点最近事件时间 */
    private LocalDateTime lastEventTime;

    /** CDP数仓实时检查点最近接收时间 */
    private LocalDateTime lastReceivedAt;

    /** CDP数仓实时检查点最近送达时间 */
    private LocalDateTime lastDeliveredAt;

    /** CDP数仓实时检查点最近投递来源 */
    private String lastDeliverySource;

    /** CDP数仓实时检查点送达数量 */
    private Long deliveredCount;

    /** CDP数仓实时检查点失败数量 */
    private Long failureCount;

    /** CDP数仓实时检查点最近失败时间 */
    private LocalDateTime lastFailureAt;

    /** CDP数仓实时检查点最近失败消息 */
    private String lastFailureMessage;

    /** CDP数仓实时检查点创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓实时检查点最后更新时间 */
    private LocalDateTime updatedAt;
}
