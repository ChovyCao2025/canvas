package org.chovy.canvas.canvas.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.domain.UserInputForm;
import org.chovy.canvas.canvas.domain.UserInputResponse;
import org.chovy.canvas.canvas.domain.UserInputStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserInputApplicationService {

    private final UserInputFormRepository formRepository;
    private final UserInputResponseRepository responseRepository;
    private final UserInputResumePort resumePort;
    private final Clock clock;

    public UserInputApplicationService(UserInputFormRepository formRepository,
                                       UserInputResponseRepository responseRepository,
                                       ObjectProvider<UserInputResumePort> resumePort) {
        this(formRepository,
                responseRepository,
                resumePort.getIfAvailable(() -> request -> {
                }),
                Clock.systemDefaultZone());
    }

    UserInputApplicationService(UserInputFormRepository formRepository,
                                UserInputResponseRepository responseRepository,
                                UserInputResumePort resumePort,
                                Clock clock) {
        this.formRepository = formRepository;
        this.responseRepository = responseRepository;
        this.resumePort = resumePort;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public PendingInput createPending(CreatePendingCommand command) {
        requirePendingCommand(command);
        String idempotencyKey = idempotencyKey(command.executionId(), command.nodeId(), command.userId());
        return responseRepository.findByTenantIdAndIdempotencyKey(command.tenantId(), idempotencyKey)
                .map(existing -> new PendingInput(existing.formId(), existing.id(), existing.status().name(),
                        existing.expiresAt(), existing.timeoutNodeId()))
                .orElseGet(() -> createNewPending(command, idempotencyKey));
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmitResult submit(Long responseId, SubmitCommand command) {
        UserInputResponse current = responseRepository.findById(responseId)
                .orElseThrow(() -> new IllegalArgumentException("User input response not found: " + responseId));
        if (current.status() != UserInputStatus.PENDING) {
            return new SubmitResult(current.id(), current.status().name(), true);
        }
        String responseJson = JsonSupport.toJson(command == null || command.response() == null
                ? Map.of()
                : command.response());
        UserInputResponse completed = responseRepository.completePending(current.id(), responseJson, now())
                .orElse(null);
        if (completed == null) {
            UserInputResponse latest = responseRepository.findById(responseId)
                    .orElseThrow(() -> new IllegalArgumentException("User input response not found: " + responseId));
            return new SubmitResult(latest.id(), latest.status().name(), true);
        }
        Map<String, Object> payload = resumePayload(completed, command == null ? Map.of() : command.response());
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

    private static String idempotencyKey(String executionId, String nodeId, String userId) {
        return "USER_INPUT:" + executionId + ":" + nodeId + ":" + userId;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

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

    public record CreatePendingCommand(
            Long tenantId,
            Long canvasId,
            Long versionId,
            String executionId,
            String nodeId,
            String userId,
            String schemaJson,
            String completedNodeId,
            String timeoutNodeId,
            LocalDateTime expiresAt) {
    }

    public record SubmitCommand(
            Map<String, Object> response,
            String operator) {
    }

    public record PendingInput(
            Long formId,
            Long responseId,
            String status,
            LocalDateTime expiresAt,
            String timeoutNodeId) {
    }

    public record SubmitResult(
            Long responseId,
            String status,
            boolean duplicate) {
    }
}
