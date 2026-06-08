package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dataset")
public class BiDatasetDO {

    /** BI数据集主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI数据集数据集业务键 */
    private String datasetKey;

    /** BI数据集名称 */
    private String name;

    /** BI数据集数据集类型 */
    private String datasetType;

    /** 关联的来源引用 ID */
    private Long sourceRefId;

    /** BI数据集表表达式 */
    private String tableExpression;

    /** BI数据集租户列 */
    private String tenantColumn;

    /** BI数据集模型明细 JSON */
    private String modelJson;

    /** BI数据集当前状态 */
    private String status;

    /** BI数据集创建人 */
    private String createdBy;

    /** BI数据集创建时间 */
    private LocalDateTime createdAt;

    /** BI数据集最后更新时间 */
    private LocalDateTime updatedAt;
}
