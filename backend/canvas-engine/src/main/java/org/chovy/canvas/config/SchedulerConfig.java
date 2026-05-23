package org.chovy.canvas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 调度线程池配置（供画布定时任务与人群任务复用）。
 *
 * <p>daemon 线程可避免应用关闭时被调度线程阻塞退出。
 * 默认线程数适合中小规模场景，生产建议按任务峰值压测后调整。
 *
 * 线程池职责：
 * - 承载 Spring `TaskScheduler` 场景（cron/once）；
 * - 不承载大规模 CPU 计算任务（那类任务应走专用执行器）。
 */
@Configuration
public class SchedulerConfig {

    /** 统一任务调度线程池。 */
    @Bean
    public TaskScheduler canvasTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 当前规模先固定 4 线程，后续可按任务堆积情况做配置化扩容
        scheduler.setPoolSize(4);
        // 统一前缀便于日志和线程 dump 快速识别
        scheduler.setThreadNamePrefix("canvas-scheduler-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }
}
