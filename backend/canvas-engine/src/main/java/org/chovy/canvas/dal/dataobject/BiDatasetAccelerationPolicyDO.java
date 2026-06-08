package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasetAccelerationPolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dataset_acceleration_policy")
public class BiDatasetAccelerationPolicyDO {

    /** BI数据集加速策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** BI数据集加速策略数据集业务键 */
    private String datasetKey;
    /** BI数据集加速策略是否启用 */
    private Boolean enabled;
    /** BI数据集加速策略加速模式 */
    private String accelerationMode;
    /** BI数据集加速策略刷新模式 */
    private String refreshMode;
    /** BI数据集加速策略刷新间隔分钟 */
    private Long refreshIntervalMinutes;
    /** BI数据集加速策略缓存 TTL 秒数 */
    private Long ttlSeconds;
    /** BI数据集加速策略最大行数 */
    private Long maxRows;
    /** BI数据集加速策略Cron表达式 */
    private String cronExpression;
    /** BI数据集加速策略物化表 */
    private String materializedTable;
    /** BI数据集加速策略最近状态 */
    private String lastStatus;
    /** 关联的最近运行 ID */
    private Long lastRunId;
    /** BI数据集加速策略最近刷新时间 */
    private LocalDateTime lastRefreshedAt;
    /** BI数据集加速策略最后更新人 */
    private String updatedBy;
    /** BI数据集加速策略创建时间 */
    private LocalDateTime createdAt;
    /** BI数据集加速策略最后更新时间 */
    private LocalDateTime updatedAt;
}
