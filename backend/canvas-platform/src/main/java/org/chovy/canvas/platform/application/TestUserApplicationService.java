package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.TestUserFacade;
import org.chovy.canvas.platform.domain.TestUserCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试用户应用服务，负责测试用户集合、用户样本和预览数据。
 */
@Service
public class TestUserApplicationService implements TestUserFacade {

    /**
     * 测试用户接口缺省租户标识。
     */
    private static final Long DEFAULT_TENANT_ID = 0L;

    /**
     * 测试用户接口缺省操作者。
     */
    private static final String DEFAULT_ACTOR = "system";

    /**
     * 保存测试用户集合和样本数据的目录。
     */
    private final TestUserCatalog catalog;

    /**
     * 使用默认内存目录创建测试用户应用服务。
     */
    public TestUserApplicationService() {
        this(new TestUserCatalog());
    }

    /**
     * 使用指定目录创建测试用户应用服务。
     *
     * @param catalog 测试用户目录
     */
    public TestUserApplicationService(TestUserCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询测试用户集合。
     *
     * @param tenantId 租户标识
     * @return 测试用户集合列表
     */
    @Override
    public List<Map<String, Object>> listSets(Long tenantId) {
        return catalog.listSets(tenantIdOrDefault(tenantId));
    }

    /**
     * 创建测试用户集合。
     *
     * @param tenantId 租户标识
     * @param payload 集合创建参数
     * @param actor 操作者
     * @return 创建后的集合记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSet(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createSet(tenantIdOrDefault(tenantId), requireText(payload, "name", "name is required"),
                text(payload, "description"), actorOrDefault(actor));
    }

    /**
     * 查询集合内测试用户。
     *
     * @param tenantId 租户标识
     * @param setId 测试用户集合标识
     * @return 测试用户列表
     */
    @Override
    public List<Map<String, Object>> listUsers(Long tenantId, Long setId) {
        return catalog.listUsers(tenantIdOrDefault(tenantId), requireId(setId, "setId is required"));
    }

    /**
     * 创建测试用户。
     *
     * @param tenantId 租户标识
     * @param setId 测试用户集合标识
     * @param payload 用户创建参数
     * @return 创建后的测试用户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUser(Long tenantId, Long setId, Map<String, Object> payload) {
        return catalog.createUser(tenantIdOrDefault(tenantId), requireId(setId, "setId is required"),
                requireText(payload, "userId", "userId is required"), text(payload, "displayName"),
                mapValue(payload, "profile"), mapValue(payload, "inputParams"));
    }

    /**
     * 查询单个测试用户。
     *
     * @param tenantId 租户标识
     * @param id 测试用户标识
     * @return 测试用户记录
     */
    @Override
    public Map<String, Object> getUser(Long tenantId, Long id) {
        return catalog.getUser(tenantIdOrDefault(tenantId), requireId(id, "test user id is required"));
    }

    /**
     * 预览测试用户输入和画像。
     *
     * @param tenantId 租户标识
     * @param id 测试用户标识
     * @return 测试用户预览结果
     */
    @Override
    public Map<String, Object> preview(Long tenantId, Long id) {
        return catalog.preview(tenantIdOrDefault(tenantId), requireId(id, "test user id is required"));
    }

    /**
     * 将缺失租户标识归一到测试默认租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 将缺失操作者归一为系统操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    /**
     * 校验正数标识。
     *
     * @param id 原始标识
     * @param message 校验失败时使用的异常消息
     * @return 合法标识
     * @throws IllegalArgumentException 当标识为空或非正数时抛出
     */
    private static Long requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    /**
     * 读取并校验必填文本字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @param message 校验失败时使用的异常消息
     * @return 修剪后的文本
     * @throws IllegalArgumentException 当字段缺失或为空白时抛出
     */
    private static String requireText(Map<String, Object> payload, String key, String message) {
        String value = text(payload, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 从请求体读取字符串字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @return 字符串字段值；字段缺失时返回 null
     */
    private static String text(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 从请求体读取 Map 字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @return Map 字段值；字段缺失或类型不匹配时返回空 Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> payload, String key) {
        if (payload == null || !(payload.get(key) instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return (Map<String, Object>) map;
    }
}
