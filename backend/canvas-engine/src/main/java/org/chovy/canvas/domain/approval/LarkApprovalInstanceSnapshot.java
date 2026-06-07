package org.chovy.canvas.domain.approval;

import java.util.List;

public record LarkApprovalInstanceSnapshot(
        String instanceCode,
        String status,
        List<LarkApprovalTaskSnapshot> tasks) {
}
