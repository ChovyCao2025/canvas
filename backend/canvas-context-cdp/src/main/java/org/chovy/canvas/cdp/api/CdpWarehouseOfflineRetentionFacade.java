package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

public interface CdpWarehouseOfflineRetentionFacade {

    OfflineCyclePlanView offlineCyclePlan(Long tenantId, LocalDateTime now, int backfillLimit,
                                          int aggregationWindowMinutes);

    OfflineCycleResultView runOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                           int aggregationWindowMinutes, String operator);

    RetentionPlanView retentionPlan(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                    int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays);

    RetentionCleanupResultView runRetention(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                            int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays,
                                            String operator);

    record OfflineCyclePlanView(Long tenantId,
                                LocalDateTime plannedAt,
                                int backfillLimit,
                                int aggregationWindowMinutes,
                                List<OfflineCycleStepPlanView> steps) {
    }

    record OfflineCycleStepPlanView(String stepKey,
                                    String status,
                                    String reason,
                                    Long sourceStartId,
                                    Long sourceEndId,
                                    LocalDateTime windowStart,
                                    LocalDateTime windowEnd) {
    }

    record OfflineCycleResultView(Long tenantId,
                                  LocalDateTime ranAt,
                                  String operator,
                                  String status,
                                  long loadedRows,
                                  long failedRows,
                                  Long sourceEndId,
                                  String errorMessage,
                                  List<OfflineCycleStepResultView> steps) {
    }

    record OfflineCycleStepResultView(String stepKey,
                                      String status,
                                      long loadedRows,
                                      long failedRows,
                                      Long sourceEndId,
                                      LocalDateTime windowStart,
                                      LocalDateTime windowEnd,
                                      String message) {
    }

    record RetentionPlanView(Long tenantId,
                             LocalDateTime generatedAt,
                             RetentionTargetPlanView syncRuns,
                             RetentionTargetPlanView realtimeRetries,
                             RetentionTargetPlanView resolvedIncidents,
                             long totalEligibleRows) {
    }

    record RetentionTargetPlanView(String targetKey,
                                   int retentionDays,
                                   LocalDateTime cutoff,
                                   long eligibleRows,
                                   String rule) {
    }

    record RetentionCleanupResultView(Long tenantId,
                                      LocalDateTime cleanedAt,
                                      String operator,
                                      RetentionTargetResultView syncRuns,
                                      RetentionTargetResultView realtimeRetries,
                                      RetentionTargetResultView resolvedIncidents,
                                      long totalDeletedRows) {
    }

    record RetentionTargetResultView(String targetKey,
                                     int retentionDays,
                                     LocalDateTime cutoff,
                                     long eligibleRows,
                                     int deletedRows) {
    }
}
