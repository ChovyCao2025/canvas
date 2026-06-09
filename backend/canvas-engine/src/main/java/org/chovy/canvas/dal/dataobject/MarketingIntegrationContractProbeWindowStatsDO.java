package org.chovy.canvas.dal.dataobject;

import lombok.Data;

/**
 * MarketingIntegrationContractProbeWindowStatsDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
public class MarketingIntegrationContractProbeWindowStatsDO {

    /** 统计窗口内集成契约探测总次数 */
    private Long totalCount;

    /** 统计窗口内失败或异常的探测次数 */
    private Long badCount;
}
