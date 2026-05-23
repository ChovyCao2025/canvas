package org.chovy.canvas.dto.cdp;

import java.util.List;

public record CanvasUserDetailDTO(
        String userId,
        CdpUserDetailDTO profile,
        List<CdpUserTagDTO> tags,
        List<CdpUserCanvasSummaryDTO> canvasRows
) {}
