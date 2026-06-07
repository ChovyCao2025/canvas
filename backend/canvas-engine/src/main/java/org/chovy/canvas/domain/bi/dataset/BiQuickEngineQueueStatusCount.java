package org.chovy.canvas.domain.bi.dataset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiQuickEngineQueueStatusCount {

    private String status;
    private Long count;
}
