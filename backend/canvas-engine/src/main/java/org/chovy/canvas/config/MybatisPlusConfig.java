package org.chovy.canvas.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p>当前仅启用分页插件，避免一次性拉取过大结果集。
 * 如需多租户/数据权限拦截，可在此继续追加 InnerInterceptor。
 * 该配置属于基础设施层，不放业务判断逻辑。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器（当前启用 MySQL 分页插件）。
     *
     * <p>注意：
     * - 分页插件顺序会影响 SQL 改写行为；
     * - 后续若新增拦截器，需确认与分页插件的先后关系。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 把 limit/offset 下推到数据库，避免内存分页
        // 默认不加性能分析拦截器，生产环境避免额外开销
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
