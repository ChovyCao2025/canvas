package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDashboardWidgetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dashboard_widget")
public class BiDashboardWidgetDO {

    /** BI仪表板组件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的仪表板 ID */
    private Long dashboardId;

    /** BI仪表板组件组件业务键 */
    private String widgetKey;

    /** 关联的图表 ID */
    private Long chartId;

    /** BI仪表板组件组件类型 */
    private String widgetType;

    /** BI仪表板组件标题 */
    private String title;

    /** BI仪表板组件布局明细 JSON */
    private String layoutJson;

    /** BI仪表板组件查询覆盖配置 JSON */
    private String queryOverrideJson;

    /** BI仪表板组件交互明细 JSON */
    private String interactionJson;

    /** BI仪表板组件创建时间 */
    private LocalDateTime createdAt;

    /** BI仪表板组件最后更新时间 */
    private LocalDateTime updatedAt;
}
