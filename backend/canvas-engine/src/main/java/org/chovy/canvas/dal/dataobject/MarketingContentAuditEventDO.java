package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentAuditEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_audit_event")
public class MarketingContentAuditEventDO {
    /** 营销内容审计事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销内容审计事件事件类型 */
    private String eventType;
    /** 营销内容审计事件目标类型 */
    private String targetType;
    /** 营销内容审计事件目标业务键 */
    private String targetKey;
    /** 营销内容审计事件操作人 */
    private String actor;
    /** 营销内容审计事件原值明细 JSON */
    private String oldValueJson;
    /** 营销内容审计事件新值明细 JSON */
    private String newValueJson;
    /** 营销内容审计事件备注 */
    private String note;
    /** 营销内容审计事件创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
