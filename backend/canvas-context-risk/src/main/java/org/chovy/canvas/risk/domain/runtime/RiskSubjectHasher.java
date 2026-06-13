package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控主体哈希器。
 */
@FunctionalInterface
public interface RiskSubjectHasher {

    /**
     * 对原始主体标识计算稳定哈希。
     */
    String hash(String rawSubject);
}
