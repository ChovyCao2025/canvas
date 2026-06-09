package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * EventAttrDefinitionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("event_attr_definition")
public class EventAttrDefinitionDO {
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    /** 事件属性定义主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 事件属性定义事件编码 */
    private String eventCode;
    /** 事件属性定义属性名称 */
    private String attrName;
    /** 事件属性定义属性类型 */
    private String attrType;
    /** 事件属性定义当前状态 */
    private String status;
    /** 事件属性定义样本值 */
    private String sampleValue;
    /** 事件属性定义首次可见时间 */
    private LocalDateTime firstSeenAt;
    /** 事件属性定义最近可见时间 */
    private LocalDateTime lastSeenAt;
    /** 事件属性定义批准人 */
    private String approvedBy;
    /** 事件属性定义批准时间 */
    private LocalDateTime approvedAt;
    /** 事件属性定义创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 事件属性定义最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
