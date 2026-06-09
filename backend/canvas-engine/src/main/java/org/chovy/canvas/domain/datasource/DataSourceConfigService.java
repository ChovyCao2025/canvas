package org.chovy.canvas.domain.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DataSourceConfigService 编排 domain.datasource 场景的领域业务规则。
 */
@Service
@RequiredArgsConstructor
public class DataSourceConfigService {

    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final DataSourceCredentialCipher credentialCipher;

    /**
     * 分页查询当前租户可见的数据源配置。
     * 查询始终受操作者租户约束，可按数据源类型和启用状态过滤，返回值中的密码仍为存储态密文。
     */
    public PageResult<DataSourceConfigDO> list(int page, int size, String type, Integer enabled,
                                               TenantContext operator) {
        LambdaQueryWrapper<DataSourceConfigDO> wrapper = tenantScopedWrapper(operator)
                .orderByDesc(DataSourceConfigDO::getId);
        if (type != null && !type.isBlank()) {
            wrapper.eq(DataSourceConfigDO::getType, type);
        }
        if (enabled != null) {
            wrapper.eq(DataSourceConfigDO::getEnabled, enabled);
        }
        Page<DataSourceConfigDO> result = dataSourceConfigMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    /**
     * 在当前租户下创建数据源配置。
     * 方法会校验必填连接信息并加密密码后入库，返回插入后的配置实体。
     */
    public DataSourceConfigDO create(DataSourceConfigDO body, TenantContext operator) {
        normalizeForCreate(body);
        body.setTenantId(requireTenantId(operator));
        body.setPassword(credentialCipher.encrypt(body.getPassword()));
        dataSourceConfigMapper.insert(body);
        return body;
    }

    @Transactional
    /**
     * 更新当前租户可见的数据源配置。
     * 先校验配置归属租户，未传新密码时保留原密文，传入密码时重新加密保存。
     */
    public void update(Long id, DataSourceConfigDO body, TenantContext operator) {
        Long tenantId = requireTenantId(operator);
        DataSourceConfigDO existing = requireVisible(id, operator);
        body.setId(id);
        body.setTenantId(tenantId);
        normalizeForUpdate(body, existing);
        dataSourceConfigMapper.updateById(body);
    }

    @Transactional
    /**
     * 轮换数据源密码。
     * 只更新当前租户可见配置的密码密文并保留其它连接字段，避免普通更新接口误清空敏感配置。
     */
    public void rotatePassword(Long id, String rawPassword, TenantContext operator) {
        requireText(rawPassword, "password");
        Long tenantId = requireTenantId(operator);
        DataSourceConfigDO existing = requireVisible(id, operator);
        DataSourceConfigDO rotated = new DataSourceConfigDO();
        rotated.setId(id);
        rotated.setTenantId(tenantId);
        rotated.setName(existing.getName());
        rotated.setType(existing.getType());
        rotated.setUrl(existing.getUrl());
        rotated.setUsername(existing.getUsername());
        rotated.setPassword(credentialCipher.encrypt(rawPassword));
        rotated.setDriverClassName(existing.getDriverClassName());
        rotated.setDescription(existing.getDescription());
        rotated.setEnabled(existing.getEnabled());
        rotated.setCreatedBy(existing.getCreatedBy());
        dataSourceConfigMapper.updateById(rotated);
    }

    @Transactional
    /**
     * 删除当前租户可见的数据源配置。
     * 删除前会按租户过滤查找，找不到时按不存在处理并抛出异常。
     */
    public void delete(Long id, TenantContext operator) {
        requireVisible(id, operator);
        dataSourceConfigMapper.deleteById(id);
    }

    /**
     * 读取并要求数据源配置对当前操作者租户可见。
     * 返回存储态配置，密码仍保持密文，通常用于管理端校验归属。
     */
    public DataSourceConfigDO requireVisible(Long id, TenantContext operator) {
        DataSourceConfigDO config = dataSourceConfigMapper.selectOne(
                tenantScopedWrapper(operator).eq(DataSourceConfigDO::getId, id).last("LIMIT 1"));
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        return config;
    }

    /**
     * 读取运行时使用的数据源配置。
     * 运行时入口按 ID 读取并解密密码，供执行引擎建立连接；调用方需在上层完成权限控制。
     */
    public DataSourceConfigDO getForRuntime(Long id) {
        DataSourceConfigDO config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        config.setPassword(credentialCipher.decrypt(config.getPassword()));
        return config;
    }

    /**
     * 解密存储态数据源密码。
     * 供需要单独处理凭据的运行时代码复用，不执行租户或配置可见性校验。
     */
    public String decryptPassword(String storedPassword) {
        return credentialCipher.decrypt(storedPassword);
    }

    /**
     * 执行 tenantScopedWrapper 流程，围绕 tenant scoped wrapper 完成校验、计算或结果组装。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 tenantScopedWrapper 流程生成的业务结果。
     */
    private LambdaQueryWrapper<DataSourceConfigDO> tenantScopedWrapper(TenantContext operator) {
        return new LambdaQueryWrapper<DataSourceConfigDO>()
                .eq(DataSourceConfigDO::getTenantId, requireTenantId(operator));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 require tenant id 计算得到的数量、金额或指标值。
     */
    private Long requireTenantId(TenantContext operator) {
        if (operator == null || operator.tenantId() == null) {
            throw new IllegalStateException("tenantId is required for data source operation");
        }
        return operator.tenantId();
    }

    /**
     * 规范化输入值。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void normalizeForCreate(DataSourceConfigDO body) {
        normalizeCommon(body);
        requireText(body.getPassword(), "password");
    }

    /**
     * 规范化输入值。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param existing existing 参数，用于 normalizeForUpdate 流程中的校验、计算或对象转换。
     */
    private void normalizeForUpdate(DataSourceConfigDO body, DataSourceConfigDO existing) {
        normalizeCommon(body);
        if (body.getPassword() == null || body.getPassword().isBlank()) {
            body.setPassword(existing.getPassword());
        } else {
            body.setPassword(credentialCipher.encrypt(body.getPassword()));
        }
        if (body.getCreatedBy() == null) {
            body.setCreatedBy(existing.getCreatedBy());
        }
    }

    /**
     * 规范化输入值。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void normalizeCommon(DataSourceConfigDO body) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (body.getType() == null || body.getType().isBlank()) {
            body.setType("JDBC");
        }
        if (!"JDBC".equals(body.getType())) {
            throw new IllegalArgumentException("Unsupported data source type: " + body.getType());
        }
        requireText(body.getName(), "name");
        requireText(body.getUrl(), "url");
        requireText(body.getUsername(), "username");
        if (body.getDriverClassName() == null || body.getDriverClassName().isBlank()) {
            body.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     */
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }
}
