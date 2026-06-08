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

/**
 * Polls due BI subscriptions and alert rules for the configured scheduler tenant.
 *
 * <p>The scheduler only decides eligibility and lease ownership. Permission checks, alert evaluation, channel
 * dispatch, and log persistence stay inside {@link BiDeliveryRuntimeService}.</p>
 */
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

    /**
     * 执行 BiDeliverySchedulerService 流程，围绕 bi delivery scheduler service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行 BiDeliverySchedulerService 流程，围绕 bi delivery scheduler service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiDeliverySchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param leaseServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiDeliverySchedulerService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 执行 BiDeliverySchedulerService 流程，围绕 bi delivery scheduler service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiDeliverySchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     */
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

    /**
     * 执行 BiDeliverySchedulerService 流程，围绕 bi delivery scheduler service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param enabled enabled 参数，用于 BiDeliverySchedulerService 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param leaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param leaseTtlSeconds lease ttl seconds 参数，用于 BiDeliverySchedulerService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     */
    @Scheduled(fixedDelayString = "${canvas.bi.delivery.scheduler.fixed-delay-ms:60000}")
    public void scheduledCycle() {
        runScheduledOnce(LocalDateTime.now());
    }

    /**
     * Runs one scheduler pass, guarded by a distributed lease when the lease service is available.
     */
    public BiDeliverySchedulerResult runScheduledOnce(LocalDateTime now) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return runDueOnce(tenantId, operator, role, now);
        } finally {
            leaseService.release(tenantId, SCHEDULER_LEASE_KEY);
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    public BiDeliverySchedulerResult runDueOnce(Long tenantId, String username, String role, LocalDateTime now) {
        if (!running.compareAndSet(false, true)) {
            return new BiDeliverySchedulerResult(0, 0, 0, 0, 1, 0);
        }
        try {
            // The in-process guard protects manual invocations and scheduled ticks on the same JVM.
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
                    BiDeliveryRunResult result = deliveryRuntimeService.runSubscription(
                            scopedTenantId, subscription.getId(), effectiveUser, effectiveRole);
                    if (STATUS_TRIGGERED.equals(result.status())) {
                        subscriptionsTriggered++;
                    } else {
                        skipped++;
                    }
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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

    /**
     * 执行 subscriptions 流程，围绕 subscriptions 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 subscriptions 汇总后的集合、分页或映射视图。
     */
    private List<BiSubscriptionDO> subscriptions(Long tenantId) {
        List<BiSubscriptionDO> rows = subscriptionMapper.selectList(new LambdaQueryWrapper<BiSubscriptionDO>()
                .eq(BiSubscriptionDO::getTenantId, tenantId)
                .orderByAsc(BiSubscriptionDO::getId)
                .last("LIMIT " + limit));
        return rows == null ? List.of() : rows;
    }

    /**
     * 执行 alerts 流程，围绕 alerts 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 alerts 汇总后的集合、分页或映射视图。
     */
    private List<BiAlertRuleDO> alerts(Long tenantId) {
        List<BiAlertRuleDO> rows = alertRuleMapper.selectList(new LambdaQueryWrapper<BiAlertRuleDO>()
                .eq(BiAlertRuleDO::getTenantId, tenantId)
                .orderByAsc(BiAlertRuleDO::getId)
                .last("LIMIT " + limit));
        return rows == null ? List.of() : rows;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 latestLog 流程中的校验、计算或对象转换。
     * @return 返回 latestLog 流程生成的业务结果。
     */
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

    /**
     * 判断业务条件是否成立。
     *
     * @param schedule schedule 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param defaultIntervalMinutes default interval minutes 参数，用于 isDue 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isDue(Map<String, Object> schedule,
                          LocalDateTime lastRunAt,
                          LocalDateTime firstEligibleAt,
                          LocalDateTime now,
                          long defaultIntervalMinutes) {
        String cronExpression = text(schedule.get("cronExpression"));
        if (hasText(cronExpression)) {
            // Cron takes precedence because it is the most specific schedule shape accepted by the API.
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

    /**
     * 判断业务条件是否成立。
     *
     * @param cronExpression cron expression 参数，用于 isCronDue 流程中的校验、计算或对象转换。
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isCronDue(String cronExpression,
                              LocalDateTime lastRunAt,
                              LocalDateTime firstEligibleAt,
                              LocalDateTime now) {
        try {
            CronExpression parsed = CronExpression.parse(cronExpression);
            LocalDateTime anchor = lastRunAt == null ? firstEligibleAt.minusSeconds(1) : lastRunAt;
            LocalDateTime next = parsed.next(anchor);
            return next != null && !next.isAfter(now);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param intervalMinutes interval minutes 参数，用于 isIntervalDue 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 判断业务条件是否成立。
     *
     * @param schedule schedule 参数，用于 isDailyDue 流程中的校验、计算或对象转换。
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 判断业务条件是否成立。
     *
     * @param schedule schedule 参数，用于 isWeeklyDue 流程中的校验、计算或对象转换。
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 判断业务条件是否成立。
     *
     * @param schedule schedule 参数，用于 isMonthlyDue 流程中的校验、计算或对象转换。
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 执行 occurrenceDue 流程，围绕 occurrence due 完成校验、计算或结果组装。
     *
     * @param occurrence occurrence 参数，用于 occurrenceDue 流程中的校验、计算或对象转换。
     * @param lastRunAt 时间参数，用于计算窗口、过期或审计时间。
     * @param firstEligibleAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 occurrence due 的布尔判断结果。
     */
    private boolean occurrenceDue(LocalDateTime occurrence,
                                  LocalDateTime lastRunAt,
                                  LocalDateTime firstEligibleAt,
                                  LocalDateTime now) {
        if (occurrence.isAfter(now) || occurrence.isBefore(firstEligibleAt)) {
            return false;
        }
        // A calendar occurrence can trigger once; subsequent passes wait for the next occurrence.
        return lastRunAt == null || lastRunAt.isBefore(occurrence);
    }

    /**
     * 执行 firstEligibleAt 流程，围绕 first eligible at 完成校验、计算或结果组装。
     *
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     * @param updatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 firstEligibleAt 流程生成的业务结果。
     */
    private LocalDateTime firstEligibleAt(LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt != null) {
            return createdAt;
        }
        if (updatedAt != null) {
            return updatedAt;
        }
        return LocalDateTime.MIN.plusYears(1);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param log log 参数，用于 latestAt 流程中的校验、计算或对象转换。
     * @return 返回 latestAt 流程生成的业务结果。
     */
    private LocalDateTime latestAt(BiDeliveryLogDO log) {
        return log == null ? null : log.getCreatedAt();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param String string 参数，用于 scheduledTime 流程中的校验、计算或对象转换。
     * @param schedule schedule 参数，用于 scheduledTime 流程中的校验、计算或对象转换。
     * @return 返回 scheduledTime 流程生成的业务结果。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 执行 dayOfWeek 流程，围绕 day of week 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 dayOfWeek 流程中的校验、计算或对象转换。
     * @param schedule schedule 参数，用于 dayOfWeek 流程中的校验、计算或对象转换。
     * @return 返回 dayOfWeek 流程生成的业务结果。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultText 流程中的校验、计算或对象转换。
     * @return 返回 default text 生成的文本或业务键。
     */
    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 执行 longValue 流程，围绕 long value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 执行 intValue 流程，围绕 int value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 int value 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 leaseTtl 流程，围绕 lease ttl 完成校验、计算或结果组装。
     *
     * @return 返回 leaseTtl 流程生成的业务结果。
     */
    private Duration leaseTtl() {
        return Duration.ofSeconds(leaseTtlSeconds);
    }
}
