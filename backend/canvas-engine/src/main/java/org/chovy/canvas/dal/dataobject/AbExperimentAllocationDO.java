package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AbExperimentAllocationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ab_experiment_allocation")
public class AbExperimentAllocationDO {

    /** A/B实验分流配置主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的实验 ID */
    private Long experimentId;

    /** 关联的流量层 ID */
    private Long layerId;

    /** A/B实验分流配置实验变体标识 */
    private String variantKey;

    /** A/B实验分流配置流量分配比例 */
    private BigDecimal allocationPct;

    /** A/B实验分流配置分桶起始值 */
    private Integer bucketStart;

    /** A/B实验分流配置分桶结束值 */
    private Integer bucketEnd;

    /** A/B实验分流配置当前状态 */
    private String status;

    /** A/B实验分流配置创建时间 */
    private LocalDateTime createdAt;

    /** A/B实验分流配置最后更新时间 */
    private LocalDateTime updatedAt;
}
