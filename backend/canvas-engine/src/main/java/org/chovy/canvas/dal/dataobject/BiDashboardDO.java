package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDashboardDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dashboard")
public class BiDashboardDO {

    /** BI仪表板主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI仪表板仪表板业务键 */
    private String dashboardKey;

    /** BI仪表板名称 */
    private String name;

    /** BI仪表板说明 */
    private String description;

    /** BI仪表板主题明细 JSON */
    private String themeJson;

    /** BI仪表板筛选明细 JSON */
    private String filterJson;

    /** BI仪表板当前状态 */
    private String status;

    /** BI仪表板版本号 */
    private Integer version;

    /** BI仪表板创建人 */
    private String createdBy;

    /** BI仪表板创建时间 */
    private LocalDateTime createdAt;

    /** BI仪表板最后更新时间 */
    private LocalDateTime updatedAt;
}
