package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.datasource.DataSourceTableMeta;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据源配置 HTTP 控制器，根路由为 {@code /canvas/data-sources}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/data-sources")
@RequiredArgsConstructor
public class DataSourceConfigController {

    /** 数据源配置 Mapper，用于读写数据源配置。 */
    private final DataSourceConfigMapper dataSourceConfigMapper;
    /** 敏感字段加解密工具。 */
    private final SecretCipher secretCipher;

    @GetMapping
    public Mono<R<PageResult<DataSourceConfigDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer enabled
    ) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<DataSourceConfigDO> wrapper = new LambdaQueryWrapper<DataSourceConfigDO>()
                    .orderByDesc(DataSourceConfigDO::getId);
            if (type != null && !type.isBlank()) {
                wrapper.eq(DataSourceConfigDO::getType, type);
            }
            if (enabled != null) {
                wrapper.eq(DataSourceConfigDO::getEnabled, enabled);
            }
            Page<DataSourceConfigDO> result = dataSourceConfigMapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 list Tables 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/{id}/tables")
    public Mono<R<List<DataSourceTableMeta>>> listTables(@PathVariable Long id) {
        // JDBC 元数据读取会建立真实数据库连接，必须放在线程池中执行。
        return Mono.fromCallable(() -> R.ok(readJdbcTables(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 create 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping
    public Mono<R<DataSourceConfigDO>> create(@RequestBody DataSourceConfigDO body) {
        return Mono.fromCallable(() -> {
            normalize(body);
            encryptPassword(body);
            dataSourceConfigMapper.insert(body);
            return R.ok(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 update 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @param body body 请求体、消息体或事件载荷
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody DataSourceConfigDO body) {
        return Mono.fromCallable(() -> {
            body.setId(id);
            normalize(body);
            encryptPassword(body);
            dataSourceConfigMapper.updateById(body);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 处理 delete 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param id id 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            dataSourceConfigMapper.deleteById(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行 normalize 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param body body 请求体、消息体或事件载荷
     */
    private static void normalize(DataSourceConfigDO body) {
        if (body.getType() == null || body.getType().isBlank()) {
            body.setType("JDBC");
        }
        // 当前只支持 JDBC 数据源，先阻断未知类型避免后续按错误驱动建连。
        if (!"JDBC".equals(body.getType())) {
            throw new IllegalArgumentException("Unsupported data source type: " + body.getType());
        }
        requireText(body.getName(), "name");
        requireText(body.getUrl(), "url");
        requireText(body.getUsername(), "username");
        requireText(body.getPassword(), "password");
        if (body.getDriverClassName() == null || body.getDriverClassName().isBlank()) {
            // 管理端未传驱动时按 MySQL 默认值补齐，保持旧配置兼容。
            body.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
    }

    /**
     * 执行 require Text 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param field field 方法执行所需的业务参数
     */
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }

    private void encryptPassword(DataSourceConfigDO body) {
        body.setPassword(secretCipher.encrypt(body.getPassword()));
    }

    /**
     * 查询或读取 read Jdbc Tables 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param id id 对应的业务主键或标识
     * @return 查询、转换或计算得到的结果集合
     */
    private List<DataSourceTableMeta> readJdbcTables(Long id) throws Exception {
        DataSourceConfigDO config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        if (config.getEnabled() == null || config.getEnabled() == 0) {
            throw new IllegalArgumentException("Data source disabled: " + id);
        }
        if (!"JDBC".equals(config.getType())) {
            throw new IllegalArgumentException("Unsupported data source type: " + config.getType());
        }
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(config.getDriverClassName())
                .url(config.getUrl())
                .username(config.getUsername())
                .password(secretCipher.decrypt(config.getPassword()))
                .build();
        try (Connection connection = dataSource.getConnection()) {
            // 通过 JDBC DatabaseMetaData 读取表和视图，不执行用户表数据查询。
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            List<DataSourceTableMeta> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tables.add(new DataSourceTableMeta(tableName, readColumns(metaData, catalog, tableName)));
                }
            }
            tables.sort((left, right) -> left.name().compareToIgnoreCase(right.name()));
            return tables;
        }
    }

    /**
     * 查询或读取 read Columns 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param metaData metaData 方法执行所需的业务参数
     * @param catalog catalog 方法执行所需的业务参数
     * @param tableName tableName 方法执行所需的业务参数
     * @return 查询、转换或计算得到的结果集合
     */
    private static List<String> readColumns(DatabaseMetaData metaData, String catalog, String tableName) throws Exception {
        List<String> columns = new ArrayList<>();
        // 只取列名供前端配置选择，不暴露字段样本值。
        try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
