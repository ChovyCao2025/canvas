package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingFormSubmissionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_form_submission")
public class MarketingFormSubmissionDO {

    /** 营销表单提交主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 关联的表单 ID */
    private Long formId;

    /** 营销表单提交公开业务键 */
    private String publicKey;

    /** 关联的用户 ID */
    private String userId;

    /** 关联的匿名 ID */
    private String anonymousId;

    /** 营销表单提交响应内容 JSON */
    private String responseJson;

    /** 营销表单提交UTM 参数 JSON */
    private String utmJson;

    /** 营销表单提交同意渠道 */
    private String consentChannel;

    /** 营销表单提交同意状态 */
    private String consentStatus;

    /** 营销表单提交幂等键 */
    private String idempotencyKey;

    /** 营销表单提交用户代理 */
    private String userAgent;

    /** 营销表单提交提交 IP 哈希 */
    private String submitIpHash;

    /** 营销表单提交TRIGGEREVENTCODE */
    private String triggerEventCode;

    /** 营销表单提交创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
