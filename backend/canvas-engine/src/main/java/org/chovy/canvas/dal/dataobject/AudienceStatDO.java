package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人群计算状态（audience_stat）。
 */
@Data
@TableName("audience_stat")
public class AudienceStatDO {

    /** 关联的人群 ID（主键）。 */
    @TableId
    private Long audienceId;

    /** 预估人群规模（用户数）。 */
    private Long estimatedSize;

    /** Bitmap 占用大小（KB）。 */
    private Integer bitmapSizeKb;

    /** 最近一次计算完成时间。 */
    private LocalDateTime computedAt;

    /** 计算状态：PENDING | COMPUTING | READY | FAILED。 */
    private String status;

    /** 失败原因（status=FAILED 时有值）。 */
    private String errorMsg;
}
