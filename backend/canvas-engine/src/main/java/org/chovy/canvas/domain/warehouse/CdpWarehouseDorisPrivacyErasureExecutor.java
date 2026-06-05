package org.chovy.canvas.domain.warehouse;

public interface CdpWarehouseDorisPrivacyErasureExecutor {

    Result execute(Command command);

    record Command(
            String assetKey,
            Long tenantId,
            String subjectType,
            String subjectValue,
            boolean dryRun,
            String actor) {
    }

    record Result(
            String status,
            long matchedCount,
            long affectedCount,
            String proofMessage,
            String errorMessage) {
    }
}
