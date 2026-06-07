package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_workspace_member")
public class BiWorkspaceMemberDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private String userId;

    private String roleKey;

    private LocalDateTime createdAt;
}
