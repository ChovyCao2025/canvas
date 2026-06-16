package org.chovy.canvas.canvas.application;

import java.time.LocalDateTime;
import java.util.Optional;

import org.chovy.canvas.canvas.domain.UserInputResponse;

/**
 * 定义UserInputResponseRepository对外提供的能力契约。
 */
public interface UserInputResponseRepository {

    /**
     * 保存。
     */
    UserInputResponse save(UserInputResponse response);

    /**
     * 处理completePending。
     */
    Optional<UserInputResponse> completePending(Long responseId, String responseJson, LocalDateTime updatedAt);

    /**
     * 查询ByTenantIdAndIdempotencyKey。
     */
    Optional<UserInputResponse> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    /**
     * 查询by标识。
     */
    Optional<UserInputResponse> findById(Long responseId);
}
