package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiResourceCommentDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_resource_comment")
public class BiResourceCommentDO {

    /** BI资源备注主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI资源备注资源类型 */
    private String resourceType;
    /** BI资源备注资源业务键 */
    private String resourceKey;
    /** BI资源备注组件业务键 */
    private String widgetKey;
    /** BI资源备注备注文本 */
    private String commentText;
    /** BI资源备注创建人 */
    private String createdBy;
    /** BI资源备注创建时间 */
    private LocalDateTime createdAt;
    /** BI资源备注删除时间 */
    private LocalDateTime deletedAt;
}
