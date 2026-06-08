package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiPortalMenuDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_portal_menu")
public class BiPortalMenuDO {

    /** BI门户菜单主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的门户 ID */
    private Long portalId;

    /** BI门户菜单菜单业务键 */
    private String menuKey;

    /** BI门户菜单父级菜单业务键 */
    private String parentMenuKey;

    /** BI门户菜单标题 */
    private String title;

    /** BI门户菜单资源类型 */
    private String resourceType;

    /** BI门户菜单资源 ID */
    private Long resourceId;

    /** BI门户菜单外部URL */
    private String externalUrl;

    /** BI门户菜单可见性配置 JSON */
    private String visibilityJson;

    /** BI门户菜单排序序号 */
    private Integer sortOrder;

    /** BI门户菜单创建时间 */
    private LocalDateTime createdAt;
}
