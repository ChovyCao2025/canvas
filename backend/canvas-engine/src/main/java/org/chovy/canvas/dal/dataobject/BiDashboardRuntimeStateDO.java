package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDashboardRuntimeStateDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dashboard_runtime_state")
public class BiDashboardRuntimeStateDO {

    /** BI仪表板运行时状态主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI仪表板运行时状态仪表板业务键 */
    private String dashboardKey;
    /** BI仪表板运行时状态用户名 */
    private String username;
    /** BI仪表板运行时状态运行参数 JSON */
    private String parameterJson;
    /** BI仪表板运行时状态创建时间 */
    private LocalDateTime createdAt;
    /** BI仪表板运行时状态最后更新时间 */
    private LocalDateTime updatedAt;
}
