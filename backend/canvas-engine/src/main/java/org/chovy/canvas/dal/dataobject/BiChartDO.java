package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiChartDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_chart")
public class BiChartDO {

    /** BI图表主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI图表图表业务键 */
    private String chartKey;

    /** BI图表名称 */
    private String name;

    /** BI图表图表类型 */
    private String chartType;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI图表查询条件 JSON */
    private String queryJson;

    /** BI图表样式明细 JSON */
    private String styleJson;

    /** BI图表交互明细 JSON */
    private String interactionJson;

    /** BI图表当前状态 */
    private String status;

    /** BI图表创建人 */
    private String createdBy;

    /** BI图表创建时间 */
    private LocalDateTime createdAt;

    /** BI图表最后更新时间 */
    private LocalDateTime updatedAt;
}
