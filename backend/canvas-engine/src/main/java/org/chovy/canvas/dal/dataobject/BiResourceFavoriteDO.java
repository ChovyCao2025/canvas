package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiResourceFavoriteDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_resource_favorite")
public class BiResourceFavoriteDO {

    /** BI资源收藏主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI资源收藏资源类型 */
    private String resourceType;
    /** BI资源收藏资源业务键 */
    private String resourceKey;
    /** BI资源收藏用户名 */
    private String username;
    /** BI资源收藏创建时间 */
    private LocalDateTime createdAt;
}
