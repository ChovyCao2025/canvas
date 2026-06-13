package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.cdp.domain.CdpTagDefinition;
import org.chovy.canvas.cdp.domain.CdpTagRepository;
import org.chovy.canvas.cdp.domain.CdpUserTag;
import org.chovy.canvas.cdp.domain.CdpUserTagHistory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisCdpTagRepository implements CdpTagRepository {

    private final TagDefinitionMapper definitionMapper;
    private final CdpUserTagMapper tagMapper;
    private final CdpUserTagHistoryMapper historyMapper;
    private final CdpPersistenceConverter converter;

    public MybatisCdpTagRepository(TagDefinitionMapper definitionMapper,
                                   CdpUserTagMapper tagMapper,
                                   CdpUserTagHistoryMapper historyMapper,
                                   CdpPersistenceConverter converter) {
        this.definitionMapper = definitionMapper;
        this.tagMapper = tagMapper;
        this.historyMapper = historyMapper;
        this.converter = converter;
    }

    @Override
    public CdpTagDefinition findEnabledDefinition(String tagCode) {
        TagDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<TagDefinitionDO>()
                .eq(TagDefinitionDO::getTagCode, tagCode)
                .eq(TagDefinitionDO::getEnabled, 1)
                .last("LIMIT 1"));
        return converter.toTagDefinition(row);
    }

    @Override
    public CdpUserTag findCurrentTag(Long tenantId, String userId, String tagCode) {
        CdpUserTagDO row = tagMapper.selectOne(new LambdaQueryWrapper<CdpUserTagDO>()
                .eq(CdpUserTagDO::getTenantId, tenantId)
                .eq(CdpUserTagDO::getUserId, userId)
                .eq(CdpUserTagDO::getTagCode, tagCode)
                .last("LIMIT 1"));
        return converter.toUserTag(row);
    }

    @Override
    public boolean saveHistory(CdpUserTagHistory row) {
        try {
            historyMapper.insert(converter.toUserTagHistoryRow(row));
            return true;
        } catch (DuplicateKeyException duplicate) {
            if (row.idempotencyKey() == null || row.idempotencyKey().isBlank()) {
                throw duplicate;
            }
            return false;
        }
    }

    @Override
    public CdpUserTag saveCurrentTag(CdpUserTag tag) {
        CdpUserTagDO row = converter.toUserTagRow(tag);
        if (row.getId() == null) {
            tagMapper.insert(row);
        } else {
            tagMapper.updateById(row);
        }
        return converter.toUserTag(row);
    }

    @Override
    public List<CdpUserTag> listCurrentTags(Long tenantId, String userId) {
        return tagMapper.selectList(new LambdaQueryWrapper<CdpUserTagDO>()
                        .eq(CdpUserTagDO::getTenantId, tenantId)
                        .eq(CdpUserTagDO::getUserId, userId)
                        .eq(CdpUserTagDO::getStatus, "ACTIVE")
                        .orderByDesc(CdpUserTagDO::getUpdatedAt))
                .stream()
                .map(converter::toUserTag)
                .toList();
    }

    @Override
    public List<CdpUserTagHistory> listHistory(Long tenantId, String userId) {
        return historyMapper.selectList(new LambdaQueryWrapper<CdpUserTagHistoryDO>()
                        .eq(CdpUserTagHistoryDO::getTenantId, tenantId)
                        .eq(CdpUserTagHistoryDO::getUserId, userId)
                        .orderByDesc(CdpUserTagHistoryDO::getOperatedAt))
                .stream()
                .map(converter::toUserTagHistory)
                .toList();
    }
}
