package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDashboardVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dashboard_version")
public class BiDashboardVersionDO {

    /** BI仪表板版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的仪表板 ID */
    private Long dashboardId;

    /** BI仪表板版本仪表板业务键 */
    private String dashboardKey;

    /** BI仪表板版本版本号 */
    private Integer version;

    /** BI仪表板版本当前状态 */
    private String status;

    /** BI仪表板版本预设配置 JSON */
    private String presetJson;

    /** BI仪表板版本发布人 */
    private String publishedBy;

    /** BI仪表板版本创建时间 */
    private LocalDateTime createdAt;
}
