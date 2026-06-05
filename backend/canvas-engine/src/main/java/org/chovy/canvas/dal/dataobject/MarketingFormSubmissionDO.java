package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_form_submission")
public class MarketingFormSubmissionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    private Long formId;

    private String publicKey;

    private String userId;

    private String anonymousId;

    private String responseJson;

    private String utmJson;

    private String consentChannel;

    private String consentStatus;

    private String idempotencyKey;

    private String userAgent;

    private String submitIpHash;

    private String triggerEventCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
