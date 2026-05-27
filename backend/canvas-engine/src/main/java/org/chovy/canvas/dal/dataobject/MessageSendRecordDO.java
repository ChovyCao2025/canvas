package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息发送记录 数据对象，对应数据库表 {@code message_send_record}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("message_send_record")
public class MessageSendRecordDO {
    /** 消息待发送状态 */
    public static final String STATUS_PENDING = "PENDING";

    /** 消息已发送状态 */
    public static final String STATUS_SENT = "SENT";

    /** 消息发送失败状态 */
    public static final String STATUS_FAILED = "FAILED";

    /** 消息因策略或条件跳过发送状态 */
    public static final String STATUS_SKIPPED = "SKIPPED";

    @TableId(type = IdType.AUTO)
    /** 消息发送记录主键 ID */
    private Long id;

    /** 所属画布执行 ID */
    private String executionId;

    /** 所属画布 ID */
    private Long canvasId;

    /** 触达用户 ID */
    private String userId;

    /** 触达节点 ID */
    private String nodeId;

    /** 触达渠道，如 SMS、EMAIL、PUSH、WECHAT、IN_APP */
    private String channel;

    /** 消息模板 ID */
    private String templateId;

    /** 幂等键，用于防止同一节点重复发送 */
    private String idempotencyKey;

    /** 发送请求参数 JSON，记录模板变量和渠道参数 */
    private String requestPayload;

    /** 发送状态，取值见 STATUS_* 常量 */
    private String status;

    /** 外部渠道返回的消息 ID */
    private String externalMessageId;

    /** 发送失败原因 */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
