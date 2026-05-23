package org.chovy.canvas.dto.cdp;

import java.util.List;

public record CdpBatchTagReq(
        String operationType,
        String tagCode,
        String tagValue,
        List<String> userIds,
        String reason,
        String operator
) {}
