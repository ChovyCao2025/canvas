package org.chovy.canvas;

import org.chovy.canvas.domain.canvas.CanvasExamplesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CanvasExamplesProperties.class)
public class CanvasEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanvasEngineApplication.class, args);
    }
}
