package org.chovy.canvas.boot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = "org.chovy.canvas", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = "org.chovy.canvas")
public class CanvasBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanvasBootApplication.class, args);
    }
}
