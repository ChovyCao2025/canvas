package org.chovy.canvas.domain.risk.graph;

import java.util.List;
import java.util.Map;

/**
 * 风控图谱摘要。
 *
 * @param targetSubjectId 目标主体编号
 * @param associationCounts 按标识类型统计的关联数量
 * @param connections 相邻主体连接列表
 * @param listHits 名单命中列表
 */
public record RiskGraphSummary(
        String targetSubjectId,
        Map<String, Integer> associationCounts,
        List<RiskGraphConnection> connections,
        List<RiskGraphListHit> listHits
) {
}
