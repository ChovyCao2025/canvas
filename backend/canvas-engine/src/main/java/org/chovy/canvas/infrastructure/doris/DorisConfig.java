package org.chovy.canvas.infrastructure.doris;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Optional Doris JDBC configuration.
 *
 * <p>Doris exposes a MySQL-compatible protocol on FE port 9030, so the
 * existing MySQL JDBC driver is enough. Beans are only created when explicitly
 * enabled to keep local tests and lightweight development independent of Doris.
 */
@Configuration
@ConditionalOnProperty(prefix = "canvas.doris", name = "enabled", havingValue = "true")
public class DorisConfig {

    /**
     * dataSource 处理 infrastructure.doris 场景的业务逻辑。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @return 返回 dataSource 流程生成的业务结果。
     */
    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * dorisDataSource 处理 infrastructure.doris 场景的业务逻辑。
     * @param jdbcUrl jdbc url 参数，用于 dorisDataSource 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 dorisDataSource 流程中的校验、计算或对象转换。
     * @param driverClassName 名称文本，用于展示或唯一性校验。
     * @param poolSize pool size 参数，用于 dorisDataSource 流程中的校验、计算或对象转换。
     * @return 返回 dorisDataSource 流程生成的业务结果。
     */
    @Bean(name = "dorisDataSource", destroyMethod = "close")
    public DataSource dorisDataSource(
            @Value("${canvas.doris.jdbc-url:jdbc:mysql://localhost:9030/canvas_dws}") String jdbcUrl,
            @Value("${canvas.doris.username:root}") String username,
            @Value("${canvas.doris.password:}") String password,
            @Value("${canvas.doris.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName,
            @Value("${canvas.doris.pool.size:10}") int poolSize) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(poolSize);
        ds.setMinimumIdle(Math.min(2, Math.max(1, poolSize)));
        ds.setConnectionTimeout(5000);
        ds.setValidationTimeout(2000);
        ds.setIdleTimeout(300000);
        ds.setMaxLifetime(1800000);
        ds.setPoolName("canvas-doris-pool");
        return ds;
    }

    /**
     * dorisJdbcTemplate 处理 infrastructure.doris 场景的业务逻辑。
     * @param dorisDataSource doris data source 参数，用于 dorisJdbcTemplate 流程中的校验、计算或对象转换。
     * @return 返回 dorisJdbcTemplate 流程生成的业务结果。
     */
    @Bean(name = "dorisJdbcTemplate")
    public JdbcTemplate dorisJdbcTemplate(@Qualifier("dorisDataSource") DataSource dorisDataSource) {
        return new JdbcTemplate(dorisDataSource);
    }
}
