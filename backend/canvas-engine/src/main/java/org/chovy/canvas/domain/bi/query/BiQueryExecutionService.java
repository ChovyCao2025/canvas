package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineAdmissionDecision;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueAdmissionCommand;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueJobView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * BiQueryExecutionService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiQueryExecutionService {

    private final BiQueryCompiler compiler;
    private final BiQueryExecutor executor;
    private final BiQueryHistoryRecorder historyRecorder;
    private final BiQueryResultCache resultCache;
    private final BiDatasetSpecResolver datasetSpecResolver;
    private final Clock clock;
    private final CdpWarehouseFieldGovernanceService fieldGovernanceService;
    private final BiPermissionService permissionService;
    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final BiQueryCachePolicyService cachePolicyService;
    private final BiDatasetAccelerationService datasetAccelerationService;
    private final BiQueryGovernancePolicyService governancePolicyService;
    private final BiQuickEngineCapacityService quickEngineCapacityService;
    private final BiQuickEngineQueueService quickEngineQueueService;

    @Autowired
    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapperProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapperProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param governancePolicyServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetAccelerationServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param quickEngineCapacityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param quickEngineQueueServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
                                   ObjectProvider<CdpWarehouseFieldGovernanceService> fieldGovernanceServiceProvider,
                                   ObjectProvider<BiPermissionService> permissionServiceProvider,
                                   ObjectProvider<CdpWarehouseAvailabilityService> availabilityServiceProvider,
                                   ObjectProvider<CdpWarehouseConsumerAvailabilityService>
                                           consumerAvailabilityServiceProvider,
                                   ObjectProvider<BiAuditLogMapper> auditLogMapperProvider,
                                   ObjectProvider<ObjectMapper> objectMapperProvider,
                                   ObjectProvider<BiQueryGovernancePolicyService> governancePolicyServiceProvider,
                                   ObjectProvider<BiQueryCachePolicyService> cachePolicyServiceProvider,
                                   ObjectProvider<BiDatasetAccelerationService> datasetAccelerationServiceProvider,
                                   ObjectProvider<BiQuickEngineCapacityService> quickEngineCapacityServiceProvider,
                                   ObjectProvider<BiQuickEngineQueueService> quickEngineQueueServiceProvider) {
        this(new BiQueryCompiler(), executor, historyRecorder, resultCache,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                fieldGovernanceServiceProvider.getIfAvailable(),
                permissionServiceProvider.getIfAvailable(),
                availabilityServiceProvider == null ? null : availabilityServiceProvider.getIfAvailable(),
                consumerAvailabilityServiceProvider == null
                        ? null
                        : consumerAvailabilityServiceProvider.getIfAvailable(),
                Clock.systemUTC(),
                auditLogMapperProvider == null ? null : auditLogMapperProvider.getIfAvailable(),
                objectMapperProvider == null ? null : objectMapperProvider.getIfAvailable(),
                cachePolicyServiceProvider == null ? null : cachePolicyServiceProvider.getIfAvailable(),
                datasetAccelerationServiceProvider == null ? null : datasetAccelerationServiceProvider.getIfAvailable(),
                governancePolicyServiceProvider == null ? null : governancePolicyServiceProvider.getIfAvailable(),
                quickEngineCapacityServiceProvider == null ? null : quickEngineCapacityServiceProvider.getIfAvailable(),
                quickEngineQueueServiceProvider == null ? null : quickEngineQueueServiceProvider.getIfAvailable());
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   ObjectProvider<CdpWarehouseFieldGovernanceService> fieldGovernanceServiceProvider) {
        this(new BiQueryCompiler(), executor, historyRecorder, resultCache,
                BiDatasetSpecResolver.builtIn(), fieldGovernanceServiceProvider.getIfAvailable(), null, null,
                Clock.systemUTC());
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, BiQueryResultCache.noop(), clock);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper) {
        this(compiler, executor, historyRecorder, BiQueryResultCache.noop(), BiDatasetSpecResolver.builtIn(),
                null, null, null, null, clock, auditLogMapper, objectMapper);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, BiDatasetSpecResolver.builtIn(), null, null, null,
                clock);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService, null, null,
                clock);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, null, clock);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, null, clock);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, null, null);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, null);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, null);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetAccelerationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, datasetAccelerationService, null);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetAccelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param governancePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService,
                                   BiQueryGovernancePolicyService governancePolicyService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, datasetAccelerationService, governancePolicyService, null);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetAccelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param governancePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param quickEngineCapacityService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService,
                                   BiQueryGovernancePolicyService governancePolicyService,
                                   BiQuickEngineCapacityService quickEngineCapacityService) {
        this(compiler, executor, historyRecorder, resultCache, datasetSpecResolver, fieldGovernanceService,
                permissionService, availabilityService, consumerAvailabilityService, clock, auditLogMapper,
                objectMapper, cachePolicyService, datasetAccelerationService, governancePolicyService,
                quickEngineCapacityService, null);
    }

    /**
     * 初始化 BiQueryExecutionService 实例。
     *
     * @param compiler compiler 参数，用于 BiQueryExecutionService 流程中的校验、计算或对象转换。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param historyRecorder 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param resultCache 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param fieldGovernanceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param availabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cachePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetAccelerationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param governancePolicyService 依赖组件，用于完成数据访问或外部能力调用。
     * @param quickEngineCapacityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param quickEngineQueueService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiQueryExecutionService(BiQueryCompiler compiler,
                                   BiQueryExecutor executor,
                                   BiQueryHistoryRecorder historyRecorder,
                                   BiQueryResultCache resultCache,
                                   BiDatasetSpecResolver datasetSpecResolver,
                                   CdpWarehouseFieldGovernanceService fieldGovernanceService,
                                   BiPermissionService permissionService,
                                   CdpWarehouseAvailabilityService availabilityService,
                                   CdpWarehouseConsumerAvailabilityService consumerAvailabilityService,
                                   Clock clock,
                                   BiAuditLogMapper auditLogMapper,
                                   ObjectMapper objectMapper,
                                   BiQueryCachePolicyService cachePolicyService,
                                   BiDatasetAccelerationService datasetAccelerationService,
                                   BiQueryGovernancePolicyService governancePolicyService,
                                   BiQuickEngineCapacityService quickEngineCapacityService,
                                   BiQuickEngineQueueService quickEngineQueueService) {
        this.compiler = compiler;
        this.executor = executor;
        this.historyRecorder = historyRecorder;
        this.resultCache = resultCache;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.fieldGovernanceService = fieldGovernanceService;
        this.permissionService = permissionService;
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.clock = clock;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.cachePolicyService = cachePolicyService;
        this.datasetAccelerationService = datasetAccelerationService;
        this.governancePolicyService = governancePolicyService;
        this.quickEngineCapacityService = quickEngineCapacityService;
        this.quickEngineQueueService = quickEngineQueueService;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 testService 流程生成的业务结果。
     */
    public static BiQueryExecutionService testService() {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(),
                BiQueryHistoryRecorder.noop(),
                BiQueryResultCache.noop(),
                Clock.systemUTC());
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回流程执行后的业务结果。
     */
    public BiQueryResult execute(BiQueryRequest request, BiQueryContext context) {
        BiDatasetSpec dataset = effectiveDataset(datasetSpecResolver.dataset(request.datasetKey(), context.tenantId()),
                context.tenantId());
        long startedAtMs = clock.instant().toEpochMilli();
        enforceGovernancePolicy(request, context, startedAtMs);
        BiPermissionService.BiPreparedQuery prepared = prepareQuery(request, context, dataset, startedAtMs);
        BiQueryRequest scopedRequest = prepared.request();
        enforceFieldPolicy(scopedRequest, context, dataset, startedAtMs);
        BiCompiledQuery query = compiler.compile(dataset, scopedRequest, context.tenantId());
        String sqlHash = hash(query, prepared.permissionSignature());
        BiQueryCachePolicy.ResourcePolicy cachePolicy = effectiveCachePolicy(
                context.tenantId(),
                scopedRequest.datasetKey(),
                scopedRequest.dashboardKey());
        BiQueryResult cachedResult = shouldUseCache(cachePolicy)
                ? resultCache.get(sqlHash)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(result -> result.asCached(Math.max(0, clock.instant().toEpochMilli() - startedAtMs)))
                .orElse(null)
                : null;
        if (cachedResult != null) {
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    cachedResult.rowCount(),
                    cachedResult.durationMs(),
                    "CACHE_HIT",
                    null));
            return cachedResult;
        }
        BiQuickEngineAdmissionState quickEngineAdmission =
                enforceQuickEngineAdmission(scopedRequest, context, sqlHash, startedAtMs);
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            List<Map<String, Object>> rows = executor.execute(query, dataset, sqlHash);
            List<Map<String, Object>> visibleRows = applyMasks(rows, prepared.columnMasks());
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            BiQueryResult result = new BiQueryResult(
                    dataset.datasetKey(),
                    columns(dataset, scopedRequest),
                    visibleRows,
                    visibleRows.size(),
                    durationMs,
                    sqlHash);
            if (shouldUseCache(cachePolicy)) {
                resultCache.put(sqlHash, result, Duration.ofSeconds(cachePolicy.ttlSeconds()));
            }
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    result.rowCount(),
                    durationMs,
                    "SUCCESS",
                    null));
            completeQueuedAdmission(context, quickEngineAdmission);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return result;
        } catch (RuntimeException e) {
            blockQueuedAdmission(context, quickEngineAdmission, e.getMessage());
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    scopedRequest,
                    sqlHash,
                    0,
                    durationMs,
                    "FAILED",
                    e.getMessage()));
            throw e;
        } finally {
            if (quickEngineAdmission.admitted()) {
                quickEngineCapacityService.releaseQuery(context.tenantId());
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 explain 流程生成的业务结果。
     */
    public BiQueryExplanation explain(BiQueryRequest request, BiQueryContext context) {
        BiDatasetSpec dataset = effectiveDataset(datasetSpecResolver.dataset(request.datasetKey(), context.tenantId()),
                context.tenantId());
        long startedAtMs = clock.instant().toEpochMilli();
        BiPermissionService.BiPreparedQuery prepared = prepareQuery(request, context, dataset, startedAtMs);
        BiQueryRequest scopedRequest = prepared.request();
        enforceFieldPolicy(scopedRequest, context, dataset, startedAtMs);
        BiCompiledQuery query = compiler.compile(dataset, scopedRequest, context.tenantId());
        return new BiQueryExplanation(
                dataset.datasetKey(),
                hash(query, prepared.permissionSignature()),
                query.parameters().size(),
                executor.explain(query, dataset));
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param sqlHash sql hash 参数，用于 cancel 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    public BiQueryCancellationResult cancel(String sqlHash) {
        return cancel(sqlHash, new BiQueryContext(0L, "system"));
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param sqlHash sql hash 参数，用于 cancel 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回布尔判断结果。
     */
    public BiQueryCancellationResult cancel(String sqlHash, BiQueryContext context) {
        if (sqlHash == null || sqlHash.isBlank()) {
            return new BiQueryCancellationResult("", false, "sqlHash is required");
        }
        boolean cancelled = executor.cancel(sqlHash);
        BiQueryCancellationResult result = new BiQueryCancellationResult(
                sqlHash,
                cancelled,
                cancelled ? "cancellation requested" : "not running");
        auditCancellation(result, context);
        return result;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 executeWithAvailabilityGate 流程中的校验、计算或对象转换。
     * @param allowWarn allow warn 参数，用于 executeWithAvailabilityGate 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public GatedBiQueryResult executeWithAvailabilityGate(BiQueryRequest request,
                                                          BiQueryContext context,
                                                          LocalDateTime from,
                                                          LocalDateTime to,
                                                          String mode,
                                                          boolean allowWarn) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (availabilityService == null) {
            throw new IllegalStateException("warehouse availability service is not configured");
        }
        Long tenantId = context == null || context.tenantId() == null ? 0L : context.tenantId();
        String username = context == null || context.username() == null ? "system" : context.username();
        long startedAtMs = clock.instant().toEpochMilli();
        CdpWarehouseAvailabilityService.AvailabilityDecision availability =
                availabilityService.evaluate(tenantId, from, to, mode);
        String availabilityStatus = availability == null ? "FAIL" : availability.status();
        if ("FAIL".equalsIgnoreCase(availabilityStatus)) {
            return blockedResult(tenantId, username, request, availability, startedAtMs,
                    "warehouse availability FAIL");
        }
        if ("WARN".equalsIgnoreCase(availabilityStatus) && !allowWarn) {
            return blockedResult(tenantId, username, request, availability, startedAtMs,
                    "warehouse availability WARN requires allowWarn=true");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiQueryResult queryResult = execute(request, context == null ? new BiQueryContext(0L, "system") : context);
        String reason = "WARN".equalsIgnoreCase(availabilityStatus)
                ? "warehouse availability WARN accepted by operator"
                : "warehouse availability PASS";
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new GatedBiQueryResult(
                tenantId,
                request.datasetKey(),
                "EXECUTED",
                reason,
                availability,
                queryResult);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param contractKey 业务键，用于在同一租户下定位资源。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回流程执行后的业务结果。
     */
    public ContractGatedBiQueryResult executeWithConsumerAvailabilityContract(
            BiQueryRequest request,
            BiQueryContext context,
            String contractKey,
            LocalDateTime from,
            LocalDateTime to) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (contractKey == null || contractKey.isBlank()) {
            throw new IllegalArgumentException("contractKey is required");
        }
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        Long tenantId = context == null || context.tenantId() == null ? 0L : context.tenantId();
        String username = context == null || context.username() == null ? "system" : context.username();
        long startedAtMs = clock.instant().toEpochMilli();
        CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation evaluation =
                consumerAvailabilityService.evaluateContract(tenantId, contractKey, from, to);
        if (evaluation == null || !evaluation.allowed()) {
            String reason = evaluation == null
                    ? "consumer availability contract evaluation failed"
                    : evaluation.message();
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    tenantId,
                    username,
                    request,
                    requestHash("WAREHOUSE_CONSUMER_CONTRACT_BLOCKED:" + contractKey, request),
                    0,
                    durationMs,
                    "BLOCKED",
                    reason));
            return new ContractGatedBiQueryResult(
                    tenantId,
                    request.datasetKey(),
                    contractKey,
                    "BLOCKED",
                    reason,
                    evaluation,
                    null);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiQueryResult queryResult = execute(request, context == null ? new BiQueryContext(0L, "system") : context);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ContractGatedBiQueryResult(
                tenantId,
                request.datasetKey(),
                contractKey,
                "EXECUTED",
                evaluation.message(),
                evaluation,
                queryResult);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param availability availability 参数，用于 blockedResult 流程中的校验、计算或对象转换。
     * @param startedAtMs started at ms 参数，用于 blockedResult 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 blockedResult 流程生成的业务结果。
     */
    private GatedBiQueryResult blockedResult(Long tenantId,
                                             String username,
                                             BiQueryRequest request,
                                             CdpWarehouseAvailabilityService.AvailabilityDecision availability,
                                             long startedAtMs,
                                             String reason) {
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
        recordHistory(new BiQueryHistoryEntry(
                tenantId,
                username,
                request,
                requestHash("WAREHOUSE_AVAILABILITY_BLOCKED", request),
                0,
                durationMs,
                "BLOCKED",
                reason));
        return new GatedBiQueryResult(
                tenantId,
                request.datasetKey(),
                "BLOCKED",
                reason,
                availability,
                null);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param dataset dataset 参数，用于 prepareQuery 流程中的校验、计算或对象转换。
     * @param startedAtMs started at ms 参数，用于 prepareQuery 流程中的校验、计算或对象转换。
     * @return 返回 prepareQuery 流程生成的业务结果。
     */
    private BiPermissionService.BiPreparedQuery prepareQuery(BiQueryRequest request,
                                                             BiQueryContext context,
                                                             BiDatasetSpec dataset,
                                                             long startedAtMs) {
        if (permissionService == null) {
            return new BiPermissionService.BiPreparedQuery(request, List.of(), "bi-permission:none");
        }
        try {
            return permissionService.prepareQuery(dataset, request, context, BiPermissionService.ACTION_USE);
        } catch (RuntimeException e) {
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    request,
                    requestHash(request),
                    0,
                    durationMs,
                    "FAILED",
                    e.getMessage()));
            throw e;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 effectiveCachePolicy 流程生成的业务结果。
     */
    private BiQueryCachePolicy.ResourcePolicy effectiveCachePolicy(Long tenantId, String datasetKey, String dashboardKey) {
        if (cachePolicyService == null) {
            return BiQueryCachePolicy.defaults().defaultPolicy();
        }
        return cachePolicyService.effectivePolicy(tenantId, datasetKey, dashboardKey);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 effectiveDataset 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 effectiveDataset 流程生成的业务结果。
     */
    private BiDatasetSpec effectiveDataset(BiDatasetSpec dataset, Long tenantId) {
        if (datasetAccelerationService == null) {
            return dataset;
        }
        return datasetAccelerationService.applyAcceleration(tenantId, dataset);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param cachePolicy 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @return 返回布尔判断结果。
     */
    private boolean shouldUseCache(BiQueryCachePolicy.ResourcePolicy cachePolicy) {
        return cachePolicy == null
                || (cachePolicy.enabled()
                && !BiQueryCachePolicy.MODE_DIRECT_QUERY.equals(cachePolicy.cacheMode()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 applyMasks 流程中的校验、计算或对象转换。
     * @param masks masks 参数，用于 applyMasks 流程中的校验、计算或对象转换。
     * @return 返回 applyMasks 流程生成的业务结果。
     */
    private List<Map<String, Object>> applyMasks(List<Map<String, Object>> rows,
                                                 List<BiPermissionService.BiColumnMask> masks) {
        if (permissionService == null) {
            return rows;
        }
        return permissionService.applyMasks(rows, masks);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param dataset dataset 参数，用于 enforceFieldPolicy 流程中的校验、计算或对象转换。
     * @param startedAtMs started at ms 参数，用于 enforceFieldPolicy 流程中的校验、计算或对象转换。
     */
    private void enforceFieldPolicy(BiQueryRequest request,
                                    BiQueryContext context,
                                    BiDatasetSpec dataset,
                                    long startedAtMs) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (fieldGovernanceService == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        try {
            fieldGovernanceService.enforceBiQuery(
                    dataset,
                    request,
                    context,
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    CdpWarehouseFieldGovernanceService.ACTION_BI_EXECUTE);
        } catch (RuntimeException e) {
            long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
            recordHistory(new BiQueryHistoryEntry(
                    context.tenantId(),
                    context.username(),
                    request,
                    requestHash(request),
                    0,
                    durationMs,
                    "FAILED",
                    e.getMessage()));
            throw e;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param startedAtMs started at ms 参数，用于 enforceGovernancePolicy 流程中的校验、计算或对象转换。
     */
    private void enforceGovernancePolicy(BiQueryRequest request,
                                         BiQueryContext context,
                                         long startedAtMs) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (governancePolicyService == null) {
            return;
        }
        BiQueryGovernancePolicy policy = governancePolicyService.currentPolicy(context.tenantId());
        BiQueryGovernancePolicy.DatasetPolicy datasetPolicy =
                (policy == null ? BiQueryGovernancePolicy.defaults() : policy).datasetPolicy(request.datasetKey());
        if (request.limit() <= datasetPolicy.quotaRows()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        String message = "BI query limit exceeds governance quota for dataset " + request.datasetKey();
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
        recordHistory(new BiQueryHistoryEntry(
                context.tenantId(),
                context.username(),
                request,
                requestHash("BI_QUERY_GOVERNANCE_QUOTA", request),
                0,
                durationMs,
                "BLOCKED",
                message));
        throw new IllegalArgumentException(message);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param sqlHash sql hash 参数，用于 enforceQuickEngineAdmission 流程中的校验、计算或对象转换。
     * @param startedAtMs started at ms 参数，用于 enforceQuickEngineAdmission 流程中的校验、计算或对象转换。
     * @return 返回 enforceQuickEngineAdmission 流程生成的业务结果。
     */
    private BiQuickEngineAdmissionState enforceQuickEngineAdmission(BiQueryRequest request,
                                                                    BiQueryContext context,
                                                                    String sqlHash,
                                                                    long startedAtMs) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (quickEngineCapacityService == null) {
            return BiQuickEngineAdmissionState.notAdmitted();
        }
        BiQuickEngineAdmissionDecision decision = quickEngineCapacityService.admitQueryOrWait(context.tenantId(), 50);
        if (decision == null || decision.allowed()) {
            Long queuedJobId = null;
            if (isQueuedAdmission(decision)) {
                queuedJobId = persistQueuedAdmission(request, context, sqlHash, decision);
                long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
                recordHistory(new BiQueryHistoryEntry(
                        context.tenantId(),
                        context.username(),
                        request,
                        sqlHash,
                        0,
                        durationMs,
                        "QUEUED",
                        decision.message()));
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new BiQuickEngineAdmissionState(decision != null, queuedJobId);
        }
        String status = decision.status() == null || decision.status().isBlank()
                ? "BLOCKED"
                : decision.status().trim().toUpperCase(Locale.ROOT);
        String message = decision.message() == null || decision.message().isBlank()
                ? "Quick Engine tenant pool admission denied"
                : decision.message();
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - startedAtMs);
        recordHistory(new BiQueryHistoryEntry(
                context.tenantId(),
                context.username(),
                request,
                sqlHash,
                0,
                durationMs,
                status,
                message));
        throw new IllegalStateException(message);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param decision decision 参数，用于 isQueuedAdmission 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isQueuedAdmission(BiQuickEngineAdmissionDecision decision) {
        return decision != null
                && ("ADMITTED_AFTER_QUEUE".equals(normalize(decision.status())) || decision.queued());
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param sqlHash sql hash 参数，用于 persistQueuedAdmission 流程中的校验、计算或对象转换。
     * @param decision decision 参数，用于 persistQueuedAdmission 流程中的校验、计算或对象转换。
     * @return 返回 persist queued admission 计算得到的数量、金额或指标值。
     */
    private Long persistQueuedAdmission(BiQueryRequest request,
                                        BiQueryContext context,
                                        String sqlHash,
                                        BiQuickEngineAdmissionDecision decision) {
        if (quickEngineQueueService == null) {
            return null;
        }
        String poolKey = decision.tenantPoolPolicy() == null ? null : decision.tenantPoolPolicy().poolKey();
        Integer queueTimeoutSeconds = decision.tenantPoolPolicy() == null
                ? null
                : decision.tenantPoolPolicy().queueTimeoutSeconds();
        try {
            BiQuickEngineQueueJobView job = quickEngineQueueService.enqueue(context.tenantId(), new BiQuickEngineQueueAdmissionCommand(
                    poolKey,
                    sqlHash,
                    request.datasetKey(),
                    context.username(),
                    queueTimeoutSeconds));
            return job == null ? null : job.id();
        } catch (RuntimeException ignored) {
            // Queue persistence is operational evidence; admission has already been granted.
            return null;
        }
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param admission admission 参数，用于 completeQueuedAdmission 流程中的校验、计算或对象转换。
     */
    private void completeQueuedAdmission(BiQueryContext context, BiQuickEngineAdmissionState admission) {
        if (quickEngineQueueService == null || admission.queuedJobId() == null) {
            return;
        }
        try {
            quickEngineQueueService.completeQueuedAdmission(context.tenantId(), admission.queuedJobId());
        } catch (RuntimeException ignored) {
            // Queue persistence is operational evidence; query success must remain authoritative.
        }
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param admission admission 参数，用于 blockQueuedAdmission 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     */
    private void blockQueuedAdmission(BiQueryContext context,
                                      BiQuickEngineAdmissionState admission,
                                      String reason) {
        if (quickEngineQueueService == null || admission.queuedJobId() == null) {
            return;
        }
        try {
            quickEngineQueueService.blockQueuedAdmission(context.tenantId(), admission.queuedJobId(), reason);
        } catch (RuntimeException ignored) {
            // Queue persistence is operational evidence; datasource failure remains authoritative.
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * BiQuickEngineAdmissionState 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record BiQuickEngineAdmissionState(boolean admitted, Long queuedJobId) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 notAdmitted 流程生成的业务结果。
         */
        static BiQuickEngineAdmissionState notAdmitted() {
            return new BiQuickEngineAdmissionState(false, null);
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param entry entry 参数，用于 recordHistory 流程中的校验、计算或对象转换。
     */
    private void recordHistory(BiQueryHistoryEntry entry) {
        try {
            historyRecorder.record(entry);
        } catch (RuntimeException ignored) {
            // Query history is audit metadata; a recorder outage must not break report reads.
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param result result 参数，用于 auditCancellation 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void auditCancellation(BiQueryCancellationResult result, BiQueryContext context) {
        if (auditLogMapper == null || result == null) {
            return;
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(scopedContext.tenantId());
        row.setActorId(scopedContext.username());
        row.setActionKey("BI_QUERY_CANCEL_REQUEST");
        row.setResourceType("BI_QUERY");
        row.setDetailJson(toJson(Map.of(
                "sqlHash", result.sqlHash(),
                "cancelled", result.cancelled(),
                "message", result.message())));
        row.setCreatedAt(LocalDateTime.now(clock));
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Cancellation requests must not be blocked by audit storage availability.
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 columns 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 columns 汇总后的集合、分页或映射视图。
     */
    private List<BiQueryColumn> columns(BiDatasetSpec dataset, BiQueryRequest request) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return java.util.stream.Stream.concat(
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        request.dimensions().stream()
                                .map(key -> {
                                    BiFieldSpec field = dataset.fields().get(key);
                                    return new BiQueryColumn(key, field.role().name(), field.valueType());
                                }),
                        request.metrics().stream()
                                .map(key -> new BiQueryColumn(key, "METRIC", dataset.metrics().get(key).valueType())))
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param query query 参数，用于 hash 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String hash(BiCompiledQuery query) {
        return hash(query, "bi-permission:none");
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param query query 参数，用于 hash 流程中的校验、计算或对象转换。
     * @param permissionSignature permission signature 参数，用于 hash 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String hash(BiCompiledQuery query, String permissionSignature) {
        // 准备本次处理所需的上下文和中间变量。
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            digest.update(query.sql().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(query.parameters().toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(String.valueOf(permissionSignature).getBytes(StandardCharsets.UTF_8));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 request hash 生成的文本或业务键。
     */
    private String requestHash(BiQueryRequest request) {
        return requestHash("BI_REQUEST", request);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 request hash 生成的文本或业务键。
     */
    private String requestHash(String reason, BiQueryRequest request) {
        // 准备本次处理所需的上下文和中间变量。
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            digest.update(reason.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(String.valueOf(request).getBytes(StandardCharsets.UTF_8));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * GatedBiQueryResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record GatedBiQueryResult(
            Long tenantId,
            String datasetKey,
            String status,
            String reason,
            CdpWarehouseAvailabilityService.AvailabilityDecision availability,
            BiQueryResult queryResult) {
    }

    /**
     * ContractGatedBiQueryResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ContractGatedBiQueryResult(
            Long tenantId,
            String datasetKey,
            String contractKey,
            String status,
            String reason,
            CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerAvailability,
            BiQueryResult queryResult) {
    }
}
