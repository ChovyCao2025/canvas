package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasetFieldDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dataset_field")
public class BiDatasetFieldDO {

    /** BI数据集字段主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI数据集字段字段业务键 */
    private String fieldKey;

    /** BI数据集字段展示名称 */
    private String displayName;

    /** BI数据集字段列表达式 */
    private String columnExpression;

    /** BI数据集字段角色业务键 */
    private String roleKey;

    /** BI数据集字段数据类型 */
    private String dataType;

    /** BI数据集字段语义类型 */
    private String semanticType;

    /** BI数据集字段默认聚合 */
    private String defaultAggregation;

    /** BI数据集字段格式模式 */
    private String formatPattern;

    /** BI数据集字段单位 */
    private String unit;

    /** BI数据集字段是否可见 */
    private Boolean visible;

    /** BI数据集字段敏感级别 */
    private String sensitiveLevel;

    /** BI数据集字段排序序号 */
    private Integer sortOrder;

    /** BI数据集字段创建时间 */
    private LocalDateTime createdAt;

    /** BI数据集字段最后更新时间 */
    private LocalDateTime updatedAt;
}
