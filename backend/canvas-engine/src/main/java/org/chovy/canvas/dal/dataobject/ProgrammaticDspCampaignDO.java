package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("programmatic_dsp_campaign")
public class ProgrammaticDspCampaignDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String campaignKey;

    private String campaignName;

    private String objective;

    private BigDecimal budgetAmount;

    private String currency;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String status;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
