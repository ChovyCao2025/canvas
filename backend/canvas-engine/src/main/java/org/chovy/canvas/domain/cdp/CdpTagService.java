package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserTagDTO;
import org.chovy.canvas.dto.cdp.CdpUserTagHistoryDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagHistoryDO;
import org.chovy.canvas.dal.mapper.CdpUserTagHistoryMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;

@Service
@RequiredArgsConstructor
public class CdpTagService {

    private final TagDefinitionMapper tagDefinitionMapper;
    private final CdpUserTagMapper userTagMapper;
    private final CdpUserTagHistoryMapper historyMapper;
    private final CdpUserService userService;

    public CdpUserTagDO setTag(String userId, CdpTagWriteReq req) {
        String normalizedUserId = requireText(userId, "userId");
        String tagCode = requireText(req.tagCode(), "tagCode");
        TagDefinitionDO def = getEnabledTag(tagCode);
        String sourceType = req.sourceType() == null || req.sourceType().isBlank() ? "MANUAL" : req.sourceType();
        if ("MANUAL".equals(sourceType) && Integer.valueOf(0).equals(def.getManualEnabled())) {
            throw new IllegalArgumentException("标签不允许人工打标: " + tagCode);
        }
        String value = normalizeValue(def.getValueType(), req.tagValue());
        userService.ensureUser(normalizedUserId, sourceType, req.sourceRefId());

        CdpUserTagDO existing = userTagMapper.selectOne(new LambdaQueryWrapper<CdpUserTagDO>()
                .eq(CdpUserTagDO::getUserId, normalizedUserId)
                .eq(CdpUserTagDO::getTagCode, tagCode));
        String oldValue = existing != null ? existing.getTagValue() : null;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = req.expiresAt();
        if (expiresAt == null && def.getDefaultTtlDays() != null) {
            expiresAt = now.plusDays(def.getDefaultTtlDays());
        }

        CdpUserTagDO tag = existing != null ? existing : new CdpUserTagDO();
        tag.setUserId(normalizedUserId);
        tag.setTagCode(tagCode);
        tag.setTagValue(value);
        tag.setValueType(def.getValueType() == null ? "STRING" : def.getValueType());
        tag.setSourceType(sourceType);
        tag.setSourceRefId(req.sourceRefId());
        tag.setStatus("ACTIVE");
        tag.setEffectiveAt(now);
        tag.setExpiresAt(expiresAt);
        tag.setCreatedBy(req.operator());
        if (existing == null) {
            userTagMapper.insert(tag);
        } else {
            userTagMapper.updateById(tag);
        }

        writeHistory(normalizedUserId, tagCode, oldValue, value, "SET", sourceType,
                req.sourceRefId(), req.idempotencyKey(), req.reason(), req.operator());
        return tag;
    }

    public void removeTag(String userId, String tagCode, String reason, String operator) {
        String normalizedUserId = requireText(userId, "userId");
        String normalizedTagCode = requireText(tagCode, "tagCode");
        CdpUserTagDO existing = userTagMapper.selectOne(new LambdaQueryWrapper<CdpUserTagDO>()
                .eq(CdpUserTagDO::getUserId, normalizedUserId)
                .eq(CdpUserTagDO::getTagCode, normalizedTagCode));
        if (existing == null) {
            return;
        }
        String oldValue = existing.getTagValue();
        existing.setStatus("REMOVED");
        userTagMapper.updateById(existing);
        writeHistory(normalizedUserId, normalizedTagCode, oldValue, null, "REMOVE", "MANUAL",
                null, null, reason, operator);
    }

    public List<CdpUserTagDTO> listCurrentTags(String userId) {
        return userTagMapper.selectList(new LambdaQueryWrapper<CdpUserTagDO>()
                        .eq(CdpUserTagDO::getUserId, requireText(userId, "userId"))
                        .eq(CdpUserTagDO::getStatus, "ACTIVE")
                        .orderByDesc(CdpUserTagDO::getUpdatedAt))
                .stream()
                .map(tag -> new CdpUserTagDTO(tag.getTagCode(), tag.getTagCode(), tag.getTagValue(),
                        tag.getValueType(), tag.getSourceType(), tag.getStatus(),
                        tag.getEffectiveAt(), tag.getExpiresAt(), tag.getUpdatedAt()))
                .toList();
    }

    public List<CdpUserTagHistoryDTO> listHistory(String userId) {
        return historyMapper.selectList(new LambdaQueryWrapper<CdpUserTagHistoryDO>()
                        .eq(CdpUserTagHistoryDO::getUserId, requireText(userId, "userId"))
                        .orderByDesc(CdpUserTagHistoryDO::getOperatedAt))
                .stream()
                .map(item -> new CdpUserTagHistoryDTO(item.getTagCode(), item.getOldValue(), item.getNewValue(),
                        item.getOperation(), item.getSourceType(), item.getSourceRefId(),
                        item.getReason(), item.getOperator(), item.getOperatedAt()))
                .toList();
    }

    private TagDefinitionDO getEnabledTag(String tagCode) {
        TagDefinitionDO def = tagDefinitionMapper.selectOne(new LambdaQueryWrapper<TagDefinitionDO>()
                .eq(TagDefinitionDO::getTagCode, tagCode)
                .eq(TagDefinitionDO::getEnabled, 1)
                .last("LIMIT 1"));
        if (def == null) {
            throw new IllegalArgumentException("标签不存在或已禁用: " + tagCode);
        }
        return def;
    }

    private void writeHistory(String userId, String tagCode, String oldValue, String newValue,
                              String operation, String sourceType, String sourceRefId,
                              String idempotencyKey, String reason, String operator) {
        CdpUserTagHistoryDO history = new CdpUserTagHistoryDO();
        history.setUserId(userId);
        history.setTagCode(tagCode);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setOperation(operation);
        history.setSourceType(sourceType);
        history.setSourceRefId(sourceRefId);
        history.setIdempotencyKey(idempotencyKey);
        history.setReason(reason);
        history.setOperator(operator);
        history.setOperatedAt(LocalDateTime.now());
        try {
            historyMapper.insert(history);
        } catch (DuplicateKeyException duplicate) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw duplicate;
            }
        }
    }

    private String normalizeValue(String valueType, String value) {
        String type = valueType == null || valueType.isBlank() ? "STRING" : valueType;
        if (value == null) {
            return null;
        }
        return switch (type) {
            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("BOOLEAN 标签值只能是 true 或 false");
                }
                yield value.toLowerCase();
            }
            case "NUMBER" -> {
                try {
                    Double.parseDouble(value);
                    yield value;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("NUMBER 标签值必须是数字");
                }
            }
            case "JSON", "STRING" -> value;
            default -> throw new IllegalArgumentException("不支持的标签值类型: " + type);
        };
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }
}
