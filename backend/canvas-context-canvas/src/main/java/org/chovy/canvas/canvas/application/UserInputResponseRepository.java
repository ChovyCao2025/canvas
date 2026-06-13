package org.chovy.canvas.canvas.application;

import java.time.LocalDateTime;
import java.util.Optional;

import org.chovy.canvas.canvas.domain.UserInputResponse;

public interface UserInputResponseRepository {

    UserInputResponse save(UserInputResponse response);

    Optional<UserInputResponse> completePending(Long responseId, String responseJson, LocalDateTime updatedAt);

    Optional<UserInputResponse> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    Optional<UserInputResponse> findById(Long responseId);
}
