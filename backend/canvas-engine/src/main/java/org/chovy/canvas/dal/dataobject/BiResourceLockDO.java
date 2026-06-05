package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_resource_lock")
public class BiResourceLockDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long workspaceId;
    private String resourceType;
    private String resourceKey;
    private String lockToken;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime expiresAt;
}
