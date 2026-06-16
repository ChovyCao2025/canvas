package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceConnectorCapability;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreviewRequest;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceConnectionTestResult;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceCredentialRotationCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceCredentialRotationView;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationResult;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingCommand;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingView;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaSnapshotView;
import org.chovy.canvas.domain.datasource.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * BiDatasourceController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/datasources")
public class BiDatasourceController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * onboarding服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasourceOnboardingService onboardingService;
    /**
     * 运行态服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasourceRuntimeService runtimeService;
    /**
     * data来源config服务，用于承接对应业务能力和领域编排。
     */
    private final DataSourceConfigService dataSourceConfigService;
    /**
     * fileupload服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasourceFileUploadService fileUploadService;
    /**
     * filematerialization服务，用于承接对应业务能力和领域编排。
     */
    private final BiDatasourceFileMaterializationService fileMaterializationService;

    /**
     * 创建 BiDatasourceController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService) {
        this(tenantContextResolver, onboardingService, null, null, null, null);
    }

    /**
     * 创建 BiDatasourceController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService) {
        this(tenantContextResolver, onboardingService, runtimeService, null, null, null);
    }

    /**
     * 创建 BiDatasourceController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param dataSourceConfigService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService,
                                  DataSourceConfigService dataSourceConfigService) {
        this(tenantContextResolver, onboardingService, runtimeService, dataSourceConfigService, null, null);
    }

    /**
     * 创建 BiDatasourceController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param dataSourceConfigService 依赖组件，用于完成数据访问或外部能力调用。
     * @param fileUploadService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService,
                                  DataSourceConfigService dataSourceConfigService,
                                  BiDatasourceFileUploadService fileUploadService) {
        this(tenantContextResolver,
                onboardingService,
                runtimeService,
                dataSourceConfigService,
                fileUploadService,
                null);
    }

    /**
     * 创建 BiDatasourceController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param dataSourceConfigService 依赖组件，用于完成数据访问或外部能力调用。
     * @param fileUploadService 依赖组件，用于完成数据访问或外部能力调用。
     * @param fileMaterializationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiDatasourceController(TenantContextResolver tenantContextResolver,
                                  BiDatasourceOnboardingService onboardingService,
                                  BiDatasourceRuntimeService runtimeService,
                                  DataSourceConfigService dataSourceConfigService,
                                  BiDatasourceFileUploadService fileUploadService,
                                  BiDatasourceFileMaterializationService fileMaterializationService) {
        this.tenantContextResolver = tenantContextResolver;
        this.onboardingService = onboardingService;
        this.runtimeService = runtimeService;
        this.dataSourceConfigService = dataSourceConfigService;
        this.fileUploadService = fileUploadService;
        this.fileMaterializationService = fileMaterializationService;
    }
    /**
     * 查询可接入的 BI 数据源连接器目录。
     * 返回各连接器的能力、凭据要求和预览支持情况；该目录是平台级静态能力，不绑定具体租户数据。
     *
     * @return 数据源连接器能力列表。
     */
    @GetMapping("/connectors")
    public Mono<R<List<BiDatasourceConnectorCapability>>> connectorCatalog() {
        return Mono.fromCallable(() -> R.ok(onboardingService.connectorCatalog()))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 查询当前租户已接入或正在接入的 BI 数据源。
     * 租户 ID 来自登录上下文，返回配置状态、连接器类型和最近处理结果，用于数据源管理页展示。
     *
     * @return 当前租户的数据源接入视图列表。
     */
    @GetMapping("/onboarding")
    public Mono<R<List<BiDatasourceOnboardingView>>> listOnboardingSources() {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(onboardingService.listOnboardingSources(normalizeTenant(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建一个当前租户的数据源接入配置。
     * 操作人写入为创建者，后续连接测试、schema 预览和同步都基于该配置执行。
     *
     * @param command 数据源接入配置，包含连接器类型、名称和连接参数。
     * @return 创建后的数据源接入视图。
     */
    @PostMapping("/onboarding")
    public Mono<R<BiDatasourceOnboardingView>> createDatasource(@RequestBody BiDatasourceOnboardingCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(onboardingService.createOnboardingSource(
                                normalizeTenant(context),
                                normalizeUsername(context),
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 上传文件并登记为当前租户的文件型 BI 数据源。
     * 文件内容会被读取到内存后交给上传服务解析元数据，生成接入记录但不立即物化为查询数据集。
     *
     * @param file 上传的 CSV、Excel 等数据文件。
     * @param name 数据源显示名称。
     * @param description 数据源描述，可为空。
     * @param sheetName Excel 工作表名称，可为空。
     * @param delimiter 文本文件分隔符，默认逗号。
     * @param headerRow 首行是否为表头，默认是。
     * @param encoding 文本编码，默认 UTF-8。
     * @return 文件型数据源接入视图。
     */
    @PostMapping(value = "/file-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<BiDatasourceOnboardingView>> uploadFileDatasource(
            @RequestPart("file") FilePart file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sheetName,
            @RequestParam(defaultValue = ",") String delimiter,
            @RequestParam(defaultValue = "true") boolean headerRow,
            @RequestParam(defaultValue = "UTF-8") String encoding) {
        return currentTenant().flatMap(context -> DataBufferUtils.join(file.content())
                .flatMap(buffer -> {
                    byte[] bytes;
                    try {
                        bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                    } finally {
                        // 文件内容已复制到 byte[]，必须释放 DataBuffer 防止上传大文件时占用堆外内存。
                        DataBufferUtils.release(buffer);
                    }
                    return Mono.fromCallable(() -> R.ok(requireFileUploadService().upload(
                                    normalizeTenant(context),
                                    normalizeUsername(context),
                                    file.filename(),
                                    bytes,
                                    new BiDatasourceFileUploadCommand(
                                            name,
                                            description,
                                            sheetName,
                                            delimiter,
                                            headerRow,
                                            encoding))))
                            .subscribeOn(Schedulers.boundedElastic());
                }));
    }
    /**
     * 上传文件并立即物化为可查询的数据集。
     * 当前租户、操作人和角色会传入物化服务；执行会创建或更新文件型数据源、抽取 schema，并按参数生成数据集资源。
     *
     * @param file 上传的 CSV、Excel 等数据文件。
     * @param name 数据源显示名称。
     * @param description 数据源描述，可为空。
     * @param sheetName Excel 工作表名称，可为空。
     * @param delimiter 文本文件分隔符，默认逗号。
     * @param headerRow 首行是否为表头，默认是。
     * @param encoding 文本编码，默认 UTF-8。
     * @param datasetKey 指定生成的数据集键；为空时由服务生成。
     * @param datasetName 数据集显示名称，可为空。
     * @param tenantColumn 写入物化表的租户列名，默认 tenant_id。
     * @param schemaLimit schema 推断最多读取的列或样本限制，默认 200。
     * @param maxRows 单次物化最多导入的行数，默认 100000。
     * @return 文件上传和物化结果。
     */
    @PostMapping(value = "/file-upload/materialize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<R<BiDatasourceFileMaterializationResult>> uploadAndMaterializeFileDatasource(
            @RequestPart("file") FilePart file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sheetName,
            @RequestParam(defaultValue = ",") String delimiter,
            @RequestParam(defaultValue = "true") boolean headerRow,
            @RequestParam(defaultValue = "UTF-8") String encoding,
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String datasetName,
            @RequestParam(defaultValue = "tenant_id") String tenantColumn,
            @RequestParam(defaultValue = "200") int schemaLimit,
            @RequestParam(defaultValue = "100000") long maxRows) {
        return currentTenant().flatMap(context -> DataBufferUtils.join(file.content())
                .flatMap(buffer -> {
                    byte[] bytes;
                    try {
                        bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                    } finally {
                        // 物化路径同样需要在复制后释放上传缓冲区。
                        DataBufferUtils.release(buffer);
                    }
                    return Mono.fromCallable(() -> R.ok(requireFileMaterializationService().uploadAndMaterialize(
                                    normalizeTenant(context),
                                    normalizeUsername(context),
                                    normalizeRole(context),
                                    file.filename(),
                                    bytes,
                                    new BiDatasourceFileMaterializationCommand(
                                            name,
                                            description,
                                            sheetName,
                                            delimiter,
                                            headerRow,
                                            encoding,
                                            datasetKey,
                                            datasetName,
                                            tenantColumn,
                                            schemaLimit,
                                            maxRows))))
                            .subscribeOn(Schedulers.boundedElastic());
                }));
    }
    /**
     * 更新当前租户的一条数据源接入配置。
     * {@code id} 必须属于当前租户；操作人会被记录为最后修改者，连接参数变更会影响后续测试、预览和同步。
     *
     * @param id 数据源接入记录标识。
     * @param command 新的数据源配置。
     * @return 更新后的数据源接入视图。
     */
    @PutMapping("/onboarding/{id}")
    public Mono<R<BiDatasourceOnboardingView>> updateDatasource(@PathVariable Long id,
                                                                @RequestBody BiDatasourceOnboardingCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(onboardingService.updateOnboardingSource(
                                normalizeTenant(context),
                                normalizeUsername(context),
                                id,
                                command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 对当前租户的数据源执行一次连接测试。
     * 测试会使用已保存的连接配置访问外部系统，并可能更新最近测试状态、耗时和错误摘要。
     *
     * @param id 数据源接入记录标识。
     * @return 连接测试结果。
     */
    @PostMapping("/{id}/connection-test")
    public Mono<R<BiDatasourceConnectionTestResult>> testConnection(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().testConnection(id, normalizeTenant(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 轮换当前租户数据源的连接凭据。
     * 当前实现更新 JDBC 密码并返回轮换视图；调用者身份来自租户上下文，用于记录操作者。
     *
     * @param id 数据源配置标识。
     * @param command 新凭据内容；为空时由下游按空密码处理。
     * @return 凭据轮换结果。
     */
    @PostMapping("/{id}/credential-rotation")
    public Mono<R<BiDatasourceCredentialRotationView>> rotateCredential(
            @PathVariable Long id,
            @RequestBody BiDatasourceCredentialRotationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    requireDataSourceConfigService().rotatePassword(
                            id,
                            command == null ? null : command.password(),
                            context);
                    return R.ok(new BiDatasourceCredentialRotationView(
                            id,
                            "jdbc-" + id,
                            normalizeUsername(context)));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 实时预览当前租户数据源的 schema。
     * 根据已保存连接读取字段结构，{@code limit} 控制采样规模；该接口不保存快照。
     *
     * @param id 数据源接入记录标识。
     * @param limit schema 预览采样上限，默认 100。
     * @return schema 预览结果。
     */
    @GetMapping("/{id}/schema-preview")
    public Mono<R<BiDatasourceSchemaPreview>> schemaPreview(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "100") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().previewSchema(id, normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 预览 API 型数据源返回的数据样例。
     * 请求体可覆盖分页、路径或参数等预览配置；结果用于接入调试，是否记录状态由运行服务决定。
     *
     * @param id 数据源接入记录标识。
     * @param request API 预览参数，可为空。
     * @return API 数据预览结果。
     */
    @PostMapping("/{id}/api-preview")
    public Mono<R<BiDatasourceApiPreview>> apiPreview(@PathVariable Long id,
                                                      @RequestBody(required = false) BiDatasourceApiPreviewRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().previewApiData(id, normalizeTenant(context), request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 同步当前租户数据源的 schema 快照。
     * 同步会访问外部数据源、生成新的 schema 快照，并记录触发人；API 数据源可通过请求体提供采样配置。
     *
     * @param id 数据源接入记录标识。
     * @param limit 采样或字段读取上限，默认 100。
     * @param request API 预览参数，可为空。
     * @return 最新 schema 快照视图。
     */
    @PostMapping("/{id}/schema-sync")
    public Mono<R<BiDatasourceSchemaSnapshotView>> syncSchema(@PathVariable Long id,
                                                              @RequestParam(defaultValue = "100") int limit,
                                                              @RequestBody(required = false) BiDatasourceApiPreviewRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().syncSchema(
                                id,
                                normalizeTenant(context),
                                normalizeUsername(context),
                                limit,
                                request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 读取当前租户数据源最近一次 schema 快照。
     * 只返回已经同步保存的快照，不主动访问外部数据源。
     *
     * @param id 数据源接入记录标识。
     * @return 最近一次 schema 快照。
     */
    @GetMapping("/{id}/schema-snapshot")
    public Mono<R<BiDatasourceSchemaSnapshotView>> latestSchemaSnapshot(@PathVariable Long id) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().latestSchemaSnapshot(id, normalizeTenant(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询当前租户数据源的 schema 快照历史。
     * 用于比较字段变化和排查同步问题，{@code limit} 控制返回的最近快照数量。
     *
     * @param id 数据源接入记录标识。
     * @param limit 最多返回的快照条数，默认 20。
     * @return schema 快照历史列表。
     */
    @GetMapping("/{id}/schema-snapshots")
    public Mono<R<List<BiDatasourceSchemaSnapshotView>>> schemaSnapshotHistory(@PathVariable Long id,
                                                                               @RequestParam(defaultValue = "20") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRuntimeService().schemaSnapshotHistory(id, normalizeTenant(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeUsername(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    /**
     * 规范化输入值。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeRole(TenantContext context) {
        return context == null ? null : context.role();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireRuntimeService 流程生成的业务结果。
     */
    private BiDatasourceRuntimeService requireRuntimeService() {
        if (runtimeService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param configured" configured"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("BI datasource runtime service is not configured");
        }
        return runtimeService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireDataSourceConfigService 流程生成的业务结果。
     */
    private DataSourceConfigService requireDataSourceConfigService() {
        if (dataSourceConfigService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param configured" configured"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("data source config service is not configured");
        }
        return dataSourceConfigService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireFileUploadService 流程生成的业务结果。
     */
    private BiDatasourceFileUploadService requireFileUploadService() {
        if (fileUploadService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param configured" configured"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("BI datasource file upload service is not configured");
        }
        return fileUploadService;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireFileMaterializationService 流程生成的业务结果。
     */
    private BiDatasourceFileMaterializationService requireFileMaterializationService() {
        if (fileMaterializationService == null) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param configured" configured"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("BI datasource file materialization service is not configured");
        }
        return fileMaterializationService;
    }
}
