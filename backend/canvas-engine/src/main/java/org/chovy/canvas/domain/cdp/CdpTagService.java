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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagHistoryDO;
import org.chovy.canvas.dal.mapper.CdpUserTagHistoryMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;

/**
 * Cdp Tag CDP 领域服务。
 *
 * <p>负责用户画像、身份、标签和画布参与记录等客户数据能力，为画布执行和管理端查询提供统一入口。
 * <p>该层隔离 CDP 数据结构与上层业务，集中处理状态、历史和幂等语义。
 */
@Service
@RequiredArgsConstructor
public class CdpTagService {

    /** 标签定义 Mapper，用于读取标签元数据、值类型和人工打标开关。 */
    private final TagDefinitionMapper tagDefinitionMapper;
    /** CDP 用户标签 Mapper。 */
    private final CdpUserTagMapper userTagMapper;
    /** CDP 用户标签历史 Mapper。 */
    private final CdpUserTagHistoryMapper historyMapper;
    /** CDP 用户服务。 */
    private final CdpUserService userService;

    /** 为用户写入或更新 CDP 标签，并记录变更历史。 */
    @Transactional
    public CdpUserTagDO setTag(String userId, CdpTagWriteReq req) {
        return setTag(null, userId, req);
    }

    /** 为指定租户内用户写入或更新 CDP 标签，并记录变更历史。 */
    @Transactional
    public CdpUserTagDO setTag(Long tenantId, String userId, CdpTagWriteReq req) {
        String normalizedUserId = requireText(userId, "userId");
        String tagCode = requireText(req.tagCode(), "tagCode");
        TagDefinitionDO def = getEnabledTag(tagCode);
        String sourceType = req.sourceType() == null || req.sourceType().isBlank() ? "MANUAL" : req.sourceType();
        if ("MANUAL".equals(sourceType) && Integer.valueOf(0).equals(def.getManualEnabled())) {
            throw new IllegalArgumentException("标签不允许人工打标: " + tagCode);
        }
        String value = normalizeValue(def.getValueType(), req.tagValue());

        CdpUserTagDO existing = userTagMapper.selectOne(userTagQuery(tenantId)
                .eq(CdpUserTagDO::getUserId, normalizedUserId)
                .eq(CdpUserTagDO::getTagCode, tagCode));
        String oldValue = existing != null ? existing.getTagValue() : null;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = req.expiresAt();
        if (expiresAt == null && def.getDefaultTtlDays() != null) {
            expiresAt = now.plusDays(def.getDefaultTtlDays());
        }
        // 先写历史表占用幂等键，重复请求直接短路，避免当前标签被重复覆盖。
        boolean reserved = writeHistory(tenantId, normalizedUserId, tagCode, oldValue, value, "SET", sourceType,
                req.sourceRefId(), req.idempotencyKey(), req.reason(), req.operator());
        if (!reserved) {
            return existing != null ? existing : userTagMapper.selectOne(userTagQuery(tenantId)
                    .eq(CdpUserTagDO::getUserId, normalizedUserId)
                    .eq(CdpUserTagDO::getTagCode, tagCode));
        }

        // 标签写入前补齐用户主档，保证用户标签表不会出现孤立 userId。
        userService.ensureUser(tenantId, normalizedUserId, sourceType, req.sourceRefId());

        CdpUserTagDO tag = existing != null ? existing : new CdpUserTagDO();
        tag.setTenantId(tenantId);
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
            // 当前标签表只保留最新生效值，完整变更轨迹已经由历史表承载。
            userTagMapper.updateById(tag);
        }

        return tag;
    }

    /** 移除用户标签，并写入标签变更历史。 */
    public void removeTag(String userId, String tagCode, String reason, String operator) {
        removeTag(null, userId, tagCode, reason, operator);
    }

    /** 移除指定租户内用户标签，并写入标签变更历史。 */
    public void removeTag(Long tenantId, String userId, String tagCode, String reason, String operator) {
        String normalizedUserId = requireText(userId, "userId");
        String normalizedTagCode = requireText(tagCode, "tagCode");
        CdpUserTagDO existing = userTagMapper.selectOne(userTagQuery(tenantId)
                .eq(CdpUserTagDO::getUserId, normalizedUserId)
                .eq(CdpUserTagDO::getTagCode, normalizedTagCode));
        if (existing == null) {
            return;
        }
        String oldValue = existing.getTagValue();
        existing.setStatus("REMOVED");
        userTagMapper.updateById(existing);
        writeHistory(tenantId, normalizedUserId, normalizedTagCode, oldValue, null, "REMOVE", "MANUAL",
                null, null, reason, operator);
    }

    /** 查询用户当前生效标签。 */
    public List<CdpUserTagDTO> listCurrentTags(String userId) {
        return listCurrentTags(null, userId);
    }

    /** 查询指定租户内用户当前生效标签。 */
    public List<CdpUserTagDTO> listCurrentTags(Long tenantId, String userId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return userTagMapper.selectList(userTagQuery(tenantId)
                        .eq(CdpUserTagDO::getUserId, requireText(userId, "userId"))
                        .eq(CdpUserTagDO::getStatus, "ACTIVE")
                        .orderByDesc(CdpUserTagDO::getUpdatedAt))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(tag -> new CdpUserTagDTO(tag.getTagCode(), tag.getTagCode(), tag.getTagValue(),
                        tag.getValueType(), tag.getSourceType(), tag.getStatus(),
                        tag.getEffectiveAt(), tag.getExpiresAt(), tag.getUpdatedAt()))
                .toList();
    }

    /** 查询用户标签变更历史。 */
    public List<CdpUserTagHistoryDTO> listHistory(String userId) {
        return listHistory(null, userId);
    }

    /** 查询指定租户内用户标签变更历史。 */
    public List<CdpUserTagHistoryDTO> listHistory(Long tenantId, String userId) {
        return historyMapper.selectList(tagHistoryQuery(tenantId)
                        .eq(CdpUserTagHistoryDO::getUserId, requireText(userId, "userId"))
                        .orderByDesc(CdpUserTagHistoryDO::getOperatedAt))
                .stream()
                .map(item -> new CdpUserTagHistoryDTO(item.getTagCode(), item.getOldValue(), item.getNewValue(),
                        item.getOperation(), item.getSourceType(), item.getSourceRefId(),
                        item.getReason(), item.getOperator(), item.getOperatedAt()))
                .toList();
    }

    /** 查询已启用的标签定义，不存在或禁用时阻止写入。 */
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

    /** 写入标签变更历史，并通过幂等键拦截重复写入。 */
    private boolean writeHistory(Long tenantId, String userId, String tagCode, String oldValue, String newValue,
                                 String operation, String sourceType, String sourceRefId,
                                 String idempotencyKey, String reason, String operator) {
        CdpUserTagHistoryDO history = new CdpUserTagHistoryDO();
        history.setTenantId(tenantId);
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
            return true;
        } catch (DuplicateKeyException duplicate) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw duplicate;
            }
            // 唯一键冲突且存在幂等键时表示同一来源请求已处理过，调用方复用当前标签状态。
            return false;
        }
    }

    /** 按标签定义的值类型校验并规范化标签值。 */
    private String normalizeValue(String valueType, String value) {
        // 准备本次处理所需的上下文和中间变量。
        String type = valueType == null || valueType.isBlank() ? "STRING" : valueType;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) {
            return null;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /** 校验必填文本字段并返回去除首尾空白后的值。 */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 userTagQuery 流程生成的业务结果。
     */
    private LambdaQueryWrapper<CdpUserTagDO> userTagQuery(Long tenantId) {
        LambdaQueryWrapper<CdpUserTagDO> query = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            query.eq(CdpUserTagDO::getTenantId, tenantId);
        }
        return query;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tagHistoryQuery 流程生成的业务结果。
     */
    private LambdaQueryWrapper<CdpUserTagHistoryDO> tagHistoryQuery(Long tenantId) {
        LambdaQueryWrapper<CdpUserTagHistoryDO> query = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            query.eq(CdpUserTagHistoryDO::getTenantId, tenantId);
        }
        return query;
    }
}
