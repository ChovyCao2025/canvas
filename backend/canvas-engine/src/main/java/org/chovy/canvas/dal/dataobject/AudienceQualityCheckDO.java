package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_quality_check")
public class AudienceQualityCheckDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long audienceId;

    private Long mysqlCount;

    private Long dorisCount;

    private Long bitmapCount;

    private Long freshnessLagMinutes;

    private Double bitmapDriftRatio;

    private String verdict;

    private String detailJson;

    private LocalDateTime checkedAt;
}
