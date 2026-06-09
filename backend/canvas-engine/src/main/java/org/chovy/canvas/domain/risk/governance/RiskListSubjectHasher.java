package org.chovy.canvas.domain.risk.governance;

/**
 * 风控名单主体哈希器。
 */
@FunctionalInterface
public interface RiskListSubjectHasher {

    /**
     * 对原始主体标识计算名单匹配哈希。
     */
    String hash(String rawSubject);
}
