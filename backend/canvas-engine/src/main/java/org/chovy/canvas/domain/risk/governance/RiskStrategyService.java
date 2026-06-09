package org.chovy.canvas.domain.risk.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.config.CanvasRuntimeMetrics;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.domain.risk.runtime.RiskCompiledStrategy;
import org.chovy.canvas.domain.risk.runtime.RiskFailPolicy;
import org.chovy.canvas.domain.risk.runtime.RiskStrategyCompiler;
import org.chovy.canvas.domain.risk.runtime.RiskStrategyRuleDefinition;
import org.chovy.canvas.domain.risk.runtime.RiskStrategyRuleGroupDefinition;
import org.chovy.canvas.domain.risk.runtime.RiskStrategySnapshot;
import org.chovy.canvas.web.risk.RiskStrategyAuditSink;
import org.chovy.canvas.web.risk.RiskStrategyRuntimeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 风控策略治理服务，协同版本生命周期、审批边界、审计事件和运行时缓存失效。
 */
public class RiskStrategyService implements RiskActiveStrategyReader {

    private final RiskStrategyAuditSink auditSink;
    private final RiskStrategyRuntimeCache runtimeCache;
    private final CanvasRuntimeMetrics metrics;
    private final StateStore store;
    private final RiskStrategyCompiler compiler = new RiskStrategyCompiler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建策略治理服务，未传入指标组件时仅执行治理和审计逻辑。
     */
    public RiskStrategyService(RiskStrategyAuditSink auditSink, RiskStrategyRuntimeCache runtimeCache) {
        this(auditSink, runtimeCache, null, new InMemoryStateStore());
    }

    /**
     * 创建策略治理服务，并注入审计、运行时缓存和可选指标组件。
     */
    public RiskStrategyService(RiskStrategyAuditSink auditSink,
                               RiskStrategyRuntimeCache runtimeCache,
                               CanvasRuntimeMetrics metrics) {
        this(auditSink, runtimeCache, metrics, new InMemoryStateStore());
    }

    /**
     * 创建策略治理服务，并注入可替换状态仓储。
     */
    public RiskStrategyService(RiskStrategyAuditSink auditSink,
                               RiskStrategyRuntimeCache runtimeCache,
                               CanvasRuntimeMetrics metrics,
                               StateStore store) {
        this.auditSink = auditSink;
        this.runtimeCache = runtimeCache;
        this.metrics = metrics;
        this.store = store == null ? new InMemoryStateStore() : store;
    }

    /**
     * 创建或追加策略草稿版本，并把当前策略状态置为草稿。
     */
    public RiskStrategyView createDraft(Long tenantId, RiskStrategyCommand command, String actor) {
        Key key = new Key(tenantId, command.strategyKey());
        StrategyState state = store.find(key.tenantId(), key.strategyKey()).orElseGet(() -> new StrategyState(
                tenantId,
                command.sceneKey(),
                command.strategyKey(),
                command.name(),
                command.riskLevel(),
                actor));
        // 草稿创建总是追加新版本；激活和回滚决定线上实际服务的版本。
        int version = state.versions.size() + 1;
        StrategyVersion versionState = new StrategyVersion(
                version,
                RiskStrategyLifecycleStatus.DRAFT,
                command.definitionJson(),
                null,
                actor,
                null,
                null);
        state.versions.put(version, versionState);
        state.draftVersion = version;
        state.status = RiskStrategyLifecycleStatus.DRAFT;
        state.riskLevel = command.riskLevel();
        store.save(state);
        record(tenantId, "DRAFT_CREATED", command.strategyKey(), version, actor);
        return state.view();
    }

    /**
     * 查询租户下策略列表，可按场景过滤。
     */
    public List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey) {
        return store.findByTenant(tenantId).stream()
                .filter(state -> sceneKey == null || sceneKey.isBlank() || Objects.equals(state.sceneKey, sceneKey))
                .sorted(java.util.Comparator.comparing(state -> state.strategyKey))
                .map(StrategyState::view)
                .toList();
    }

    /**
     * 查询单个策略聚合视图。
     */
    public RiskStrategyView getStrategy(Long tenantId, String strategyKey) {
        return state(tenantId, strategyKey).view();
    }

    /**
     * 查询策略版本列表。
     */
    public List<RiskStrategyVersionView> listVersions(Long tenantId, String strategyKey) {
        StrategyState state = state(tenantId, strategyKey);
        return state.versions.values().stream()
                .sorted(java.util.Comparator.comparingInt(StrategyVersion::version))
                .map(version -> version.view(tenantId, strategyKey))
                .toList();
    }

    /**
     * 校验策略版本定义，并记录校验结果状态。
     */
    public RiskStrategyVersionView validate(Long tenantId, String strategyKey, int version, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion current = version(state, version);
        requireStatus(current, RiskStrategyLifecycleStatus.DRAFT);
        boolean valid = current.definitionJson() != null && !current.definitionJson().contains("INVALID");
        RiskStrategyLifecycleStatus nextStatus = valid ? RiskStrategyLifecycleStatus.VALIDATED : RiskStrategyLifecycleStatus.FAILED;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        StrategyVersion updated = current.withStatus(nextStatus)
                .withValidationJson("{\"valid\":" + valid + "}");
        state.versions.put(version, updated);
        state.status = nextStatus;
        store.save(state);
        record(tenantId, "VALIDATED", strategyKey, version, actor);
        return updated.view(tenantId, strategyKey);
    }

    /**
     * 标记策略版本已完成离线仿真。
     */
    public RiskStrategyVersionView markSimulated(Long tenantId, String strategyKey, int version, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion current = version(state, version);
        requireStatus(current, RiskStrategyLifecycleStatus.VALIDATED);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        StrategyVersion updated = current.withStatus(RiskStrategyLifecycleStatus.SIMULATED);
        state.versions.put(version, updated);
        state.status = RiskStrategyLifecycleStatus.SIMULATED;
        store.save(state);
        record(tenantId, "SIMULATED", strategyKey, version, actor);
        return updated.view(tenantId, strategyKey);
    }

    /**
     * 提交策略版本进入审批流程，并执行高风险策略的仿真前置约束。
     */
    public RiskStrategyVersionView submit(Long tenantId, String strategyKey, int version, String actor) {
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion current = version(state, version);
        if (current.status() == RiskStrategyLifecycleStatus.FAILED) {
            throw new IllegalStateException("validation failed");
        }
        // 高风险策略必须先仿真，才能进入人工审批边界。
        if (isHighRisk(state) && current.status() != RiskStrategyLifecycleStatus.SIMULATED) {
            throw new IllegalStateException("high-risk strategy requires simulation before submit");
        }
        if (!isHighRisk(state) && current.status() != RiskStrategyLifecycleStatus.VALIDATED
                && current.status() != RiskStrategyLifecycleStatus.SIMULATED) {
            throw new IllegalStateException("strategy must be validated before submit");
        }
        StrategyVersion updated = current.withStatus(RiskStrategyLifecycleStatus.APPROVAL_PENDING)
                .withSubmittedBy(actor);
        state.versions.put(version, updated);
        state.status = RiskStrategyLifecycleStatus.APPROVAL_PENDING;
        store.save(state);
        record(tenantId, "SUBMITTED", strategyKey, version, actor);
        return updated.view(tenantId, strategyKey);
    }

    /**
     * 审批待发布策略版本，并阻止提交人审批自己的高风险变更。
     */
    public RiskStrategyVersionView approve(Long tenantId, String strategyKey, int version, String actor) {
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion current = version(state, version);
        requireStatus(current, RiskStrategyLifecycleStatus.APPROVAL_PENDING);
        // 制作人与复核人分离，避免提交者审批自己的高风险变更。
        if (Objects.equals(current.submittedBy(), actor)) {
            throw new IllegalStateException("submitter cannot approve same strategy version");
        }
        StrategyVersion updated = current.withApprovedBy(actor);
        state.versions.put(version, updated);
        store.save(state);
        record(tenantId, "APPROVED", strategyKey, version, actor);
        return updated.view(tenantId, strategyKey);
    }

    /**
     * 激活指定策略版本，并让运行时缓存失效以加载新快照。
     */
    public RiskStrategyView activate(Long tenantId, String strategyKey, int version, String actor) {
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion current = version(state, version);
        if (isHighRisk(state)) {
            if (current.status() != RiskStrategyLifecycleStatus.APPROVAL_PENDING || current.approvedBy() == null) {
                throw new IllegalStateException("high-risk strategy requires simulation and approval before activation");
            }
        // 根据前序判断结果进入后续条件分支。
        } else if (current.status() != RiskStrategyLifecycleStatus.VALIDATED
                && current.status() != RiskStrategyLifecycleStatus.SIMULATED
                && current.status() != RiskStrategyLifecycleStatus.APPROVAL_PENDING) {
            throw new IllegalStateException("strategy cannot be activated from " + current.status());
        }
        StrategyVersion updated = current.withStatus(RiskStrategyLifecycleStatus.ACTIVE);
        state.versions.put(version, updated);
        state.status = RiskStrategyLifecycleStatus.ACTIVE;
        state.activeVersion = version;
        store.save(state);
        // 活跃策略快照会被运行时读取器缓存，任何服务版本变更都必须清理缓存。
        runtimeCache.invalidate(tenantId, strategyKey);
        record(tenantId, "ACTIVATED", strategyKey, version, actor);
        if (metrics != null) {
            metrics.recordRiskStrategyActivation(state.sceneKey, strategyKey, "ACTIVE");
        }
        return state.view();
    }

    /**
     * 将线上活跃版本指向历史目标版本，不修改历史版本定义内容。
     */
    public RiskStrategyView rollback(Long tenantId, String strategyKey, int targetVersion, String actor) {
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion target = version(state, targetVersion);
        if (target.status() == RiskStrategyLifecycleStatus.DRAFT || target.status() == RiskStrategyLifecycleStatus.ARCHIVED) {
            throw new IllegalStateException("rollback target must be immutable");
        }
        // 回滚只重定向活跃版本，不改写历史版本载荷。
        state.status = RiskStrategyLifecycleStatus.ROLLED_BACK;
        state.activeVersion = targetVersion;
        store.save(state);
        runtimeCache.invalidate(tenantId, strategyKey);
        record(tenantId, "ROLLED_BACK", strategyKey, targetVersion, actor);
        return state.view();
    }

    /**
     * 暂停当前活跃策略，并清理运行时缓存。
     */
    public RiskStrategyView pause(Long tenantId, String strategyKey, String actor) {
        StrategyState state = state(tenantId, strategyKey);
        if (state.status != RiskStrategyLifecycleStatus.ACTIVE || state.activeVersion == null) {
            throw new IllegalStateException("only active strategy can be paused");
        }
        StrategyVersion active = version(state, state.activeVersion);
        state.versions.put(state.activeVersion, active.withStatus(RiskStrategyLifecycleStatus.PAUSED));
        state.status = RiskStrategyLifecycleStatus.PAUSED;
        store.save(state);
        runtimeCache.invalidate(tenantId, strategyKey);
        record(tenantId, "PAUSED", strategyKey, state.activeVersion, actor);
        if (metrics != null) {
            metrics.recordRiskStrategyActivation(state.sceneKey, strategyKey, "PAUSED");
        }
        return state.view();
    }

    /**
     * 对比两个策略版本的关键定义差异，供审批和回滚前查看。
     */
    public RiskStrategyDiffView diff(Long tenantId, String strategyKey, int left, int right) {
        StrategyState state = state(tenantId, strategyKey);
        StrategyVersion leftVersion = version(state, left);
        StrategyVersion rightVersion = version(state, right);
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(leftVersion.definitionJson(), rightVersion.definitionJson())) {
            changes.add("definition changed");
        }
        // 动作信号是审核人最关心的高风险变化，因此单独暴露。
        if (action(leftVersion.definitionJson()).isPresent() && action(rightVersion.definitionJson()).isPresent()
                && !Objects.equals(action(leftVersion.definitionJson()).get(), action(rightVersion.definitionJson()).get())) {
            changes.add("action changed");
        }
        return new RiskStrategyDiffView(strategyKey, left, right, changes);
    }

    /**
     * 查找指定租户和场景的活跃策略，并编译为运行时可执行计划。
     */
    @Override
    public RiskCompiledStrategy findActiveStrategy(Long tenantId, String sceneKey) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return store.findByTenant(tenantId).stream()
                .filter(state -> Objects.equals(state.tenantId, tenantId))
                .filter(state -> Objects.equals(state.sceneKey, sceneKey))
                .filter(state -> state.status == RiskStrategyLifecycleStatus.ACTIVE)
                .filter(state -> state.activeVersion != null)
                .sorted(java.util.Comparator.comparing(state -> state.strategyKey))
                .findFirst()
                .map(state -> compiler.compile(snapshot(state, version(state, state.activeVersion))))
                .orElse(null);
    }

    /**
     * 将治理态策略版本转换为编译器输入快照。
     */
    private RiskStrategySnapshot snapshot(StrategyState state, StrategyVersion version) {
        try {
            JsonNode root = objectMapper.readTree(version.definitionJson());
            return new RiskStrategySnapshot(
                    state.tenantId,
                    state.sceneKey,
                    state.strategyKey,
                    version.version(),
                    enumValue(RiskRuntimeMode.class, text(root, "mode", "ENFORCE")),
                    integer(root, "trafficPercent", 100),
                    enumValue(RiskFailPolicy.class, text(root, "failPolicy", "FAIL_REVIEW")),
                    integer(root, "latencyBudgetMs", 50),
                    groups(root),
                    Map.of("riskLevel", state.riskLevel));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception error) {
            throw new IllegalStateException("active risk strategy cannot be compiled: " + state.strategyKey, error);
        }
    }

    /**
     * 从策略 JSON 中解析规则组，兼容旧版仅包含 rules 的定义格式。
     */
    private List<RiskStrategyRuleGroupDefinition> groups(JsonNode root) {
        JsonNode groupNodes = root.path("groups");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!groupNodes.isArray()) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            groupNodes = objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                    .put("groupKey", "default")
                    .put("groupType", "HARD_RULE")
                    .put("executionOrder", 0)
                    .put("matchPolicy", "ANY_MATCHED")
                    .put("enabled", true)
                    .set("rules", root.path("rules")));
        }
        List<RiskStrategyRuleGroupDefinition> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < groupNodes.size(); i++) {
            JsonNode group = groupNodes.get(i);
            result.add(new RiskStrategyRuleGroupDefinition(
                    text(group, "groupKey", "group-" + i),
                    text(group, "groupType", "HARD_RULE"),
                    integer(group, "executionOrder", i),
                    text(group, "matchPolicy", "ANY_MATCHED"),
                    !group.has("enabled") || group.path("enabled").asBoolean(),
                    rules(group.path("rules"))));
        }
        return result;
    }

    /**
     * 从规则组 JSON 中解析规则定义列表。
     */
    private List<RiskStrategyRuleDefinition> rules(JsonNode ruleNodes) {
        if (!ruleNodes.isArray()) {
            return List.of();
        }
        List<RiskStrategyRuleDefinition> result = new ArrayList<>();
        for (int i = 0; i < ruleNodes.size(); i++) {
            JsonNode rule = ruleNodes.get(i);
            result.add(new RiskStrategyRuleDefinition(
                    text(rule, "ruleKey", "rule-" + i),
                    integer(rule, "priority", 0),
                    enumValue(RiskRuntimeMode.class, text(rule, "mode", "ENFORCE")),
                    dslJson(rule),
                    text(rule, "action", "REVIEW"),
                    integer(rule, "scoreDelta", 0),
                    text(rule, "reasonCode", text(rule, "ruleKey", "rule-" + i)),
                    labels(rule.path("labels"))));
        }
        return result;
    }

    /**
     * 读取规则 DSL JSON 字符串，必要时将对象节点序列化为字符串。
     */
    private String dslJson(JsonNode rule) {
        JsonNode dslJson = rule.get("dslJson");
        if (dslJson != null && dslJson.isTextual()) {
            return dslJson.asText();
        }
        JsonNode dsl = rule.get("dsl");
        try {
            return dsl == null || dsl.isMissingNode() || dsl.isNull()
                    ? "{\"logic\":\"AND\",\"conditions\":[],\"groups\":[]}"
                    : objectMapper.writeValueAsString(dsl);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception error) {
            throw new IllegalArgumentException("rule DSL cannot be serialized", error);
        }
    }

    /**
     * 从 JSON 数组中提取规则标签。
     */
    private List<String> labels(JsonNode labels) {
        if (!labels.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        labels.forEach(label -> result.add(label.asText()));
        return result;
    }

    /**
     * 读取文本字段，缺失或空白时返回默认值。
     */
    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
    }

    /**
     * 读取整数字段，缺失或不可转整数时返回默认值。
     */
    private int integer(JsonNode node, String field, int fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || !value.canConvertToInt() ? fallback : value.asInt();
    }

    /**
     * 按枚举名称解析配置值。
     */
    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return Enum.valueOf(type, value);
    }

    /**
     * 粗略提取策略定义中的动作信号，用于治理差异提示。
     */
    private java.util.Optional<String> action(String definitionJson) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (definitionJson == null) {
            return java.util.Optional.empty();
        }
        if (definitionJson.contains("BLOCK")) {
            return java.util.Optional.of("BLOCK");
        }
        if (definitionJson.contains("REVIEW")) {
            return java.util.Optional.of("REVIEW");
        }
        if (definitionJson.contains("ALLOW")) {
            return java.util.Optional.of("ALLOW");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return java.util.Optional.empty();
    }

    /**
     * 判断策略是否属于高风险治理等级。
     */
    private boolean isHighRisk(StrategyState state) {
        return "HIGH".equalsIgnoreCase(state.riskLevel);
    }

    /**
     * 校验策略版本当前状态是否符合预期。
     */
    private void requireStatus(StrategyVersion version, RiskStrategyLifecycleStatus expected) {
        if (version.status() != expected) {
            throw new IllegalStateException("expected " + expected + " but was " + version.status());
        }
    }

    /**
     * 获取租户下指定策略的内存治理状态。
     */
    private StrategyState state(Long tenantId, String strategyKey) {
        return store.find(tenantId, strategyKey)
                .orElseThrow(() -> new IllegalArgumentException("strategy not found: " + strategyKey));
    }

    /**
     * 获取指定策略版本，不存在时抛出业务异常。
     */
    private StrategyVersion version(StrategyState state, int version) {
        StrategyVersion result = state.versions.get(version);
        if (result == null) {
            throw new IllegalArgumentException("strategy version not found: " + version);
        }
        return result;
    }

    /**
     * 写入策略治理审计事件。
     */
    private void record(Long tenantId, String eventType, String strategyKey, int version, String actor) {
        auditSink.record(tenantId, eventType, strategyKey, version, actor);
    }

    /**
     * 策略内存索引键，租户和策略键共同唯一定位一条策略。
     */
    public record Key(Long tenantId, String strategyKey) {
    }

    /**
     * 策略状态仓储，生产环境使用 JDBC，单元测试可继续使用内存实现。
     */
    public interface StateStore {
        Optional<StrategyState> find(Long tenantId, String strategyKey);

        Collection<StrategyState> findByTenant(Long tenantId);

        void save(StrategyState state);
    }

    /**
     * 策略状态内存仓储，作为测试和无数据库环境的默认实现。
     */
    public static final class InMemoryStateStore implements StateStore {
        private final Map<Key, StrategyState> strategies = new HashMap<>();

        @Override
        public Optional<StrategyState> find(Long tenantId, String strategyKey) {
            return Optional.ofNullable(strategies.get(new Key(tenantId, strategyKey)));
        }

        @Override
        public Collection<StrategyState> findByTenant(Long tenantId) {
            return strategies.values().stream()
                    .filter(state -> Objects.equals(state.tenantId, tenantId))
                    .toList();
        }

        @Override
        public void save(StrategyState state) {
            strategies.put(new Key(state.tenantId, state.strategyKey), state);
        }
    }

    /**
     * 策略聚合的内存状态，模拟持久化模型中的策略主表和版本集合。
     */
    public static final class StrategyState {
        /** 租户编号。 */
        private final Long tenantId;
        /** 场景业务键。 */
        private final String sceneKey;
        /** 策略业务键。 */
        private final String strategyKey;
        /** 策略展示名称。 */
        private final String name;
        /** 策略负责人或创建人。 */
        private final String owner;
        /** 策略版本集合，按版本号保持插入顺序。 */
        private final Map<Integer, StrategyVersion> versions = new LinkedHashMap<>();
        /** 当前聚合状态。 */
        private RiskStrategyLifecycleStatus status = RiskStrategyLifecycleStatus.DRAFT;
        /** 当前线上活跃版本号。 */
        private Integer activeVersion;
        /** 当前最新草稿版本号。 */
        private Integer draftVersion;
        /** 风险等级，用于高风险审批约束。 */
        private String riskLevel;

        /**
         * 创建策略内存状态。
         */
        public StrategyState(Long tenantId, String sceneKey, String strategyKey, String name, String riskLevel, String owner) {
            this.tenantId = tenantId;
            this.sceneKey = sceneKey;
            this.strategyKey = strategyKey;
            this.name = name;
            this.riskLevel = riskLevel;
            this.owner = owner;
        }

        public Long tenantId() {
            return tenantId;
        }

        public String sceneKey() {
            return sceneKey;
        }

        public String strategyKey() {
            return strategyKey;
        }

        public String name() {
            return name;
        }

        public String owner() {
            return owner;
        }

        public Map<Integer, StrategyVersion> versions() {
            return versions;
        }

        public RiskStrategyLifecycleStatus status() {
            return status;
        }

        public void status(RiskStrategyLifecycleStatus status) {
            this.status = status;
        }

        public Integer activeVersion() {
            return activeVersion;
        }

        public void activeVersion(Integer activeVersion) {
            this.activeVersion = activeVersion;
        }

        public Integer draftVersion() {
            return draftVersion;
        }

        public void draftVersion(Integer draftVersion) {
            this.draftVersion = draftVersion;
        }

        public String riskLevel() {
            return riskLevel;
        }

        public void riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        /**
         * 将内存状态投影为治理视图。
         */
        private RiskStrategyView view() {
            return new RiskStrategyView(tenantId, sceneKey, strategyKey, name, status,
                    activeVersion, draftVersion, riskLevel, owner);
        }
    }

    /**
     * 策略版本状态，保存定义 JSON、校验结果和提交审批人信息。
     */
    public record StrategyVersion(
            int version,
            RiskStrategyLifecycleStatus status,
            String definitionJson,
            String validationJson,
            String createdBy,
            String submittedBy,
            String approvedBy
    ) {
        /**
         * 返回状态变更后的新版本对象。
         */
        private StrategyVersion withStatus(RiskStrategyLifecycleStatus newStatus) {
            return new StrategyVersion(version, newStatus, definitionJson, validationJson, createdBy, submittedBy, approvedBy);
        }

        /**
         * 返回带校验结果 JSON 的新版本对象。
         */
        private StrategyVersion withValidationJson(String newValidationJson) {
            return new StrategyVersion(version, status, definitionJson, newValidationJson, createdBy, submittedBy, approvedBy);
        }

        /**
         * 返回记录提交人的新版本对象。
         */
        private StrategyVersion withSubmittedBy(String actor) {
            return new StrategyVersion(version, status, definitionJson, validationJson, createdBy, actor, approvedBy);
        }

        /**
         * 返回记录审批人的新版本对象。
         */
        private StrategyVersion withApprovedBy(String actor) {
            return new StrategyVersion(version, status, definitionJson, validationJson, createdBy, submittedBy, actor);
        }

        /**
         * 将版本状态投影为治理接口视图。
         */
        private RiskStrategyVersionView view(Long tenantId, String strategyKey) {
            return new RiskStrategyVersionView(tenantId, strategyKey, version, status, definitionJson,
                    validationJson, createdBy, submittedBy, approvedBy);
        }
    }
}
