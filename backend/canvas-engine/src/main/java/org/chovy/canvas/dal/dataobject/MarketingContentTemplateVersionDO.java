package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_content_template_version")
public class MarketingContentTemplateVersionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String templateKey;
    private Integer versionNo;
    private String displayName;
    private String channel;
    private String subject;
    private String body;
    private String designJson;
    private String assetRefsJson;
    private String variablesJson;
    private String status;
    private String reviewNotes;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public String getChangeNote() {
        return reviewNotes;
    }

    public void setChangeNote(String changeNote) {
        this.reviewNotes = changeNote;
    }
}
