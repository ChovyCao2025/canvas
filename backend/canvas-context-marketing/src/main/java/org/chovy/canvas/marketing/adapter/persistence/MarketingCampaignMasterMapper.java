package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定义MarketingCampaignMasterMapper的营销上下文访问契约。
 */
@Mapper
public interface MarketingCampaignMasterMapper extends BaseMapper<MarketingCampaignMasterDO> {
}
