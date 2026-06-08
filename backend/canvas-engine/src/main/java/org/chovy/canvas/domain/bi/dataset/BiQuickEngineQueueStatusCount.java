package org.chovy.canvas.domain.bi.dataset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BiQuickEngineQueueStatusCount 编排 domain.bi.dataset 场景的领域业务规则。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiQuickEngineQueueStatusCount {

    private String status;
    private Long count;
}
