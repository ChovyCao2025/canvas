package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_input_response")
public class UserInputResponseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long formId;

    private Long canvasId;

    private Long versionId;

    private String executionId;

    private String nodeId;

    private String userId;

    private String responseJson;

    private String status;

    private String idempotencyKey;

    private String completedNodeId;

    private String timeoutNodeId;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
