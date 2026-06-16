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
import java.util.Objects;

/**
 * 定义 RiskPersistenceMappingTest 的风控模块职责和数据契约。
 */
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


    /**
     * 执行 initMyBatisPlusTableInfo 相关的风控处理逻辑。
     */
    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        MAPPINGS.forEach(mapping -> TableInfoHelper.initTableInfo(assistant, mapping.doType()));
    }

    /**
     * 执行 riskFoundationDataObjectsExposeExpectedTableMetadata 相关的风控处理逻辑。
     */
    @Test
    void riskFoundationDataObjectsExposeExpectedTableMetadata() {
        MAPPINGS.forEach(mapping -> {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(mapping.doType());

            assertThat(tableInfo.getTableName()).isEqualTo(mapping.tableName());
            assertThat(tableInfo.getKeyProperty()).isEqualTo("id");
            assertThat(fieldProperties(tableInfo)).contains(mapping.requiredFields());
        });
    }

    /**
     * 执行 riskFoundationMappersBindToExpectedDataObjects 相关的风控处理逻辑。
     */
    @Test
    void riskFoundationMappersBindToExpectedDataObjects() {
        MAPPINGS.forEach(mapping -> assertThat(baseMapperModel(mapping.mapperType()))
                .isEqualTo(mapping.doType()));
    }

    /**
     * 执行 fieldProperties 相关的风控处理逻辑。
     */
    private List<String> fieldProperties(TableInfo tableInfo) {
        return tableInfo.getFieldList().stream()
                .map(field -> field.getProperty())
                .toList();
    }

    /**
     * 执行 baseMapperModel 相关的风控处理逻辑。
     */
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

    /**
     * 定义 RiskMapping 的风控模块职责和数据契约。
     */
    private static final class RiskMapping {

        /**
         * RiskMapping 的 doType 字段。
         */
        private final Class<?> doType;


        /**
         * RiskMapping 的 mapperType 字段。
         */
        private final Class<?> mapperType;


        /**
         * RiskMapping 的 tableName 字段。
         */
        private final String tableName;


        /**
         * RiskMapping 的 requiredFields 字段。
         */
        private final String[] requiredFields;


        /**
         * 创建 RiskMapping。
         *
         * @param doType RiskMapping 的 doType 字段
         * @param mapperType RiskMapping 的 mapperType 字段
         * @param tableName RiskMapping 的 tableName 字段
         * @param requiredFields RiskMapping 的 requiredFields 字段
         */
        public RiskMapping(Class<?> doType, Class<?> mapperType, String tableName, String... requiredFields) {
            this.doType = doType;
            this.mapperType = mapperType;
            this.tableName = tableName;
            this.requiredFields = requiredFields;
        }

        /**
         * 返回 RiskMapping 的 doType 字段。
         *
         * @return doType 字段值
         */
        public Class<?> doType() {
            return doType;
        }

        /**
         * 返回 RiskMapping 的 mapperType 字段。
         *
         * @return mapperType 字段值
         */
        public Class<?> mapperType() {
            return mapperType;
        }

        /**
         * 返回 RiskMapping 的 tableName 字段。
         *
         * @return tableName 字段值
         */
        public String tableName() {
            return tableName;
        }

        /**
         * 返回 RiskMapping 的 requiredFields 字段。
         *
         * @return requiredFields 字段值
         */
        public String[] requiredFields() {
            return requiredFields;
        }

        /**
         * 比较当前 RiskMapping 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RiskMapping other)) {
                return false;
            }
            return Objects.equals(doType, other.doType)
                    && Objects.equals(mapperType, other.mapperType)
                    && Objects.equals(tableName, other.tableName)
                    && Objects.equals(requiredFields, other.requiredFields);
        }

        /**
         * 计算 RiskMapping 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(doType, mapperType, tableName, requiredFields);
        }

        /**
         * 返回 RiskMapping 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "RiskMapping[doType=" + doType + ", mapperType=" + mapperType + ", tableName=" + tableName + ", requiredFields=" + requiredFields + "]";
        }
    }
}
