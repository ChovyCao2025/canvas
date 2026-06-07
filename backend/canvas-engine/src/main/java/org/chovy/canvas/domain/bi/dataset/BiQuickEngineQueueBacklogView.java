package org.chovy.canvas.domain.bi.dataset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiQuickEngineQueueBacklogView {

    private Long tenantId;
    private String poolKey;
    private Long readyCount;
    private LocalDateTime oldestQueuedAt;
}
