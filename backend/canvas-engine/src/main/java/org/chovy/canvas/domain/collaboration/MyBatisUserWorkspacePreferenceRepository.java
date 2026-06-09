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

/**
 * MyBatisUserWorkspacePreferenceRepository 编排 domain.collaboration 场景的领域业务规则。
 */
@Repository
public class MyBatisUserWorkspacePreferenceRepository implements UserWorkspacePreferenceService.PreferenceRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserWorkspacePreferenceMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 MyBatisUserWorkspacePreferenceRepository 实例并注入 domain.collaboration 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MyBatisUserWorkspacePreferenceRepository(UserWorkspacePreferenceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /**
     * find 查询 domain.collaboration 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param preferenceKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * upsert 更新 domain.collaboration 场景的业务状态。
     * @param preference preference 参数，用于 upsert 流程中的校验、计算或对象转换。
     */
    @Override
    public void upsert(UserWorkspacePreferenceService.StoredPreference preference) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        UserWorkspacePreferenceDO existing = mapper.selectOne(query(
                preference.tenantId(),
                preference.userId(),
                preference.preferenceKey()));
        UserWorkspacePreferenceDO row = existing == null ? new UserWorkspacePreferenceDO() : existing;
        row.setTenantId(preference.tenantId());
        row.setUserId(preference.userId());
        row.setPreferenceKey(preference.preferenceKey());
        row.setPreferenceJson(writePreferenceJson(preference.preferenceJson()));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (existing == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param preferenceKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private LambdaQueryWrapper<UserWorkspacePreferenceDO> query(Long tenantId, String userId, String preferenceKey) {
        return new LambdaQueryWrapper<UserWorkspacePreferenceDO>()
                .eq(UserWorkspacePreferenceDO::getTenantId, tenantId)
                .eq(UserWorkspacePreferenceDO::getUserId, userId)
                .eq(UserWorkspacePreferenceDO::getPreferenceKey, preferenceKey);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 readPreferenceJson 流程生成的业务结果。
     */
    private Map<String, Object> readPreferenceJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param String string 参数，用于 writePreferenceJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write preference json 生成的文本或业务键。
     */
    private String writePreferenceJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("preference_json is not serializable", e);
        }
    }
}
