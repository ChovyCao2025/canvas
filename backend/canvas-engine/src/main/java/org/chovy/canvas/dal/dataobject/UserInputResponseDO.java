package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserInputResponseDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("user_input_response")
public class UserInputResponseDO {

    /** 用户输入响应主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的表单 ID */
    private Long formId;

    /** 关联的画布 ID */
    private Long canvasId;

    /** 关联的版本 ID */
    private Long versionId;

    /** 关联的执行 ID */
    private String executionId;

    /** 关联的节点 ID */
    private String nodeId;

    /** 关联的用户 ID */
    private String userId;

    /** 用户输入响应响应内容 JSON */
    private String responseJson;

    /** 用户输入响应当前状态 */
    private String status;

    /** 用户输入响应幂等键 */
    private String idempotencyKey;

    /** 关联的完成节点 ID */
    private String completedNodeId;

    /** 关联的超时节点 ID */
    private String timeoutNodeId;

    /** 用户输入响应过期时间 */
    private LocalDateTime expiresAt;

    /** 用户输入响应创建时间 */
    private LocalDateTime createdAt;

    /** 用户输入响应最后更新时间 */
    private LocalDateTime updatedAt;
}
