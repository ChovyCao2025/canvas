package org.chovy.canvas.engine.audience;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceSchedulerService {

    private final TaskScheduler taskScheduler;
    private final AudienceDefinitionMapper definitionMapper;

    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        refreshAll();
    }

    public void refreshAll() {
        List<AudienceDefinition> definitions = definitionMapper.selectList(null);
        for (AudienceDefinition definition : definitions) {
            schedule(definition, () -> {
            });
        }
    }

    public void refresh(AudienceDefinition definition, Runnable job) {
        schedule(definition, job);
    }

    private void schedule(AudienceDefinition definition, Runnable job) {
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

    private String normalizeCron(String cronExpression) {
        String normalized = cronExpression.trim().replaceAll("\\s+", " ");
        String[] fields = normalized.split(" ");
        if (fields.length == 5) {
            return "0 " + normalized;
        }
        return normalized;
    }

    public void cancel(Long audienceId) {
        ScheduledFuture<?> future = tasks.remove(audienceId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @PreDestroy
    void shutdown() {
        tasks.values().forEach(future -> future.cancel(false));
        tasks.clear();
    }
}
