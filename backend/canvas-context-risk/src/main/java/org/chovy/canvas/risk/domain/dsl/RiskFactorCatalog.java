package org.chovy.canvas.risk.domain.dsl;

import java.util.Optional;

/**
 * 风控因子目录，用于按业务键查找规则可引用的特征定义。
 */
public interface RiskFactorCatalog {

    /**
     * 按因子键查找特征定义，未注册时返回空。
     */
    Optional<RiskFactorDefinition> findByKey(String key);
}
