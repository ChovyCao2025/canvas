package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiWorkspaceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_workspace")
public class BiWorkspaceDO {

    /** BI工作空间主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** BI工作空间工作空间业务键 */
    private String workspaceKey;

    /** BI工作空间名称 */
    private String name;

    /** BI工作空间说明 */
    private String description;

    /** BI工作空间当前状态 */
    private String status;

    /** BI工作空间创建人 */
    private String createdBy;

    /** BI工作空间创建时间 */
    private LocalDateTime createdAt;

    /** BI工作空间最后更新时间 */
    private LocalDateTime updatedAt;
}
