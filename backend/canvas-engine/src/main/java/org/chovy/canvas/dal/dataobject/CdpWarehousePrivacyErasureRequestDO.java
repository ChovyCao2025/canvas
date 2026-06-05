package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_privacy_erasure_request")
public class CdpWarehousePrivacyErasureRequestDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String requestKey;

    private String subjectType;

    private String subjectHash;

    private String subjectRefMasked;

    private String reason;

    private String requestedBy;

    private String status;

    private LocalDateTime dueAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String targetAssetsJson;

    private String evidenceJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
