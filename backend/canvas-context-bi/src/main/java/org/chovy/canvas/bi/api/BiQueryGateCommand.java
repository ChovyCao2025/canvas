package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiQueryGateCommand 命令。
 */
public record BiQueryGateCommand(
        /**
         * 查询定义。
         */
        BiQueryCommand query,
        /**
         * from 字段值。
         */
        LocalDateTime from,
        /**
         * to 字段值。
         */
        LocalDateTime to,
        /**
         * mode 字段值。
         */
        String mode,
        boolean allowWarn) {
}
