package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiQueryContractGateCommand(
        BiQueryCommand query,
        String contractKey,
        LocalDateTime from,
        LocalDateTime to) {
}
