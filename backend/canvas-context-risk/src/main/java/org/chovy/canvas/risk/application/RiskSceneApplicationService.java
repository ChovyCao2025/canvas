package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Objects;

import org.chovy.canvas.risk.api.RiskSceneFacade;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.chovy.canvas.risk.domain.governance.RiskSceneRepository;
import org.springframework.stereotype.Service;

/**
 * 定义 RiskSceneApplicationService 的风控模块职责和数据契约。
 */
@Service
public class RiskSceneApplicationService implements RiskSceneFacade {

    private static final List<SceneDefinition> DEFAULT_SCENES = List.of(
            new SceneDefinition(
                    "MARKETING_BENEFIT_ISSUE",
                    "营销权益风控",
                    "risk.marketing.benefit.v1",
                    "ENFORCE",
                    "FAIL_REVIEW",
                    50,
                    "risk-ops"),
            new SceneDefinition(
                    "MESSAGE_SEND_PRECHECK",
                    "触达发送前置风控",
                    "risk.message.send.v1",
                    "ENFORCE",
                    "FAIL_REVIEW",
                    50,
                    "risk-ops"),
            new SceneDefinition(
                    "ACCOUNT_DEVICE_ABUSE",
                    "账号与设备风控",
                    "risk.account.device.v1",
                    "ENFORCE",
                    "FAIL_CLOSED",
                    80,
                    "risk-security"),
            new SceneDefinition(
                    "TRANSACTION_PAYMENT_PRECHECK",
                    "交易支付前置风控",
                    "risk.transaction.payment.v1",
                    "ENFORCE",
                    "FAIL_CLOSED",
                    50,
                    "risk-payment"),
            new SceneDefinition(
                    "CONTENT_SAFETY_REVIEW",
                    "内容安全风控",
                    "risk.content.safety.v1",
                    "MARK",
                    "FAIL_REVIEW",
                    100,
                    "risk-content"),
            new SceneDefinition(
                    "AI_DECISION_GUARDRAIL",
                    "AI 决策护栏",
                    "risk.ai.guardrail.v1",
                    "ENFORCE",
                    "FAIL_REVIEW",
                    80,
                    "risk-ai"));


    /**
     * 保存 repository 对应的风控状态或配置。
     */
    private final RiskSceneRepository repository;

    public RiskSceneApplicationService(RiskSceneRepository repository) {
        this.repository = repository;
    }

    /**
     * 执行 listScenes 相关的风控处理逻辑。
     */
    @Override
    public List<RiskSceneView> listScenes(Long tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        List<RiskSceneView> scenes = repository.listScenes(tenantId);
        if (!scenes.isEmpty()) {
            return scenes;
        }
        List<RiskSceneView> defaults = defaultScenes(tenantId);
        repository.saveAll(defaults);
        return defaults;
    }

    /**
     * 执行 defaultScenes 相关的风控处理逻辑。
     */
    private static List<RiskSceneView> defaultScenes(Long tenantId) {
        return DEFAULT_SCENES.stream()
                .map(scene -> scene.toView(tenantId))
                .toList();
    }

    /**
     * 定义 SceneDefinition 的风控模块职责和数据契约。
     */
    private static final class SceneDefinition {

        /**
         * SceneDefinition 的 sceneKey 字段。
         */
        private final String sceneKey;


        /**
         * SceneDefinition 的 displayName 字段。
         */
        private final String displayName;


        /**
         * SceneDefinition 的 eventSchemaKey 字段。
         */
        private final String eventSchemaKey;


        /**
         * SceneDefinition 的 defaultMode 字段。
         */
        private final String defaultMode;


        /**
         * SceneDefinition 的 failPolicy 字段。
         */
        private final String failPolicy;


        /**
         * SceneDefinition 的 latencyBudgetMs 字段。
         */
        private final Integer latencyBudgetMs;


        /**
         * SceneDefinition 的 owner 字段。
         */
        private final String owner;


        /**
         * 创建 SceneDefinition。
         *
         * @param sceneKey SceneDefinition 的 sceneKey 字段
         * @param displayName SceneDefinition 的 displayName 字段
         * @param eventSchemaKey SceneDefinition 的 eventSchemaKey 字段
         * @param defaultMode SceneDefinition 的 defaultMode 字段
         * @param failPolicy SceneDefinition 的 failPolicy 字段
         * @param latencyBudgetMs SceneDefinition 的 latencyBudgetMs 字段
         * @param owner SceneDefinition 的 owner 字段
         */
        public SceneDefinition(String sceneKey, String displayName, String eventSchemaKey, String defaultMode, String failPolicy, Integer latencyBudgetMs, String owner) {
            this.sceneKey = sceneKey;
            this.displayName = displayName;
            this.eventSchemaKey = eventSchemaKey;
            this.defaultMode = defaultMode;
            this.failPolicy = failPolicy;
            this.latencyBudgetMs = latencyBudgetMs;
            this.owner = owner;
        }

        /**
         * 返回 SceneDefinition 的 sceneKey 字段。
         *
         * @return sceneKey 字段值
         */
        public String sceneKey() {
            return sceneKey;
        }

        /**
         * 返回 SceneDefinition 的 displayName 字段。
         *
         * @return displayName 字段值
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回 SceneDefinition 的 eventSchemaKey 字段。
         *
         * @return eventSchemaKey 字段值
         */
        public String eventSchemaKey() {
            return eventSchemaKey;
        }

        /**
         * 返回 SceneDefinition 的 defaultMode 字段。
         *
         * @return defaultMode 字段值
         */
        public String defaultMode() {
            return defaultMode;
        }

        /**
         * 返回 SceneDefinition 的 failPolicy 字段。
         *
         * @return failPolicy 字段值
         */
        public String failPolicy() {
            return failPolicy;
        }

        /**
         * 返回 SceneDefinition 的 latencyBudgetMs 字段。
         *
         * @return latencyBudgetMs 字段值
         */
        public Integer latencyBudgetMs() {
            return latencyBudgetMs;
        }

        /**
         * 返回 SceneDefinition 的 owner 字段。
         *
         * @return owner 字段值
         */
        public String owner() {
            return owner;
        }

        /**
         * 比较当前 SceneDefinition 与其他对象是否相等。
         *
         * @param o 待比较对象
         * @return 相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SceneDefinition other)) {
                return false;
            }
            return Objects.equals(sceneKey, other.sceneKey)
                    && Objects.equals(displayName, other.displayName)
                    && Objects.equals(eventSchemaKey, other.eventSchemaKey)
                    && Objects.equals(defaultMode, other.defaultMode)
                    && Objects.equals(failPolicy, other.failPolicy)
                    && Objects.equals(latencyBudgetMs, other.latencyBudgetMs)
                    && Objects.equals(owner, other.owner);
        }

        /**
         * 计算 SceneDefinition 的哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(sceneKey, displayName, eventSchemaKey, defaultMode, failPolicy, latencyBudgetMs, owner);
        }

        /**
         * 返回 SceneDefinition 的调试字符串。
         *
         * @return 调试字符串
         */
        @Override
        public String toString() {
            return "SceneDefinition[sceneKey=" + sceneKey + ", displayName=" + displayName + ", eventSchemaKey=" + eventSchemaKey + ", defaultMode=" + defaultMode + ", failPolicy=" + failPolicy + ", latencyBudgetMs=" + latencyBudgetMs + ", owner=" + owner + "]";
        }

        /**
         * 执行 toView 相关的风控处理逻辑。
         */
        private RiskSceneView toView(Long tenantId) {
                    return new RiskSceneView(
                            tenantId,
                            sceneKey,
                            displayName,
                            eventSchemaKey,
                            "ACTIVE",
                            defaultMode,
                            failPolicy,
                            latencyBudgetMs,
                            owner);
                }
    }
}
