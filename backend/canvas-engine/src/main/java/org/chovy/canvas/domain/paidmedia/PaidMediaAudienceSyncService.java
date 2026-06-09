package org.chovy.canvas.domain.paidmedia;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceDestinationDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceMemberDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceSyncRunDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceMemberMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceSyncRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * PaidMediaAudienceSyncService 编排 domain.paidmedia 场景的领域业务规则。
 */
@Service
public class PaidMediaAudienceSyncService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final PaidMediaAudienceDestinationMapper destinationMapper;
    private final PaidMediaAudienceMemberMapper memberMapper;
    private final PaidMediaAudienceSyncRunMapper runMapper;
    private final AudienceDefinitionMapper audienceMapper;
    private final CdpUserProfileMapper profileMapper;
    private final MarketingConsentMapper consentMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 PaidMediaAudienceSyncService 实例并注入 domain.paidmedia 场景依赖。
     * @param destinationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param memberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public PaidMediaAudienceSyncService(PaidMediaAudienceDestinationMapper destinationMapper,
                                        PaidMediaAudienceMemberMapper memberMapper,
                                        PaidMediaAudienceSyncRunMapper runMapper,
                                        AudienceDefinitionMapper audienceMapper,
                                        CdpUserProfileMapper profileMapper,
                                        MarketingConsentMapper consentMapper,
                                        ObjectMapper objectMapper) {
        this(destinationMapper, memberMapper, runMapper, audienceMapper, profileMapper, consentMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 PaidMediaAudienceSyncService 流程，围绕 paid media audience sync service 完成校验、计算或结果组装。
     *
     * @param destinationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param memberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    PaidMediaAudienceSyncService(PaidMediaAudienceDestinationMapper destinationMapper,
                                 PaidMediaAudienceMemberMapper memberMapper,
                                 PaidMediaAudienceSyncRunMapper runMapper,
                                 AudienceDefinitionMapper audienceMapper,
                                 CdpUserProfileMapper profileMapper,
                                 MarketingConsentMapper consentMapper,
                                 ObjectMapper objectMapper,
                                 Clock clock) {
        this.destinationMapper = destinationMapper;
        this.memberMapper = memberMapper;
        this.runMapper = runMapper;
        this.audienceMapper = audienceMapper;
        this.profileMapper = profileMapper;
        this.consentMapper = consentMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 新增或更新付费媒体人群同步目标。
     * 以 Provider 和 destinationKey 唯一定位，保存外部账户、人群 ID、标识类型、同意渠道和启用状态。
     */
    public PaidMediaAudienceDestinationView upsertDestination(Long tenantId,
                                                              PaidMediaDestinationCommand command,
                                                              String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("paid-media destination command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String destinationKey = required(command.destinationKey(), "destinationKey");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        PaidMediaAudienceDestinationDO row = destinationMapper.selectOne(
                new LambdaQueryWrapper<PaidMediaAudienceDestinationDO>()
                        .eq(PaidMediaAudienceDestinationDO::getTenantId, scopedTenantId)
                        .eq(PaidMediaAudienceDestinationDO::getProvider, provider)
                        .eq(PaidMediaAudienceDestinationDO::getDestinationKey, destinationKey)
                        .last("LIMIT 1"));
        if (row == null) {
            row = new PaidMediaAudienceDestinationDO();
            row.setTenantId(scopedTenantId);
            row.setProvider(provider);
            row.setDestinationKey(destinationKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setDisplayName(defaultString(command.displayName(), destinationKey));
        row.setAccountId(trimToNull(command.accountId()));
        row.setExternalAudienceId(trimToNull(command.externalAudienceId()));
        row.setIdentifierTypesJson(json(normalizeIdentifierTypes(command.identifierTypes())));
        row.setConsentChannel(normalizeText(command.consentChannel(), "PAID_MEDIA").toUpperCase(Locale.ROOT));
        row.setEnforceConsent(Boolean.FALSE.equals(command.enforceConsent()) ? 0 : 1);
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            destinationMapper.insert(row);
        } else {
            destinationMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toDestinationView(row);
    }

    /**
     * 为指定目标同步一批付费媒体人群成员。
     * 方法会创建运行记录，校验目标和人群租户归属，按用户同意状态筛选并写入成员明细；当前只落库同步结果，不直接调用媒体 API。
     */
    public PaidMediaAudienceSyncRunView syncAudience(Long tenantId,
                                                     PaidMediaAudienceSyncCommand command,
                                                     String actor) {
        if (command == null) {
            throw new IllegalArgumentException("paid-media audience sync command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        List<String> userIds = distinctUsers(command.userIds());
        PaidMediaAudienceDestinationDO destination = destinationMapper.selectById(requiredId(
                command.destinationId(), "destinationId"));
        String provider = destination == null ? "UNKNOWN" : defaultString(destination.getProvider(), "UNKNOWN");
        LocalDateTime startedAt = now();
        PaidMediaAudienceSyncRunDO run = insertRunningRun(scopedTenantId, provider, command, userIds.size(), actor,
                startedAt);
        try {
            validateDestination(scopedTenantId, destination);
            AudienceDefinitionDO audience = audienceMapper.selectById(requiredId(command.audienceId(), "audienceId"));
            validateAudience(scopedTenantId, audience);
            SyncCounters counters = processUsers(scopedTenantId, destination, command.audienceId(), run.getId(), userIds,
                    startedAt);
            completeRun(run, "SUCCESS", counters.eligibleUsers(), counters.skippedUsers(), 0,
                    command.externalOperationId(), null, now());
            return toRunView(run);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            completeRun(run, "FAILED", 0, 0, Math.max(1, userIds.size()), command.externalOperationId(),
                    ex.getMessage(), now());
            throw ex;
        }
    }

    /**
     * 查询租户内付费媒体人群同步运行记录。
     * 可按目标、人群和状态过滤，返回创建时间倒序的运行视图。
     */
    public List<PaidMediaAudienceSyncRunView> runs(Long tenantId, PaidMediaAudienceRunQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("paid-media run query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String status = normalizeOptionalUpper(query.status());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(runMapper.selectList(
                new LambdaQueryWrapper<PaidMediaAudienceSyncRunDO>()
                        .eq(PaidMediaAudienceSyncRunDO::getTenantId, scopedTenantId)
                        .eq(query.destinationId() != null, PaidMediaAudienceSyncRunDO::getDestinationId,
                                query.destinationId())
                        .eq(query.audienceId() != null, PaidMediaAudienceSyncRunDO::getAudienceId, query.audienceId())
                        .eq(status != null, PaidMediaAudienceSyncRunDO::getStatus, status)
                        .orderByDesc(PaidMediaAudienceSyncRunDO::getCreatedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.destinationId() == null || Objects.equals(query.destinationId(), row.getDestinationId()))
                .filter(row -> query.audienceId() == null || Objects.equals(query.audienceId(), row.getAudienceId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toRunView)
                .toList();
    }

    /**
     * 查询租户内付费媒体同步成员明细。
     * 可按运行批次和资格状态过滤，用于查看哪些用户被纳入或因同意策略被跳过。
     */
    public List<PaidMediaAudienceMemberView> members(Long tenantId, PaidMediaAudienceMemberQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("paid-media member query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String status = normalizeOptionalUpper(query.eligibilityStatus());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(memberMapper.selectList(
                new LambdaQueryWrapper<PaidMediaAudienceMemberDO>()
                        .eq(PaidMediaAudienceMemberDO::getTenantId, scopedTenantId)
                        .eq(query.runId() != null, PaidMediaAudienceMemberDO::getRunId, query.runId())
                        .eq(status != null, PaidMediaAudienceMemberDO::getEligibilityStatus, status)
                        .orderByDesc(PaidMediaAudienceMemberDO::getCreatedAt)
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.runId() == null || Objects.equals(query.runId(), row.getRunId()))
                .filter(row -> status == null || status.equals(row.getEligibilityStatus()))
                .limit(limit)
                .map(this::toMemberView)
                .toList();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param provider provider 参数，用于 insertRunningRun 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param requestedCount requested count 参数，用于 insertRunningRun 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insertRunningRun 流程生成的业务结果。
     */
    private PaidMediaAudienceSyncRunDO insertRunningRun(Long tenantId,
                                                       String provider,
                                                       PaidMediaAudienceSyncCommand command,
                                                       int requestedCount,
                                                       String actor,
                                                       LocalDateTime startedAt) {
        // 准备本次处理所需的上下文和中间变量。
        PaidMediaAudienceSyncRunDO row = new PaidMediaAudienceSyncRunDO();
        row.setTenantId(tenantId);
        row.setDestinationId(command.destinationId());
        row.setAudienceId(command.audienceId());
        row.setProvider(defaultString(provider, "UNKNOWN").toUpperCase(Locale.ROOT));
        row.setStatus("RUNNING");
        row.setRequestedCount(requestedCount);
        row.setEligibleCount(0);
        row.setSkippedCount(0);
        row.setFailedCount(0);
        row.setExternalOperationId(command.externalOperationId());
        row.setMetadataJson(json(command.metadata()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setStartedAt(startedAt);
        row.setCreatedAt(startedAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(startedAt);
        runMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param run run 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param eligibleCount eligible count 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param skippedCount skipped count 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param failedCount failed count 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param externalOperationId 业务对象 ID，用于定位具体记录。
     * @param errorMessage error message 参数，用于 completeRun 流程中的校验、计算或对象转换。
     * @param finishedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void completeRun(PaidMediaAudienceSyncRunDO run,
                             String status,
                             int eligibleCount,
                             int skippedCount,
                             int failedCount,
                             String externalOperationId,
                             String errorMessage,
                             LocalDateTime finishedAt) {
        run.setStatus(status);
        run.setEligibleCount(eligibleCount);
        run.setSkippedCount(skippedCount);
        run.setFailedCount(failedCount);
        run.setExternalOperationId(externalOperationId);
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(finishedAt);
        run.setUpdatedAt(finishedAt);
        runMapper.updateById(run);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param destination destination 参数，用于 processUsers 流程中的校验、计算或对象转换。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param runId 业务对象 ID，用于定位具体记录。
     * @param userIds user ids 参数，用于 processUsers 流程中的校验、计算或对象转换。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 processUsers 流程生成的业务结果。
     */
    private SyncCounters processUsers(Long tenantId,
                                      PaidMediaAudienceDestinationDO destination,
                                      Long audienceId,
                                      Long runId,
                                      List<String> userIds,
                                      LocalDateTime syncedAt) {
        int eligibleUsers = 0;
        int skippedUsers = 0;
        List<String> identifierTypes = list(destination.getIdentifierTypesJson());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String userId : userIds) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            CdpUserProfileDO profile = profileMapper.selectOne(
                    new LambdaQueryWrapper<CdpUserProfileDO>()
                            .eq(CdpUserProfileDO::getTenantId, tenantId)
                            .eq(CdpUserProfileDO::getUserId, userId)
                            .last("LIMIT 1"));
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (profile == null || !tenantId.equals(profile.getTenantId())) {
                insertSkippedMember(tenantId, destination, audienceId, runId, userId, "PROFILE_NOT_FOUND", syncedAt);
                skippedUsers++;
                continue;
            }
            if (enabled(destination.getEnforceConsent()) && !hasConsent(tenantId, userId,
                    destination.getConsentChannel())) {
                insertSkippedMember(tenantId, destination, audienceId, runId, userId, "CONSENT_DENIED", syncedAt);
                skippedUsers++;
                continue;
            }
            List<Identifier> identifiers = identifiers(profile, identifierTypes);
            if (identifiers.isEmpty()) {
                insertSkippedMember(tenantId, destination, audienceId, runId, userId, "MISSING_IDENTIFIER", syncedAt);
                skippedUsers++;
                continue;
            }
            for (Identifier identifier : identifiers) {
                insertMember(tenantId, destination, audienceId, runId, userId, identifier.type(),
                        sha256(identifier.value()), "ELIGIBLE", null, syncedAt);
            }
            eligibleUsers++;
        }
        return new SyncCounters(eligibleUsers, skippedUsers);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param consentChannel consent channel 参数，用于 hasConsent 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasConsent(Long tenantId, String userId, String consentChannel) {
        MarketingConsentDO row = consentMapper.selectOne(
                new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, tenantId)
                        .eq(MarketingConsentDO::getUserId, userId)
                        .eq(MarketingConsentDO::getChannel, consentChannel)
                        .last("LIMIT 1"));
        return row != null
                && tenantId.equals(row.getTenantId())
                && MarketingConsentDO.OPT_IN.equals(row.getConsentStatus());
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param destination destination 参数，用于 insertSkippedMember 流程中的校验、计算或对象转换。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param runId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void insertSkippedMember(Long tenantId,
                                     PaidMediaAudienceDestinationDO destination,
                                     Long audienceId,
                                     Long runId,
                                     String userId,
                                     String reason,
                                     LocalDateTime syncedAt) {
        insertMember(tenantId, destination, audienceId, runId, userId, "UNKNOWN", null, "SKIPPED", reason, syncedAt);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param destination destination 参数，用于 insertMember 流程中的校验、计算或对象转换。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param runId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param identifierType 类型标识，用于选择对应处理分支。
     * @param identifierHash identifier hash 参数，用于 insertMember 流程中的校验、计算或对象转换。
     * @param eligibilityStatus 业务状态，用于筛选或推进状态流转。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void insertMember(Long tenantId,
                              PaidMediaAudienceDestinationDO destination,
                              Long audienceId,
                              Long runId,
                              String userId,
                              String identifierType,
                              String identifierHash,
                              String eligibilityStatus,
                              String reason,
                              LocalDateTime syncedAt) {
        PaidMediaAudienceMemberDO row = new PaidMediaAudienceMemberDO();
        row.setTenantId(tenantId);
        row.setRunId(runId);
        row.setDestinationId(destination.getId());
        row.setAudienceId(audienceId);
        row.setProvider(destination.getProvider());
        row.setUserId(userId);
        row.setIdentifierType(identifierType);
        row.setIdentifierHash(identifierHash);
        row.setEligibilityStatus(eligibilityStatus);
        row.setReason(reason);
        row.setSyncedAt(syncedAt);
        row.setCreatedAt(syncedAt);
        row.setUpdatedAt(syncedAt);
        memberMapper.insert(row);
    }

    /**
     * 执行 identifiers 流程，围绕 identifiers 完成校验、计算或结果组装。
     *
     * @param profile profile 参数，用于 identifiers 流程中的校验、计算或对象转换。
     * @param identifierTypes identifier types 参数，用于 identifiers 流程中的校验、计算或对象转换。
     * @return 返回 identifiers 汇总后的集合、分页或映射视图。
     */
    private List<Identifier> identifiers(CdpUserProfileDO profile, List<String> identifierTypes) {
        List<Identifier> identifiers = new ArrayList<>();
        for (String type : identifierTypes) {
            if ("EMAIL".equals(type)) {
                String email = normalizeEmail(profile.getEmail());
                if (email != null) {
                    identifiers.add(new Identifier(type, email));
                }
            // 根据前序判断结果进入后续条件分支。
            } else if ("PHONE".equals(type)) {
                String phone = normalizePhone(profile.getPhone());
                if (phone != null) {
                    identifiers.add(new Identifier(type, phone));
                }
            }
        }
        return identifiers;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param destination destination 参数，用于 validateDestination 流程中的校验、计算或对象转换。
     */
    private void validateDestination(Long tenantId, PaidMediaAudienceDestinationDO destination) {
        if (destination == null || !tenantId.equals(destination.getTenantId())) {
            throw new IllegalArgumentException("paid-media destination is not found");
        }
        if (!enabled(destination.getEnabled())) {
            throw new IllegalStateException("paid-media destination is disabled");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audience audience 参数，用于 validateAudience 流程中的校验、计算或对象转换。
     */
    private void validateAudience(Long tenantId, AudienceDefinitionDO audience) {
        if (audience == null || !tenantId.equals(audience.getTenantId())) {
            throw new IllegalArgumentException("audience is not found");
        }
        if (!enabled(audience.getEnabled())) {
            throw new IllegalStateException("audience is disabled");
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private PaidMediaAudienceDestinationView toDestinationView(PaidMediaAudienceDestinationDO row) {
        return new PaidMediaAudienceDestinationView(
                row.getId(),
                row.getTenantId(),
                row.getProvider(),
                row.getDestinationKey(),
                row.getDisplayName(),
                row.getAccountId(),
                row.getExternalAudienceId(),
                list(row.getIdentifierTypesJson()),
                row.getConsentChannel(),
                enabled(row.getEnforceConsent()),
                enabled(row.getEnabled()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private PaidMediaAudienceSyncRunView toRunView(PaidMediaAudienceSyncRunDO row) {
        return new PaidMediaAudienceSyncRunView(
                row.getId(),
                row.getTenantId(),
                row.getDestinationId(),
                row.getAudienceId(),
                row.getProvider(),
                row.getStatus(),
                defaultInt(row.getRequestedCount()),
                defaultInt(row.getEligibleCount()),
                defaultInt(row.getSkippedCount()),
                defaultInt(row.getFailedCount()),
                row.getExternalOperationId(),
                row.getErrorMessage(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getStartedAt(),
                row.getFinishedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private PaidMediaAudienceMemberView toMemberView(PaidMediaAudienceMemberDO row) {
        return new PaidMediaAudienceMemberView(
                row.getId(),
                row.getTenantId(),
                row.getRunId(),
                row.getDestinationId(),
                row.getAudienceId(),
                row.getProvider(),
                row.getUserId(),
                row.getIdentifierType(),
                row.getIdentifierHash(),
                row.getEligibilityStatus(),
                row.getReason(),
                row.getSyncedAt());
    }

    /**
     * 规范化输入值。
     *
     * @param values values 参数，用于 normalizeIdentifierTypes 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizeIdentifierTypes(List<String> values) {
        Set<String> types = new LinkedHashSet<>();
        for (String value : values == null ? List.<String>of() : values) {
            if (hasText(value)) {
                types.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        return types.isEmpty() ? List.of("EMAIL", "PHONE") : List.copyOf(types);
    }

    /**
     * 执行 distinctUsers 流程，围绕 distinct users 完成校验、计算或结果组装。
     *
     * @param userIds user ids 参数，用于 distinctUsers 流程中的校验、计算或对象转换。
     * @return 返回 distinct users 汇总后的集合、分页或映射视图。
     */
    private List<String> distinctUsers(List<String> userIds) {
        Set<String> users = new LinkedHashSet<>();
        for (String userId : userIds == null ? List.<String>of() : userIds) {
            if (hasText(userId)) {
                users.add(userId.trim());
            }
        }
        return List.copyOf(users);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("paid-media audience sync JSON serialization failed", ex);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<String> list(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeEmail(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePhone(String value) {
        if (!hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value == null || value == 1;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private Long requiredId(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeText 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default int 计算得到的数量、金额或指标值。
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * Identifier 数据记录。
     */
    private record Identifier(String type, String value) {
    }

    /**
     * SyncCounters 数据记录。
     */
    private record SyncCounters(int eligibleUsers, int skippedUsers) {
    }
}
