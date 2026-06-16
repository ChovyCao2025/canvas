package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiQueryContractGateCommand 命令。
 */
public record BiQueryContractGateCommand(
        /**
         * 查询定义。
         */
        BiQueryCommand query,
        /**
         * contractKey 对应的业务键。
         */
        String contractKey,
        /**
         * from 字段值。
         */
        LocalDateTime from,
        LocalDateTime to) {
}
