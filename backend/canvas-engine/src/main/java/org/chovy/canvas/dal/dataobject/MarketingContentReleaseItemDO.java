package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentReleaseItemDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_release_item")
public class MarketingContentReleaseItemDO {
    /** 营销内容发布事项主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的发布 ID */
    private Long releaseId;
    /** 营销内容发布事项事项类型 */
    private String itemType;
    /** 营销内容发布事项事项业务键 */
    private String itemKey;
    /** 营销内容发布事项状态 */
    private String itemStatus;
    /** 营销内容发布事项快照明细 JSON */
    private String snapshotJson;
    /** 营销内容发布事项创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
