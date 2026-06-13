package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.canvas.application.UserInputResponseRepository;
import org.chovy.canvas.canvas.domain.UserInputResponse;
import org.chovy.canvas.canvas.domain.UserInputStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserInputResponseRepository implements UserInputResponseRepository {

    private final UserInputResponseMapper mapper;

    public MybatisUserInputResponseRepository(UserInputResponseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserInputResponse save(UserInputResponse response) {
        UserInputResponseDO row = UserInputPersistenceMapper.toResponseRow(response);
        if (row.getId() == null) {
            int inserted = mapper.insert(row);
            if (inserted <= 0) {
                throw new IllegalStateException("User input response insert affected 0 rows");
            }
        } else {
            int updated = mapper.updateById(row);
            if (updated <= 0) {
                throw new IllegalStateException("User input response update affected 0 rows: " + row.getId());
            }
        }
        return UserInputPersistenceMapper.toResponseDomain(row);
    }

    @Override
    public Optional<UserInputResponse> completePending(Long responseId, String responseJson, LocalDateTime updatedAt) {
        UserInputResponseDO update = new UserInputResponseDO();
        update.setStatus(UserInputStatus.COMPLETED.name());
        update.setResponseJson(responseJson);
        update.setUpdatedAt(updatedAt);
        int updated = mapper.update(update, new LambdaUpdateWrapper<UserInputResponseDO>()
                .eq(UserInputResponseDO::getId, responseId)
                .eq(UserInputResponseDO::getStatus, UserInputStatus.PENDING.name()));
        if (updated <= 0) {
            return Optional.empty();
        }
        return findById(responseId);
    }

    @Override
    public Optional<UserInputResponse> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey) {
        UserInputResponseDO row = mapper.selectOne(new LambdaQueryWrapper<UserInputResponseDO>()
                .eq(UserInputResponseDO::getTenantId, tenantId)
                .eq(UserInputResponseDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        return Optional.ofNullable(UserInputPersistenceMapper.toResponseDomain(row));
    }

    @Override
    public Optional<UserInputResponse> findById(Long responseId) {
        return Optional.ofNullable(UserInputPersistenceMapper.toResponseDomain(mapper.selectById(responseId)));
    }
}
