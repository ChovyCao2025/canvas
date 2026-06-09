package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpAudienceSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_audience_snapshot")
public class CdpAudienceSnapshotDO {
    /** CDP人群快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的人群 ID */
    private Long audienceId;
    /** CDP人群快照预估大小 */
    private Long estimatedSize;
    /** CDP人群快照位图业务键 */
    private String bitmapKey;
    /** CDP人群快照快照来源 */
    private String snapshotSource;
    /** CDP人群快照创建人 */
    private String createdBy;
    /** CDP人群快照创建时间 */
    private LocalDateTime createdAt;
}
