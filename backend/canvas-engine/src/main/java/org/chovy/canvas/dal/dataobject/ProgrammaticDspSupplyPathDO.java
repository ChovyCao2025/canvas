package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ProgrammaticDspSupplyPathDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("programmatic_dsp_supply_path")
public class ProgrammaticDspSupplyPathDO {

    /** 程序化DSP供给路径主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的行事项 ID */
    private Long lineItemId;

    /** 程序化DSP供给路径交易平台业务键 */
    private String exchangeKey;

    /** 关联的交易 ID */
    private String dealId;

    /** 关联的商家 ID */
    private String sellerId;

    /** 程序化DSP供给路径商家领域 */
    private String sellerDomain;

    /** 程序化DSP供给路径库存类型 */
    private String inventoryType;

    /** 程序化DSP供给路径广告文本状态 */
    private String adsTxtStatus;

    /** 程序化DSP供给路径卖家JSON状态 */
    private String sellersJsonStatus;

    /** 程序化DSPSUPPLY路径SCHAINCOMPLETE */
    private Integer schainComplete;

    /** 程序化DSP供给路径当前状态 */
    private String status;

    /** 程序化DSP供给路径扩展元数据 JSON */
    private String metadataJson;

    /** 程序化DSP供给路径创建人 */
    private String createdBy;

    /** 程序化DSP供给路径创建时间 */
    private LocalDateTime createdAt;

    /** 程序化DSP供给路径最后更新时间 */
    private LocalDateTime updatedAt;
}
