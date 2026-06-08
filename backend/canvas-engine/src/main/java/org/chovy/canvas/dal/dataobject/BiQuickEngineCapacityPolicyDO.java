package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiQuickEngineCapacityPolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_quick_engine_capacity_policy")
public class BiQuickEngineCapacityPolicyDO {

    /** BI快速引擎容量策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** BI快速引擎容量策略是否启用 */
    private Boolean enabled;
    /** BI快速引擎容量策略容量限制行数 */
    private Long capacityLimitRows;
    /** BI快速引擎容量策略预警阈值百分比 */
    private Integer warningThresholdPercent;
    /** BI快速引擎容量策略严重阈值百分比 */
    private Integer criticalThresholdPercent;
    /** BI快速引擎容量策略通知渠道列表 */
    private String notificationChannels;
    /** BI快速引擎容量策略通知接收人列表 */
    private String notificationReceivers;
    /** BI快速引擎容量策略池业务键 */
    private String poolKey;
    /** BI快速引擎容量策略最大并发查询数 */
    private Integer maxConcurrentQueries;
    /** BI快速引擎容量策略队列限制 */
    private Integer queueLimit;
    /** BI快速引擎容量策略队列超时秒数 */
    private Integer queueTimeoutSeconds;
    /** BI快速引擎容量策略池权重 */
    private Integer poolWeight;
    /** BI快速引擎容量策略最后更新人 */
    private String updatedBy;
    /** BI快速引擎容量策略创建时间 */
    private LocalDateTime createdAt;
    /** BI快速引擎容量策略最后更新时间 */
    private LocalDateTime updatedAt;
}
