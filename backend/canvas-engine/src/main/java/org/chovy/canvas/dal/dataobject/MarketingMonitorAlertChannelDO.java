package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * MarketingMonitorAlertChannelDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_alert_channel")
public class MarketingMonitorAlertChannelDO {

    /** 营销监控告警渠道主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控告警渠道渠道业务键 */
    private String channelKey;

    /** 营销监控告警渠道渠道类型 */
    private String channelType;

    /** 营销监控告警渠道展示名称 */
    private String displayName;

    /** 营销监控告警渠道端点URL */
    private String endpointUrl;

    /** 营销监控告警渠道是否启用 */
    private Integer enabled;

    /** 营销监控告警渠道最小严重级别 */
    private String minSeverity;

    /** 营销监控告警渠道告警类型明细 JSON */
    private String alertTypesJson;

    /** 营销监控告警渠道签名模式 */
    private String signingMode;

    /** 营销监控告警渠道密钥前缀 */
    private String secretPrefix;

    /** 营销监控告警渠道密钥哈希 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String secretHash;

    /** 营销监控告警渠道密钥密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String secretCiphertext;

    /** 营销监控告警渠道扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控告警渠道最大尝试 */
    private Integer maxAttempts;

    /** 营销监控告警渠道创建人 */
    private String createdBy;

    /** 营销监控告警渠道创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控告警渠道最后更新时间 */
    private LocalDateTime updatedAt;
}
