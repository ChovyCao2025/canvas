package org.chovy.canvas.canvas.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.UserInputFacade;
import org.chovy.canvas.canvas.domain.UserInputForm;
import org.chovy.canvas.canvas.domain.UserInputResponse;
import org.chovy.canvas.canvas.domain.UserInputStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 封装UserInputApplicationService相关的业务逻辑。
 */
@Service
public class UserInputApplicationService implements UserInputFacade {

    /**
     * 保存formRepository。
     */
    private final UserInputFormRepository formRepository;

    /**
     * 保存responseRepository。
     */
    private final UserInputResponseRepository responseRepository;

    /**
     * 保存resumePort。
     */
    private final UserInputResumePort resumePort;

    /**
     * 保存时钟。
     */
    private final Clock clock;

    /**
     * 使用用户输入仓储和恢复端口创建应用服务。
     */
    @Autowired
    public UserInputApplicationService(UserInputFormRepository formRepository,
                                       UserInputResponseRepository responseRepository,
                                       ObjectProvider<UserInputResumePort> resumePort) {
        this(formRepository,
                responseRepository,
                resumePort.getIfAvailable(() -> request -> {
                }),
                Clock.systemDefaultZone());
    }

    /**
     * 使用用户输入仓储和恢复端口创建应用服务。
     */
    UserInputApplicationService(UserInputFormRepository formRepository,
                                UserInputResponseRepository responseRepository,
                                UserInputResumePort resumePort,
                                Clock clock) {
        this.formRepository = formRepository;
        this.responseRepository = responseRepository;
        this.resumePort = resumePort;
        this.clock = clock;
    }

    /**
     * 创建Pending。
     */
    @Transactional(rollbackFor = Exception.class)
    public PendingInput createPending(CreatePendingCommand command) {
        requirePendingCommand(command);
        String idempotencyKey = idempotencyKey(command.executionId(), command.nodeId(), command.userId());
        // 同一执行、节点和用户只能有一个等待输入，避免刷新或重试创建重复表单。
        return responseRepository.findByTenantIdAndIdempotencyKey(command.tenantId(), idempotencyKey)
                .map(existing -> new PendingInput(existing.formId(), existing.id(), existing.status().name(),
                        existing.expiresAt(), existing.timeoutNodeId()))
                .orElseGet(() -> createNewPending(command, idempotencyKey));
    }

    /**
     * 处理submit。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInputFacade.SubmitResult submit(Long responseId, UserInputFacade.SubmitCommand command) {
        SubmitResult result = submit(responseId, new SubmitCommand(
                command == null ? Map.of() : command.response(),
                command == null ? null : command.operator()));
        return new UserInputFacade.SubmitResult(result.responseId(), result.status(), result.duplicate());
    }

    /**
     * 处理submit。
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmitResult submit(Long responseId, SubmitCommand command) {
        UserInputResponse current = responseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("User input response not found: " + responseId));
        if (current.status() != UserInputStatus.PENDING) {
            // 已完成或超时的响应按重复提交处理，保证用户端重试具备幂等语义。
            return new SubmitResult(current.id(), current.status().name(), true);
        }
        String responseJson = JsonSupport.toJson(command == null || command.response() == null
                ? Map.of()
                : command.response());
        UserInputResponse completed = responseRepository.completePending(current.id(), responseJson, now())
                .orElse(null);
        if (completed == null) {
            // 条件更新失败说明并发请求已抢先完成，重新读取最新状态返回给调用方。
            UserInputResponse latest = responseRepository.findById(responseId)
                    .orElseThrow(() -> new IllegalArgumentException("User input response not found: " + responseId));
            return new SubmitResult(latest.id(), latest.status().name(), true);
        }
        Map<String, Object> payload = resumePayload(completed, command == null ? Map.of() : command.response());
        // 恢复执行必须发生在提交之后，避免引擎读取到尚未持久化的输入结果。
        AfterCommitExecutor.runAfterCommitOrNow(() -> resumePort.requestResume(new UserInputResumeRequest(
                completed.tenantId(),
                completed.canvasId(),
                completed.versionId(),
                completed.executionId(),
                completed.nodeId(),
                completed.userId(),
                completed.id(),
                completed.status().name(),
                payload)));
        return new SubmitResult(completed.id(), completed.status().name(), false);
    }

    /**
     * 创建NewPending。
     */
    private PendingInput createNewPending(CreatePendingCommand command, String idempotencyKey) {
        LocalDateTime now = now();
        UserInputForm form = formRepository.save(UserInputForm.pending(
                null,
                command.tenantId(),
                command.canvasId(),
                command.versionId(),
                command.executionId(),
                command.nodeId(),
                command.userId(),
                command.schemaJson(),
                command.completedNodeId(),
                command.timeoutNodeId(),
                command.expiresAt(),
                now));
        UserInputResponse response = responseRepository.save(UserInputResponse.pending(
                null,
                command.tenantId(),
                form.id(),
                command.canvasId(),
                command.versionId(),
                command.executionId(),
                command.nodeId(),
                command.userId(),
                idempotencyKey,
                command.completedNodeId(),
                command.timeoutNodeId(),
                command.expiresAt()).withTimestamps(now, now));
        return new PendingInput(form.id(), response.id(), response.status().name(),
                response.expiresAt(), response.timeoutNodeId());
    }

    /**
     * 处理resumePayload。
     */
    private Map<String, Object> resumePayload(UserInputResponse response, Map<String, Object> inputResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceNodeId", response.nodeId());
        payload.put("waitResumeStatus", response.status().name());
        payload.put("executionId", response.executionId());
        payload.put("inputResponseId", response.id());
        payload.put("inputResponse", inputResponse == null ? Map.of() : inputResponse);
        payload.put("completedNodeId", response.completedNodeId());
        payload.put("timeoutNodeId", response.timeoutNodeId());
        return payload;
    }

    /**
     * 处理idempotencyKey。
     */
    private static String idempotencyKey(String executionId, String nodeId, String userId) {
        return "USER_INPUT:" + executionId + ":" + nodeId + ":" + userId;
    }

    /**
     * 处理now。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 校验并返回PendingCommand。
     */
    private static void requirePendingCommand(CreatePendingCommand command) {
        if (command == null
                || command.tenantId() == null
                || command.canvasId() == null
                || command.versionId() == null
                || command.executionId() == null
                || command.nodeId() == null
                || command.userId() == null
                || command.schemaJson() == null
                || command.schemaJson().isBlank()) {
            throw new IllegalArgumentException("User input runtime context is incomplete");
        }
    }

    /**
     * 承载CreatePendingCommand的数据快照。
     */
    public record CreatePendingCommand(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录版本标识。
             */
            Long versionId,
            /**
             * 记录execution标识。
             */
            String executionId,
            /**
             * 记录节点标识。
             */
            String nodeId,
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录schemaJSON 内容。
             */
            String schemaJson,
            /**
             * 记录completed node标识。
             */
            String completedNodeId,
            /**
             * 记录timeout node标识。
             */
            String timeoutNodeId,
            /**
             * 记录expires时间。
             */
            LocalDateTime expiresAt) {
    }

    /**
     * 承载SubmitCommand的数据快照。
     */
    public record SubmitCommand(
            /**
             * 记录响应。
             */
            Map<String, Object> response,
            /**
             * 记录操作人。
             */
            String operator) {
    }

    /**
     * 承载PendingInput的数据快照。
     */
    public record PendingInput(
            /**
             * 记录form标识。
             */
            Long formId,
            /**
             * 记录响应标识。
             */
            Long responseId,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录expires时间。
             */
            LocalDateTime expiresAt,
            /**
             * 记录timeout node标识。
             */
            String timeoutNodeId) {
    }

    /**
     * 承载SubmitResult的数据快照。
     */
    public record SubmitResult(
            /**
             * 记录响应标识。
             */
            Long responseId,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录duplicate。
             */
            boolean duplicate) {
    }
}
