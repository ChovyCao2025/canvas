package org.chovy.canvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 画布执行引擎启动入口。
 *
 * <p>负责装配后端 WebFlux API、执行引擎、调度器与缓存组件。
 * 该类不承载业务代码，所有运行逻辑均由 Spring Bean 生命周期驱动。
 */
@SpringBootApplication
public class CanvasEngineApplication {
    /** Spring Boot 启动方法。 */
    public static void main(String[] args) {
        // args 通常由运行容器注入（profile、JVM 参数等）。
        // 从这里启动整个画布引擎进程并进入 Spring 生命周期。
        // 本项目的 WebFlux/Scheduler/Redis 组件都在启动后自动装配。
        SpringApplication.run(CanvasEngineApplication.class, args);
    }
}
