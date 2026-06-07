package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dashboard_runtime_state")
public class BiDashboardRuntimeStateDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long workspaceId;
    private String dashboardKey;
    private String username;
    private String parameterJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
