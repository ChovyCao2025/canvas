package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_portal_version")
public class BiPortalVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workspaceId;

    private Long portalId;

    private String portalKey;

    private Integer version;

    private String status;

    private String resourceJson;

    private String publishedBy;

    private LocalDateTime createdAt;
}
