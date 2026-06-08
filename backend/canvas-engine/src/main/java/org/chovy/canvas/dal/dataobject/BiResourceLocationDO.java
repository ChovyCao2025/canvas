package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiResourceLocationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_resource_location")
public class BiResourceLocationDO {

    /** BI资源位置主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI资源位置资源类型 */
    private String resourceType;
    /** BI资源位置资源业务键 */
    private String resourceKey;
    /** BI资源位置文件夹业务键 */
    private String folderKey;
    /** BI资源位置排序序号 */
    private Integer sortOrder;
    /** BI资源位置移动人 */
    private String movedBy;
    /** BI资源位置移动时间 */
    private LocalDateTime movedAt;
}
