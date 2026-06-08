package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiPublishApprovalDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_publish_approval")
public class BiPublishApprovalDO {

    /** BI发布审批主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的工作空间 ID */
    private Long workspaceId;
    /** BI发布审批资源类型 */
    private String resourceType;
    /** BI发布审批资源业务键 */
    private String resourceKey;
    /** BI发布审批当前状态 */
    private String status;
    /** BI发布审批原因说明 */
    private String reason;
    /** BI发布审批请求人 */
    private String requestedBy;
    /** BI发布审批请求时间 */
    private LocalDateTime requestedAt;
    /** BI发布审批审核人 */
    private String reviewedBy;
    /** BI发布审批审核时间 */
    private LocalDateTime reviewedAt;
    /** BI发布审批评审备注 */
    private String reviewComment;
}
