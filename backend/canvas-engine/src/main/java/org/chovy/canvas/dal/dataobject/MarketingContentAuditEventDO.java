package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_content_audit_event")
public class MarketingContentAuditEventDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String eventType;
    private String targetType;
    private String targetKey;
    private String actor;
    private String oldValueJson;
    private String newValueJson;
    private String note;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
