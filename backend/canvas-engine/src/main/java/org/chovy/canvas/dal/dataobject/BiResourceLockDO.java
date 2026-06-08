package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiResourceLockDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_resource_lock")
public class BiResourceLockDO {

    /** BI资源锁主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI资源锁资源类型 */
    private String resourceType;
    /** BI资源锁资源业务键 */
    private String resourceKey;
    /** BI资源锁锁令牌 */
    private String lockToken;
    /** BI资源锁锁定人 */
    private String lockedBy;
    /** BI资源锁锁定时间 */
    private LocalDateTime lockedAt;
    /** BI资源锁过期时间 */
    private LocalDateTime expiresAt;
}
