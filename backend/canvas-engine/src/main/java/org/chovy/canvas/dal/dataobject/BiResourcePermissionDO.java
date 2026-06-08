package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiResourcePermissionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_resource_permission")
public class BiResourcePermissionDO {

    /** BI资源权限主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI资源权限资源类型 */
    private String resourceType;

    /** BI资源权限资源 ID */
    private Long resourceId;

    /** BI资源权限主体类型 */
    private String subjectType;

    /** 关联的主体 ID */
    private String subjectId;

    /** BI资源权限动作业务键 */
    private String actionKey;

    /** BI资源权限效果 */
    private String effect;

    /** BI资源权限创建人 */
    private String createdBy;

    /** BI资源权限创建时间 */
    private LocalDateTime createdAt;
}
