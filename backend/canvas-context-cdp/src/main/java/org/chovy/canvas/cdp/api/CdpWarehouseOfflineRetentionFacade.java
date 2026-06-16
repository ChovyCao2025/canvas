package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义 CdpWarehouseOfflineRetentionFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseOfflineRetentionFacade {

    /**
     * 执行 offlineCyclePlan 对应的 CDP 业务操作。
     */
    OfflineCyclePlanView offlineCyclePlan(Long tenantId, LocalDateTime now, int backfillLimit,
                                          /**
                                           * aggregation Window Minutes)。
                                           */
                                          int aggregationWindowMinutes);

    /**
     * 执行 runOfflineCycle 对应的 CDP 业务操作。
     */
    OfflineCycleResultView runOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                           /**
                                            * operator)。
                                            */
                                           int aggregationWindowMinutes, String operator);

    /**
     * 执行 retentionPlan 对应的 CDP 业务操作。
     */
    RetentionPlanView retentionPlan(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                    /**
                                     * resolved Incident Retention Days)。
                                     */
                                    int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays);

    /**
     * 执行 runRetention 对应的 CDP 业务操作。
     */
    RetentionCleanupResultView runRetention(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                            int realtimeRetryRetentionDays, int resolvedIncidentRetentionDays,
                                            /**
                                             * operator)。
                                             */
                                            String operator);

    /**
     * 表示 OfflineCyclePlanView 的业务数据或处理组件。
     */
    final class OfflineCyclePlanView {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * planned At。
         */
        private final LocalDateTime plannedAt;

        /**
         * backfill Limit。
         */
        private final int backfillLimit;

        /**
         * aggregation Window Minutes。
         */
        private final int aggregationWindowMinutes;

        /**
         * steps。
         */
        private final List<OfflineCycleStepPlanView> steps;

        /**
         * 使用记录字段创建 OfflineCyclePlanView。
         */
        public OfflineCyclePlanView(
                Long tenantId,
                LocalDateTime plannedAt,
                int backfillLimit,
                int aggregationWindowMinutes,
                List<OfflineCycleStepPlanView> steps) {
            this.tenantId = tenantId;
            this.plannedAt = plannedAt;
            this.backfillLimit = backfillLimit;
            this.aggregationWindowMinutes = aggregationWindowMinutes;
            this.steps = steps;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回planned At。
         */
        public LocalDateTime plannedAt() {
            return plannedAt;
        }

        /**
         * 返回backfill Limit。
         */
        public int backfillLimit() {
            return backfillLimit;
        }

        /**
         * 返回aggregation Window Minutes。
         */
        public int aggregationWindowMinutes() {
            return aggregationWindowMinutes;
        }

        /**
         * 返回steps。
         */
        public List<OfflineCycleStepPlanView> steps() {
            return steps;
        }

        /**
         * 按所有字段比较 OfflineCyclePlanView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OfflineCyclePlanView that = (OfflineCyclePlanView) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(plannedAt, that.plannedAt)
                    && java.util.Objects.equals(backfillLimit, that.backfillLimit)
                    && java.util.Objects.equals(aggregationWindowMinutes, that.aggregationWindowMinutes)
                    && java.util.Objects.equals(steps, that.steps);
        }

        /**
         * 根据所有字段计算 OfflineCyclePlanView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, plannedAt, backfillLimit, aggregationWindowMinutes, steps);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "OfflineCyclePlanView[" + "tenantId=" + tenantId + ", plannedAt=" + plannedAt + ", backfillLimit=" + backfillLimit + ", aggregationWindowMinutes=" + aggregationWindowMinutes + ", steps=" + steps + "]";
        }
    }

    /**
     * 表示 OfflineCycleStepPlanView 的业务数据或处理组件。
     */
    final class OfflineCycleStepPlanView {

        /**
         * step Key。
         */
        private final String stepKey;

        /**
         * 状态。
         */
        private final String status;

        /**
         * 原因。
         */
        private final String reason;

        /**
         * source Start Id。
         */
        private final Long sourceStartId;

        /**
         * source End Id。
         */
        private final Long sourceEndId;

        /**
         * window Start。
         */
        private final LocalDateTime windowStart;

        /**
         * window End。
         */
        private final LocalDateTime windowEnd;

        /**
         * 使用记录字段创建 OfflineCycleStepPlanView。
         */
        public OfflineCycleStepPlanView(
                String stepKey,
                String status,
                String reason,
                Long sourceStartId,
                Long sourceEndId,
                LocalDateTime windowStart,
                LocalDateTime windowEnd) {
            this.stepKey = stepKey;
            this.status = status;
            this.reason = reason;
            this.sourceStartId = sourceStartId;
            this.sourceEndId = sourceEndId;
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
        }

        /**
         * 返回step Key。
         */
        public String stepKey() {
            return stepKey;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回source Start Id。
         */
        public Long sourceStartId() {
            return sourceStartId;
        }

        /**
         * 返回source End Id。
         */
        public Long sourceEndId() {
            return sourceEndId;
        }

        /**
         * 返回window Start。
         */
        public LocalDateTime windowStart() {
            return windowStart;
        }

        /**
         * 返回window End。
         */
        public LocalDateTime windowEnd() {
            return windowEnd;
        }

        /**
         * 按所有字段比较 OfflineCycleStepPlanView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OfflineCycleStepPlanView that = (OfflineCycleStepPlanView) o;
            return java.util.Objects.equals(stepKey, that.stepKey)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(reason, that.reason)
                    && java.util.Objects.equals(sourceStartId, that.sourceStartId)
                    && java.util.Objects.equals(sourceEndId, that.sourceEndId)
                    && java.util.Objects.equals(windowStart, that.windowStart)
                    && java.util.Objects.equals(windowEnd, that.windowEnd);
        }

        /**
         * 根据所有字段计算 OfflineCycleStepPlanView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(stepKey, status, reason, sourceStartId, sourceEndId, windowStart, windowEnd);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "OfflineCycleStepPlanView[" + "stepKey=" + stepKey + ", status=" + status + ", reason=" + reason + ", sourceStartId=" + sourceStartId + ", sourceEndId=" + sourceEndId + ", windowStart=" + windowStart + ", windowEnd=" + windowEnd + "]";
        }
    }

    /**
     * 表示 OfflineCycleResultView 的业务数据或处理组件。
     */
    final class OfflineCycleResultView {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * ran At。
         */
        private final LocalDateTime ranAt;

        /**
         * operator。
         */
        private final String operator;

        /**
         * 状态。
         */
        private final String status;

        /**
         * loaded Rows。
         */
        private final long loadedRows;

        /**
         * failed Rows。
         */
        private final long failedRows;

        /**
         * source End Id。
         */
        private final Long sourceEndId;

        /**
         * error Message。
         */
        private final String errorMessage;

        /**
         * steps。
         */
        private final List<OfflineCycleStepResultView> steps;

        /**
         * 使用记录字段创建 OfflineCycleResultView。
         */
        public OfflineCycleResultView(
                Long tenantId,
                LocalDateTime ranAt,
                String operator,
                String status,
                long loadedRows,
                long failedRows,
                Long sourceEndId,
                String errorMessage,
                List<OfflineCycleStepResultView> steps) {
            this.tenantId = tenantId;
            this.ranAt = ranAt;
            this.operator = operator;
            this.status = status;
            this.loadedRows = loadedRows;
            this.failedRows = failedRows;
            this.sourceEndId = sourceEndId;
            this.errorMessage = errorMessage;
            this.steps = steps;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回ran At。
         */
        public LocalDateTime ranAt() {
            return ranAt;
        }

        /**
         * 返回operator。
         */
        public String operator() {
            return operator;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回loaded Rows。
         */
        public long loadedRows() {
            return loadedRows;
        }

        /**
         * 返回failed Rows。
         */
        public long failedRows() {
            return failedRows;
        }

        /**
         * 返回source End Id。
         */
        public Long sourceEndId() {
            return sourceEndId;
        }

        /**
         * 返回error Message。
         */
        public String errorMessage() {
            return errorMessage;
        }

        /**
         * 返回steps。
         */
        public List<OfflineCycleStepResultView> steps() {
            return steps;
        }

        /**
         * 按所有字段比较 OfflineCycleResultView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OfflineCycleResultView that = (OfflineCycleResultView) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(ranAt, that.ranAt)
                    && java.util.Objects.equals(operator, that.operator)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(loadedRows, that.loadedRows)
                    && java.util.Objects.equals(failedRows, that.failedRows)
                    && java.util.Objects.equals(sourceEndId, that.sourceEndId)
                    && java.util.Objects.equals(errorMessage, that.errorMessage)
                    && java.util.Objects.equals(steps, that.steps);
        }

        /**
         * 根据所有字段计算 OfflineCycleResultView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, ranAt, operator, status, loadedRows, failedRows, sourceEndId, errorMessage, steps);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "OfflineCycleResultView[" + "tenantId=" + tenantId + ", ranAt=" + ranAt + ", operator=" + operator + ", status=" + status + ", loadedRows=" + loadedRows + ", failedRows=" + failedRows + ", sourceEndId=" + sourceEndId + ", errorMessage=" + errorMessage + ", steps=" + steps + "]";
        }
    }

    /**
     * 表示 OfflineCycleStepResultView 的业务数据或处理组件。
     */
    final class OfflineCycleStepResultView {

        /**
         * step Key。
         */
        private final String stepKey;

        /**
         * 状态。
         */
        private final String status;

        /**
         * loaded Rows。
         */
        private final long loadedRows;

        /**
         * failed Rows。
         */
        private final long failedRows;

        /**
         * source End Id。
         */
        private final Long sourceEndId;

        /**
         * window Start。
         */
        private final LocalDateTime windowStart;

        /**
         * window End。
         */
        private final LocalDateTime windowEnd;

        /**
         * 消息。
         */
        private final String message;

        /**
         * 使用记录字段创建 OfflineCycleStepResultView。
         */
        public OfflineCycleStepResultView(
                String stepKey,
                String status,
                long loadedRows,
                long failedRows,
                Long sourceEndId,
                LocalDateTime windowStart,
                LocalDateTime windowEnd,
                String message) {
            this.stepKey = stepKey;
            this.status = status;
            this.loadedRows = loadedRows;
            this.failedRows = failedRows;
            this.sourceEndId = sourceEndId;
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            this.message = message;
        }

        /**
         * 返回step Key。
         */
        public String stepKey() {
            return stepKey;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回loaded Rows。
         */
        public long loadedRows() {
            return loadedRows;
        }

        /**
         * 返回failed Rows。
         */
        public long failedRows() {
            return failedRows;
        }

        /**
         * 返回source End Id。
         */
        public Long sourceEndId() {
            return sourceEndId;
        }

        /**
         * 返回window Start。
         */
        public LocalDateTime windowStart() {
            return windowStart;
        }

        /**
         * 返回window End。
         */
        public LocalDateTime windowEnd() {
            return windowEnd;
        }

        /**
         * 返回消息。
         */
        public String message() {
            return message;
        }

        /**
         * 按所有字段比较 OfflineCycleStepResultView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OfflineCycleStepResultView that = (OfflineCycleStepResultView) o;
            return java.util.Objects.equals(stepKey, that.stepKey)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(loadedRows, that.loadedRows)
                    && java.util.Objects.equals(failedRows, that.failedRows)
                    && java.util.Objects.equals(sourceEndId, that.sourceEndId)
                    && java.util.Objects.equals(windowStart, that.windowStart)
                    && java.util.Objects.equals(windowEnd, that.windowEnd)
                    && java.util.Objects.equals(message, that.message);
        }

        /**
         * 根据所有字段计算 OfflineCycleStepResultView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(stepKey, status, loadedRows, failedRows, sourceEndId, windowStart, windowEnd, message);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "OfflineCycleStepResultView[" + "stepKey=" + stepKey + ", status=" + status + ", loadedRows=" + loadedRows + ", failedRows=" + failedRows + ", sourceEndId=" + sourceEndId + ", windowStart=" + windowStart + ", windowEnd=" + windowEnd + ", message=" + message + "]";
        }
    }

    /**
     * 表示 RetentionPlanView 的业务数据或处理组件。
     */
    final class RetentionPlanView {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * generated At。
         */
        private final LocalDateTime generatedAt;

        /**
         * sync Runs。
         */
        private final RetentionTargetPlanView syncRuns;

        /**
         * realtime Retries。
         */
        private final RetentionTargetPlanView realtimeRetries;

        /**
         * resolved Incidents。
         */
        private final RetentionTargetPlanView resolvedIncidents;

        /**
         * total Eligible Rows。
         */
        private final long totalEligibleRows;

        /**
         * 使用记录字段创建 RetentionPlanView。
         */
        public RetentionPlanView(
                Long tenantId,
                LocalDateTime generatedAt,
                RetentionTargetPlanView syncRuns,
                RetentionTargetPlanView realtimeRetries,
                RetentionTargetPlanView resolvedIncidents,
                long totalEligibleRows) {
            this.tenantId = tenantId;
            this.generatedAt = generatedAt;
            this.syncRuns = syncRuns;
            this.realtimeRetries = realtimeRetries;
            this.resolvedIncidents = resolvedIncidents;
            this.totalEligibleRows = totalEligibleRows;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回generated At。
         */
        public LocalDateTime generatedAt() {
            return generatedAt;
        }

        /**
         * 返回sync Runs。
         */
        public RetentionTargetPlanView syncRuns() {
            return syncRuns;
        }

        /**
         * 返回realtime Retries。
         */
        public RetentionTargetPlanView realtimeRetries() {
            return realtimeRetries;
        }

        /**
         * 返回resolved Incidents。
         */
        public RetentionTargetPlanView resolvedIncidents() {
            return resolvedIncidents;
        }

        /**
         * 返回total Eligible Rows。
         */
        public long totalEligibleRows() {
            return totalEligibleRows;
        }

        /**
         * 按所有字段比较 RetentionPlanView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RetentionPlanView that = (RetentionPlanView) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(generatedAt, that.generatedAt)
                    && java.util.Objects.equals(syncRuns, that.syncRuns)
                    && java.util.Objects.equals(realtimeRetries, that.realtimeRetries)
                    && java.util.Objects.equals(resolvedIncidents, that.resolvedIncidents)
                    && java.util.Objects.equals(totalEligibleRows, that.totalEligibleRows);
        }

        /**
         * 根据所有字段计算 RetentionPlanView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, generatedAt, syncRuns, realtimeRetries, resolvedIncidents, totalEligibleRows);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "RetentionPlanView[" + "tenantId=" + tenantId + ", generatedAt=" + generatedAt + ", syncRuns=" + syncRuns + ", realtimeRetries=" + realtimeRetries + ", resolvedIncidents=" + resolvedIncidents + ", totalEligibleRows=" + totalEligibleRows + "]";
        }
    }

    /**
     * 表示 RetentionTargetPlanView 的业务数据或处理组件。
     */
    final class RetentionTargetPlanView {

        /**
         * target Key。
         */
        private final String targetKey;

        /**
         * retention Days。
         */
        private final int retentionDays;

        /**
         * cutoff。
         */
        private final LocalDateTime cutoff;

        /**
         * eligible Rows。
         */
        private final long eligibleRows;

        /**
         * rule。
         */
        private final String rule;

        /**
         * 使用记录字段创建 RetentionTargetPlanView。
         */
        public RetentionTargetPlanView(
                String targetKey,
                int retentionDays,
                LocalDateTime cutoff,
                long eligibleRows,
                String rule) {
            this.targetKey = targetKey;
            this.retentionDays = retentionDays;
            this.cutoff = cutoff;
            this.eligibleRows = eligibleRows;
            this.rule = rule;
        }

        /**
         * 返回target Key。
         */
        public String targetKey() {
            return targetKey;
        }

        /**
         * 返回retention Days。
         */
        public int retentionDays() {
            return retentionDays;
        }

        /**
         * 返回cutoff。
         */
        public LocalDateTime cutoff() {
            return cutoff;
        }

        /**
         * 返回eligible Rows。
         */
        public long eligibleRows() {
            return eligibleRows;
        }

        /**
         * 返回rule。
         */
        public String rule() {
            return rule;
        }

        /**
         * 按所有字段比较 RetentionTargetPlanView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RetentionTargetPlanView that = (RetentionTargetPlanView) o;
            return java.util.Objects.equals(targetKey, that.targetKey)
                    && java.util.Objects.equals(retentionDays, that.retentionDays)
                    && java.util.Objects.equals(cutoff, that.cutoff)
                    && java.util.Objects.equals(eligibleRows, that.eligibleRows)
                    && java.util.Objects.equals(rule, that.rule);
        }

        /**
         * 根据所有字段计算 RetentionTargetPlanView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(targetKey, retentionDays, cutoff, eligibleRows, rule);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "RetentionTargetPlanView[" + "targetKey=" + targetKey + ", retentionDays=" + retentionDays + ", cutoff=" + cutoff + ", eligibleRows=" + eligibleRows + ", rule=" + rule + "]";
        }
    }

    /**
     * 表示 RetentionCleanupResultView 的业务数据或处理组件。
     */
    final class RetentionCleanupResultView {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * cleaned At。
         */
        private final LocalDateTime cleanedAt;

        /**
         * operator。
         */
        private final String operator;

        /**
         * sync Runs。
         */
        private final RetentionTargetResultView syncRuns;

        /**
         * realtime Retries。
         */
        private final RetentionTargetResultView realtimeRetries;

        /**
         * resolved Incidents。
         */
        private final RetentionTargetResultView resolvedIncidents;

        /**
         * total Deleted Rows。
         */
        private final long totalDeletedRows;

        /**
         * 使用记录字段创建 RetentionCleanupResultView。
         */
        public RetentionCleanupResultView(
                Long tenantId,
                LocalDateTime cleanedAt,
                String operator,
                RetentionTargetResultView syncRuns,
                RetentionTargetResultView realtimeRetries,
                RetentionTargetResultView resolvedIncidents,
                long totalDeletedRows) {
            this.tenantId = tenantId;
            this.cleanedAt = cleanedAt;
            this.operator = operator;
            this.syncRuns = syncRuns;
            this.realtimeRetries = realtimeRetries;
            this.resolvedIncidents = resolvedIncidents;
            this.totalDeletedRows = totalDeletedRows;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回cleaned At。
         */
        public LocalDateTime cleanedAt() {
            return cleanedAt;
        }

        /**
         * 返回operator。
         */
        public String operator() {
            return operator;
        }

        /**
         * 返回sync Runs。
         */
        public RetentionTargetResultView syncRuns() {
            return syncRuns;
        }

        /**
         * 返回realtime Retries。
         */
        public RetentionTargetResultView realtimeRetries() {
            return realtimeRetries;
        }

        /**
         * 返回resolved Incidents。
         */
        public RetentionTargetResultView resolvedIncidents() {
            return resolvedIncidents;
        }

        /**
         * 返回total Deleted Rows。
         */
        public long totalDeletedRows() {
            return totalDeletedRows;
        }

        /**
         * 按所有字段比较 RetentionCleanupResultView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RetentionCleanupResultView that = (RetentionCleanupResultView) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(cleanedAt, that.cleanedAt)
                    && java.util.Objects.equals(operator, that.operator)
                    && java.util.Objects.equals(syncRuns, that.syncRuns)
                    && java.util.Objects.equals(realtimeRetries, that.realtimeRetries)
                    && java.util.Objects.equals(resolvedIncidents, that.resolvedIncidents)
                    && java.util.Objects.equals(totalDeletedRows, that.totalDeletedRows);
        }

        /**
         * 根据所有字段计算 RetentionCleanupResultView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, cleanedAt, operator, syncRuns, realtimeRetries, resolvedIncidents, totalDeletedRows);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "RetentionCleanupResultView[" + "tenantId=" + tenantId + ", cleanedAt=" + cleanedAt + ", operator=" + operator + ", syncRuns=" + syncRuns + ", realtimeRetries=" + realtimeRetries + ", resolvedIncidents=" + resolvedIncidents + ", totalDeletedRows=" + totalDeletedRows + "]";
        }
    }

    /**
     * 表示 RetentionTargetResultView 的业务数据或处理组件。
     */
    final class RetentionTargetResultView {

        /**
         * target Key。
         */
        private final String targetKey;

        /**
         * retention Days。
         */
        private final int retentionDays;

        /**
         * cutoff。
         */
        private final LocalDateTime cutoff;

        /**
         * eligible Rows。
         */
        private final long eligibleRows;

        /**
         * deleted Rows。
         */
        private final int deletedRows;

        /**
         * 使用记录字段创建 RetentionTargetResultView。
         */
        public RetentionTargetResultView(
                String targetKey,
                int retentionDays,
                LocalDateTime cutoff,
                long eligibleRows,
                int deletedRows) {
            this.targetKey = targetKey;
            this.retentionDays = retentionDays;
            this.cutoff = cutoff;
            this.eligibleRows = eligibleRows;
            this.deletedRows = deletedRows;
        }

        /**
         * 返回target Key。
         */
        public String targetKey() {
            return targetKey;
        }

        /**
         * 返回retention Days。
         */
        public int retentionDays() {
            return retentionDays;
        }

        /**
         * 返回cutoff。
         */
        public LocalDateTime cutoff() {
            return cutoff;
        }

        /**
         * 返回eligible Rows。
         */
        public long eligibleRows() {
            return eligibleRows;
        }

        /**
         * 返回deleted Rows。
         */
        public int deletedRows() {
            return deletedRows;
        }

        /**
         * 按所有字段比较 RetentionTargetResultView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RetentionTargetResultView that = (RetentionTargetResultView) o;
            return java.util.Objects.equals(targetKey, that.targetKey)
                    && java.util.Objects.equals(retentionDays, that.retentionDays)
                    && java.util.Objects.equals(cutoff, that.cutoff)
                    && java.util.Objects.equals(eligibleRows, that.eligibleRows)
                    && java.util.Objects.equals(deletedRows, that.deletedRows);
        }

        /**
         * 根据所有字段计算 RetentionTargetResultView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(targetKey, retentionDays, cutoff, eligibleRows, deletedRows);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "RetentionTargetResultView[" + "targetKey=" + targetKey + ", retentionDays=" + retentionDays + ", cutoff=" + cutoff + ", eligibleRows=" + eligibleRows + ", deletedRows=" + deletedRows + "]";
        }
    }
}
