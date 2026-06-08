package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiResourceOwnershipDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_resource_ownership")
public class BiResourceOwnershipDO {

    /** BI资源归属主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI资源归属资源类型 */
    private String resourceType;
    /** BI资源归属资源业务键 */
    private String resourceKey;
    /** BI资源归属负责人用户 */
    private String ownerUser;
    /** BI资源归属转移人 */
    private String transferredBy;
    /** BI资源归属转移时间 */
    private LocalDateTime transferredAt;
}
