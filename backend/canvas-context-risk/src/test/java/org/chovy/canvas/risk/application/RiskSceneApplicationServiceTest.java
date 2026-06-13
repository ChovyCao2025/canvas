package org.chovy.canvas.risk.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.risk.adapter.persistence.MybatisRiskSceneRepository;
import org.chovy.canvas.risk.adapter.persistence.RiskSceneDO;
import org.chovy.canvas.risk.adapter.persistence.RiskSceneMapper;
import org.chovy.canvas.risk.api.RiskSceneView;
import org.chovy.canvas.risk.domain.governance.RiskSceneRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

class RiskSceneApplicationServiceTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RiskSceneDO.class);
    }

    @Test
    void listScenesSeedsDeterministicDefaultCatalogWhenRepositoryIsEmpty() {
        FakeRepository repository = new FakeRepository();
        RiskSceneApplicationService service = new RiskSceneApplicationService(repository);

        List<RiskSceneView> scenes = service.listScenes(42L);

        assertThat(scenes)
                .extracting(RiskSceneView::sceneKey)
                .containsExactly(
                        "MARKETING_BENEFIT_ISSUE",
                        "MESSAGE_SEND_PRECHECK",
                        "ACCOUNT_DEVICE_ABUSE",
                        "TRANSACTION_PAYMENT_PRECHECK",
                        "CONTENT_SAFETY_REVIEW",
                        "AI_DECISION_GUARDRAIL");
        assertThat(scenes)
                .extracting(
                        RiskSceneView::sceneKey,
                        RiskSceneView::displayName,
                        RiskSceneView::eventSchemaKey,
                        RiskSceneView::defaultMode,
                        RiskSceneView::failPolicy,
                        RiskSceneView::latencyBudgetMs,
                        RiskSceneView::owner)
                .containsExactly(
                        tuple("MARKETING_BENEFIT_ISSUE", "营销权益风控", "risk.marketing.benefit.v1",
                                "ENFORCE", "FAIL_REVIEW", 50, "risk-ops"),
                        tuple("MESSAGE_SEND_PRECHECK", "触达发送前置风控", "risk.message.send.v1",
                                "ENFORCE", "FAIL_REVIEW", 50, "risk-ops"),
                        tuple("ACCOUNT_DEVICE_ABUSE", "账号与设备风控", "risk.account.device.v1",
                                "ENFORCE", "FAIL_CLOSED", 80, "risk-security"),
                        tuple("TRANSACTION_PAYMENT_PRECHECK", "交易支付前置风控", "risk.transaction.payment.v1",
                                "ENFORCE", "FAIL_CLOSED", 50, "risk-payment"),
                        tuple("CONTENT_SAFETY_REVIEW", "内容安全风控", "risk.content.safety.v1",
                                "MARK", "FAIL_REVIEW", 100, "risk-content"),
                        tuple("AI_DECISION_GUARDRAIL", "AI 决策护栏", "risk.ai.guardrail.v1",
                                "ENFORCE", "FAIL_REVIEW", 80, "risk-ai"));
        assertThat(scenes)
                .allSatisfy(scene -> assertThat(scene.tenantId()).isEqualTo(42L))
                .first()
                .satisfies(scene -> {
                    assertThat(scene.displayName()).isEqualTo("营销权益风控");
                    assertThat(scene.eventSchemaKey()).isEqualTo("risk.marketing.benefit.v1");
                    assertThat(scene.status()).isEqualTo("ACTIVE");
                    assertThat(scene.defaultMode()).isEqualTo("ENFORCE");
                    assertThat(scene.failPolicy()).isEqualTo("FAIL_REVIEW");
                    assertThat(scene.latencyBudgetMs()).isEqualTo(50);
                    assertThat(scene.owner()).isEqualTo("risk-ops");
                });
        assertThat(repository.saved)
                .hasSize(6)
                .extracting(RiskSceneView::sceneKey)
                .containsExactlyElementsOf(scenes.stream().map(RiskSceneView::sceneKey).toList());
    }

    @Test
    void listScenesReturnsRepositoryCatalogWithoutSeedingWhenRowsExist() {
        FakeRepository repository = new FakeRepository();
        repository.rows = List.of(new RiskSceneView(
                42L,
                "CUSTOM_SCENE",
                "Custom scene",
                "risk.custom.v1",
                "ACTIVE",
                "SHADOW",
                "FAIL_OPEN",
                25,
                "custom-owner"));
        RiskSceneApplicationService service = new RiskSceneApplicationService(repository);

        List<RiskSceneView> scenes = service.listScenes(42L);

        assertThat(scenes).containsExactlyElementsOf(repository.rows);
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void mybatisRepositorySeedsRowsWithAuditTimestampsAndIgnoresDuplicateSceneKeys() {
        RiskSceneMapper mapper = mock(RiskSceneMapper.class);
        doThrow(new DuplicateKeyException("duplicate scene"))
                .when(mapper)
                .insert(any(RiskSceneDO.class));
        MybatisRiskSceneRepository repository = new MybatisRiskSceneRepository(mapper);

        assertThatCode(() -> repository.saveAll(List.of(scene(
                42L,
                "MARKETING_BENEFIT_ISSUE",
                "营销权益风控",
                "risk.marketing.benefit.v1",
                "ENFORCE",
                "FAIL_REVIEW",
                50,
                "risk-ops"))))
                .doesNotThrowAnyException();

        ArgumentCaptor<RiskSceneDO> rowCaptor = ArgumentCaptor.forClass(RiskSceneDO.class);
        verify(mapper).insert(rowCaptor.capture());
        RiskSceneDO row = rowCaptor.getValue();
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isEqualTo(row.getCreatedAt());
    }

    @Test
    void mybatisRepositoryListsOnlyActiveScenesForTenant() {
        RiskSceneMapper mapper = mock(RiskSceneMapper.class);
        MybatisRiskSceneRepository repository = new MybatisRiskSceneRepository(mapper);

        repository.listScenes(42L);

        ArgumentCaptor<LambdaQueryWrapper<RiskSceneDO>> wrapperCaptor = ArgumentCaptor.captor();
        verify(mapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<RiskSceneDO> wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment())
                .contains("tenant_id", "status", "ORDER BY");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(42L, "ACTIVE");
    }

    private static final class FakeRepository implements RiskSceneRepository {
        private List<RiskSceneView> rows = List.of();
        private final List<RiskSceneView> saved = new ArrayList<>();

        @Override
        public List<RiskSceneView> listScenes(Long tenantId) {
            return rows;
        }

        @Override
        public void saveAll(List<RiskSceneView> scenes) {
            saved.addAll(scenes);
            rows = List.copyOf(scenes);
        }
    }

    private static RiskSceneView scene(
            Long tenantId,
            String sceneKey,
            String displayName,
            String eventSchemaKey,
            String defaultMode,
            String failPolicy,
            Integer latencyBudgetMs,
            String owner) {
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
