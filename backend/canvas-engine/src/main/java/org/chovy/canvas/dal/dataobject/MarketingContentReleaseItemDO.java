package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_content_release_item")
public class MarketingContentReleaseItemDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long releaseId;
    private String itemType;
    private String itemKey;
    private String itemStatus;
    private String snapshotJson;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
