package org.chovy.canvas.boot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Canvas Boot 运行时的 Spring Boot 启动入口。
 *
 * 该入口负责扫描新模块化包结构下的 Spring 组件，并仅注册带有 MyBatis {@link Mapper} 注解的 Mapper 接口。
 */
@MapperScan(basePackages = "org.chovy.canvas", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = "org.chovy.canvas")
public class CanvasBootApplication {

    /**
     * 启动 Canvas Boot 应用进程。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CanvasBootApplication.class, args);
    }
}
