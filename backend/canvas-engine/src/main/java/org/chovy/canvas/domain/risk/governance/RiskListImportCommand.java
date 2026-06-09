package org.chovy.canvas.domain.risk.governance;

import java.util.List;

/**
 * 风控名单批量导入命令。
 *
 * @param rows 待导入条目列表
 */
public record RiskListImportCommand(List<RiskListEntryCommand> rows) {

    public RiskListImportCommand {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
