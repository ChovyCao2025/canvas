package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Objects;

import org.chovy.canvas.risk.api.RiskSceneFacade;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.chovy.canvas.risk.domain.governance.RiskSceneRepository;
import org.springframework.stereotype.Service;

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

    private final RiskSceneRepository repository;

    public RiskSceneApplicationService(RiskSceneRepository repository) {
        this.repository = repository;
    }

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

    private static List<RiskSceneView> defaultScenes(Long tenantId) {
        return DEFAULT_SCENES.stream()
                .map(scene -> scene.toView(tenantId))
                .toList();
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
