package org.chovy.canvas.domain.risk.governance;

import java.util.List;

/**
 * 风控场景目录服务，提供第一批生产场景的租户级读取能力。
 */
public class RiskSceneService {

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

    private final StateStore store;

    /**
     * 创建默认场景服务，使用内存默认场景目录。
     */
    public RiskSceneService() {
        this(new DefaultSceneStore());
    }

    /**
     * 创建可替换仓储的场景服务。
     */
    public RiskSceneService(StateStore store) {
        this.store = store == null ? new DefaultSceneStore() : store;
    }

    /**
     * 查询租户可用的风控场景。
     */
    public List<RiskSceneView> listScenes(Long tenantId) {
        return store.listScenes(tenantId);
    }

    /**
     * 返回租户级默认场景种子。
     */
    static List<RiskSceneView> defaultScenes(Long tenantId) {
        return DEFAULT_SCENES.stream()
                .map(scene -> scene.toView(tenantId))
                .toList();
    }

    /**
     * 风控场景仓储，生产环境使用 JDBC，测试可使用默认内存种子。
     */
    public interface StateStore {
        List<RiskSceneView> listScenes(Long tenantId);
    }

    /**
     * 默认场景仓储。
     */
    private static final class DefaultSceneStore implements StateStore {
        @Override
        public List<RiskSceneView> listScenes(Long tenantId) {
            return defaultScenes(tenantId);
        }
    }

    private record SceneDefinition(
            String sceneKey,
            String displayName,
            String eventSchemaKey,
            String defaultMode,
            String failPolicy,
            Integer latencyBudgetMs,
            String owner) {

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
