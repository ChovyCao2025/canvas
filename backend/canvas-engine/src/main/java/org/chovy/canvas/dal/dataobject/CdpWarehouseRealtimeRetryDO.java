package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseRealtimeRetryDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_realtime_retry")
public class CdpWarehouseRealtimeRetryDO {

    /** CDP数仓实时重试主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的事件日志 ID */
    private Long eventLogId;

    /** 关联的消息 ID */
    private String messageId;

    /** CDP数仓实时重试事件编码 */
    private String eventCode;

    /** CDP数仓实时重试当前状态 */
    private String status;

    /** CDP数仓实时重试尝试数量 */
    private Integer attemptCount;

    /** CDP数仓实时重试首次错误 */
    private String firstError;

    /** CDP数仓实时重试最近错误 */
    private String lastError;

    /** CDP数仓实时重试下次重试时间 */
    private LocalDateTime nextRetryAt;

    /** CDP数仓实时重试锁定人 */
    private String lockedBy;

    /** CDP数仓实时重试锁定时间 */
    private LocalDateTime lockedAt;

    /** CDP数仓实时重试创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓实时重试最后更新时间 */
    private LocalDateTime updatedAt;

    /** CDP数仓实时重试结束时间 */
    private LocalDateTime finishedAt;
}
