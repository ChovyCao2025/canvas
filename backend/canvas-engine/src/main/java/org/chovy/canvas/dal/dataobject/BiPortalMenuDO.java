package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_portal_menu")
public class BiPortalMenuDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long portalId;

    private String menuKey;

    private String parentMenuKey;

    private String title;

    private String resourceType;

    private Long resourceId;

    private String externalUrl;

    private String visibilityJson;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
