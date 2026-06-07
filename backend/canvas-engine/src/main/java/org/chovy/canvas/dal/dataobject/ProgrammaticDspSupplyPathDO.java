package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("programmatic_dsp_supply_path")
public class ProgrammaticDspSupplyPathDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long lineItemId;

    private String exchangeKey;

    private String dealId;

    private String sellerId;

    private String sellerDomain;

    private String inventoryType;

    private String adsTxtStatus;

    private String sellersJsonStatus;

    private Integer schainComplete;

    private String status;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
