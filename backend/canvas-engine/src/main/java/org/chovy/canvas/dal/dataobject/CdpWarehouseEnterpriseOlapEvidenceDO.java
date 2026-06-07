package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_enterprise_olap_evidence")
public class CdpWarehouseEnterpriseOlapEvidenceDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String evidenceKey;
    private String source;
    private String status;
    private String reason;
    private LocalDateTime measuredAt;
    private LocalDateTime expiresAt;
    private String evidenceJson;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
