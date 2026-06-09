package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingMonitorAlertDeliveryDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_alert_delivery")
public class MarketingMonitorAlertDeliveryDO {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
    public static final String DEAD = "DEAD";

    /** 营销监控告警投递主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的告警 ID */
    private Long alertId;

    /** 关联的渠道 ID */
    private Long channelId;

    /** 营销监控告警投递渠道业务键 */
    private String channelKey;

    /** 营销监控告警投递渠道类型 */
    private String channelType;

    /** 关联的投递 ID */
    private String deliveryId;

    /** 营销监控告警投递尝试 */
    private Integer attempt;

    /** 营销监控告警投递HTTP状态 */
    private Integer httpStatus;

    /** 营销监控告警投递请求载荷 */
    private String requestPayload;

    /** 营销监控告警投递响应正文 */
    private String responseBody;

    /** 营销监控告警投递当前状态 */
    private String status;

    /** 营销监控告警投递下次重试时间 */
    private LocalDateTime nextRetryAt;

    /** 营销监控告警投递错误信息 */
    private String errorMessage;

    /** 营销监控告警投递终端原因 */
    private String terminalReason;

    /** 营销监控告警投递创建人 */
    private String createdBy;

    /** 营销监控告警投递创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控告警投递最后更新时间 */
    private LocalDateTime updatedAt;
}
