package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ProgrammaticDspSeatDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("programmatic_dsp_seat")
public class ProgrammaticDspSeatDO {

    /** 程序化DSP席位主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 程序化DSP席位服务商 */
    private String provider;

    /** 程序化DSP席位席位业务键 */
    private String seatKey;

    /** 程序化DSP席位展示名称 */
    private String displayName;

    /** 关联的广告主账户 ID */
    private String advertiserAccountId;

    /** 程序化DSP席位币种 */
    private String currency;

    /** 程序化DSP席位时区 */
    private String timezone;

    /** 程序化DSP席位供给链路执行 */
    private String supplyChainEnforcement;

    /** 程序化DSP席位是否启用 */
    private Integer enabled;

    /** 程序化DSP席位扩展元数据 JSON */
    private String metadataJson;

    /** 程序化DSP席位创建人 */
    private String createdBy;

    /** 程序化DSP席位创建时间 */
    private LocalDateTime createdAt;

    /** 程序化DSP席位最后更新时间 */
    private LocalDateTime updatedAt;
}
