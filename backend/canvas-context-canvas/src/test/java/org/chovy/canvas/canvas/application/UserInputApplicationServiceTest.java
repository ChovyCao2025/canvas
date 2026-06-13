package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.chovy.canvas.canvas.domain.UserInputForm;
import org.chovy.canvas.canvas.domain.UserInputResponse;
import org.chovy.canvas.canvas.domain.UserInputStatus;
import org.junit.jupiter.api.Test;

class UserInputApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-04T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void createPendingIsIdempotentByRuntimeKey() {
        InMemoryUserInputFormRepository forms = new InMemoryUserInputFormRepository();
        InMemoryUserInputResponseRepository responses = new InMemoryUserInputResponseRepository();
        CapturingResumePort resumePort = new CapturingResumePort();
        UserInputApplicationService service = new UserInputApplicationService(forms, responses, resumePort, CLOCK);
        UserInputResponse existing = responses.save(UserInputResponse.pending(
                null, 7L, 11L, 10L, 20L, "exec-1", "input-1", "user-1",
                "USER_INPUT:exec-1:input-1:user-1", "done-1", "timeout-1", null));

        UserInputApplicationService.PendingInput pending = service.createPending(new UserInputApplicationService.CreatePendingCommand(
                7L, 10L, 20L, "exec-1", "input-1", "user-1",
                "{\"fields\":[{\"key\":\"email\"}]}", "done-1", "timeout-1", null));

        assertThat(pending.responseId()).isEqualTo(existing.id());
        assertThat(pending.status()).isEqualTo(UserInputStatus.PENDING.name());
        assertThat(forms.rows).isEmpty();
    }

    @Test
    void createPendingStoresFormAndResponseOwnedByCanvas() {
        InMemoryUserInputFormRepository forms = new InMemoryUserInputFormRepository();
        InMemoryUserInputResponseRepository responses = new InMemoryUserInputResponseRepository();
        UserInputApplicationService service = new UserInputApplicationService(forms, responses, new CapturingResumePort(), CLOCK);

        UserInputApplicationService.PendingInput pending = service.createPending(new UserInputApplicationService.CreatePendingCommand(
                7L, 10L, 20L, "exec-1", "input-1", "user-1",
                "{\"fields\":[{\"key\":\"email\"}]}", "done-1", "timeout-1",
                LocalDateTime.of(2026, 6, 4, 10, 30)));

        assertThat(pending.formId()).isEqualTo(1L);
        assertThat(pending.responseId()).isEqualTo(1L);
        assertThat(forms.rows).hasSize(1);
        assertThat(responses.rows).hasSize(1);
        assertThat(forms.rows.get(0).schemaJson()).contains("email");
        assertThat(responses.rows.get(0).idempotencyKey()).isEqualTo("USER_INPUT:exec-1:input-1:user-1");
        assertThat(responses.rows.get(0).status()).isEqualTo(UserInputStatus.PENDING);
    }

    @Test
    void submitCompletesPendingResponseAndRequestsExecutionResumeThroughPort() {
        InMemoryUserInputFormRepository forms = new InMemoryUserInputFormRepository();
        InMemoryUserInputResponseRepository responses = new InMemoryUserInputResponseRepository();
        CapturingResumePort resumePort = new CapturingResumePort();
        UserInputResponse pending = responses.save(UserInputResponse.pending(
                null, 7L, 11L, 10L, 20L, "exec-1", "input-1", "user-1",
                "USER_INPUT:exec-1:input-1:user-1", "done-1", "timeout-1", null));
        UserInputApplicationService service = new UserInputApplicationService(forms, responses, resumePort, CLOCK);

        UserInputApplicationService.SubmitResult result = service.submit(pending.id(),
                new UserInputApplicationService.SubmitCommand(Map.of("email", "a@example.com"), "alice"));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.status()).isEqualTo(UserInputStatus.COMPLETED.name());
        assertThat(responses.findById(pending.id())).get()
                .extracting(UserInputResponse::status, UserInputResponse::responseJson)
                .containsExactly(UserInputStatus.COMPLETED, "{\"email\":\"a@example.com\"}");
        assertThat(resumePort.request).isNotNull();
        assertThat(resumePort.request.responseId()).isEqualTo(pending.id());
        assertThat(resumePort.request.resumeStatus()).isEqualTo(UserInputStatus.COMPLETED.name());
        assertThat(resumePort.request.payload())
                .containsEntry("sourceNodeId", "input-1")
                .containsEntry("waitResumeStatus", UserInputStatus.COMPLETED.name())
                .containsEntry("executionId", "exec-1")
                .containsEntry("completedNodeId", "done-1")
                .containsEntry("timeoutNodeId", "timeout-1");
    }

    @Test
    void submitAlreadyCompletedResponseIsDuplicateAndDoesNotRequestResume() {
        InMemoryUserInputResponseRepository responses = new InMemoryUserInputResponseRepository();
        CapturingResumePort resumePort = new CapturingResumePort();
        UserInputResponse completed = responses.save(UserInputResponse.pending(
                null, 7L, 11L, 10L, 20L, "exec-1", "input-1", "user-1",
                "USER_INPUT:exec-1:input-1:user-1", "done-1", "timeout-1", null)
                .complete("{\"email\":\"a@example.com\"}"));
        UserInputApplicationService service = new UserInputApplicationService(
                new InMemoryUserInputFormRepository(), responses, resumePort, CLOCK);

        UserInputApplicationService.SubmitResult result = service.submit(completed.id(),
                new UserInputApplicationService.SubmitCommand(Map.of("email", "b@example.com"), "alice"));

        assertThat(result.duplicate()).isTrue();
        assertThat(result.status()).isEqualTo(UserInputStatus.COMPLETED.name());
        assertThat(resumePort.request).isNull();
    }

    @Test
    void submitLosingPendingRaceIsDuplicateAndDoesNotRequestResume() {
        RaceLosingUserInputResponseRepository responses = new RaceLosingUserInputResponseRepository();
        CapturingResumePort resumePort = new CapturingResumePort();
        UserInputResponse pending = responses.save(UserInputResponse.pending(
                null, 7L, 11L, 10L, 20L, "exec-1", "input-1", "user-1",
                "USER_INPUT:exec-1:input-1:user-1", "done-1", "timeout-1", null));
        UserInputApplicationService service = new UserInputApplicationService(
                new InMemoryUserInputFormRepository(), responses, resumePort, CLOCK);

        UserInputApplicationService.SubmitResult result = service.submit(pending.id(),
                new UserInputApplicationService.SubmitCommand(Map.of("email", "b@example.com"), "alice"));

        assertThat(result.duplicate()).isTrue();
        assertThat(result.status()).isEqualTo(UserInputStatus.COMPLETED.name());
        assertThat(resumePort.request).isNull();
        assertThat(responses.findById(pending.id())).get()
                .extracting(UserInputResponse::responseJson)
                .isEqualTo("{\"email\":\"already@example.com\"}");
    }

    private static final class CapturingResumePort implements UserInputResumePort {
        private UserInputResumeRequest request;

        @Override
        public void requestResume(UserInputResumeRequest request) {
            this.request = request;
        }
    }

    private static final class InMemoryUserInputFormRepository implements UserInputFormRepository {
        private final List<UserInputForm> rows = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public UserInputForm save(UserInputForm form) {
            UserInputForm saved = form.id() == null ? form.withId(nextId++) : form;
            rows.removeIf(row -> row.id().equals(saved.id()));
            rows.add(saved);
            return saved;
        }
    }

    private static class InMemoryUserInputResponseRepository implements UserInputResponseRepository {
        private final List<UserInputResponse> rows = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public UserInputResponse save(UserInputResponse response) {
            UserInputResponse saved = response.id() == null ? response.withId(nextId++) : response;
            rows.removeIf(row -> row.id().equals(saved.id()));
            rows.add(saved);
            return saved;
        }

        @Override
        public Optional<UserInputResponse> completePending(Long responseId, String responseJson, LocalDateTime updatedAt) {
            Optional<UserInputResponse> current = findById(responseId);
            if (current.isEmpty() || current.get().status() != UserInputStatus.PENDING) {
                return Optional.empty();
            }
            return Optional.of(save(current.get().complete(responseJson, updatedAt)));
        }

        @Override
        public Optional<UserInputResponse> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey) {
            return rows.stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .filter(row -> row.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public Optional<UserInputResponse> findById(Long responseId) {
            return rows.stream().filter(row -> row.id().equals(responseId)).findFirst();
        }
    }

    private static final class RaceLosingUserInputResponseRepository extends InMemoryUserInputResponseRepository {
        private boolean returnStalePendingOnce = true;

        @Override
        public Optional<UserInputResponse> findById(Long responseId) {
            Optional<UserInputResponse> current = super.findById(responseId);
            if (returnStalePendingOnce
                    && current.isPresent()
                    && current.get().status() == UserInputStatus.PENDING) {
                returnStalePendingOnce = false;
                super.save(current.get().complete("{\"email\":\"already@example.com\"}", LocalDateTime.now(CLOCK)));
                return current;
            }
            return current;
        }

        @Override
        public Optional<UserInputResponse> completePending(Long responseId, String responseJson, LocalDateTime updatedAt) {
            Optional<UserInputResponse> current = super.findById(responseId);
            if (current.isEmpty() || current.get().status() != UserInputStatus.PENDING) {
                return Optional.empty();
            }
            return Optional.of(super.save(current.get().complete(responseJson, updatedAt)));
        }
    }
}
