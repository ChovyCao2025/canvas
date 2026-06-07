package org.chovy.canvas.domain.collaboration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.UserWorkspacePreferenceDO;
import org.chovy.canvas.dal.mapper.UserWorkspacePreferenceMapper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class MyBatisUserWorkspacePreferenceRepository implements UserWorkspacePreferenceService.PreferenceRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserWorkspacePreferenceMapper mapper;
    private final ObjectMapper objectMapper;

    public MyBatisUserWorkspacePreferenceRepository(UserWorkspacePreferenceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<UserWorkspacePreferenceService.Preference> find(Long tenantId, String userId, String preferenceKey) {
        UserWorkspacePreferenceDO row = mapper.selectOne(query(tenantId, userId, preferenceKey));
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new UserWorkspacePreferenceService.Preference(
                row.getPreferenceKey(),
                readPreferenceJson(row.getPreferenceJson())));
    }

    @Override
    public void upsert(UserWorkspacePreferenceService.StoredPreference preference) {
        UserWorkspacePreferenceDO existing = mapper.selectOne(query(
                preference.tenantId(),
                preference.userId(),
                preference.preferenceKey()));
        UserWorkspacePreferenceDO row = existing == null ? new UserWorkspacePreferenceDO() : existing;
        row.setTenantId(preference.tenantId());
        row.setUserId(preference.userId());
        row.setPreferenceKey(preference.preferenceKey());
        row.setPreferenceJson(writePreferenceJson(preference.preferenceJson()));
        if (existing == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
    }

    private LambdaQueryWrapper<UserWorkspacePreferenceDO> query(Long tenantId, String userId, String preferenceKey) {
        return new LambdaQueryWrapper<UserWorkspacePreferenceDO>()
                .eq(UserWorkspacePreferenceDO::getTenantId, tenantId)
                .eq(UserWorkspacePreferenceDO::getUserId, userId)
                .eq(UserWorkspacePreferenceDO::getPreferenceKey, preferenceKey);
    }

    private Map<String, Object> readPreferenceJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    private String writePreferenceJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("preference_json is not serializable", e);
        }
    }
}
