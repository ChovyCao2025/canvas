package org.chovy.canvas.domain.approval;

import org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.ApprovalTaskDO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LarkApprovalProviderTest {

    @Test
    void supportsOnlyLarkProvider() {
        LarkApprovalProvider provider = new LarkApprovalProvider(new RecordingLarkApprovalClient());

        assertThat(provider.supports("LARK")).isTrue();
        assertThat(provider.supports("lark")).isTrue();
        assertThat(provider.supports("LOCAL")).isFalse();
    }

    @Test
    void submitBindsPrecreatedLarkInstanceAndTaskIdsFromSnapshot() {
        LarkApprovalProvider provider = new LarkApprovalProvider(new RecordingLarkApprovalClient());
        ApprovalInstanceDO instance = instance(null);
        ApprovalTaskDO first = task(201L, null);
        ApprovalTaskDO second = task(202L, null);

        ApprovalExternalSubmissionResult result = provider.submit(
                definition(),
                instance,
                List.of(first, second),
                new ApprovalSubmitCommand(
                        7L,
                        "CANVAS_PUBLISH_DEFAULT",
                        "CANVAS",
                        "CANVAS",
                        "62",
                        91L,
                        "alice",
                        "准备发布",
                        "HIGH",
                        "[]",
                        """
                                {
                                  "lark": {
                                    "instanceCode": "lark-instance-101",
                                    "taskIdsByLocalTaskId": {
                                      "201": "lark-task-201",
                                      "202": "lark-task-202"
                                    }
                                  }
                                }
                                """,
                        List.of("bob", "tenant_admin"),
                        null,
                        "PUBLISH_CANVAS"));

        assertThat(result.externalInstanceId()).isEqualTo("lark-instance-101");
        assertThat(result.externalTaskIdsByLocalTaskId())
                .containsEntry(201L, "lark-task-201")
                .containsEntry(202L, "lark-task-202");
    }

    @Test
    void submitCreatesLarkInstanceWhenSnapshotCarriesCreatePayload() {
        RecordingLarkApprovalClient client = new RecordingLarkApprovalClient();
        client.createdInstanceCode = "created-lark-instance-101";
        LarkApprovalProvider provider = new LarkApprovalProvider(client);

        ApprovalExternalSubmissionResult result = provider.submit(
                definition(),
                instance(null),
                List.of(task(201L, null)),
                new ApprovalSubmitCommand(
                        7L,
                        "CANVAS_PUBLISH_DEFAULT",
                        "CANVAS",
                        "CANVAS",
                        "62",
                        91L,
                        "alice",
                        "准备发布",
                        "HIGH",
                        "[]",
                        """
                                {
                                  "lark": {
                                    "create": {
                                      "openId": "ou_submitter",
                                      "departmentId": "od_department",
                                      "form": "[{\\\"id\\\":\\\"reason\\\",\\\"type\\\":\\\"textarea\\\",\\\"value\\\":\\\"准备发布\\\"}]"
                                    }
                                  }
                                }
                                """,
                        List.of("bob"),
                        null,
                        "PUBLISH_CANVAS"));

        assertThat(result.externalInstanceId()).isEqualTo("created-lark-instance-101");
        assertThat(result.externalTaskIdsByLocalTaskId()).isEmpty();
        assertThat(client.creates)
                .containsExactly("7:lark-definition-code:canvas-approval-101:ou_submitter:null:od_department:"
                        + "[{\"id\":\"reason\",\"type\":\"textarea\",\"value\":\"准备发布\"}]");
    }

    @Test
    void submitCreatesLarkInstanceAndBindsTasksByMappedApproverOpenId() {
        RecordingLarkApprovalClient client = new RecordingLarkApprovalClient();
        client.createdInstanceCode = "created-lark-instance-101";
        client.instance = new LarkApprovalInstanceSnapshot(
                "created-lark-instance-101",
                "PENDING",
                List.of(new LarkApprovalTaskSnapshot("lark-task-201", "PENDING", "ou_bob")));
        ApprovalLarkUserIdentityResolver identityResolver = tenantUsernameResolver(Map.of(
                "7:bob", new ApprovalLarkUserIdentity("ou_bob", "u_bob", "od_growth")));
        LarkApprovalProvider provider = new LarkApprovalProvider(client, identityResolver);
        ApprovalTaskDO task = task(201L, null);
        task.setApprover("bob");

        ApprovalExternalSubmissionResult result = provider.submit(
                definition(),
                instance(null),
                List.of(task),
                new ApprovalSubmitCommand(
                        7L,
                        "CANVAS_PUBLISH_DEFAULT",
                        "CANVAS",
                        "CANVAS",
                        "62",
                        91L,
                        "alice",
                        "准备发布",
                        "HIGH",
                        "[]",
                        """
                                {
                                  "lark": {
                                    "create": {
                                      "openId": "ou_submitter",
                                      "form": "[]"
                                    }
                                  }
                                }
                                """,
                        List.of("bob"),
                        null,
                        "PUBLISH_CANVAS"));

        assertThat(result.externalInstanceId()).isEqualTo("created-lark-instance-101");
        assertThat(result.externalTaskIdsByLocalTaskId())
                .containsExactly(Map.entry(201L, "lark-task-201"));
        assertThat(client.reads).containsExactly("7:created-lark-instance-101");
    }

    @Test
    void submitRejectsCreatePayloadWithoutExternalDefinitionCode() {
        ApprovalDefinitionDO definition = definition();
        definition.setExternalDefinitionCode(null);
        LarkApprovalProvider provider = new LarkApprovalProvider(new RecordingLarkApprovalClient());

        assertThatThrownBy(() -> provider.submit(
                definition,
                instance(null),
                List.of(task(201L, null)),
                new ApprovalSubmitCommand(
                        7L,
                        "CANVAS_PUBLISH_DEFAULT",
                        "CANVAS",
                        "CANVAS",
                        "62",
                        91L,
                        "alice",
                        "准备发布",
                        "HIGH",
                        "[]",
                        """
                                {"lark":{"create":{"openId":"ou_submitter","form":"[]"}}}
                                """,
                        List.of("bob"),
                        null,
                        "PUBLISH_CANVAS")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external definition code");
    }

    @Test
    void approveCallsLarkClientWithExternalInstanceAndTaskIds() {
        RecordingLarkApprovalClient client = new RecordingLarkApprovalClient();
        LarkApprovalProvider provider = new LarkApprovalProvider(client);

        provider.decide(
                definition(),
                instance("lark-instance-101"),
                task(201L, "lark-task-201"),
                new ApprovalDecisionCommand(7L, 201L, "bob", "operator", "检查通过"),
                true);

        assertThat(client.actions)
                .containsExactly("APPROVE:7:lark-instance-101:lark-task-201:bob:检查通过");
    }

    @Test
    void rejectCallsLarkClientWithExternalInstanceAndTaskIds() {
        RecordingLarkApprovalClient client = new RecordingLarkApprovalClient();
        LarkApprovalProvider provider = new LarkApprovalProvider(client);

        provider.decide(
                definition(),
                instance("lark-instance-101"),
                task(201L, "lark-task-201"),
                new ApprovalDecisionCommand(7L, 201L, "bob", "operator", "风险未说明"),
                false);

        assertThat(client.actions)
                .containsExactly("REJECT:7:lark-instance-101:lark-task-201:bob:风险未说明");
    }

    @Test
    void decisionRejectsMissingExternalIds() {
        LarkApprovalProvider provider = new LarkApprovalProvider(new RecordingLarkApprovalClient());

        assertThatThrownBy(() -> provider.decide(
                definition(),
                instance(null),
                task(201L, "lark-task-201"),
                new ApprovalDecisionCommand(7L, 201L, "bob", "operator", "检查通过"),
                true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external instance id");

        assertThatThrownBy(() -> provider.decide(
                definition(),
                instance("lark-instance-101"),
                task(201L, null),
                new ApprovalDecisionCommand(7L, 201L, "bob", "operator", "检查通过"),
                true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("external task id");
    }

    @Test
    void syncMapsLarkInstanceAndTaskStatuses() {
        RecordingLarkApprovalClient client = new RecordingLarkApprovalClient();
        client.instance = new LarkApprovalInstanceSnapshot(
                "lark-instance-101",
                "APPROVED",
                List.of(
                        new LarkApprovalTaskSnapshot("lark-task-201", "APPROVED"),
                        new LarkApprovalTaskSnapshot("lark-task-202", "DONE")));
        LarkApprovalProvider provider = new LarkApprovalProvider(client);

        ApprovalExternalSyncResult result = provider.sync(definition(), instance("lark-instance-101"));

        assertThat(client.reads).containsExactly("7:lark-instance-101");
        assertThat(result.instanceStatus()).isEqualTo("APPROVED");
        assertThat(result.taskStatusesByExternalTaskId())
                .containsEntry("lark-task-201", "APPROVED")
                .containsEntry("lark-task-202", "APPROVED");
    }

    private ApprovalDefinitionDO definition() {
        ApprovalDefinitionDO row = new ApprovalDefinitionDO();
        row.setTenantId(7L);
        row.setDefinitionKey("CANVAS_PUBLISH_DEFAULT");
        row.setExternalProvider("LARK");
        row.setExternalDefinitionCode("lark-definition-code");
        return row;
    }

    private ApprovalInstanceDO instance(String externalInstanceId) {
        ApprovalInstanceDO row = new ApprovalInstanceDO();
        row.setId(101L);
        row.setTenantId(7L);
        row.setDefinitionKey("CANVAS_PUBLISH_DEFAULT");
        row.setExternalInstanceId(externalInstanceId);
        return row;
    }

    private ApprovalTaskDO task(Long taskId, String externalTaskId) {
        ApprovalTaskDO row = new ApprovalTaskDO();
        row.setId(taskId);
        row.setTenantId(7L);
        row.setInstanceId(101L);
        row.setApprover("bob");
        row.setExternalTaskId(externalTaskId);
        return row;
    }

    private ApprovalLarkUserIdentityResolver tenantUsernameResolver(Map<String, ApprovalLarkUserIdentity> identities) {
        return new ApprovalLarkUserIdentityResolver(null) {
            @Override
            public ApprovalLarkUserIdentity resolve(Long tenantId, String username) {
                return identities.get(tenantId + ":" + username);
            }
        };
    }

    private static final class RecordingLarkApprovalClient implements LarkApprovalClient {
        private final List<String> actions = new ArrayList<>();
        private final List<String> reads = new ArrayList<>();
        private final List<String> creates = new ArrayList<>();
        private LarkApprovalInstanceSnapshot instance;
        private String createdInstanceCode;

        @Override
        public String createInstance(LarkApprovalCreateInstanceRequest request) {
            creates.add(request.tenantId() + ":" + request.approvalCode() + ":" + request.uuid() + ":" + request.openId()
                    + ":" + request.userId() + ":" + request.departmentId() + ":" + request.form());
            return createdInstanceCode;
        }

        @Override
        public void approveTask(LarkApprovalTaskActionRequest request) {
            actions.add("APPROVE:" + request.tenantId() + ":" + request.instanceCode() + ":"
                    + request.taskId() + ":" + request.actor() + ":" + request.comment());
        }

        @Override
        public void rejectTask(LarkApprovalTaskActionRequest request) {
            actions.add("REJECT:" + request.tenantId() + ":" + request.instanceCode() + ":"
                    + request.taskId() + ":" + request.actor() + ":" + request.comment());
        }

        @Override
        public LarkApprovalInstanceSnapshot getInstance(Long tenantId, String instanceCode) {
            reads.add(tenantId + ":" + instanceCode);
            return instance;
        }
    }
}
