package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("canvas_conversion_attribution")
public class CanvasConversionAttributionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long canvasId;
    private String userId;
    private Long eventLogId;
    private Long sendRecordId;
    private String conversionEventCode;
    private BigDecimal conversionAmount;
    private String attributionModel;
    private LocalDateTime attributedAt;
}
