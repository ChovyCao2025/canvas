package org.chovy.canvas.boot;

import org.chovy.canvas.cdp.config.CdpDefaultPortConfig;
import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@MapperScan(basePackages = "org.chovy.canvas", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = "org.chovy.canvas")
@Import(CdpDefaultPortConfig.class)
public class CanvasBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanvasBootApplication.class, args);
    }

    @Bean
    CdpAcceptedEventPublisher cdpAcceptedEventPublisher() {
        return CdpAcceptedEventPublisher.noop();
    }

    @Bean
    CdpWarehouseEventSinkPort cdpWarehouseEventSinkPort() {
        return CdpWarehouseEventSinkPort.noop();
    }
}
