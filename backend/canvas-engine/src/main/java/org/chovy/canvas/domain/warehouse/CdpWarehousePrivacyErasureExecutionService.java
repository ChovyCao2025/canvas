package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
/**
 * CdpWarehousePrivacyErasureExecutionService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePrivacyErasureExecutionService {

    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";
    private static final String SKIPPED = "SKIPPED";
    private static final String USER_ID = "USER_ID";

    private final CdpWarehousePrivacyErasureService erasureService;
    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final CdpUserTagMapper tagMapper;
    private final CdpEventLogMapper eventLogMapper;
    private final CdpWarehouseRealtimeRetryMapper retryMapper;
    private final CdpWarehouseDorisPrivacyErasureExecutor dorisExecutor;

    @Autowired
    /**
     * 初始化 CdpWarehousePrivacyErasureExecutionService 实例。
     *
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param identityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param tagMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param retryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisExecutor 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehousePrivacyErasureExecutionService(
            CdpWarehousePrivacyErasureService erasureService,
            CdpUserProfileMapper profileMapper,
            CdpUserIdentityMapper identityMapper,
            CdpUserTagMapper tagMapper,
            CdpEventLogMapper eventLogMapper,
            CdpWarehouseRealtimeRetryMapper retryMapper,
            ObjectProvider<CdpWarehouseDorisPrivacyErasureExecutor> dorisExecutor) {
        this(erasureService, profileMapper, identityMapper, tagMapper, eventLogMapper, retryMapper,
                dorisExecutor == null ? null : dorisExecutor.getIfAvailable());
    }

    /**
     * 初始化 CdpWarehousePrivacyErasureExecutionService 实例。
     *
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param identityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param tagMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param retryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisExecutor 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CdpWarehousePrivacyErasureExecutionService(
            CdpWarehousePrivacyErasureService erasureService,
            CdpUserProfileMapper profileMapper,
            CdpUserIdentityMapper identityMapper,
            CdpUserTagMapper tagMapper,
            CdpEventLogMapper eventLogMapper,
            CdpWarehouseRealtimeRetryMapper retryMapper,
            CdpWarehouseDorisPrivacyErasureExecutor dorisExecutor) {
        this.erasureService = erasureService;
        this.profileMapper = profileMapper;
        this.identityMapper = identityMapper;
        this.tagMapper = tagMapper;
        this.eventLogMapper = eventLogMapper;
        this.retryMapper = retryMapper;
        this.dorisExecutor = dorisExecutor;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ErasureExecutionResult execute(Long tenantId, Long requestId, ErasureExecutionCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("erasure execution command is required");
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String subjectValue = required(command.subjectValue(), "subjectValue");
        CdpWarehousePrivacyErasureService.ErasureRequestView request =
                erasureService.get(scopedTenantId, requestId);
        String subjectType = normalize(request.subjectType());
        String expectedHash = subjectHash(scopedTenantId, subjectType, subjectValue);
        if (!expectedHash.equalsIgnoreCase(request.subjectHash())) {
            throw new IllegalArgumentException("subjectValue does not match erasure request subject hash");
        }

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpEventLogDO> matchingEvents = safe(eventLogMapper.selectList(eventQuery(scopedTenantId, subjectType, subjectValue)));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<Long> matchingEventIds = matchingEvents.stream()
                .map(CdpEventLogDO::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        List<String> userIds = resolveUserIds(scopedTenantId, subjectType, subjectValue);
        List<String> assets = selectedAssets(request, command.targetAssets());

        List<AssetExecutionResult> results = assets.stream()
                .map(asset -> executeAsset(scopedTenantId, requestId, subjectType, subjectValue,
                        command.dryRun(), actor(command.actor()), userIds, matchingEventIds, asset))
                .toList();
        return new ErasureExecutionResult(scopedTenantId, requestId, rollup(results), command.dryRun(), results);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @param dryRun dry run 参数，用于 executeAsset 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param userIds user ids 参数，用于 executeAsset 流程中的校验、计算或对象转换。
     * @param matchingEventIds matching event ids 参数，用于 executeAsset 流程中的校验、计算或对象转换。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    private AssetExecutionResult executeAsset(Long tenantId,
                                              Long requestId,
                                              String subjectType,
                                              String subjectValue,
                                              boolean dryRun,
                                              String actor,
                                              List<String> userIds,
                                              List<Long> matchingEventIds,
                                              String assetKey) {
        // 准备本次处理所需的上下文和中间变量。
        AssetExecutionResult result;
        try {
            result = switch (normalize(assetKey)) {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                case "CDP_USER_PROFILE" -> executeUserScoped(
                        tenantId,
                        assetKey,
                        dryRun,
                        userIds,
                        profileMapper,
                        new LambdaQueryWrapper<CdpUserProfileDO>()
                                .eq(CdpUserProfileDO::getTenantId, tenantId)
                                .in(!userIds.isEmpty(), CdpUserProfileDO::getUserId, userIds));
                case "CDP_USER_IDENTITY" -> executeIdentity(tenantId, subjectType, subjectValue, dryRun, userIds);
                case "CDP_USER_TAG" -> executeUserScoped(
                        tenantId,
                        assetKey,
                        dryRun,
                        userIds,
                        tagMapper,
                        new LambdaQueryWrapper<CdpUserTagDO>()
                                .eq(CdpUserTagDO::getTenantId, tenantId)
                                .in(!userIds.isEmpty(), CdpUserTagDO::getUserId, userIds));
                case "CDP_EVENT_LOG" -> executeEventLog(tenantId, subjectType, subjectValue, dryRun, matchingEventIds);
                case "REALTIME_RETRY_BUFFER" -> executeRealtimeRetry(tenantId, dryRun, matchingEventIds);
                case "DORIS_ODS_CDP_EVENT_LOG", "DORIS_DWD_CDP_USER_EVENT_FACT" ->
                        executeDoris(tenantId, subjectType, subjectValue, dryRun, actor, assetKey);
                case "AUDIENCE_BITMAP_VERSION" -> new AssetExecutionResult(
                        assetKey, WARN, 0, 0,
                        "audience bitmap erasure requires materialization rebuild proof", null);
                default -> new AssetExecutionResult(assetKey, WARN, 0, 0,
                        "asset has no executable erasure handler", null);
            };
        } catch (RuntimeException e) {
            result = new AssetExecutionResult(assetKey, FAIL, 0, 0,
                    "asset erasure failed", e.getMessage());
        }
        AssetExecutionResult safeResult = sanitize(result, subjectValue);
        recordProof(tenantId, requestId, dryRun, actor, safeResult);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return safeResult;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param dryRun dry run 参数，用于 executeUserScoped 流程中的校验、计算或对象转换。
     * @param userIds user ids 参数，用于 executeUserScoped 流程中的校验、计算或对象转换。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param query query 参数，用于 executeUserScoped 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private <T> AssetExecutionResult executeUserScoped(Long tenantId,
                                                       String assetKey,
                                                       boolean dryRun,
                                                       List<String> userIds,
                                                       BaseMapper<T> mapper,
                                                       LambdaQueryWrapper<T> query) {
        if (userIds.isEmpty()) {
            return new AssetExecutionResult(assetKey, SKIPPED, 0, 0,
                    "no user id resolved for subject", null);
        }
        return executeMapperDelete(assetKey, dryRun, mapper, query);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @param dryRun dry run 参数，用于 executeIdentity 流程中的校验、计算或对象转换。
     * @param userIds user ids 参数，用于 executeIdentity 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private AssetExecutionResult executeIdentity(Long tenantId,
                                                 String subjectType,
                                                 String subjectValue,
                                                 boolean dryRun,
                                                 List<String> userIds) {
        LambdaQueryWrapper<CdpUserIdentityDO> query = new LambdaQueryWrapper<CdpUserIdentityDO>()
                .eq(CdpUserIdentityDO::getTenantId, tenantId);
        if (USER_ID.equals(subjectType)) {
            if (userIds.isEmpty()) {
                return new AssetExecutionResult("CDP_USER_IDENTITY", SKIPPED, 0, 0,
                        "no user id resolved for subject", null);
            }
            query.in(CdpUserIdentityDO::getUserId, userIds);
        } else {
            query.eq(CdpUserIdentityDO::getIdentityType, subjectType)
                    .eq(CdpUserIdentityDO::getIdentityValue, subjectValue);
        }
        return executeMapperDelete("CDP_USER_IDENTITY", dryRun, identityMapper, query);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @param dryRun dry run 参数，用于 executeEventLog 流程中的校验、计算或对象转换。
     * @param matchingEventIds matching event ids 参数，用于 executeEventLog 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private AssetExecutionResult executeEventLog(Long tenantId,
                                                 String subjectType,
                                                 String subjectValue,
                                                 boolean dryRun,
                                                 List<Long> matchingEventIds) {
        long matched = matchingEventIds.size();
        int affected = 0;
        if (!dryRun && matched > 0) {
            affected = eventLogMapper.delete(eventQuery(tenantId, subjectType, subjectValue));
        }
        return new AssetExecutionResult("CDP_EVENT_LOG", status(dryRun), matched, affected,
                dryRun ? "dry-run matched CDP event rows" : "deleted CDP event rows", null);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dryRun dry run 参数，用于 executeRealtimeRetry 流程中的校验、计算或对象转换。
     * @param matchingEventIds matching event ids 参数，用于 executeRealtimeRetry 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private AssetExecutionResult executeRealtimeRetry(Long tenantId, boolean dryRun, List<Long> matchingEventIds) {
        if (matchingEventIds.isEmpty()) {
            return new AssetExecutionResult("REALTIME_RETRY_BUFFER", SKIPPED, 0, 0,
                    "no matching event log ids for retry buffer", null);
        }
        LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO> query =
                new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                        .eq(CdpWarehouseRealtimeRetryDO::getTenantId, tenantId)
                        .in(CdpWarehouseRealtimeRetryDO::getEventLogId, matchingEventIds);
        return executeMapperDelete("REALTIME_RETRY_BUFFER", dryRun, retryMapper, query);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param dryRun dry run 参数，用于 executeMapperDelete 流程中的校验、计算或对象转换。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param query query 参数，用于 executeMapperDelete 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private <T> AssetExecutionResult executeMapperDelete(String assetKey,
                                                         boolean dryRun,
                                                         BaseMapper<T> mapper,
                                                         LambdaQueryWrapper<T> query) {
        long matched = nullToZero(mapper.selectCount(query));
        int affected = 0;
        if (!dryRun && matched > 0) {
            affected = mapper.delete(query);
        }
        return new AssetExecutionResult(assetKey, status(dryRun), matched, affected,
                dryRun ? "dry-run matched rows for " + assetKey : "deleted rows for " + assetKey, null);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @param dryRun dry run 参数，用于 executeDoris 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    private AssetExecutionResult executeDoris(Long tenantId,
                                              String subjectType,
                                              String subjectValue,
                                              boolean dryRun,
                                              String actor,
                                              String assetKey) {
        if (dorisExecutor == null) {
            String message = "Doris privacy erasure executor is not configured";
            return new AssetExecutionResult(assetKey, dryRun ? WARN : FAIL, 0, 0,
                    dryRun ? "dry-run skipped Doris delete because executor is not configured" : "Doris delete unavailable",
                    message);
        }
        CdpWarehouseDorisPrivacyErasureExecutor.Result output = dorisExecutor.execute(
                new CdpWarehouseDorisPrivacyErasureExecutor.Command(
                        assetKey, tenantId, subjectType, subjectValue, dryRun, actor));
        return new AssetExecutionResult(
                assetKey,
                normalizeOutputStatus(output.status(), dryRun),
                output.matchedCount(),
                output.affectedCount(),
                output.proofMessage(),
                output.errorMessage());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param result result 参数，用于 sanitize 流程中的校验、计算或对象转换。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 sanitize 流程生成的业务结果。
     */
    private AssetExecutionResult sanitize(AssetExecutionResult result, String subjectValue) {
        if (result == null) {
            return null;
        }
        return new AssetExecutionResult(
                result.assetKey(),
                result.status(),
                result.matchedCount(),
                result.affectedCount(),
                redact(result.proofMessage(), subjectValue),
                redact(result.errorMessage(), subjectValue));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 redact 生成的文本或业务键。
     */
    private String redact(String value, String subjectValue) {
        if (value == null || subjectValue == null || subjectValue.isBlank()) {
            return value;
        }
        return value.replace(subjectValue, "[REDACTED_SUBJECT]");
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param dryRun dry run 参数，用于 recordProof 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param result result 参数，用于 recordProof 流程中的校验、计算或对象转换。
     */
    private void recordProof(Long tenantId,
                             Long requestId,
                             boolean dryRun,
                             String actor,
                             AssetExecutionResult result) {
        erasureService.recordAssetProof(tenantId, requestId,
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        result.assetKey(),
                        assetLayer(result.assetKey()),
                        dryRun ? "DRY_RUN" : "DELETE",
                        result.status(),
                        result.matchedCount(),
                        result.affectedCount(),
                        result.proofMessage(),
                        result.errorMessage(),
                        actor,
                        LocalDateTime.now()));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 resolve user ids 汇总后的集合、分页或映射视图。
     */
    private List<String> resolveUserIds(Long tenantId, String subjectType, String subjectValue) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (USER_ID.equals(subjectType)) {
            return List.of(subjectValue);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpUserIdentityDO> identities = safe(identityMapper.selectList(
                new LambdaQueryWrapper<CdpUserIdentityDO>()
                        .eq(CdpUserIdentityDO::getTenantId, tenantId)
                        .eq(CdpUserIdentityDO::getIdentityType, subjectType)
                        .eq(CdpUserIdentityDO::getIdentityValue, subjectValue)));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return identities.stream()
                .map(CdpUserIdentityDO::getUserId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 eventQuery 流程生成的业务结果。
     */
    private LambdaQueryWrapper<CdpEventLogDO> eventQuery(Long tenantId, String subjectType, String subjectValue) {
        LambdaQueryWrapper<CdpEventLogDO> query = new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId);
        switch (subjectType) {
            case "ANONYMOUS_ID" -> query.eq(CdpEventLogDO::getAnonymousId, subjectValue);
            case "DEVICE_ID" -> query.eq(CdpEventLogDO::getDeviceId, subjectValue);
            default -> query.eq(CdpEventLogDO::getUserId, subjectValue);
        }
        return query;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param requestedAssets requested assets 参数，用于 selectedAssets 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<String> selectedAssets(CdpWarehousePrivacyErasureService.ErasureRequestView request,
                                        List<String> requestedAssets) {
        Set<String> assets = new LinkedHashSet<>();
        List<String> input = requestedAssets == null || requestedAssets.isEmpty()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                ? request.assetProofs().stream()
                        .map(CdpWarehousePrivacyErasureService.AssetProofView::assetKey)
                        .toList()
                : requestedAssets;
        for (String asset : input) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (asset != null && !asset.isBlank()) {
                assets.add(normalize(asset));
            }
        }
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("no erasure assets selected");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(assets);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 subject hash 生成的文本或业务键。
     */
    private String subjectHash(Long tenantId, String subjectType, String subjectValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((tenantId + ":" + subjectType + ":" + subjectValue)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash erasure subject", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param results results 参数，用于 rollup 流程中的校验、计算或对象转换。
     * @return 返回 rollup 生成的文本或业务键。
     */
    private String rollup(List<AssetExecutionResult> results) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> statuses = results.stream().map(result -> normalize(result.status())).toList();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (statuses.contains(FAIL)) {
            return FAIL;
        }
        if (statuses.contains(WARN)) {
            return WARN;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dryRun dry run 参数，用于 status 流程中的校验、计算或对象转换。
     * @return 返回 status 生成的文本或业务键。
     */
    private String status(boolean dryRun) {
        return dryRun ? WARN : PASS;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param dryRun dry run 参数，用于 normalizeOutputStatus 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOutputStatus(String status, boolean dryRun) {
        String normalized = normalize(status);
        if (PASS.equals(normalized) || WARN.equals(normalized) || FAIL.equals(normalized) || SKIPPED.equals(normalized)) {
            return dryRun && PASS.equals(normalized) ? WARN : normalized;
        }
        return dryRun ? WARN : FAIL;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 asset layer 生成的文本或业务键。
     */
    private String assetLayer(String assetKey) {
        // 准备本次处理所需的上下文和中间变量。
        String asset = normalize(assetKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (asset.startsWith("DORIS_ODS")) {
            return "ODS";
        }
        if (asset.startsWith("DORIS_DWD")) {
            return "DWD";
        }
        if (asset.startsWith("DORIS_DWS")) {
            return "DWS";
        }
        if (asset.startsWith("AUDIENCE")) {
            return "ADS";
        }
        if (asset.startsWith("REALTIME")) {
            return "REALTIME";
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "CDP";
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safe 流程中的校验、计算或对象转换。
     * @return 返回 safe 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safe(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * ErasureExecutionCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ErasureExecutionCommand(
            String subjectValue,
            boolean dryRun,
            String actor,
            List<String> targetAssets) {
    }

    /**
     * ErasureExecutionResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ErasureExecutionResult(
            Long tenantId,
            Long requestId,
            String status,
            boolean dryRun,
            List<AssetExecutionResult> assetResults) {
        public ErasureExecutionResult {
            assetResults = assetResults == null ? List.of() : List.copyOf(assetResults);
        }
    }

    /**
     * AssetExecutionResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AssetExecutionResult(
            String assetKey,
            String status,
            long matchedCount,
            long affectedCount,
            String proofMessage,
            String errorMessage) {
    }
}
