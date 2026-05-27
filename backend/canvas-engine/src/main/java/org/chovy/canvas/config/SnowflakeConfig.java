package org.chovy.canvas.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Snowflake Spring 配置类。
 *
 * <p>负责注册后端运行所需的 Bean、过滤器或基础设施参数，集中管理框架层装配逻辑。
 * <p>业务代码不应直接依赖配置细节，而应通过注入后的组件使用对应能力。
 */
@Configuration
public class SnowflakeConfig {

    @Bean
    public Snowflake snowflake() {
        // 通过IP后两段自动计算workerId，确保每台机器唯一（范围0-31）
        long workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr()) >> 16 & 31;
        long datacenterId = 1L; // 固定数据中心ID，多机房可改
        return IdUtil.getSnowflake(workerId, datacenterId);
    }
}
