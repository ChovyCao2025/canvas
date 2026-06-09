package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserInputFormDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("user_input_form")
public class UserInputFormDO {

    /** 用户输入表单主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

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

    /** 用户输入表单结构定义 JSON */
    private String schemaJson;

    /** 关联的完成节点 ID */
    private String completedNodeId;

    /** 关联的超时节点 ID */
    private String timeoutNodeId;

    /** 用户输入表单过期时间 */
    private LocalDateTime expiresAt;

    /** 用户输入表单创建时间 */
    private LocalDateTime createdAt;

    /** 用户输入表单最后更新时间 */
    private LocalDateTime updatedAt;
}
