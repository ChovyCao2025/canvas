package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationId;
    private String userId;
    private String type;
    private String category;
    private String severity;
    private String status;
    private String title;
    private String content;
    private String targetUrl;
    private String actionLabel;
    private String actionUrl;
    private String taskId;
    private String bizType;
    private String bizId;
    private String dedupKey;
    private String payloadJson;
    private LocalDateTime readAt;
    private LocalDateTime archivedAt;
    private LocalDateTime deliveredAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
