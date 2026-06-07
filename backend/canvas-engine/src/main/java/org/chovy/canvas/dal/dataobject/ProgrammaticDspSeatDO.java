package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("programmatic_dsp_seat")
public class ProgrammaticDspSeatDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String provider;

    private String seatKey;

    private String displayName;

    private String advertiserAccountId;

    private String currency;

    private String timezone;

    private String supplyChainEnforcement;

    private Integer enabled;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
