package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dashboard_widget")
public class BiDashboardWidgetDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long dashboardId;

    private String widgetKey;

    private Long chartId;

    private String widgetType;

    private String title;

    private String layoutJson;

    private String queryOverrideJson;

    private String interactionJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
