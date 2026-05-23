package org.chovy.canvas.domain.customer;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_consent")
public class MarketingConsent {
    public static final String OPT_IN = "OPT_IN";
    public static final String OPT_OUT = "OPT_OUT";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String channel;
    private String consentStatus;
    private String source;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
