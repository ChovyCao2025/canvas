package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_user_tag_history")
public class CdpUserTagHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String tagCode;
    private String oldValue;
    private String newValue;
    private String operation;
    private String sourceType;
    private String sourceRefId;
    private String idempotencyKey;
    private String reason;
    private String operator;
    private LocalDateTime operatedAt;
}
