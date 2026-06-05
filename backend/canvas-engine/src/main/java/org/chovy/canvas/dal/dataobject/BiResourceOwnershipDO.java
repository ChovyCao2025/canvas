package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_resource_ownership")
public class BiResourceOwnershipDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long workspaceId;
    private String resourceType;
    private String resourceKey;
    private String ownerUser;
    private String transferredBy;
    private LocalDateTime transferredAt;
}
