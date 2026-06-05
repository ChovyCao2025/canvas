package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_audience_snapshot")
public class CdpAudienceSnapshotDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long audienceId;
    private Long estimatedSize;
    private String bitmapKey;
    private String snapshotSource;
    private String createdBy;
    private LocalDateTime createdAt;
}
