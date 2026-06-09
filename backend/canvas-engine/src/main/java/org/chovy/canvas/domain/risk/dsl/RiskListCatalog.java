package org.chovy.canvas.domain.risk.dsl;

import java.util.Optional;

/**
 * 风控名单目录，用于按名单键查找名单主体和值类型定义。
 */
public interface RiskListCatalog {

    /**
     * 按名单键查找名单定义，未注册时返回空。
     */
    Optional<RiskListDefinition> findByKey(String key);
}
