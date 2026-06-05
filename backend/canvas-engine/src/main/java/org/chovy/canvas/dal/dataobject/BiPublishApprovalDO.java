package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_publish_approval")
public class BiPublishApprovalDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long workspaceId;
    private String resourceType;
    private String resourceKey;
    private String status;
    private String reason;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
}
