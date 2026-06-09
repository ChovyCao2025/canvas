package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingAssetDO;
import org.chovy.canvas.dal.dataobject.MarketingContentAuditEventDO;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryDO;
import org.chovy.canvas.dal.dataobject.MarketingContentReleaseDO;
import org.chovy.canvas.dal.dataobject.MarketingContentReleaseItemDO;
import org.chovy.canvas.dal.dataobject.MarketingContentTemplateDO;
import org.chovy.canvas.dal.mapper.MarketingAssetMapper;
import org.chovy.canvas.dal.mapper.MarketingContentAuditEventMapper;
import org.chovy.canvas.dal.mapper.MarketingContentEntryMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseItemMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseMapper;
import org.chovy.canvas.dal.mapper.MarketingContentTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
/**
 * MarketingContentReleaseService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class MarketingContentReleaseService {

    private static final Set<String> SOURCE_TYPES = Set.of("TEMPLATE", "ENTRY");

    private final MarketingContentReleaseMapper releaseMapper;
    private final MarketingContentReleaseItemMapper itemMapper;
    private final MarketingContentAuditEventMapper auditMapper;
    private final MarketingContentTemplateMapper templateMapper;
    private final MarketingContentEntryMapper entryMapper;
    private final MarketingAssetMapper assetMapper;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 MarketingContentReleaseService 实例。
     *
     * @param releaseMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param itemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param templateMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param entryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param assetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingContentReleaseService(MarketingContentReleaseMapper releaseMapper,
                                          MarketingContentReleaseItemMapper itemMapper,
                                          MarketingContentAuditEventMapper auditMapper,
                                          MarketingContentTemplateMapper templateMapper,
                                          MarketingContentEntryMapper entryMapper,
                                          MarketingAssetMapper assetMapper,
                                          ObjectMapper objectMapper) {
        this.releaseMapper = releaseMapper;
        this.itemMapper = itemMapper;
        this.auditMapper = auditMapper;
        this.templateMapper = templateMapper;
        this.entryMapper = entryMapper;
        this.assetMapper = assetMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenant tenant 参数，用于 validate 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回布尔判断结果。
     */
    public ValidationResult validate(TenantContext tenant, ValidationCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        SourceSnapshot source = loadSource(tenantId, command.sourceType(), command.sourceKey());
        List<ReleaseBlocker> blockers = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (source.row() == null) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), "source not found"));
            return new ValidationResult(false, List.copyOf(blockers), List.of());
        }
        if ("TEMPLATE".equals(source.sourceType()) && !"APPROVED".equals(source.status())) {
            blockers.add(new ReleaseBlocker("TEMPLATE", source.sourceKey(), "template is not approved"));
        }
        if ("ENTRY".equals(source.sourceType()) && !"PUBLISHED".equals(source.status())) {
            blockers.add(new ReleaseBlocker("ENTRY", source.sourceKey(), "entry is not published"));
        }
        validateSourceShape(source, blockers);
        List<String> assetRefs = List.of();
        try {
            assetRefs = assetRefs(source.assetRefsJson());
        } catch (IllegalArgumentException ex) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), ex.getMessage()));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String assetKey : assetRefs) {
            MarketingAssetDO asset = findAsset(tenantId, assetKey);
            if (asset == null) {
                blockers.add(new ReleaseBlocker("ASSET", assetKey, "asset not found"));
                continue;
            }
            if (!"READY".equals(asset.getStatus())) {
                blockers.add(new ReleaseBlocker("ASSET", assetKey, "asset is not ready"));
            }
            String transcodeStatus = asset.getTranscodeStatus();
            if ("VIDEO".equals(asset.getAssetType())
                    && !"READY".equals(transcodeStatus)
                    && !"EXTERNAL".equals(transcodeStatus)) {
                blockers.add(new ReleaseBlocker("ASSET", assetKey, "video transcode is not ready"));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ValidationResult(blockers.isEmpty(), List.copyOf(blockers), assetRefs);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenant tenant 参数，用于 publish 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ReleaseView publish(TenantContext tenant, ReleaseCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String actor = MarketingContentSupport.operator(tenant, command.createdBy());
        ValidationResult validation = validate(tenant, new ValidationCommand(command.sourceType(), command.sourceKey()));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!validation.ready()) {
            throw new IllegalStateException("content release gate failed: " + validation.blockers());
        }
        SourceSnapshot source = loadSource(tenantId, command.sourceType(), command.sourceKey());
        String releaseKey = releaseKey(source.sourceType(), source.sourceKey());
        int version = nextVersion(tenantId, releaseKey);
        MarketingContentReleaseDO previousActive = latestActiveRelease(tenantId, releaseKey);
        String snapshotJson = snapshotJson(source);
        String assetRefsJson = writeJson(validation.assetRefs(), "assetRefs");
        String checksum = sha256(snapshotJson + assetRefsJson);

        MarketingContentReleaseDO release = new MarketingContentReleaseDO();
        release.setTenantId(tenantId);
        release.setReleaseKey(releaseKey);
        release.setSourceType(source.sourceType());
        release.setSourceKey(source.sourceKey());
        release.setSourceVersion(version);
        release.setChannel(source.channel());
        release.setStatus("ACTIVE");
        release.setSnapshotJson(snapshotJson);
        release.setAssetRefsJson(assetRefsJson);
        release.setChecksumSha256(checksum);
        release.setCreatedBy(actor);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        release.setPublishedAt(LocalDateTime.now());
        releaseMapper.insert(release);

        supersedePreviousActiveRelease(tenantId, previousActive, actor, command.note());
        writeSourceItem(tenantId, release, source, snapshotJson);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String assetKey : validation.assetRefs()) {
            MarketingAssetDO asset = findAsset(tenantId, assetKey);
            writeAssetItem(tenantId, release, asset);
            incrementReferenceCount(asset);
        }
        writeAudit(tenantId, "RELEASE_PUBLISHED", "RELEASE", releaseKey, actor, null,
                toMap(release), command.note());
        return toView(release);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenant tenant 参数，用于 list 流程中的校验、计算或对象转换。
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @param sourceKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<ReleaseView> list(TenantContext tenant, String sourceType, String sourceKey, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentReleaseDO> query = new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .orderByDesc(MarketingContentReleaseDO::getPublishedAt)
                .orderByDesc(MarketingContentReleaseDO::getId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (MarketingContentSupport.hasText(sourceType)) {
            query.eq(MarketingContentReleaseDO::getSourceType, normalizeSourceType(sourceType));
        }
        if (MarketingContentSupport.hasText(sourceKey)) {
            query.eq(MarketingContentReleaseDO::getSourceKey,
                    MarketingContentSupport.normalizeKey(sourceKey, "sourceKey"));
        }
        if (MarketingContentSupport.hasText(status)) {
            query.eq(MarketingContentReleaseDO::getStatus, status.trim().toUpperCase());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return releaseMapper.selectList(query).stream().map(this::toView).toList();
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenant tenant 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @param releaseKey 业务键，用于在同一租户下定位资源。
     * @param MapString map string 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 resolve 流程生成的业务结果。
     */
    public ResolvedRelease resolve(TenantContext tenant, String releaseKey, Map<String, Object> context) {
        // 准备本次处理所需的上下文和中间变量。
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        MarketingContentReleaseDO release = requireActiveRelease(tenantId, releaseKey);
        String renderedSubject = null;
        String renderedBody = null;
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("TEMPLATE".equals(release.getSourceType())) {
            JsonNode node = readJson(release.getSnapshotJson(), "snapshotJson");
            renderedSubject = MarketingContentSupport.render(text(node, "subject"), context, missing);
            renderedBody = MarketingContentSupport.render(text(node, "body"), context, missing);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ResolvedRelease(
                release.getReleaseKey(),
                release.getSourceType(),
                release.getSourceKey(),
                release.getSourceVersion(),
                release.getStatus(),
                renderedSubject,
                renderedBody,
                List.copyOf(missing),
                release.getSnapshotJson(),
                releaseItems(tenantId, release.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 rollback 流程中的校验、计算或对象转换。
     * @param releaseKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 rollback 流程生成的业务结果。
     */
    public ReleaseView rollback(TenantContext tenant, String releaseKey, RollbackCommand command) {
        // 准备本次处理所需的上下文和中间变量。
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String actor = MarketingContentSupport.operator(tenant, command == null ? null : command.actor());
        MarketingContentReleaseDO release = requireActiveRelease(tenantId, releaseKey);
        Map<String, Object> oldActiveValue = toMap(release);
        MarketingContentReleaseDO restoreTarget = latestRestorableRelease(tenantId, release);
        Map<String, Object> oldRestoreValue = restoreTarget == null ? null : toMap(restoreTarget);
        release.setStatus("ROLLED_BACK");
        release.setRollbackReason(MarketingContentSupport.trimToNull(command == null ? null : command.reason()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        releaseMapper.updateById(release);
        writeAudit(tenantId, "RELEASE_ROLLED_BACK", "RELEASE", release.getReleaseKey(), actor,
                oldActiveValue,
                toMap(release),
                release.getRollbackReason());
        if (restoreTarget == null) {
            return toView(release);
        }
        restoreTarget.setStatus("ACTIVE");
        restoreTarget.setRollbackReason(null);
        releaseMapper.updateById(restoreTarget);
        writeAudit(tenantId, "RELEASE_RESTORED", "RELEASE", restoreTarget.getReleaseKey(), actor,
                oldRestoreValue,
                toMap(restoreTarget),
                release.getRollbackReason());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(restoreTarget);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 auditEvents 流程中的校验、计算或对象转换。
     * @param targetType 类型标识，用于选择对应处理分支。
     * @param targetKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 audit events 汇总后的集合、分页或映射视图。
     */
    public List<AuditEventView> auditEvents(TenantContext tenant, String targetType, String targetKey, Integer limit) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentAuditEventDO> query = new LambdaQueryWrapper<MarketingContentAuditEventDO>()
                .eq(MarketingContentAuditEventDO::getTenantId, tenantId)
                .orderByDesc(MarketingContentAuditEventDO::getCreatedAt)
                .orderByDesc(MarketingContentAuditEventDO::getId)
                .last("LIMIT " + Math.min(Math.max(limit == null ? 50 : limit, 1), 200));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (MarketingContentSupport.hasText(targetType)) {
            query.eq(MarketingContentAuditEventDO::getTargetType, targetType.trim().toUpperCase());
        }
        if (MarketingContentSupport.hasText(targetKey)) {
            query.eq(MarketingContentAuditEventDO::getTargetKey, MarketingContentSupport.normalizeKey(targetKey, "targetKey"));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return auditMapper.selectList(query).stream()
                .map(row -> new AuditEventView(
                        row.getEventType(),
                        row.getTargetType(),
                        row.getTargetKey(),
                        row.getActor(),
                        row.getNote(),
                        row.getCreatedAt()))
                .toList();
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sourceTypeValue 待处理值，用于规则计算或转换。
     * @param sourceKeyValue 待处理值，用于规则计算或转换。
     * @return 返回 loadSource 流程生成的业务结果。
     */
    private SourceSnapshot loadSource(Long tenantId, String sourceTypeValue, String sourceKeyValue) {
        String sourceType = normalizeSourceType(sourceTypeValue);
        String sourceKey = MarketingContentSupport.normalizeKey(sourceKeyValue, "sourceKey");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("TEMPLATE".equals(sourceType)) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingContentTemplateDO row = templateMapper.selectOne(new LambdaQueryWrapper<MarketingContentTemplateDO>()
                    .eq(MarketingContentTemplateDO::getTenantId, tenantId)
                    .eq(MarketingContentTemplateDO::getTemplateKey, sourceKey)
                    .last("LIMIT 1"));
            if (row == null) {
                return SourceSnapshot.empty(sourceType, sourceKey);
            }
            return new SourceSnapshot(sourceType, sourceKey, row.getStatus(), row.getChannel(), row.getAssetRefsJson(),
                    null, row);
        }
        MarketingContentEntryDO row = entryMapper.selectOne(new LambdaQueryWrapper<MarketingContentEntryDO>()
                .eq(MarketingContentEntryDO::getTenantId, tenantId)
                .eq(MarketingContentEntryDO::getEntryKey, sourceKey)
                .last("LIMIT 1"));
        if (row == null) {
            return SourceSnapshot.empty(sourceType, sourceKey);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SourceSnapshot(sourceType, sourceKey, row.getStatus(), "WEB", row.getAssetRefsJson(),
                null, row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingAssetDO findAsset(Long tenantId, String assetKey) {
        return assetMapper.selectOne(new LambdaQueryWrapper<MarketingAssetDO>()
                .eq(MarketingAssetDO::getTenantId, tenantId)
                .eq(MarketingAssetDO::getAssetKey, assetKey)
                .last("LIMIT 1"));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param releaseKey 业务键，用于在同一租户下定位资源。
     * @return 返回 requireActiveRelease 流程生成的业务结果。
     */
    private MarketingContentReleaseDO requireActiveRelease(Long tenantId, String releaseKey) {
        MarketingContentReleaseDO release = releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, MarketingContentSupport.normalizeKey(releaseKey, "releaseKey"))
                .eq(MarketingContentReleaseDO::getStatus, "ACTIVE")
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
        if (release == null) {
            throw new IllegalArgumentException("active release not found: " + releaseKey);
        }
        return release;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param releaseKey 业务键，用于在同一租户下定位资源。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private int nextVersion(Long tenantId, String releaseKey) {
        MarketingContentReleaseDO latest = releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, releaseKey)
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getSourceVersion() == null ? 1 : latest.getSourceVersion() + 1;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param releaseKey 业务键，用于在同一租户下定位资源。
     * @return 返回 latestActiveRelease 流程生成的业务结果。
     */
    private MarketingContentReleaseDO latestActiveRelease(Long tenantId, String releaseKey) {
        return releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, releaseKey)
                .eq(MarketingContentReleaseDO::getStatus, "ACTIVE")
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param release release 参数，用于 latestRestorableRelease 流程中的校验、计算或对象转换。
     * @return 返回 latestRestorableRelease 流程生成的业务结果。
     */
    private MarketingContentReleaseDO latestRestorableRelease(Long tenantId, MarketingContentReleaseDO release) {
        return releaseMapper.selectOne(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                .eq(MarketingContentReleaseDO::getTenantId, tenantId)
                .eq(MarketingContentReleaseDO::getReleaseKey, release.getReleaseKey())
                .eq(MarketingContentReleaseDO::getStatus, "SUPERSEDED")
                .lt(MarketingContentReleaseDO::getSourceVersion, release.getSourceVersion())
                .orderByDesc(MarketingContentReleaseDO::getSourceVersion)
                .last("LIMIT 1"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param previousActive previous active 参数，用于 supersedePreviousActiveRelease 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param note note 参数，用于 supersedePreviousActiveRelease 流程中的校验、计算或对象转换。
     */
    private void supersedePreviousActiveRelease(Long tenantId,
                                                MarketingContentReleaseDO previousActive,
                                                String actor,
                                                String note) {
        if (previousActive == null) {
            return;
        }
        Map<String, Object> oldValue = toMap(previousActive);
        previousActive.setStatus("SUPERSEDED");
        releaseMapper.updateById(previousActive);
        writeAudit(tenantId, "RELEASE_SUPERSEDED", "RELEASE", previousActive.getReleaseKey(), actor,
                oldValue,
                toMap(previousActive),
                note);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetRefsJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 asset refs 汇总后的集合、分页或映射视图。
     */
    private List<String> assetRefs(String assetRefsJson) {
        JsonNode node = readJson(MarketingContentSupport.hasText(assetRefsJson) ? assetRefsJson : "[]", "assetRefsJson");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!node.isArray()) {
            throw new IllegalArgumentException("assetRefsJson must be a JSON array");
        }
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (JsonNode item : node) {
            if (item.isTextual()) {
                refs.add(MarketingContentSupport.normalizeKey(item.asText(), "assetKey"));
            } else if (item.isObject() && item.hasNonNull("assetKey")) {
                refs.add(MarketingContentSupport.normalizeKey(item.get("assetKey").asText(), "assetKey"));
            } else {
                throw new IllegalArgumentException("assetRefsJson items must be strings or objects with assetKey");
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(refs);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param source source 参数，用于 validateSourceShape 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 validateSourceShape 流程中的校验、计算或对象转换。
     */
    private void validateSourceShape(SourceSnapshot source, List<ReleaseBlocker> blockers) {
        if (source.row() instanceof MarketingContentTemplateDO row) {
            requireJsonObject(row.getDesignJson(), "designJson", source, blockers);
            requireJsonArray(row.getVariablesJson(), "variablesJson", source, blockers);
            return;
        }
        if (source.row() instanceof MarketingContentEntryDO row) {
            requireJsonObject(row.getBodyJson(), "bodyJson", source, blockers);
            requireJsonObject(row.getSeoJson(), "seoJson", source, blockers);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param source source 参数，用于 requireJsonObject 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 requireJsonObject 流程中的校验、计算或对象转换。
     */
    private void requireJsonObject(String value,
                                   String field,
                                   SourceSnapshot source,
                                   List<ReleaseBlocker> blockers) {
        JsonNode node = readJsonForGate(value, field, source, blockers);
        if (node != null && !node.isObject()) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), field + " must be a JSON object"));
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param source source 参数，用于 requireJsonArray 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 requireJsonArray 流程中的校验、计算或对象转换。
     */
    private void requireJsonArray(String value,
                                  String field,
                                  SourceSnapshot source,
                                  List<ReleaseBlocker> blockers) {
        JsonNode node = readJsonForGate(value, field, source, blockers);
        if (node != null && !node.isArray()) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), field + " must be a JSON array"));
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param source source 参数，用于 readJsonForGate 流程中的校验、计算或对象转换。
     * @param blockers blockers 参数，用于 readJsonForGate 流程中的校验、计算或对象转换。
     * @return 返回 readJsonForGate 流程生成的业务结果。
     */
    private JsonNode readJsonForGate(String value,
                                     String field,
                                     SourceSnapshot source,
                                     List<ReleaseBlocker> blockers) {
        try {
            return objectMapper.readTree(MarketingContentSupport.hasText(value) ? value : "");
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            blockers.add(new ReleaseBlocker(source.sourceType(), source.sourceKey(), field + " must be valid JSON"));
            return null;
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param release release 参数，用于 writeSourceItem 流程中的校验、计算或对象转换。
     * @param source source 参数，用于 writeSourceItem 流程中的校验、计算或对象转换。
     * @param snapshotJson JSON 字符串，承载结构化配置或明细。
     */
    private void writeSourceItem(Long tenantId,
                                 MarketingContentReleaseDO release,
                                 SourceSnapshot source,
                                 String snapshotJson) {
        MarketingContentReleaseItemDO item = new MarketingContentReleaseItemDO();
        item.setTenantId(tenantId);
        item.setReleaseId(release.getId());
        item.setItemType(source.sourceType());
        item.setItemKey(source.sourceKey());
        item.setItemStatus(source.status());
        item.setSnapshotJson(snapshotJson);
        itemMapper.insert(item);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param release release 参数，用于 writeAssetItem 流程中的校验、计算或对象转换。
     * @param asset asset 参数，用于 writeAssetItem 流程中的校验、计算或对象转换。
     */
    private void writeAssetItem(Long tenantId, MarketingContentReleaseDO release, MarketingAssetDO asset) {
        MarketingContentReleaseItemDO item = new MarketingContentReleaseItemDO();
        item.setTenantId(tenantId);
        item.setReleaseId(release.getId());
        item.setItemType("ASSET");
        item.setItemKey(asset.getAssetKey());
        item.setItemStatus(asset.getStatus());
        item.setSnapshotJson(assetSnapshot(asset));
        itemMapper.insert(item);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param asset asset 参数，用于 incrementReferenceCount 流程中的校验、计算或对象转换。
     */
    private void incrementReferenceCount(MarketingAssetDO asset) {
        asset.setReferenceCount((asset.getReferenceCount() == null ? 0 : asset.getReferenceCount()) + 1);
        assetMapper.updateById(asset);
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param releaseId 业务对象 ID，用于定位具体记录。
     * @return 返回 release items 汇总后的集合、分页或映射视图。
     */
    private List<ResolvedAsset> releaseItems(Long tenantId, Long releaseId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (releaseId == null) {
            return List.of();
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return itemMapper.selectList(new LambdaQueryWrapper<MarketingContentReleaseItemDO>()
                        .eq(MarketingContentReleaseItemDO::getTenantId, tenantId)
                        .eq(MarketingContentReleaseItemDO::getReleaseId, releaseId)
                        .eq(MarketingContentReleaseItemDO::getItemType, "ASSET"))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(item -> new ResolvedAsset(item.getItemKey(), item.getItemStatus(), item.getSnapshotJson()))
                .toList();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param targetType 类型标识，用于选择对应处理分支。
     * @param targetKey 业务键，用于在同一租户下定位资源。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param oldValue 待处理值，用于规则计算或转换。
     * @param newValue 待处理值，用于规则计算或转换。
     * @param note note 参数，用于 writeAudit 流程中的校验、计算或对象转换。
     */
    private void writeAudit(Long tenantId,
                            String eventType,
                            String targetType,
                            String targetKey,
                            String actor,
                            Object oldValue,
                            Object newValue,
                            String note) {
        MarketingContentAuditEventDO row = new MarketingContentAuditEventDO();
        row.setTenantId(tenantId);
        row.setEventType(eventType);
        row.setTargetType(targetType);
        row.setTargetKey(targetKey);
        row.setActor(actor);
        row.setOldValueJson(oldValue == null ? null : writeJson(oldValue, "oldValue"));
        row.setNewValueJson(newValue == null ? null : writeJson(newValue, "newValue"));
        row.setNote(MarketingContentSupport.trimToNull(note));
        auditMapper.insert(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 template snapshot 生成的文本或业务键。
     */
    private String templateSnapshot(MarketingContentTemplateDO row) {
        return writeJson(Map.of(
                "templateKey", row.getTemplateKey(),
                "displayName", row.getDisplayName(),
                "channel", row.getChannel(),
                "subject", row.getSubject() == null ? "" : row.getSubject(),
                "body", row.getBody(),
                "designJson", readJson(row.getDesignJson(), "designJson"),
                "assetRefsJson", readJson(row.getAssetRefsJson(), "assetRefsJson"),
                "variablesJson", readJson(row.getVariablesJson(), "variablesJson"),
                "status", row.getStatus()), "template snapshot");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 entry snapshot 生成的文本或业务键。
     */
    private String entrySnapshot(MarketingContentEntryDO row) {
        return writeJson(Map.of(
                "entryKey", row.getEntryKey(),
                "contentType", row.getContentType(),
                "title", row.getTitle(),
                "slug", row.getSlug() == null ? "" : row.getSlug(),
                "locale", row.getLocale() == null ? "" : row.getLocale(),
                "summary", row.getSummary() == null ? "" : row.getSummary(),
                "bodyJson", readJson(row.getBodyJson(), "bodyJson"),
                "seoJson", readJson(row.getSeoJson(), "seoJson"),
                "assetRefsJson", readJson(row.getAssetRefsJson(), "assetRefsJson"),
                "status", row.getStatus()), "entry snapshot");
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param source source 参数，用于 snapshotJson 流程中的校验、计算或对象转换。
     * @return 返回 snapshot json 生成的文本或业务键。
     */
    private String snapshotJson(SourceSnapshot source) {
        if (source.row() instanceof MarketingContentTemplateDO row) {
            return templateSnapshot(row);
        }
        if (source.row() instanceof MarketingContentEntryDO row) {
            return entrySnapshot(row);
        }
        return "{}";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 asset snapshot 生成的文本或业务键。
     */
    private String assetSnapshot(MarketingAssetDO row) {
        return writeJson(Map.of(
                "assetKey", row.getAssetKey(),
                "name", row.getName(),
                "assetType", row.getAssetType(),
                "mimeType", row.getMimeType(),
                "storageUrl", row.getStorageUrl(),
                "status", row.getStatus(),
                "transcodeStatus", row.getTranscodeStatus() == null ? "" : row.getTranscodeStatus(),
                "posterUrl", row.getPosterUrl() == null ? "" : row.getPosterUrl()), "asset snapshot");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> toMap(MarketingContentReleaseDO row) {
        return Map.of(
                "releaseKey", row.getReleaseKey(),
                "sourceType", row.getSourceType(),
                "sourceKey", row.getSourceKey(),
                "sourceVersion", row.getSourceVersion(),
                "status", row.getStatus(),
                "checksumSha256", row.getChecksumSha256());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ReleaseView toView(MarketingContentReleaseDO row) {
        return new ReleaseView(
                row.getReleaseKey(),
                row.getSourceType(),
                row.getSourceKey(),
                row.getSourceVersion(),
                row.getChannel(),
                row.getStatus(),
                row.getChecksumSha256());
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @param sourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回 release key 生成的文本或业务键。
     */
    private String releaseKey(String sourceType, String sourceKey) {
        return MarketingContentSupport.normalizeKey(sourceType.toLowerCase() + "-" + sourceKey, "releaseKey");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeSourceType(String sourceType) {
        return MarketingContentSupport.normalizeUpper(sourceType, "TEMPLATE", SOURCE_TYPES, "content source type");
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 readJson 流程生成的业务结果。
     */
    private JsonNode readJson(String value, String field) {
        try {
            return objectMapper.readTree(MarketingContentSupport.hasText(value) ? value : "{}");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value, String field) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * ValidationCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ValidationCommand(String sourceType, String sourceKey) {
    }

    /**
     * ReleaseCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ReleaseCommand(String sourceType, String sourceKey, String createdBy, String note) {
    }

    /**
     * RollbackCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RollbackCommand(String actor, String reason) {
    }

    /**
     * ReleaseBlocker 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ReleaseBlocker(String scope, String key, String reason) {
    }

    /**
     * ValidationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ValidationResult(boolean ready, List<ReleaseBlocker> blockers, List<String> assetRefs) {
    }

    /**
     * ReleaseView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ReleaseView(
            String releaseKey,
            String sourceType,
            String sourceKey,
            Integer sourceVersion,
            String channel,
            String status,
            String checksumSha256) {
    }

    /**
     * ResolvedAsset 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ResolvedAsset(String assetKey, String status, String snapshotJson) {
    }

    /**
     * ResolvedRelease 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ResolvedRelease(
            String releaseKey,
            String sourceType,
            String sourceKey,
            Integer sourceVersion,
            String status,
            String renderedSubject,
            String renderedBody,
            List<String> missingVariables,
            String snapshotJson,
            List<ResolvedAsset> assets) {
    }

    /**
     * AuditEventView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AuditEventView(
            String eventType,
            String targetType,
            String targetKey,
            String actor,
            String note,
            LocalDateTime createdAt) {
    }

    /**
     * SourceSnapshot 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record SourceSnapshot(
            String sourceType,
            String sourceKey,
            String status,
            String channel,
            String assetRefsJson,
            String snapshotJson,
            Object row) {

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param sourceType 类型标识，用于选择对应处理分支。
         * @param sourceKey 业务键，用于在同一租户下定位资源。
         * @return 返回 empty 流程生成的业务结果。
         */
        static SourceSnapshot empty(String sourceType, String sourceKey) {
            return new SourceSnapshot(sourceType, sourceKey, null, "WEB", "[]", "{}", null);
        }
    }
}
