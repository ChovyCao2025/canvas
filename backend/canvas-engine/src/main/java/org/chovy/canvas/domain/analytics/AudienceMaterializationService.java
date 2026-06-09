package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.engine.audience.StableUserIndexService;
import org.chovy.canvas.engine.audience.VersionedAudienceBitmapStore;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * AudienceMaterializationService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class AudienceMaterializationService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SOURCE_OLAP = "OLAP";
    private static final int MAX_ERROR_LENGTH = 1000;

    private final AudienceDefinitionRepository definitions;
    private final BehaviorAudienceOlapRepository olap;
    private final BehaviorAudienceRuleCompiler compiler;
    private final StableUserIndexService indexService;
    private final VersionedAudienceBitmapStore bitmapStore;
    private final AudienceMaterializationRunMapper runMapper;
    private final int maxRows;

    @Autowired
    /**
     * 初始化 AudienceMaterializationService 实例。
     *
     * @param definitions definitions 参数，用于 AudienceMaterializationService 流程中的校验、计算或对象转换。
     * @param olap olap 参数，用于 AudienceMaterializationService 流程中的校验、计算或对象转换。
     * @param compiler compiler 参数，用于 AudienceMaterializationService 流程中的校验、计算或对象转换。
     * @param indexService 依赖组件，用于完成数据访问或外部能力调用。
     * @param bitmapStore bitmap store 参数，用于 AudienceMaterializationService 流程中的校验、计算或对象转换。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param maxRows max rows 参数，用于 AudienceMaterializationService 流程中的校验、计算或对象转换。
     */
    public AudienceMaterializationService(AudienceDefinitionRepository definitions,
                                          BehaviorAudienceOlapRepository olap,
                                          BehaviorAudienceRuleCompiler compiler,
                                          StableUserIndexService indexService,
                                          VersionedAudienceBitmapStore bitmapStore,
                                          AudienceMaterializationRunMapper runMapper,
                                          @Value("${canvas.warehouse.audience-materialization.max-rows:100000}")
                                          int maxRows) {
        this.definitions = definitions;
        this.olap = olap;
        this.compiler = compiler;
        this.indexService = indexService;
        this.bitmapStore = bitmapStore;
        this.runMapper = runMapper;
        this.maxRows = maxRows;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 materialize 流程生成的业务结果。
     */
    public MaterializationResult materialize(Long tenantId, Long audienceId, String operator) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (audienceId == null) {
            throw new IllegalArgumentException("audienceId is required");
        }

        AudienceDefinitionDO definition = definitions.requireEnabled(scopedTenantId, audienceId);
        if (definition == null) {
            throw new IllegalArgumentException("audience definition is required");
        }

        Long version = nextVersion(scopedTenantId, audienceId);
        AudienceMaterializationRunDO run = newRun(scopedTenantId, audienceId, version, definition, operator);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        runMapper.insert(run);

        try {
            BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query =
                    compiler.compile(scopedTenantId, definition.getRuleJson(), maxRows);
            List<String> matchedUsers = olap.findMatchingUsers(query);
            if (matchedUsers == null) {
                matchedUsers = List.of();
            }

            RoaringBitmap bitmap = new RoaringBitmap();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String userId : matchedUsers) {
                long userIndex = indexService.getOrCreateIndex(scopedTenantId, userId);
                if (userIndex < 0 || userIndex > Integer.MAX_VALUE) {
                    throw new IllegalStateException("user index exceeds bitmap range");
                }
                bitmap.add((int) userIndex);
            }

            String bitmapKey = bitmapStore.saveVersion(scopedTenantId, audienceId, version, bitmap, SOURCE_OLAP);
            bitmapStore.markReady(scopedTenantId, audienceId, version);

            run.setStatus(STATUS_SUCCESS);
            run.setMatchedUsers((long) matchedUsers.size());
            run.setBitmapKey(bitmapKey);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return new MaterializationResult(STATUS_SUCCESS, matchedUsers.size());
        } catch (Exception ex) {
            run.setStatus(STATUS_FAILED);
            run.setErrorMessage(limit(ex.getMessage()));
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return new MaterializationResult(STATUS_FAILED, 0L);
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param version version 参数，用于 newRun 流程中的校验、计算或对象转换。
     * @param definition definition 参数，用于 newRun 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 newRun 流程生成的业务结果。
     */
    private AudienceMaterializationRunDO newRun(Long tenantId,
                                                Long audienceId,
                                                Long version,
                                                AudienceDefinitionDO definition,
                                                String operator) {
        AudienceMaterializationRunDO run = new AudienceMaterializationRunDO();
        run.setTenantId(tenantId);
        run.setAudienceId(audienceId);
        run.setVersion(version);
        run.setStatus(STATUS_RUNNING);
        run.setRuleJson(definition.getRuleJson());
        run.setMatchedUsers(0L);
        run.setStartedAt(LocalDateTime.now());
        run.setCreatedBy(operator);
        return run;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private Long nextVersion(Long tenantId, Long audienceId) {
        Long version = runMapper.nextVersion(tenantId, audienceId);
        return version == null || version < 1 ? 1L : version;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String message) {
        String value = message == null ? "materialization failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * AudienceDefinitionRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface AudienceDefinitionRepository {
        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @return 返回 requireEnabled 流程生成的业务结果。
         */
        AudienceDefinitionDO requireEnabled(Long tenantId, Long audienceId);
    }

    /**
     * BehaviorAudienceOlapRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface BehaviorAudienceOlapRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param query query 参数，用于 findMatchingUsers 流程中的校验、计算或对象转换。
         * @return 返回符合条件的数据列表或视图。
         */
        List<String> findMatchingUsers(BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query);
    }

    /**
     * MaterializationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record MaterializationResult(String status, long matchedUsers) {
    }
}
