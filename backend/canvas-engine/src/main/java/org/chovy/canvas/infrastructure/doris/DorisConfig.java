package org.chovy.canvas.infrastructure.doris;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean(name = "dorisJdbcTemplate")
    public JdbcTemplate dorisJdbcTemplate(@Qualifier("dorisDataSource") DataSource dorisDataSource) {
        return new JdbcTemplate(dorisDataSource);
    }
}
