package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AudienceQualityCheckDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("audience_quality_check")
public class AudienceQualityCheckDO {

    /** 人群质量检查主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的人群 ID */
    private Long audienceId;

    /** 人群质量检查MYSQL数量 */
    private Long mysqlCount;

    /** 人群质量检查Doris 侧人群数量 */
    private Long dorisCount;

    /** 人群质量检查位图数量 */
    private Long bitmapCount;

    /** 人群质量检查新鲜度延迟分钟 */
    private Long freshnessLagMinutes;

    /** 人群质量检查位图漂移比例 */
    private Double bitmapDriftRatio;

    /** 人群质量检查质量检查判定结果 */
    private String verdict;

    /** 人群质量检查明细 JSON */
    private String detailJson;

    /** 人群质量检查检查时间 */
    private LocalDateTime checkedAt;
}
