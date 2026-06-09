package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.EventAttrDefinitionDO;
import org.chovy.canvas.dal.mapper.EventAttrDefinitionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * EventAttributeDiscoveryService 编排 domain.cdp 场景的领域业务规则。
 */
@Service
@RequiredArgsConstructor
public class EventAttributeDiscoveryService {
    private final EventAttrDefinitionMapper attrMapper;

    /**
     * 执行业务操作 discover，作为CDP 客户数据的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @param properties 事件属性集合，用于属性发现、规则匹配或实时人群判断
     */
    public void discover(Long tenantId, String eventCode, Map<String, Object> properties) {
        if (eventCode == null || eventCode.isBlank() || properties == null || properties.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (var entry : properties.entrySet()) {
            String attrName = entry.getKey();
            if (attrName == null || attrName.isBlank()) {
                continue;
            }
            EventAttrDefinitionDO existing = attrMapper.selectOne(new LambdaQueryWrapper<EventAttrDefinitionDO>()
                    .eq(EventAttrDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                    .eq(EventAttrDefinitionDO::getEventCode, eventCode)
                    .eq(EventAttrDefinitionDO::getAttrName, attrName.trim())
                    .last("LIMIT 1"));
            if (existing != null) {
                existing.setLastSeenAt(now);
                existing.setSampleValue(sample(entry.getValue()));
                attrMapper.updateById(existing);
                continue;
            }
            EventAttrDefinitionDO created = new EventAttrDefinitionDO();
            created.setTenantId(normalizeTenantId(tenantId));
            created.setEventCode(eventCode);
            created.setAttrName(attrName.trim());
            created.setAttrType(inferType(entry.getValue()));
            created.setStatus(EventAttrDefinitionDO.PENDING_REVIEW);
            created.setSampleValue(sample(entry.getValue()));
            created.setFirstSeenAt(now);
            created.setLastSeenAt(now);
            try {
                attrMapper.insert(created);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DuplicateKeyException duplicate) {
                EventAttrDefinitionDO raced = attrMapper.selectOne(new LambdaQueryWrapper<EventAttrDefinitionDO>()
                        .eq(EventAttrDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                        .eq(EventAttrDefinitionDO::getEventCode, eventCode)
                        .eq(EventAttrDefinitionDO::getAttrName, attrName.trim())
                        .last("LIMIT 1"));
                if (raced != null) {
                    raced.setLastSeenAt(now);
                    raced.setSampleValue(sample(entry.getValue()));
                    attrMapper.updateById(raced);
                } else {
                    throw duplicate;
                }
            }
        }
    }

    /**
     * 查询业务列表，作为CDP 客户数据的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<EventAttrDefinitionDO> list(Long tenantId, String status) {
        return attrMapper.selectList(new LambdaQueryWrapper<EventAttrDefinitionDO>()
                .eq(EventAttrDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                .eq(status != null && !status.isBlank(), EventAttrDefinitionDO::getStatus, status)
                .orderByDesc(EventAttrDefinitionDO::getLastSeenAt));
    }

    /**
     * 更新业务记录，作为CDP 客户数据的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param id 目标业务记录 ID，需与租户边界匹配
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param operator 操作人标识，用于审计字段、状态流转记录或治理追踪
     */
    public void updateStatus(Long tenantId, Long id, String status, String operator) {
        if (!EventAttrDefinitionDO.APPROVED.equals(status) && !EventAttrDefinitionDO.REJECTED.equals(status)) {
            throw new IllegalArgumentException("unsupported attribute status: " + status);
        }
        EventAttrDefinitionDO row = attrMapper.selectById(id);
        if (row == null || !normalizeTenantId(tenantId).equals(row.getTenantId())) {
            throw new IllegalArgumentException("event attribute not found: " + id);
        }
        row.setStatus(status);
        row.setApprovedBy(operator);
        row.setApprovedAt(LocalDateTime.now());
        attrMapper.updateById(row);
    }

    /**
     * 执行业务操作 inferType，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * @param value 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回业务计算后的字符串结果
     */
    public String inferType(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        if (value instanceof Map<?, ?> || value instanceof Iterable<?>) {
            return "JSON";
        }
        String text = value == null ? "" : String.valueOf(value);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return text.matches("\\d{4}-\\d{2}-\\d{2}.*") ? "DATE" : "STRING";
    }

    /**
     * 执行 sample 流程，围绕 sample 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sample 生成的文本或业务键。
     */
    private String sample(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
