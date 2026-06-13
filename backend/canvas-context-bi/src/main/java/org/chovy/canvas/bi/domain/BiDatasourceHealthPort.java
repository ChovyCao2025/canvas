package org.chovy.canvas.bi.domain;

public interface BiDatasourceHealthPort {

    BiDatasourceHealth findLatestHealth(Long tenantId, BiResourceKey sourceKey);
}
