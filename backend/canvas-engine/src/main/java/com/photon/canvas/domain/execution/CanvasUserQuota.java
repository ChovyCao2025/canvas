package com.photon.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("canvas_user_quota")
public class CanvasUserQuota {

    private Long      canvasId;
    private String    userId;
    private LocalDate triggerDate;
    private Integer   dailyCount;
    private Integer   totalCount;
    private LocalDateTime lastTriggerAt;
}
