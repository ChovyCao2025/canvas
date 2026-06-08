package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiChartVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_chart_version")
public class BiChartVersionDO {

    /** BI图表版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的图表 ID */
    private Long chartId;

    /** BI图表版本图表业务键 */
    private String chartKey;

    /** BI图表版本版本号 */
    private Integer version;

    /** BI图表版本当前状态 */
    private String status;

    /** BI图表版本资源内容 JSON */
    private String resourceJson;

    /** BI图表版本发布人 */
    private String publishedBy;

    /** BI图表版本创建时间 */
    private LocalDateTime createdAt;
}
