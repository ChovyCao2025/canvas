package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息 数据对象，对应数据库表 {@code notification}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("notification")
public class NotificationDO {
    @TableId(type = IdType.AUTO)
    /** 通知表自增主键 ID */
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 对外通知 ID，用于前端、WebSocket 和接口查询 */
    private String notificationId;

    /** 接收通知的用户 ID */
    private String userId;

    /** 通知类型，如 TASK、SYSTEM、CANVAS */
    private String type;

    /** 通知分类，用于消息中心分组展示 */
    private String category;

    /** 通知严重级别，如 INFO、WARN、ERROR */
    private String severity;

    /** 通知状态，如 UNREAD、READ、ARCHIVED */
    private String status;

    /** 通知标题 */
    private String title;

    /** 通知正文内容 */
    private String content;

    /** 点击通知时跳转的目标页面地址 */
    private String targetUrl;

    /** 操作按钮展示文案 */
    private String actionLabel;

    /** 操作按钮跳转地址 */
    private String actionUrl;

    /** 关联异步任务 ID，非任务通知为空 */
    private String taskId;

    /** 关联业务类型，如 AUDIENCE、CANVAS、TAG_IMPORT */
    private String bizType;

    /** 关联业务对象 ID */
    private String bizId;

    /** 通知去重键，用于防止同一业务事件重复生成通知 */
    private String dedupKey;

    /** 通知扩展载荷 JSON */
    private String payloadJson;

    /** 通知已读时间，未读时为空 */
    private LocalDateTime readAt;

    /** 通知归档时间，未归档时为空 */
    private LocalDateTime archivedAt;

    /** 通知推送到实时通道的时间，未推送时为空 */
    private LocalDateTime deliveredAt;

    @TableField(fill = FieldFill.INSERT)
    /** 通知创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;
}
