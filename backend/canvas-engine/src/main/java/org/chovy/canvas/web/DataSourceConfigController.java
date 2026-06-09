package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.compliance.AuditEventService;
import org.chovy.canvas.domain.datasource.DataSourceTableMeta;
import org.chovy.canvas.dto.datasource.DataSourceConfigReq;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据源配置 HTTP 控制器，根路由为 {@code /canvas/data-sources}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/data-sources")
@Validated
public class DataSourceConfigController {

    /** 数据源配置 Mapper，用于读写数据源配置。 */
    private final DataSourceConfigMapper dataSourceConfigMapper;
    /** 敏感字段加解密工具。 */
    private final SecretCipher secretCipher;
    /** 租户上下文解析器，用于数据源配置隔离。 */
    private final TenantContextResolver tenantContextResolver;
    /** 合规审计服务，可选注入以兼容轻量单元测试。 */
    private AuditEventService auditEventService;

    /**
     * 创建 DataSourceConfigController 实例并注入 web 场景依赖。
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 DataSourceConfigController 流程中的校验、计算或对象转换。
     */
    public DataSourceConfigController(DataSourceConfigMapper dataSourceConfigMapper,
                                      SecretCipher secretCipher) {
        this(dataSourceConfigMapper, secretCipher, null);
    }

    /**
     * 创建 DataSourceConfigController 实例并注入 web 场景依赖。
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 DataSourceConfigController 流程中的校验、计算或对象转换。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public DataSourceConfigController(DataSourceConfigMapper dataSourceConfigMapper,
                                      SecretCipher secretCipher,
                                      TenantContextResolver tenantContextResolver) {
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.secretCipher = secretCipher;
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 可选注入数据源配置审计服务，用于记录连接信息创建、更新和删除动作。
     *
     * <p>数据源接口会先解析租户上下文再写审计事件；该 setter 只负责接收可用的审计组件，
     * 不参与密钥解密、连接测试或租户过滤逻辑。</p>
     *
     * @param auditEventService 合规审计服务实例，未装配时相关接口跳过审计写入。
     */
    @Autowired(required = false)
    public void setAuditEventService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }
    /**
     * 分页查询当前租户可见的数据源连接配置。
     *
     * <p>普通租户只能看到自身配置；平台上下文可按 {@code tenantId} 过滤，用于运营侧排查跨租户连接问题。
     * 返回结果仅包含连接元数据，密码字段在写入时已加密，调用方不应依赖该接口获取明文密钥。</p>
     *
     * @param page 分页页码，从 1 开始，未传时读取第一页。
     * @param size 每页数量，未传时默认 20 条。
     * @param type 数据源类型过滤条件，当前主要用于筛选 JDBC 配置。
     * @param enabled 启停状态过滤条件，{@code 1} 表示启用，{@code 0} 表示禁用。
     * @param tenantId 平台上下文下的目标租户 ID，普通租户传入时仍会被租户隔离规则约束。
     * @return 异步返回分页后的数据源配置列表。
     */
    @GetMapping
    public Mono<R<PageResult<DataSourceConfigDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) Long tenantId
    ) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            LambdaQueryWrapper<DataSourceConfigDO> wrapper = new LambdaQueryWrapper<DataSourceConfigDO>()
                    .orderByDesc(DataSourceConfigDO::getId);
            applyTenantFilter(wrapper, context, tenantId);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (type != null && !type.isBlank()) {
                wrapper.eq(DataSourceConfigDO::getType, type);
            }
            if (enabled != null) {
                wrapper.eq(DataSourceConfigDO::getEnabled, enabled);
            }
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            Page<DataSourceConfigDO> result = dataSourceConfigMapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 读取指定 JDBC 数据源暴露的表结构清单。
     *
     * <p>接口会先校验当前租户对数据源配置的访问权，再临时建立 JDBC 连接读取元数据。
     * 该过程可能受到远端数据库网络、账号权限和驱动配置影响，因此放在弹性线程池中执行。</p>
     *
     * @param id 数据源配置 ID，必须属于当前租户或平台授权范围。
     * @return 异步返回表名、表类型等元数据信息。
     */
    @GetMapping("/{id}/tables")
    public Mono<R<List<DataSourceTableMeta>>> listTables(@PathVariable Long id) {
        // JDBC 元数据读取会建立真实数据库连接，必须放在线程池中执行。
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(readJdbcTables(id, context)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 创建当前租户的数据源连接配置。
     *
     * <p>写入前会补齐默认 JDBC 类型、校验必填连接字段并加密密码；租户字段由服务端上下文决定，
     * 避免客户端伪造 {@code tenantId} 把连接配置写入其他租户。成功后记录数据源凭据创建审计事件。</p>
     *
     * @param req 数据源创建请求，包含连接 URL、用户名、密码、驱动和启停状态。
     * @return 异步返回已写入的数据源配置记录。
     */
    @PostMapping
    public Mono<R<DataSourceConfigDO>> create(@Valid @RequestBody DataSourceConfigReq req) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            DataSourceConfigDO body = req.toDataObject();
            applyWriteTenant(body, context, null);
            normalize(body);
            encryptPassword(body);
            dataSourceConfigMapper.insert(body);
            recordDataSourceAudit(context, "data-source credential create", body);
            return R.ok(body);
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 更新指定数据源连接配置。
     *
     * <p>接口先读取旧记录验证租户访问权，再按当前上下文重写租户归属并重新加密密码。
     * 这可以防止更新请求同时修改连接内容和租户归属，成功后记录凭据变更审计事件。</p>
     *
     * @param id 待更新的数据源配置 ID。
     * @param req 新的数据源连接配置请求体。
     * @return 异步返回空响应，表示更新已完成。
     */
    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @Valid @RequestBody DataSourceConfigReq req) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            DataSourceConfigDO existing = requireDataSourceAccess(id, context);
            DataSourceConfigDO body = req.toDataObject();
            body.setId(id);
            applyWriteTenant(body, context, existing);
            normalize(body);
            encryptPassword(body);
            dataSourceConfigMapper.updateById(body);
            recordDataSourceAudit(context, "data-source credential update", body);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 删除指定数据源连接配置。
     *
     * <p>删除前会加载旧记录完成租户访问校验，并把旧记录写入审计元数据，便于追踪哪一个租户连接被移除。
     * 该接口会永久删除配置记录，调用方需要在前端完成二次确认。</p>
     *
     * @param id 待删除的数据源配置 ID。
     * @return 异步返回空响应，表示删除已完成。
     */
    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            DataSourceConfigDO existing = requireDataSourceAccess(id, context);
            dataSourceConfigMapper.deleteById(id);
            recordDataSourceAudit(context, "data-source credential delete", existing);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 归一化并校验数据源配置。
     *
     * <p>方法负责补齐默认 JDBC 类型、默认驱动和启用状态，并阻断缺少连接信息或暂不支持的数据源类型。
     * 调用方应在加密密码和写库前执行该校验。</p>
     *
     * @param body 待写入或更新的数据源配置对象。
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
     * 校验数据源连接字段必须为非空文本。
     *
     * @param value 待校验的字段值。
     * @param field 字段名称，用于构造错误信息。
     */
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void encryptPassword(DataSourceConfigDO body) {
        body.setPassword(secretCipher.encrypt(body.getPassword()));
    }

    /**
     * 读取 JDBC 数据源的表元数据。
     *
     * <p>方法会先校验当前租户是否能访问该数据源，再解密密码并临时建立数据库连接。
     * 只返回元数据，不执行用户 SQL，也不修改远端数据库。</p>
     *
     * @param id 数据源配置 ID。
     * @param context 当前租户上下文，用于访问控制。
     * @return 远端数据库暴露的表清单。
     */
    private List<DataSourceTableMeta> readJdbcTables(Long id, TenantContext context) throws Exception {
        DataSourceConfigDO config = requireDataSourceAccess(id, context);
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
     * 查询或读取业务数据。
     *
     * @param metaData meta data 参数，用于 readColumns 流程中的校验、计算或对象转换。
     * @param catalog catalog 参数，用于 readColumns 流程中的校验、计算或对象转换。
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @return 返回 read columns 汇总后的集合、分页或映射视图。
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

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(null, null, null));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(null, null, null));
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param wrapper wrapper 参数，用于 applyTenantFilter 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param requestedTenantId 业务对象 ID，用于定位具体记录。
     */
    private void applyTenantFilter(LambdaQueryWrapper<DataSourceConfigDO> wrapper,
                                   TenantContext context,
                                   Long requestedTenantId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (context == null || context.tenantId() == null) {
            if (requestedTenantId != null) {
                wrapper.eq(DataSourceConfigDO::getTenantId, requestedTenantId);
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (context.isSuperAdmin()) {
            wrapper.eq(requestedTenantId != null, DataSourceConfigDO::getTenantId, requestedTenantId);
        } else {
            wrapper.eq(DataSourceConfigDO::getTenantId, context.tenantId());
        }
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param existing existing 参数，用于 applyWriteTenant 流程中的校验、计算或对象转换。
     */
    private void applyWriteTenant(DataSourceConfigDO body, TenantContext context, DataSourceConfigDO existing) {
        if (existing != null && (!isSuperAdmin(context) || body.getTenantId() == null)) {
            body.setTenantId(existing.getTenantId());
            return;
        }
        if (context != null && context.tenantId() != null && (!context.isSuperAdmin() || body.getTenantId() == null)) {
            body.setTenantId(context.tenantId());
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 requireDataSourceAccess 流程生成的业务结果。
     */
    private DataSourceConfigDO requireDataSourceAccess(Long id, TenantContext context) {
        DataSourceConfigDO config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        if (!isSuperAdmin(context)
                && context != null
                && context.tenantId() != null
                && !Objects.equals(config.getTenantId(), context.tenantId())) {
            throw new AccessDeniedException("跨租户数据源访问被拒绝");
        }
        return config;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回布尔判断结果。
     */
    private boolean isSuperAdmin(TenantContext context) {
        return context != null && context.isSuperAdmin();
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param config 配置对象，用于控制运行参数和策略开关。
     */
    private void recordDataSourceAudit(TenantContext context, String operation, DataSourceConfigDO config) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (auditEventService == null || config == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", config.getName());
        metadata.put("type", config.getType());
        metadata.put("url", config.getUrl());
        metadata.put("username", config.getUsername());
        metadata.put("enabled", config.getEnabled());
        metadata.put("passwordPresent", config.getPassword() != null && !config.getPassword().isBlank());
        auditEventService.record(AuditEventService.AuditEventCommand.builder()
                .tenantId(config.getTenantId() == null ? (context == null ? null : context.tenantId()) : config.getTenantId())
                .actor(context == null || context.username() == null ? "system" : context.username())
                .actorRole(context == null ? null : context.role())
                .operation(operation)
                .targetType("data-source")
                .targetId(config.getId() == null ? "0" : String.valueOf(config.getId()))
                .metadata(metadata)
                .build());
    }
}
