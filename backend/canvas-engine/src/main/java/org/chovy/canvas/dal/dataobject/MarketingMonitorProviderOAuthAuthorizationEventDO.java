package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingMonitorProviderOAuthAuthorizationEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_provider_oauth_authorization_event")
public class MarketingMonitorProviderOAuthAuthorizationEventDO {

    /** 营销监控服务商OAuth授权事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的授权 ID */
    private Long authorizationId;

    /** 营销监控服务商OAuth授权事件认证状态 */
    private String authState;

    /** 营销监控服务商OAuth授权事件凭据业务键 */
    private String credentialKey;

    /** 营销监控服务商OAuth授权事件事件类型 */
    private String eventType;

    /** 营销监控服务商OAuth授权事件当前状态 */
    private String status;

    /** 营销监控服务商OAuth授权事件扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控服务商OAuth授权事件错误信息 */
    private String errorMessage;

    /** 营销监控服务商OAuth授权事件创建人 */
    private String createdBy;

    /** 营销监控服务商OAuth授权事件创建时间 */
    private LocalDateTime createdAt;
}
