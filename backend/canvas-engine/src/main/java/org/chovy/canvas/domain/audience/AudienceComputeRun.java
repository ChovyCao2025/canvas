package org.chovy.canvas.domain.audience;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audience_compute_run")
public class AudienceComputeRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long audienceId;
    private String perfRunId;
    private String perfInputId;
    private String status;
    private Long estimatedSize;
    private Integer bitmapSizeKb;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
