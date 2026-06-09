package org.chovy.canvas.domain.approval;

import java.util.List;

/**
 * LarkApprovalInstanceSnapshot 承载 domain.approval 场景中的不可变数据快照。
 * @param instanceCode instanceCode 字段。
 * @param status status 字段。
 * @param tasks tasks 字段。
 */
public record LarkApprovalInstanceSnapshot(
        String instanceCode,
        String status,
        List<LarkApprovalTaskSnapshot> tasks) {
}
