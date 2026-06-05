package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dashboard")
public class BiDashboardDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String dashboardKey;

    private String name;

    private String description;

    private String themeJson;

    private String filterJson;

    private String status;

    private Integer version;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
