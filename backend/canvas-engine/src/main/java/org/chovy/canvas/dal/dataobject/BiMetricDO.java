package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiMetricDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_metric")
public class BiMetricDO {

    /** BI指标主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI指标指标标识 */
    private String metricKey;

    /** BI指标展示名称 */
    private String displayName;

    /** BI指标表达式 */
    private String expression;

    /** BI指标聚合 */
    private String aggregation;

    /** BI指标数据类型 */
    private String dataType;

    /** BI指标单位 */
    private String unit;

    /** BI指标格式模式 */
    private String formatPattern;

    /** BI指标允许维度 JSON */
    private String allowedDimensionsJson;

    /** BI指标负责人 */
    private String owner;

    /** BI指标说明 */
    private String description;

    /** BI指标当前状态 */
    private String status;

    /** BI指标创建时间 */
    private LocalDateTime createdAt;

    /** BI指标最后更新时间 */
    private LocalDateTime updatedAt;
}
