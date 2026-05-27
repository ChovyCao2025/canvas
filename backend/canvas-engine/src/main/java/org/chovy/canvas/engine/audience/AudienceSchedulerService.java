package org.chovy.canvas.engine.audience;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 人群离线计算调度服务。
 *
 * 职责：
 * 1) 读取人群定义里的 cron 表达式；
 * 2) 把每个人群注册为独立定时任务；
 * 3) 在定义更新/禁用时替换或取消旧任务。
 *
 * 边界：
 * - 只负责“调度”不负责“计算”，计算逻辑由上层传入的 Runnable 执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceSchedulerService {

    /** 统一调度器（线程池在 SchedulerConfig 中配置）。 */
    private final TaskScheduler taskScheduler;

    /** 人群定义 Mapper，用于启动时读取全部可调度的人群定义。 */
    private final AudienceDefinitionMapper definitionMapper;

    /** audienceId 到定时任务句柄的映射，用于刷新或取消已有任务。 */
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /** 启动后加载全部人群定义并尝试注册调度。 */
    @PostConstruct
    void init() {
        // 应用重启后自动恢复调度，避免人工补注册
        refreshAll();
    }

    /** 全量读取人群定义并刷新离线人群计算调度任务。 */
    public void refreshAll() {
        List<AudienceDefinitionDO> definitions = definitionMapper.selectList(null);
        for (AudienceDefinitionDO definition : definitions) {
            // 全量刷新阶段先挂空任务，真实执行任务由业务层 refresh(...) 注入
            schedule(definition, () -> {
            });
        }
    }

    /** 刷新单个人群定义对应的调度任务，实际计算逻辑由调用方传入。 */
    public void refresh(AudienceDefinitionDO definition, Runnable job) {
        schedule(definition, job);
    }

    /** 注册单个人群定时任务（会先取消旧任务）。 */
    private void schedule(AudienceDefinitionDO definition, Runnable job) {
        // 覆盖更新前先清理旧任务，避免同一 audience 出现重复触发
        cancel(definition.getId());
        if (definition.getEnabled() == null || definition.getEnabled() == 0) {
            return;
        }
        if (definition.getCronExpression() == null || definition.getCronExpression().isBlank()) {
            return;
        }
        String cronExpression = normalizeCron(definition.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(
                job,
                new CronTrigger(cronExpression, TimeZone.getTimeZone("Asia/Shanghai"))
        );
        tasks.put(definition.getId(), future);
        log.info("[AUDIENCE] scheduled audienceId={} cron={}", definition.getId(), cronExpression);
    }

    /** 兼容 5/6 位 cron 表达式，统一为 Spring CronTrigger 可识别格式。 */
    private String normalizeCron(String cronExpression) {
        String normalized = cronExpression.trim().replaceAll("\\s+", " ");
        String[] fields = normalized.split(" ");
        if (fields.length == 5) {
            return "0 " + normalized;
        }
        return normalized;
    }

    /** 取消指定人群的后续调度任务，不中断正在执行的计算。 */
    public void cancel(Long audienceId) {
        ScheduledFuture<?> future = tasks.remove(audienceId);
        if (future != null) {
            // false: 不打断正在执行中的任务，仅取消后续调度
            future.cancel(false);
        }
    }

    /** 应用关闭时清理全部任务。 */
    @PreDestroy
    void shutdown() {
        tasks.values().forEach(future -> future.cancel(false));
        tasks.clear();
    }
}
