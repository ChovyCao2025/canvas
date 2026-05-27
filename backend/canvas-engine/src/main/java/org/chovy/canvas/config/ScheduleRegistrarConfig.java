package org.chovy.canvas.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Schedule Registrar Spring 配置类。
 *
 * <p>负责注册后端运行所需的 Bean、过滤器或基础设施参数，集中管理框架层装配逻辑。
 * <p>业务代码不应直接依赖配置细节，而应通过注入后的组件使用对应能力。
 */
@Configuration
public class ScheduleRegistrarConfig {

    /**
     * 创建并注册 task Scheduler 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @return 方法执行后的业务结果
     */
    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("canvas-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
