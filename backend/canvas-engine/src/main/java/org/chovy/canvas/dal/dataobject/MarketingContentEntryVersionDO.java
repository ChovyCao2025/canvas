package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_content_entry_version")
public class MarketingContentEntryVersionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String entryKey;
    private Integer versionNo;
    private String contentType;
    private String title;
    private String slug;
    private String locale;
    private String summary;
    private String bodyJson;
    private String seoJson;
    private String assetRefsJson;
    private String status;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
