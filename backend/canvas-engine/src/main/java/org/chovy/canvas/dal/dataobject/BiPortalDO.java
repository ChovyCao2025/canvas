package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiPortalDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_portal")
public class BiPortalDO {

    /** BI门户主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI门户门户业务键 */
    private String portalKey;

    /** BI门户名称 */
    private String name;

    /** BI门户主题明细 JSON */
    private String themeJson;

    /** BI门户当前状态 */
    private String status;

    /** BI门户创建人 */
    private String createdBy;

    /** BI门户创建时间 */
    private LocalDateTime createdAt;

    /** BI门户最后更新时间 */
    private LocalDateTime updatedAt;
}
