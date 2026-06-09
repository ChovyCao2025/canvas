package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingIntegrationContractProbeService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
@Slf4j
public class MarketingIntegrationContractProbeService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingIntegrationContractMapper contractMapper;
    private final MarketingIntegrationContractProbeRunMapper probeMapper;
    private final MarketingIntegrationContractProbeObservationMapper observationMapper;
    private final ObjectMapper objectMapper;
    private final MarketingIntegrationContractProbeAlertService alertService;
    private final MarketingIntegrationContractSloService sloService;
    private final Clock clock;

    /**
     * 创建 MarketingIntegrationContractProbeService 实例并注入 domain.marketing 场景依赖。
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param observationMapperProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param sloServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                                    MarketingIntegrationContractProbeRunMapper probeMapper,
                                                    ObjectProvider<MarketingIntegrationContractProbeObservationMapper>
                                                            observationMapperProvider,
                                                    ObjectMapper objectMapper,
                                                    ObjectProvider<MarketingIntegrationContractProbeAlertService>
                                                            alertServiceProvider,
                                                    ObjectProvider<MarketingIntegrationContractSloService>
                                                            sloServiceProvider) {
        this(contractMapper,
                probeMapper,
                observationMapperProvider == null ? null : observationMapperProvider.getIfAvailable(),
                objectMapper,
                alertServiceProvider == null ? null : alertServiceProvider.getIfAvailable(),
                sloServiceProvider == null ? null : sloServiceProvider.getIfAvailable(),
                Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingIntegrationContractProbeService 流程，围绕 marketing integration contract probe service 完成校验、计算或结果组装。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             ObjectMapper objectMapper,
                                             Clock clock) {
        this(contractMapper, probeMapper, null, objectMapper, null, null, clock);
    }

    /**
     * 执行 MarketingIntegrationContractProbeService 流程，围绕 marketing integration contract probe service 完成校验、计算或结果组装。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             ObjectMapper objectMapper,
                                             MarketingIntegrationContractProbeAlertService alertService,
                                             Clock clock) {
        this(contractMapper, probeMapper, null, objectMapper, alertService, null, clock);
    }

    /**
     * 执行 MarketingIntegrationContractProbeService 流程，围绕 marketing integration contract probe service 完成校验、计算或结果组装。
     *
     * @param contractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param observationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertService 依赖组件，用于完成数据访问或外部能力调用。
     * @param sloService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingIntegrationContractProbeService(MarketingIntegrationContractMapper contractMapper,
                                             MarketingIntegrationContractProbeRunMapper probeMapper,
                                             MarketingIntegrationContractProbeObservationMapper observationMapper,
                                             ObjectMapper objectMapper,
                                             MarketingIntegrationContractProbeAlertService alertService,
                                             MarketingIntegrationContractSloService sloService,
                                             Clock clock) {
        this.contractMapper = contractMapper;
        this.probeMapper = probeMapper;
        this.observationMapper = observationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.alertService = alertService;
        this.sloService = sloService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 recordProbe，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractProbeView recordProbe(Long tenantId,
                                                            Long contractId,
                                                            MarketingIntegrationContractProbeCommand command,
                                                            String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("integration contract probe command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO contract = contract(scopedTenantId, contractId);
        String probeKey = normalizeKey(command.probeKey(), "probeKey");
        MarketingIntegrationContractProbeRunDO row =
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                probeMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, contract.getId())
                        .eq(MarketingIntegrationContractProbeRunDO::getProbeKey, probeKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingIntegrationContractProbeRunDO();
            row.setTenantId(scopedTenantId);
            row.setContractId(contract.getId());
            row.setContractKey(contract.getContractKey());
            row.setProbeKey(probeKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setProviderFamily(contract.getProviderFamily());
        row.setEnvironment(normalizeEnvironment(command.environment(), contract.getEnvironment()));
        row.setStatus(normalizeStatus(command.status()));
        row.setHttpStatusCode(command.httpStatusCode());
        row.setLatencyMs(nonNegative(command.latencyMs()));
        row.setErrorType(trimToLimit(command.errorType(), 255));
        row.setProblemTypeUri(trimToLimit(command.problemTypeUri(), 512));
        row.setProblemTitle(trimToLimit(command.problemTitle(), 255));
        row.setProblemDetail(trimToLimit(command.problemDetail(), 1000));
        row.setErrorMessage(null);
        row.setSummary(null);
        row.setObservedAt(command.observedAt() == null ? LocalDateTime.now(clock).withNano(0) : command.observedAt());
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            probeMapper.insert(row);
        } else {
            probeMapper.updateById(row);
        }
        MarketingIntegrationContractProbeRunView runView = toRunView(row);
        appendObservation(scopedTenantId, contract, runView, actor);
        syncSlo(scopedTenantId, contract, runView.probeKey(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行业务操作 recordProbeRun，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingIntegrationContractProbeRunView recordProbeRun(Long tenantId,
                                                                   Long contractId,
                                                                   MarketingIntegrationContractProbeRunCommand command,
                                                                   String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("integration contract probe command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingIntegrationContractDO contract = contract(scopedTenantId, contractId);
        String probeKey = normalizeKey(command.probeKey(), "probeKey");
        MarketingIntegrationContractProbeRunDO row =
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                probeMapper.selectOne(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, contract.getId())
                        .eq(MarketingIntegrationContractProbeRunDO::getProbeKey, probeKey)
                        .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingIntegrationContractProbeRunDO();
            row.setTenantId(scopedTenantId);
            row.setContractId(contract.getId());
            row.setContractKey(contract.getContractKey());
            row.setProbeKey(probeKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setProviderFamily(defaultString(contract.getProviderFamily(), "UNKNOWN"));
        row.setEnvironment(normalizeEnvironment(null, contract.getEnvironment()));
        row.setStatus(normalizeStatus(command.status()));
        row.setHttpStatusCode(validateHttpStatus(command.httpStatusCode()));
        row.setLatencyMs(validateLatency(command.latencyMs()));
        row.setProblemTypeUri(trimToLimit(command.problemTypeUri(), 512));
        row.setErrorMessage(trimToLimit(command.errorMessage(), 1000));
        row.setSummary(trimToLimit(command.summary(), 512));
        row.setObservedAt(LocalDateTime.now(clock).withNano(0));
        row.setEvidenceJson(toJson(command.evidence()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            probeMapper.insert(row);
        } else {
            probeMapper.updateById(row);
        }
        MarketingIntegrationContractProbeRunView view = toRunView(row);
        appendObservation(scopedTenantId, contract, view, actor);
        syncAlert(scopedTenantId, contract, view, actor);
        syncSlo(scopedTenantId, contract, view.probeKey(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return view;
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param contractId 目标业务记录 ID，需与租户边界匹配
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractProbeView> listContractProbes(Long tenantId,
                                                                         Long contractId,
                                                                         Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        contract(scopedTenantId, contractId);
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(MarketingIntegrationContractProbeRunDO::getContractId, requiredId(contractId, "contractId"))
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractProbeView> listRecentProbes(Long tenantId,
                                                                       String status,
                                                                       Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null,
                                MarketingIntegrationContractProbeRunDO::getStatus,
                                normalizedStatus)
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .map(this::toView)
                .toList();
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param providerFamily provider family 参数，用于 listProbeRuns 流程中的校验、计算或对象转换。
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingIntegrationContractProbeRunView> listProbeRuns(Long tenantId,
                                                                        String status,
                                                                        String providerFamily,
                                                                        Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedProvider = normalizeOptionalUpper(providerFamily);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return probeMapper.selectList(new LambdaQueryWrapper<MarketingIntegrationContractProbeRunDO>()
                        .eq(MarketingIntegrationContractProbeRunDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null,
                                MarketingIntegrationContractProbeRunDO::getStatus,
                                normalizedStatus)
                        .eq(normalizedProvider != null,
                                MarketingIntegrationContractProbeRunDO::getProviderFamily,
                                normalizedProvider)
                        .orderByDesc(MarketingIntegrationContractProbeRunDO::getObservedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .filter(row -> normalizedProvider == null || normalizedProvider.equals(row.getProviderFamily()))
                .map(this::toRunView)
                .toList();
    }

    /**
     * 执行 contract 流程，围绕 contract 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractId 业务对象 ID，用于定位具体记录。
     * @return 返回 contract 流程生成的业务结果。
     */
    private MarketingIntegrationContractDO contract(Long tenantId, Long contractId) {
        MarketingIntegrationContractDO contract = contractMapper.selectById(requiredId(contractId, "contractId"));
        validateTenant(tenantId, contract == null ? null : contract.getTenantId(), "integration contract");
        return contract;
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 syncAlert 流程中的校验、计算或对象转换。
     * @param view view 参数，用于 syncAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void syncAlert(Long tenantId,
                           MarketingIntegrationContractDO contract,
                           MarketingIntegrationContractProbeRunView view,
                           String actor) {
        if (alertService == null) {
            return;
        }
        try {
            alertService.syncProbeResult(tenantId, contract, view, defaultString(actor, "system"));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe alert sync skipped contract={} error={}",
                    contract.getContractKey(), ex.getMessage());
        }
    }

    /**
     * 执行 appendObservation 流程，围绕 append observation 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 appendObservation 流程中的校验、计算或对象转换。
     * @param view view 参数，用于 appendObservation 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void appendObservation(Long tenantId,
                                   MarketingIntegrationContractDO contract,
                                   MarketingIntegrationContractProbeRunView view,
                                   String actor) {
        if (observationMapper == null) {
            return;
        }
        try {
            MarketingIntegrationContractProbeObservationDO row = new MarketingIntegrationContractProbeObservationDO();
            row.setTenantId(tenantId);
            row.setContractId(contract.getId());
            row.setProbeRunId(view.id());
            row.setContractKey(contract.getContractKey());
            row.setProviderFamily(defaultString(contract.getProviderFamily(), "UNKNOWN"));
            row.setProbeKey(view.probeKey());
            row.setEnvironment(view.environment());
            row.setStatus(view.status());
            row.setHttpStatusCode(view.httpStatusCode());
            row.setLatencyMs(view.latencyMs());
            row.setProblemTypeUri(view.problemTypeUri());
            row.setErrorMessage(view.errorMessage());
            row.setSummary(view.summary());
            row.setObservedAt(parseObservedAt(view.observedAt()));
            row.setEvidenceJson(toJson(view.evidence()));
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(LocalDateTime.now(clock).withNano(0));
            observationMapper.insert(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] probe observation append skipped contract={} probe={} error={}",
                    contract.getContractKey(), view.probeKey(), ex.getMessage());
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contract contract 参数，用于 syncSlo 流程中的校验、计算或对象转换。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void syncSlo(Long tenantId,
                         MarketingIntegrationContractDO contract,
                         String probeKey,
                         String actor) {
        if (sloService == null) {
            return;
        }
        try {
            sloService.evaluateAndSyncContract(tenantId, contract, probeKey, defaultString(actor, "system"));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            log.warn("[MARKETING-INTEGRATION] SLO evaluation skipped contract={} probe={} error={}",
                    contract.getContractKey(), probeKey, ex.getMessage());
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingIntegrationContractProbeView toView(MarketingIntegrationContractProbeRunDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingIntegrationContractProbeView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getProbeKey(),
                row.getEnvironment(),
                row.getStatus(),
                row.getHttpStatusCode(),
                row.getLatencyMs(),
                row.getErrorType(),
                row.getProblemTypeUri(),
                row.getProblemTitle(),
                row.getProblemDetail(),
                row.getObservedAt(),
                fromJson(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingIntegrationContractProbeRunView toRunView(MarketingIntegrationContractProbeRunDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingIntegrationContractProbeRunView(
                row.getId(),
                row.getTenantId(),
                row.getContractId(),
                row.getContractKey(),
                row.getProviderFamily(),
                row.getEnvironment(),
                row.getProbeKey(),
                row.getStatus(),
                row.getHttpStatusCode(),
                row.getLatencyMs(),
                row.getProblemTypeUri(),
                row.getErrorMessage(),
                row.getSummary(),
                fromJson(row.getEvidenceJson()),
                row.getObservedAt() == null ? null : row.getObservedAt().toString(),
                row.getCreatedBy(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param String string 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("probe evidence must be JSON serializable", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDateTime parseObservedAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(clock).withNano(0);
        }
        try {
            return LocalDateTime.parse(value.trim()).withNano(0);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            return LocalDateTime.now(clock).withNano(0);
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKey(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "PASS");
        return switch (status) {
            case "PASS", "WARN", "FAIL" -> status;
            default -> throw new IllegalArgumentException("unsupported integration probe status: " + status);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeEnvironment 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeEnvironment(String value, String fallback) {
        String environment = normalizeUpper(value, defaultString(fallback, "PRODUCTION"));
        return switch (environment) {
            case "PRODUCTION", "STAGING", "SANDBOX" -> environment;
            default -> throw new IllegalArgumentException("unsupported integration environment: " + environment);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non negative 计算得到的数量、金额或指标值。
     */
    private static Long nonNegative(Long value) {
        return value == null || value < 0 ? null : value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static Integer validateHttpStatus(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException("httpStatusCode must be between 100 and 599");
        }
        return value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static Long validateLatency(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("latencyMs must be non-negative");
        }
        return value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String trimToLimit(String value, int limit) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 规范化输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     * @param entity entity 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
