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

@Service
@RequiredArgsConstructor
public class DataSourceConfigService {

    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final DataSourceCredentialCipher credentialCipher;

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
    public DataSourceConfigDO create(DataSourceConfigDO body, TenantContext operator) {
        normalizeForCreate(body);
        body.setTenantId(requireTenantId(operator));
        body.setPassword(credentialCipher.encrypt(body.getPassword()));
        dataSourceConfigMapper.insert(body);
        return body;
    }

    @Transactional
    public void update(Long id, DataSourceConfigDO body, TenantContext operator) {
        Long tenantId = requireTenantId(operator);
        DataSourceConfigDO existing = requireVisible(id, operator);
        body.setId(id);
        body.setTenantId(tenantId);
        normalizeForUpdate(body, existing);
        dataSourceConfigMapper.updateById(body);
    }

    @Transactional
    public void delete(Long id, TenantContext operator) {
        requireVisible(id, operator);
        dataSourceConfigMapper.deleteById(id);
    }

    public DataSourceConfigDO requireVisible(Long id, TenantContext operator) {
        DataSourceConfigDO config = dataSourceConfigMapper.selectOne(
                tenantScopedWrapper(operator).eq(DataSourceConfigDO::getId, id).last("LIMIT 1"));
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        return config;
    }

    public DataSourceConfigDO getForRuntime(Long id) {
        DataSourceConfigDO config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        config.setPassword(credentialCipher.decrypt(config.getPassword()));
        return config;
    }

    public String decryptPassword(String storedPassword) {
        return credentialCipher.decrypt(storedPassword);
    }

    private LambdaQueryWrapper<DataSourceConfigDO> tenantScopedWrapper(TenantContext operator) {
        return new LambdaQueryWrapper<DataSourceConfigDO>()
                .eq(DataSourceConfigDO::getTenantId, requireTenantId(operator));
    }

    private Long requireTenantId(TenantContext operator) {
        if (operator == null || operator.tenantId() == null) {
            throw new IllegalStateException("tenantId is required for data source operation");
        }
        return operator.tenantId();
    }

    private void normalizeForCreate(DataSourceConfigDO body) {
        normalizeCommon(body);
        requireText(body.getPassword(), "password");
    }

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

    private void normalizeCommon(DataSourceConfigDO body) {
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

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }
}
