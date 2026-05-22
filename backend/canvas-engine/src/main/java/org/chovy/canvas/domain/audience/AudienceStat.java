package org.chovy.canvas.domain.audience;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_stat")
public class AudienceStat {

    @TableId
    private Long audienceId;

    private Long estimatedSize;
    private Integer bitmapSizeKb;
    private LocalDateTime computedAt;
    private String status;
    private String errorMsg;
}
