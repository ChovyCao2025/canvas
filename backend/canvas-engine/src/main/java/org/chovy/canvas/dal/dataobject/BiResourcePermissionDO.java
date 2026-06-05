package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_resource_permission")
public class BiResourcePermissionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String resourceType;

    private Long resourceId;

    private String subjectType;

    private String subjectId;

    private String actionKey;

    private String effect;

    private String createdBy;

    private LocalDateTime createdAt;
}
