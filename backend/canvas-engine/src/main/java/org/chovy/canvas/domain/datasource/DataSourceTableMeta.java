package org.chovy.canvas.domain.datasource;

import java.util.List;

/**
 * 数据源表元数据不可变值对象。
 *
 * <p>用于描述外部数据源中的表名和可用列集合，供人群规则、SQL 生成和字段校验流程复用。
 * <p>record 天然不可变，适合作为只读元数据在服务之间传递，避免调用方直接操作裸 Map。
 */
public record DataSourceTableMeta(
        String name,
        List<String> columns
) {
}
