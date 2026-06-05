package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("event_attr_definition")
public class EventAttrDefinitionDO {
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String eventCode;
    private String attrName;
    private String attrType;
    private String status;
    private String sampleValue;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
