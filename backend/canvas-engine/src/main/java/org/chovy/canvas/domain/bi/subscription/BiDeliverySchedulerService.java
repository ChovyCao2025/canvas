package org.chovy.canvas.domain.bi.subscription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAlertRuleDO;
import org.chovy.canvas.dal.dataobject.BiDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiAlertRuleMapper;
import org.chovy.canvas.dal.mapper.BiDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class BiDeliverySchedulerService {

    private static final String JOB_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String JOB_ALERT = "ALERT";
    private static final String CHANNEL_EVALUATION = "EVALUATION";
    private static final String STATUS_TRIGGERED = "TRIGGERED";
    private static final String SCHEDULER_LEASE_KEY = "BI_DELIVERY_SCHEDULER";

    private final BiSubscriptionMapper subscriptionMapper;
    private final BiAlertRuleMapper alertRuleMapper;
    private final BiDeliveryLogMapper deliveryLogMapper;
    private final BiDeliveryRuntimeService deliveryRuntimeService;
    private final BiDeliverySchedulerLeaseService leaseService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Long tenantId;
    private final String operator;
    private final String role;
    private final int limit;
    private final long leaseTtlSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BiDeliverySchedulerService(BiSubscriptionMapper subscriptionMapper,
                                      BiAlertRuleMapper alertRuleMapper,
                                      BiDeliveryLogMapper deliveryLogMapper,
                                      BiDeliveryRuntimeService deliveryRuntimeService,
                                      ObjectMapper objectMapper) {
        this(subscriptionMapper,
                alertRuleMapper,
                deliveryLogMapper,
                deliveryRuntimeService,
                objectMapper,
                false,
                0L,
                "bi-delivery-scheduler",
                "SYSTEM",
                500,
                (BiDeliverySchedulerLeaseService) null,
                120);
    }

    @Autowired
    public BiDeliverySchedulerService(BiSubscriptionMapper subscriptionMapper,
                                      BiAlertRuleMapper alertRuleMapper,
                                      BiDeliveryLogMapper deliveryLogMapper,
                                      BiDeliveryRuntimeService deliveryRuntimeService,
                                      ObjectMapper objectMapper,
                                      @Value("${canvas.bi.delivery.scheduler.enabled:false}") boolean enabled,
                                      @Value("${canvas.bi.delivery.scheduler.tenant-id:0}") Long tenantId,
                                      @Value("${canvas.bi.delivery.scheduler.operator:bi-delivery-scheduler}") String operator,
                                      @Value("${canvas.bi.delivery.scheduler.role:SYSTEM}") String role,
                                      @Value("${canvas.bi.delivery.scheduler.limit:500}") int limit,
                                      ObjectProvider<BiDeliverySchedulerLeaseService> leaseServiceProvider,
                                      @Value("${canvas.bi.delivery.scheduler.lease-ttl-seconds:120}") long leaseTtlSeconds) {
        this(subscriptionMapper,
                alertRuleMapper,
                deliveryLogMapper,
                deliveryRuntimeService,
                objectMapper,
                enabled,
                tenantId,
                operator,
                role,
                limit,
                leaseServiceProvider == null ? null : leaseServiceProvider.getIfAvailable(),
                leaseTtlSeconds);
    }

    public BiDeliverySchedulerService(BiSubscriptionMapper subscriptionMapper,
                                      BiAlertRuleMapper alertRuleMapper,
                                      BiDeliveryLogMapper deliveryLogMapper,
                                      BiDeliveryRuntimeService deliveryRuntimeService,
                                      ObjectMapper objectMapper,
                                      boolean enabled,
                                      Long tenantId,
                                      String operator,
                                      String role,
                                      int limit) {
        this(subscriptionMapper,
                alertRuleMapper,
                deliveryLogMapper,
                deliveryRuntimeService,
                objectMapper,
                enabled,
                tenantId,
                operator,
                role,
                limit,
                (BiDeliverySchedulerLeaseService) null,
                120);
    }

    public BiDeliverySchedulerService(BiSubscriptionMapper subscriptionMapper,
                                      BiAlertRuleMapper alertRuleMapper,
                                      BiDeliveryLogMapper deliveryLogMapper,
                                      BiDeliveryRuntimeService deliveryRuntimeService,
                                      ObjectMapper objectMapper,
                                      boolean enabled,
                                      Long tenantId,
                                      String operator,
                                      String role,
                                      int limit,
                                      BiDeliverySchedulerLeaseService leaseService,
                                      long leaseTtlSeconds) {
        this.subscriptionMapper = subscriptionMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.deliveryLogMapper = deliveryLogMapper;
        this.deliveryRuntimeService = deliveryRuntimeService;
        this.leaseService = leaseService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.tenantId = normalizeTenant(tenantId);
        this.operator = defaultText(operator, "bi-delivery-scheduler");
        this.role = defaultText(role, "SYSTEM");
        this.limit = Math.max(1, Math.min(limit, 1000));
        this.leaseTtlSeconds = Math.max(1, leaseTtlSeconds);
    }

    @Scheduled(fixedDelayString = "${canvas.bi.delivery.scheduler.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce(LocalDateTime.now());
    }

    public BiDeliverySchedulerResult runScheduledOnce(LocalDateTime now) {
        if (!enabled) {
            return new BiDeliverySchedulerResult(0, 0, 0, 0, 0, 0);
        }
        if (leaseService == null) {
            return runDueOnce(tenantId, operator, role, now);
        }
        if (!leaseService.acquire(tenantId, SCHEDULER_LEASE_KEY, leaseTtl())) {
            return new BiDeliverySchedulerResult(0, 0, 0, 0, 1, 0);
        }
        try {
            return runDueOnce(tenantId, operator, role, now);
        } finally {
            leaseService.release(tenantId, SCHEDULER_LEASE_KEY);
        }
    }

    public BiDeliverySchedulerResult runDueOnce(Long tenantId, String username, String role, LocalDateTime now) {
        if (!running.compareAndSet(false, true)) {
            return new BiDeliverySchedulerResult(0, 0, 0, 0, 1, 0);
        }
        try {
            LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
            Long scopedTenantId = normalizeTenant(tenantId);
            String effectiveUser = defaultText(username, operator);
            String effectiveRole = defaultText(role, this.role);

            int subscriptionsChecked = 0;
            int subscriptionsTriggered = 0;
            int alertsChecked = 0;
            int alertsTriggered = 0;
            int skipped = 0;
            int failed = 0;

            for (BiSubscriptionDO subscription : subscriptions(scopedTenantId)) {
                subscriptionsChecked++;
                if (!Boolean.TRUE.equals(subscription.getEnabled())) {
                    skipped++;
                    continue;
                }
                Map<String, Object> schedule = map(subscription.getScheduleJson());
                BiDeliveryLogDO latest = latestLog(scopedTenantId, JOB_SUBSCRIPTION, subscription.getId(), null);
                if (!isDue(schedule, latestAt(latest), firstEligibleAt(subscription.getCreatedAt(), subscription.getUpdatedAt()), effectiveNow, 24 * 60)) {
                    skipped++;
                    continue;
                }
                try {
                    BiDeliveryRunResult result = deliveryRuntimeService.runSubscription(scopedTenantId, subscription.getId(), effectiveUser);
                    if (STATUS_TRIGGERED.equals(result.status())) {
                        subscriptionsTriggered++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    failed++;
                }
            }

            for (BiAlertRuleDO alert : alerts(scopedTenantId)) {
                alertsChecked++;
                if (!Boolean.TRUE.equals(alert.getEnabled())) {
                    skipped++;
                    continue;
                }
                Map<String, Object> condition = map(alert.getConditionJson());
                BiDeliveryLogDO latest = latestLog(scopedTenantId, JOB_ALERT, alert.getId(), CHANNEL_EVALUATION);
                if (!isDue(condition, latestAt(latest), firstEligibleAt(alert.getCreatedAt(), alert.getUpdatedAt()), effectiveNow, 60)) {
                    skipped++;
                    continue;
                }
                try {
                    BiDeliveryRunResult result = deliveryRuntimeService.runAlert(scopedTenantId, effectiveUser, effectiveRole, alert.getId());
                    if (STATUS_TRIGGERED.equals(result.status())) {
                        alertsTriggered++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    failed++;
                }
            }

            return new BiDeliverySchedulerResult(
                    subscriptionsChecked,
                    subscriptionsTriggered,
                    alertsChecked,
                    alertsTriggered,
                    skipped,
                    failed);
        } finally {
            running.set(false);
        }
    }

    private List<BiSubscriptionDO> subscriptions(Long tenantId) {
        List<BiSubscriptionDO> rows = subscriptionMapper.selectList(new LambdaQueryWrapper<BiSubscriptionDO>()
                .eq(BiSubscriptionDO::getTenantId, tenantId)
                .orderByAsc(BiSubscriptionDO::getId)
                .last("LIMIT " + limit));
        return rows == null ? List.of() : rows;
    }

    private List<BiAlertRuleDO> alerts(Long tenantId) {
        List<BiAlertRuleDO> rows = alertRuleMapper.selectList(new LambdaQueryWrapper<BiAlertRuleDO>()
                .eq(BiAlertRuleDO::getTenantId, tenantId)
                .orderByAsc(BiAlertRuleDO::getId)
                .last("LIMIT " + limit));
        return rows == null ? List.of() : rows;
    }

    private BiDeliveryLogDO latestLog(Long tenantId, String jobType, Long jobId, String channel) {
        LambdaQueryWrapper<BiDeliveryLogDO> query = new LambdaQueryWrapper<BiDeliveryLogDO>()
                .eq(BiDeliveryLogDO::getTenantId, tenantId)
                .eq(BiDeliveryLogDO::getJobType, jobType)
                .eq(BiDeliveryLogDO::getJobId, jobId)
                .orderByDesc(BiDeliveryLogDO::getCreatedAt)
                .orderByDesc(BiDeliveryLogDO::getId)
                .last("LIMIT 1");
        if (hasText(channel)) {
            query.eq(BiDeliveryLogDO::getChannel, channel);
        }
        List<BiDeliveryLogDO> rows = deliveryLogMapper.selectList(query);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private boolean isDue(Map<String, Object> schedule,
                          LocalDateTime lastRunAt,
                          LocalDateTime firstEligibleAt,
                          LocalDateTime now,
                          long defaultIntervalMinutes) {
        String cronExpression = text(schedule.get("cronExpression"));
        if (hasText(cronExpression)) {
            return isCronDue(cronExpression, lastRunAt, firstEligibleAt, now);
        }

        String frequency = text(schedule.getOrDefault("frequency", schedule.get("checkFrequency")));
        Long intervalMinutes = longValue(schedule.getOrDefault("intervalMinutes", schedule.get("checkIntervalMinutes")));
        if (intervalMinutes != null && intervalMinutes > 0) {
            return isIntervalDue(lastRunAt, firstEligibleAt, now, intervalMinutes);
        }

        String normalizedFrequency = hasText(frequency) ? frequency.toUpperCase(Locale.ROOT) : "";
        return switch (normalizedFrequency) {
            case "HOURLY" -> isIntervalDue(lastRunAt, firstEligibleAt, now, 60);
            case "DAILY" -> isDailyDue(schedule, lastRunAt, firstEligibleAt, now);
            case "WEEKLY" -> isWeeklyDue(schedule, lastRunAt, firstEligibleAt, now);
            case "MONTHLY" -> isMonthlyDue(schedule, lastRunAt, firstEligibleAt, now);
            default -> isIntervalDue(lastRunAt, firstEligibleAt, now, defaultIntervalMinutes);
        };
    }

    private boolean isCronDue(String cronExpression,
                              LocalDateTime lastRunAt,
                              LocalDateTime firstEligibleAt,
                              LocalDateTime now) {
        try {
            CronExpression parsed = CronExpression.parse(cronExpression);
            LocalDateTime anchor = lastRunAt == null ? firstEligibleAt.minusSeconds(1) : lastRunAt;
            LocalDateTime next = parsed.next(anchor);
            return next != null && !next.isAfter(now);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isIntervalDue(LocalDateTime lastRunAt,
                                  LocalDateTime firstEligibleAt,
                                  LocalDateTime now,
                                  long intervalMinutes) {
        long safeInterval = Math.max(1, intervalMinutes);
        if (lastRunAt == null) {
            return !firstEligibleAt.isAfter(now);
        }
        return !lastRunAt.plusMinutes(safeInterval).isAfter(now);
    }

    private boolean isDailyDue(Map<String, Object> schedule,
                               LocalDateTime lastRunAt,
                               LocalDateTime firstEligibleAt,
                               LocalDateTime now) {
        LocalTime time = scheduledTime(schedule);
        if (time == null) {
            return isIntervalDue(lastRunAt, firstEligibleAt, now, 24 * 60);
        }
        LocalDateTime occurrence = now.toLocalDate().atTime(time);
        return occurrenceDue(occurrence, lastRunAt, firstEligibleAt, now);
    }

    private boolean isWeeklyDue(Map<String, Object> schedule,
                                LocalDateTime lastRunAt,
                                LocalDateTime firstEligibleAt,
                                LocalDateTime now) {
        DayOfWeek dayOfWeek = dayOfWeek(schedule);
        LocalTime time = scheduledTime(schedule);
        if (dayOfWeek == null || time == null) {
            return isIntervalDue(lastRunAt, firstEligibleAt, now, 7 * 24 * 60);
        }
        if (now.getDayOfWeek() != dayOfWeek) {
            return false;
        }
        LocalDateTime occurrence = now.toLocalDate().atTime(time);
        return occurrenceDue(occurrence, lastRunAt, firstEligibleAt, now);
    }

    private boolean isMonthlyDue(Map<String, Object> schedule,
                                 LocalDateTime lastRunAt,
                                 LocalDateTime firstEligibleAt,
                                 LocalDateTime now) {
        Integer dayOfMonth = intValue(schedule.getOrDefault("dayOfMonth", schedule.get("monthDay")));
        LocalTime time = scheduledTime(schedule);
        if (dayOfMonth == null || time == null) {
            return isIntervalDue(lastRunAt, firstEligibleAt, now, 30L * 24 * 60);
        }
        if (now.getDayOfMonth() != Math.max(1, Math.min(dayOfMonth, now.toLocalDate().lengthOfMonth()))) {
            return false;
        }
        LocalDateTime occurrence = now.toLocalDate().atTime(time);
        return occurrenceDue(occurrence, lastRunAt, firstEligibleAt, now);
    }

    private boolean occurrenceDue(LocalDateTime occurrence,
                                  LocalDateTime lastRunAt,
                                  LocalDateTime firstEligibleAt,
                                  LocalDateTime now) {
        if (occurrence.isAfter(now) || occurrence.isBefore(firstEligibleAt)) {
            return false;
        }
        return lastRunAt == null || lastRunAt.isBefore(occurrence);
    }

    private LocalDateTime firstEligibleAt(LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt != null) {
            return createdAt;
        }
        if (updatedAt != null) {
            return updatedAt;
        }
        return LocalDateTime.MIN.plusYears(1);
    }

    private LocalDateTime latestAt(BiDeliveryLogDO log) {
        return log == null ? null : log.getCreatedAt();
    }

    private LocalTime scheduledTime(Map<String, Object> schedule) {
        String raw = text(schedule.get("time"));
        if (!hasText(raw)) {
            raw = text(schedule.get("at"));
        }
        if (!hasText(raw)) {
            return null;
        }
        try {
            return LocalTime.parse(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private DayOfWeek dayOfWeek(Map<String, Object> schedule) {
        Object raw = schedule.getOrDefault("dayOfWeek", schedule.get("weekday"));
        Integer numeric = intValue(raw);
        if (numeric != null) {
            int bounded = Math.max(1, Math.min(numeric, 7));
            return DayOfWeek.of(bounded);
        }
        String text = text(raw);
        if (!hasText(text)) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = text(value);
        if (!hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = text(value);
        if (!hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
