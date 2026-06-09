package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * MarketingMonitorSourceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_source")
public class MarketingMonitorSourceDO {

    /** 营销监控来源主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控来源来源业务键 */
    private String sourceKey;

    /** 营销监控来源来源类型 */
    private String sourceType;

    /** 营销监控来源展示名称 */
    private String displayName;

    /** 营销监控来源是否启用 */
    private Integer enabled;

    /** 营销监控来源扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控来源Webhook启用状态 */
    private Integer webhookEnabled;

    /** 营销监控来源Webhook密钥前缀 */
    private String webhookSecretPrefix;

    /** 营销监控来源Webhook密钥哈希 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String webhookSecretHash;

    /** 营销监控来源Webhook密钥密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String webhookSecretCiphertext;

    /** 营销监控来源Webhook签名容忍秒数 */
    private Integer webhookSignatureToleranceSeconds;

    /** 营销监控来源轮询启用状态 */
    private Integer pollEnabled;

    /** 营销监控来源轮询间隔分钟 */
    private Integer pollIntervalMinutes;

    /** 营销监控来源轮询游标 */
    private String pollCursor;

    /** 营销监控来源最近轮询时间 */
    private LocalDateTime lastPolledAt;

    /** 营销监控来源下次轮询时间 */
    private LocalDateTime nextPollAt;

    /** 营销监控来源最近轮询状态 */
    private String lastPollStatus;

    /** 营销监控来源创建人 */
    private String createdBy;

    /** 营销监控来源创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控来源最后更新时间 */
    private LocalDateTime updatedAt;
}
