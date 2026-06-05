package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dashboard_version")
public class BiDashboardVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private Long dashboardId;

    private String dashboardKey;

    private Integer version;

    private String status;

    private String presetJson;

    private String publishedBy;

    private LocalDateTime createdAt;
}
