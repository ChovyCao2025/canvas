package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiPortalVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_portal_version")
public class BiPortalVersionDO {

    /** BI门户版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的门户 ID */
    private Long portalId;

    /** BI门户版本门户业务键 */
    private String portalKey;

    /** BI门户版本版本号 */
    private Integer version;

    /** BI门户版本当前状态 */
    private String status;

    /** BI门户版本资源内容 JSON */
    private String resourceJson;

    /** BI门户版本发布人 */
    private String publishedBy;

    /** BI门户版本创建时间 */
    private LocalDateTime createdAt;
}
