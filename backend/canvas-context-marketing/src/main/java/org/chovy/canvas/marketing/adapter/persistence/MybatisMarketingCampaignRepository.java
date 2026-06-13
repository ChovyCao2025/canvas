package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisMarketingCampaignRepository implements MarketingCampaignRepository {

    private final MarketingCampaignMasterMapper campaignMapper;
    private final MarketingCampaignLinkMapper linkMapper;
    private final MarketingCampaignPersistenceConverter converter;

    public MybatisMarketingCampaignRepository(MarketingCampaignMasterMapper campaignMapper,
                                              MarketingCampaignLinkMapper linkMapper,
                                              MarketingCampaignPersistenceConverter converter) {
        this.campaignMapper = campaignMapper;
        this.linkMapper = linkMapper;
        this.converter = converter;
    }

    @Override
    public MarketingCampaign findByTenantAndKey(Long tenantId, CampaignKey campaignKey) {
        MarketingCampaignMasterDO row = campaignMapper.selectOne(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                .eq(MarketingCampaignMasterDO::getCampaignKey, campaignKey.value())
                .last("LIMIT 1"));
        return converter.toCampaign(row);
    }

    @Override
    public MarketingCampaign findById(Long tenantId, Long campaignId) {
        MarketingCampaignMasterDO row = campaignMapper.selectById(campaignId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return converter.toCampaign(row);
    }

    @Override
    public MarketingCampaign save(MarketingCampaign campaign) {
        MarketingCampaignMasterDO row = converter.toCampaignRow(campaign);
        if (row.getId() == null) {
            campaignMapper.insert(row);
        } else {
            campaignMapper.updateById(row);
        }
        return converter.toCampaign(row);
    }

    @Override
    public List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit) {
        String statusValue = status == null ? null : status.name();
        return campaignMapper.selectList(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                        .eq(statusValue != null, MarketingCampaignMasterDO::getStatus, statusValue)
                        .orderByDesc(MarketingCampaignMasterDO::getUpdatedAt)
                        .last("LIMIT " + limit))
                .stream()
                .filter(row -> statusValue == null || statusValue.equals(row.getStatus()))
                .map(converter::toCampaign)
                .toList();
    }

    @Override
    public MarketingCampaignLink findLink(Long tenantId, Long campaignId, String resourceType, CampaignKey resourceKey) {
        MarketingCampaignLinkDO row = linkMapper.selectOne(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                .eq(MarketingCampaignLinkDO::getTenantId, tenantId)
                .eq(MarketingCampaignLinkDO::getCampaignId, campaignId)
                .eq(MarketingCampaignLinkDO::getResourceType, resourceType)
                .eq(MarketingCampaignLinkDO::getResourceKey, resourceKey.value())
                .last("LIMIT 1"));
        return converter.toLink(row);
    }

    @Override
    public MarketingCampaignLink saveLink(MarketingCampaignLink link) {
        MarketingCampaignLinkDO row = converter.toLinkRow(link);
        if (row.getId() == null) {
            linkMapper.insert(row);
        } else {
            linkMapper.updateById(row);
        }
        return converter.toLink(row);
    }

    @Override
    public List<MarketingCampaignLink> listLinks(Long tenantId, Long campaignId) {
        return linkMapper.selectList(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, tenantId)
                        .eq(MarketingCampaignLinkDO::getCampaignId, campaignId)
                        .orderByAsc(MarketingCampaignLinkDO::getResourceType)
                        .orderByAsc(MarketingCampaignLinkDO::getResourceKey))
                .stream()
                .map(converter::toLink)
                .toList();
    }

    @Override
    public MarketingCampaignLink findLinkById(Long tenantId, Long linkId) {
        MarketingCampaignLinkDO row = linkMapper.selectById(linkId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return converter.toLink(row);
    }

    @Override
    public void deleteLink(Long tenantId, Long linkId) {
        linkMapper.deleteById(linkId);
    }
}
