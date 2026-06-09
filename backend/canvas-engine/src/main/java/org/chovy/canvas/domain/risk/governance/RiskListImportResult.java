package org.chovy.canvas.domain.risk.governance;

import java.util.List;

/**
 * 风控名单批量导入结果。
 *
 * @param totalRows 总行数
 * @param acceptedRows 接收行数
 * @param rejectedRows 拒绝行数
 * @param rowErrors 行级错误列表
 * @param importAuditId 导入审计编号
 */
public record RiskListImportResult(
        int totalRows,
        int acceptedRows,
        int rejectedRows,
        List<String> rowErrors,
        String importAuditId
) {
}
