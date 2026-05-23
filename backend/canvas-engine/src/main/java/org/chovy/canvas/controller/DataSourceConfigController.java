package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.datasource.DataSourceConfig;
import org.chovy.canvas.domain.datasource.DataSourceConfigMapper;
import org.chovy.canvas.domain.datasource.DataSourceTableMeta;
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

@RestController
@RequestMapping("/canvas/data-sources")
@RequiredArgsConstructor
public class DataSourceConfigController {

    private final DataSourceConfigMapper dataSourceConfigMapper;

    @GetMapping
    public Mono<R<PageResult<DataSourceConfig>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer enabled
    ) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<DataSourceConfig>()
                    .orderByDesc(DataSourceConfig::getId);
            if (type != null && !type.isBlank()) {
                wrapper.eq(DataSourceConfig::getType, type);
            }
            if (enabled != null) {
                wrapper.eq(DataSourceConfig::getEnabled, enabled);
            }
            Page<DataSourceConfig> result = dataSourceConfigMapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}/tables")
    public Mono<R<List<DataSourceTableMeta>>> listTables(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(readJdbcTables(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<DataSourceConfig>> create(@RequestBody DataSourceConfig body) {
        return Mono.fromCallable(() -> {
            normalize(body);
            dataSourceConfigMapper.insert(body);
            return R.ok(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody DataSourceConfig body) {
        return Mono.fromCallable(() -> {
            body.setId(id);
            normalize(body);
            dataSourceConfigMapper.updateById(body);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            dataSourceConfigMapper.deleteById(id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static void normalize(DataSourceConfig body) {
        if (body.getType() == null || body.getType().isBlank()) {
            body.setType("JDBC");
        }
        if (!"JDBC".equals(body.getType())) {
            throw new IllegalArgumentException("Unsupported data source type: " + body.getType());
        }
        requireText(body.getName(), "name");
        requireText(body.getUrl(), "url");
        requireText(body.getUsername(), "username");
        requireText(body.getPassword(), "password");
        if (body.getDriverClassName() == null || body.getDriverClassName().isBlank()) {
            body.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }

    private List<DataSourceTableMeta> readJdbcTables(Long id) throws Exception {
        DataSourceConfig config = dataSourceConfigMapper.selectById(id);
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
                .password(config.getPassword())
                .build();
        try (Connection connection = dataSource.getConnection()) {
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

    private static List<String> readColumns(DatabaseMetaData metaData, String catalog, String tableName) throws Exception {
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog, null, tableName, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
