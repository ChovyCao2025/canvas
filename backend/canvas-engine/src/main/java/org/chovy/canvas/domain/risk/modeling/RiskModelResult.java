package org.chovy.canvas.domain.risk.modeling;

import java.util.List;

/**
 * 风控模型评分结果。
 *
 * @param score 模型分数
 * @param explanations 模型解释信息
 * @param modelVersion 模型版本号
 * @param fallbackUsed 是否使用兜底结果
 */
public record RiskModelResult(
        int score,
        List<String> explanations,
        int modelVersion,
        boolean fallbackUsed
) {
}
