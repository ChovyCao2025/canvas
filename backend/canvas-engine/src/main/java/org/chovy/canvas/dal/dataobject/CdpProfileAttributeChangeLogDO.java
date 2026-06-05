package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_profile_attribute_change_log")
public class CdpProfileAttributeChangeLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String attrCode;
    private String userId;
    private String oldValue;
    private String newValue;
    private Long sourceRunId;
    private LocalDateTime changedAt;
}
