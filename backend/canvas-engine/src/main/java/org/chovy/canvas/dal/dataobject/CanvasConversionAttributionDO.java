package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CanvasConversionAttributionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("canvas_conversion_attribution")
public class CanvasConversionAttributionDO {
    /** 画布转化归因主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的画布 ID */
    private Long canvasId;
    /** 关联的用户 ID */
    private String userId;
    /** 关联的事件日志 ID */
    private Long eventLogId;
    /** 关联的发送记录 ID */
    private Long sendRecordId;
    /** 画布转化归因转化事件编码 */
    private String conversionEventCode;
    /** 画布转化归因转化金额 */
    private BigDecimal conversionAmount;
    /** 画布转化归因归因模型 */
    private String attributionModel;
    /** 画布转化归因归因权重 */
    private BigDecimal attributionWeight;
    /** 画布转化归因触点创建时间 */
    private LocalDateTime touchCreatedAt;
    /** 画布转化归因归因时间 */
    private LocalDateTime attributedAt;
}
