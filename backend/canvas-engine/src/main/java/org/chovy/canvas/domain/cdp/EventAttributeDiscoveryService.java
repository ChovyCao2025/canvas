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

@Service
@RequiredArgsConstructor
public class EventAttributeDiscoveryService {
    private final EventAttrDefinitionMapper attrMapper;

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

    public List<EventAttrDefinitionDO> list(Long tenantId, String status) {
        return attrMapper.selectList(new LambdaQueryWrapper<EventAttrDefinitionDO>()
                .eq(EventAttrDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                .eq(status != null && !status.isBlank(), EventAttrDefinitionDO::getStatus, status)
                .orderByDesc(EventAttrDefinitionDO::getLastSeenAt));
    }

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

    public String inferType(Object value) {
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
        return text.matches("\\d{4}-\\d{2}-\\d{2}.*") ? "DATE" : "STRING";
    }

    private String sample(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
