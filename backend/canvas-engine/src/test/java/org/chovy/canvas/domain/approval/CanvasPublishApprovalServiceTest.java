package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.project.CanvasProjectRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasPublishApprovalServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void submitReviewCreatesApprovalForCurrentDraftWithProjectAdminsRiskReasonsAndLarkForm() throws Exception {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        CanvasProjectMemberMapper memberMapper = mock(CanvasProjectMemberMapper.class);
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        CanvasPublishApprovalService service = service(
                canvasMapper, versionMapper, folderMapper, projectMapper, memberMapper, workflowService, mock(CanvasService.class));
        when(canvasMapper.selectById(62L)).thenReturn(canvas(62L, 7L, null));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(91L, couponGraph()));
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder(9L));
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project(9L, 1));
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member("bob", CanvasProjectRole.PROJECT_ADMIN.name())));
        when(workflowService.submit(any(ApprovalSubmitCommand.class))).thenReturn(instanceView("PENDING", 91L));

        ApprovalInstanceView view = service.submitReview(
                7L,
                62L,
                "alice",
                new CanvasPublishApprovalRequest("准备发布活动画布"));

        verify(workflowService).cancelOpen(7L, "CANVAS", "62", "alice", "new canvas publish review submitted");
        ArgumentCaptor<ApprovalSubmitCommand> command = ArgumentCaptor.forClass(ApprovalSubmitCommand.class);
        verify(workflowService).submit(command.capture());
        assertThat(command.getValue().definitionKey()).isEqualTo("CANVAS_PUBLISH_DEFAULT");
        assertThat(command.getValue().targetType()).isEqualTo("CANVAS");
        assertThat(command.getValue().targetId()).isEqualTo("62");
        assertThat(command.getValue().targetVersionId()).isEqualTo(91L);
        assertThat(command.getValue().approvers()).containsExactly("bob");
        assertThat(command.getValue().riskLevel()).isEqualTo("HIGH");
        assertThat(command.getValue().riskReasonsJson()).contains("PROJECT_REQUIRES_REVIEW");
        assertThat(command.getValue().riskReasonsJson()).contains("UNLIMITED_TOTAL_CAP");
        assertThat(command.getValue().riskReasonsJson()).contains("COUPON_OR_BENEFIT_NODE");
        assertThat(command.getValue().snapshotJson()).contains("\"lark\"");
        JsonNode snapshot = JSON.readTree(command.getValue().snapshotJson());
        assertThat(snapshot.path("canvasId").asLong()).isEqualTo(62L);
        assertThat(snapshot.path("lark").path("create").hasNonNull("openId")).isFalse();
        assertThat(snapshot.path("lark").path("create").hasNonNull("userId")).isFalse();
        JsonNode form = JSON.readTree(snapshot.path("lark").path("create").path("form").asText());
        assertThat(form).hasSize(8);
        assertThat(form.get(0).path("id").asText()).isEqualTo("canvas_name");
        assertThat(form.get(0).path("value").asText()).isEqualTo("Welcome canvas");
        assertThat(form.get(3).path("id").asText()).isEqualTo("submit_reason");
        assertThat(form.get(3).path("value").asText()).isEqualTo("准备发布活动画布");
        assertThat(form.get(6).path("id").asText()).isEqualTo("risk_reasons");
        assertThat(form.get(6).path("value").asText())
                .contains("PROJECT_REQUIRES_REVIEW")
                .contains("UNLIMITED_TOTAL_CAP")
                .contains("COUPON_OR_BENEFIT_NODE");
        assertThat(view.status()).isEqualTo("PENDING");
    }

    @Test
    void submitReviewCarriesExplicitLarkSubmitterIdentity() throws Exception {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        CanvasPublishApprovalService service = service(
                canvasMapper, versionMapper, folderMapper, projectMapper, mock(CanvasProjectMemberMapper.class),
                workflowService, mock(CanvasService.class));
        when(canvasMapper.selectById(62L)).thenReturn(canvas(62L, 7L, 1000));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(91L, simpleGraph()));
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder(9L));
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project(9L, 1));
        when(workflowService.submit(any(ApprovalSubmitCommand.class))).thenReturn(instanceView("PENDING", 91L));

        service.submitReview(
                7L,
                62L,
                "alice",
                new CanvasPublishApprovalRequest("准备发布活动画布", "ou_alice", "u_alice", "od_growth"));

        ArgumentCaptor<ApprovalSubmitCommand> command = ArgumentCaptor.forClass(ApprovalSubmitCommand.class);
        verify(workflowService).submit(command.capture());
        JsonNode create = JSON.readTree(command.getValue().snapshotJson()).path("lark").path("create");
        assertThat(create.path("openId").asText()).isEqualTo("ou_alice");
        assertThat(create.path("userId").asText()).isEqualTo("u_alice");
        assertThat(create.path("departmentId").asText()).isEqualTo("od_growth");
    }

    @Test
    void submitReviewUsesMappedLarkSubmitterIdentityWhenRequestDoesNotProvideIt() throws Exception {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        ApprovalLarkUserIdentityResolver identityResolver = mock(ApprovalLarkUserIdentityResolver.class);
        CanvasPublishApprovalService service = service(
                canvasMapper, versionMapper, folderMapper, projectMapper, mock(CanvasProjectMemberMapper.class),
                workflowService, mock(CanvasService.class), identityResolver);
        when(canvasMapper.selectById(62L)).thenReturn(canvas(62L, 7L, 1000));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(91L, simpleGraph()));
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder(9L));
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project(9L, 1));
        when(identityResolver.resolve(7L, "alice"))
                .thenReturn(new ApprovalLarkUserIdentity("ou_mapped_alice", "u_mapped_alice", "od_mapped_growth"));
        when(workflowService.submit(any(ApprovalSubmitCommand.class))).thenReturn(instanceView("PENDING", 91L));

        service.submitReview(
                7L,
                62L,
                "alice",
                new CanvasPublishApprovalRequest("准备发布活动画布"));

        ArgumentCaptor<ApprovalSubmitCommand> command = ArgumentCaptor.forClass(ApprovalSubmitCommand.class);
        verify(workflowService).submit(command.capture());
        JsonNode create = JSON.readTree(command.getValue().snapshotJson()).path("lark").path("create");
        assertThat(create.path("openId").asText()).isEqualTo("ou_mapped_alice");
        assertThat(create.path("userId").asText()).isEqualTo("u_mapped_alice");
        assertThat(create.path("departmentId").asText()).isEqualTo("od_mapped_growth");
    }

    @Test
    void publishWithApprovalGateRejectsMissingApprovalWhenProjectRequiresReview() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        CanvasPublishApprovalService service = service(
                canvasMapper, versionMapper, folderMapper, projectMapper, mock(CanvasProjectMemberMapper.class),
                workflowService, mock(CanvasService.class));
        when(canvasMapper.selectById(62L)).thenReturn(canvas(62L, 7L, 1000));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(91L, simpleGraph()));
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder(9L));
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project(9L, 1));
        when(workflowService.latestApproved(7L, "CANVAS_PUBLISH_DEFAULT", "CANVAS", "62", 91L)).thenReturn(null);

        assertThatThrownBy(() -> service.publishWithApprovalGate(7L, 62L, "alice"))
                .isInstanceOf(CanvasPublishApprovalService.CanvasPublishApprovalRequiredException.class)
                .hasMessageContaining("approval is required");
    }

    @Test
    void publishWithApprovalGateAllowsFreshApprovalForCurrentDraft() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasPublishApprovalService service = service(
                canvasMapper, versionMapper, folderMapper, projectMapper, mock(CanvasProjectMemberMapper.class),
                workflowService, canvasService);
        CanvasVersionDO published = new CanvasVersionDO();
        published.setId(92L);
        when(canvasMapper.selectById(62L)).thenReturn(canvas(62L, 7L, 1000));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(91L, simpleGraph()));
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder(9L));
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project(9L, 1));
        when(workflowService.latestApproved(7L, "CANVAS_PUBLISH_DEFAULT", "CANVAS", "62", 91L))
                .thenReturn(instanceView("APPROVED", 91L));
        when(canvasService.publish(62L, "alice")).thenReturn(published);

        CanvasVersionDO result = service.publishWithApprovalGate(7L, 62L, "alice");

        assertThat(result).isSameAs(published);
        verify(canvasService).publish(62L, "alice");
    }

    @Test
    void publishWithApprovalGateRejectsStaleApprovalForPreviousDraft() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        CanvasPublishApprovalService service = service(
                canvasMapper, versionMapper, folderMapper, projectMapper, mock(CanvasProjectMemberMapper.class),
                workflowService, mock(CanvasService.class));
        when(canvasMapper.selectById(62L)).thenReturn(canvas(62L, 7L, 1000));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(92L, simpleGraph()));
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder(9L));
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project(9L, 1));
        when(workflowService.latestApproved(7L, "CANVAS_PUBLISH_DEFAULT", "CANVAS", "62", 92L)).thenReturn(null);

        assertThatThrownBy(() -> service.publishWithApprovalGate(7L, 62L, "alice"))
                .isInstanceOf(CanvasPublishApprovalService.CanvasPublishApprovalRequiredException.class)
                .hasMessageContaining("current draft");
    }

    @Test
    void autoActionPublishesOnlyWhenApprovedDraftIsStillCurrent() {
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasPublishApprovalAutoActionHandler handler = new CanvasPublishApprovalAutoActionHandler(versionMapper, canvasService);
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(91L, simpleGraph()));
        ApprovalInstanceDO instance = approvedInstance(62L, 91L);

        handler.execute(instance, "bob");

        verify(canvasService).publish(62L, "bob");

        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft(92L, simpleGraph()));
        assertThatThrownBy(() -> handler.execute(instance, "bob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("draft changed");
    }

    private CanvasPublishApprovalService service(CanvasMapper canvasMapper,
                                                 CanvasVersionMapper versionMapper,
                                                 CanvasProjectFolderMapper folderMapper,
                                                 CanvasProjectMapper projectMapper,
                                                 CanvasProjectMemberMapper memberMapper,
                                                 ApprovalWorkflowService workflowService,
                                                 CanvasService canvasService) {
        return service(canvasMapper, versionMapper, folderMapper, projectMapper, memberMapper,
                workflowService, canvasService, null);
    }

    private CanvasPublishApprovalService service(CanvasMapper canvasMapper,
                                                 CanvasVersionMapper versionMapper,
                                                 CanvasProjectFolderMapper folderMapper,
                                                 CanvasProjectMapper projectMapper,
                                                 CanvasProjectMemberMapper memberMapper,
                                                 ApprovalWorkflowService workflowService,
                                                 CanvasService canvasService,
                                                 ApprovalLarkUserIdentityResolver identityResolver) {
        return new CanvasPublishApprovalService(
                canvasMapper,
                versionMapper,
                folderMapper,
                projectMapper,
                memberMapper,
                workflowService,
                canvasService,
                identityResolver);
    }

    private CanvasDO canvas(Long id, Long tenantId, Integer maxTotalExecutions) {
        CanvasDO row = new CanvasDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setName("Welcome canvas");
        row.setStatus(CanvasStatusEnum.DRAFT.getCode());
        row.setMaxTotalExecutions(maxTotalExecutions);
        row.setCreatedBy("alice");
        row.setUpdatedAt(LocalDateTime.parse("2026-06-06T12:00:00"));
        return row;
    }

    private CanvasVersionDO draft(Long id, String graphJson) {
        CanvasVersionDO row = new CanvasVersionDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setCanvasId(62L);
        row.setVersion(3);
        row.setStatus(VersionStatus.DRAFT.getCode());
        row.setGraphJson(graphJson);
        row.setCreatedBy("alice");
        row.setCreatedAt(LocalDateTime.parse("2026-06-06T12:05:00"));
        return row;
    }

    private CanvasProjectFolderDO folder(Long projectId) {
        CanvasProjectFolderDO row = new CanvasProjectFolderDO();
        row.setTenantId(7L);
        row.setCanvasId(62L);
        row.setProjectId(projectId);
        return row;
    }

    private CanvasProjectDO project(Long id, Integer requireReview) {
        CanvasProjectDO row = new CanvasProjectDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setProjectKey("growth");
        row.setProjectName("Growth");
        row.setStatus("ACTIVE");
        row.setRequireReviewBeforePublish(requireReview);
        return row;
    }

    private CanvasProjectMemberDO member(String username, String role) {
        CanvasProjectMemberDO row = new CanvasProjectMemberDO();
        row.setTenantId(7L);
        row.setProjectId(9L);
        row.setUsername(username);
        row.setRole(role);
        return row;
    }

    private ApprovalInstanceView instanceView(String status, Long targetVersionId) {
        return new ApprovalInstanceView(
                101L,
                7L,
                "CANVAS_PUBLISH_DEFAULT",
                "CANVAS",
                "CANVAS",
                "62",
                targetVersionId,
                status,
                "alice",
                "准备发布活动画布",
                "HIGH",
                "[\"PROJECT_REQUIRES_REVIEW\"]",
                "{\"canvasId\":62}",
                null,
                LocalDateTime.parse("2026-06-06T12:06:00"),
                "APPROVED".equals(status) ? LocalDateTime.parse("2026-06-06T12:10:00") : null,
                "APPROVED".equals(status) ? "bob" : null,
                null,
                "PUBLISH_CANVAS",
                null,
                null,
                List.of());
    }

    private ApprovalInstanceDO approvedInstance(Long canvasId, Long draftVersionId) {
        ApprovalInstanceDO row = new ApprovalInstanceDO();
        row.setId(101L);
        row.setTenantId(7L);
        row.setTargetType("CANVAS");
        row.setTargetId(String.valueOf(canvasId));
        row.setTargetVersionId(draftVersionId);
        row.setStatus("APPROVED");
        row.setAutoAction("PUBLISH_CANVAS");
        return row;
    }

    private String simpleGraph() {
        return "{\"nodes\":[{\"id\":\"start\",\"type\":\"DIRECT_CALL\"}]}";
    }

    private String couponGraph() {
        return "{\"nodes\":[{\"id\":\"start\",\"type\":\"DIRECT_CALL\"},{\"id\":\"coupon\",\"type\":\"COUPON\"}]}";
    }
}
