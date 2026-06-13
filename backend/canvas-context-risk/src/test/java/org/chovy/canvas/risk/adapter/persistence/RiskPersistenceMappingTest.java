package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.risk.adapter.persistence.RiskDecisionRunDO;
import org.chovy.canvas.risk.adapter.persistence.RiskListDO;
import org.chovy.canvas.risk.adapter.persistence.RiskListEntryDO;
import org.chovy.canvas.risk.adapter.persistence.RiskRuleHitDO;
import org.chovy.canvas.risk.adapter.persistence.RiskSceneDO;
import org.chovy.canvas.risk.adapter.persistence.RiskSimulationRunDO;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyDO;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyVersionDO;
import org.chovy.canvas.risk.adapter.persistence.RiskDecisionRunMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskListEntryMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskListMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskRuleHitMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskSceneMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskSimulationRunMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyMapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyVersionMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskPersistenceMappingTest {

    private static final List<RiskMapping> MAPPINGS = List.of(
            new RiskMapping(RiskSceneDO.class, RiskSceneMapper.class, "risk_scene",
                    "tenantId", "sceneKey", "name", "eventSchemaKey", "status", "defaultMode",
                    "failPolicy", "latencyBudgetMs", "owner", "createdAt", "updatedAt"),
            new RiskMapping(RiskStrategyDO.class, RiskStrategyMapper.class, "risk_strategy",
                    "tenantId", "sceneKey", "strategyKey", "name", "status", "activeVersion",
                    "draftVersion", "riskLevel", "owner", "createdAt", "updatedAt"),
            new RiskMapping(RiskStrategyVersionDO.class, RiskStrategyVersionMapper.class, "risk_strategy_version",
                    "tenantId", "strategyKey", "version", "status", "mode", "trafficPercent", "compiledHash",
                    "definitionJson", "validationJson", "createdBy", "submittedBy", "submittedAt", "approvedBy", "approvedAt",
                    "effectiveFrom", "effectiveTo", "createdAt", "updatedAt"),
            new RiskMapping(RiskListDO.class, RiskListMapper.class, "risk_list",
                    "tenantId", "listKey", "listType", "subjectType", "status", "requiresApproval",
                    "owner", "createdAt", "updatedAt"),
            new RiskMapping(RiskListEntryDO.class, RiskListEntryMapper.class, "risk_list_entry",
                    "tenantId", "listKey", "subjectHash", "subjectMasked", "reason", "source",
                    "effectiveFrom", "expiresAt", "createdBy", "approvalId", "createdAt"),
            new RiskMapping(RiskDecisionRunDO.class, RiskDecisionRunMapper.class, "risk_decision_run",
                    "tenantId", "requestId", "requestHash", "sceneKey", "strategyKey", "strategyVersion",
                    "subjectHash", "decision", "score", "riskBand", "mode", "latencyMs", "status",
                    "inputSnapshotJson", "outputJson", "createdAt"),
            new RiskMapping(RiskRuleHitDO.class, RiskRuleHitMapper.class, "risk_rule_hit",
                    "tenantId", "decisionRunId", "strategyKey", "strategyVersion", "groupKey",
                    "ruleKey", "mode", "action", "scoreDelta", "reasonCode", "evidenceJson", "createdAt"),
            new RiskMapping(RiskSimulationRunDO.class, RiskSimulationRunMapper.class, "risk_simulation_run",
                    "tenantId", "simulationId", "sceneKey", "strategyKey", "baselineVersion",
                    "candidateVersion", "status", "sampleSize", "changedActionCount",
                    "actionDistributionJson", "actionChangesJson", "createdAt")
    );

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        MAPPINGS.forEach(mapping -> TableInfoHelper.initTableInfo(assistant, mapping.doType()));
    }

    @Test
    void riskFoundationDataObjectsExposeExpectedTableMetadata() {
        MAPPINGS.forEach(mapping -> {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(mapping.doType());

            assertThat(tableInfo.getTableName()).isEqualTo(mapping.tableName());
            assertThat(tableInfo.getKeyProperty()).isEqualTo("id");
            assertThat(fieldProperties(tableInfo)).contains(mapping.requiredFields());
        });
    }

    @Test
    void riskFoundationMappersBindToExpectedDataObjects() {
        MAPPINGS.forEach(mapping -> assertThat(baseMapperModel(mapping.mapperType()))
                .isEqualTo(mapping.doType()));
    }

    private List<String> fieldProperties(TableInfo tableInfo) {
        return tableInfo.getFieldList().stream()
                .map(field -> field.getProperty())
                .toList();
    }

    private Class<?> baseMapperModel(Class<?> mapperType) {
        return Arrays.stream(mapperType.getGenericInterfaces())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> type.getRawType().equals(BaseMapper.class))
                .map(type -> type.getActualTypeArguments()[0])
                .filter(Class.class::isInstance)
                .map(Class.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("BaseMapper model is missing for " + mapperType.getName()));
    }

    private record RiskMapping(
            Class<?> doType,
            Class<?> mapperType,
            String tableName,
            String... requiredFields) {
    }
}
