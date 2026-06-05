package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_input_form")
public class UserInputFormDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long canvasId;

    private Long versionId;

    private String executionId;

    private String nodeId;

    private String userId;

    private String schemaJson;

    private String completedNodeId;

    private String timeoutNodeId;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
