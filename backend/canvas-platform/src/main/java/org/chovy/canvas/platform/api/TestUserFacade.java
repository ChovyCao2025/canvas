package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供测试用户集合、用户生成和预览能力的应用入口。
 */
public interface TestUserFacade {

    /**
     * 查询租户下的测试用户集合。
     *
     * @param tenantId 租户标识
     * @return 测试用户集合列表
     */
    List<Map<String, Object>> listSets(Long tenantId);

    /**
     * 创建测试用户集合。
     *
     * @param tenantId 租户标识
     * @param payload 集合创建参数
     * @param actor 操作者
     * @return 创建后的集合记录
     */
    Map<String, Object> createSet(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询集合内的测试用户。
     *
     * @param tenantId 租户标识
     * @param setId 测试用户集合标识
     * @return 测试用户列表
     */
    List<Map<String, Object>> listUsers(Long tenantId, Long setId);

    /**
     * 在指定集合中创建测试用户。
     *
     * @param tenantId 租户标识
     * @param setId 测试用户集合标识
     * @param payload 用户创建参数
     * @return 创建后的测试用户记录
     */
    Map<String, Object> createUser(Long tenantId, Long setId, Map<String, Object> payload);

    /**
     * 查询单个测试用户详情。
     *
     * @param tenantId 租户标识
     * @param id 测试用户标识
     * @return 测试用户记录
     */
    Map<String, Object> getUser(Long tenantId, Long id);

    /**
     * 生成测试用户预览数据。
     *
     * @param tenantId 租户标识
     * @param id 测试用户标识
     * @return 测试用户预览结果
     */
    Map<String, Object> preview(Long tenantId, Long id);
}
