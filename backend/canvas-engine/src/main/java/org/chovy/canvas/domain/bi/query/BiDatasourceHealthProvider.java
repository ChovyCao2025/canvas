package org.chovy.canvas.domain.bi.query;

import java.util.List;

@FunctionalInterface
public interface BiDatasourceHealthProvider {

    List<BiDatasourceHealth> health();

    static BiDatasourceHealthProvider empty() {
        return List::of;
    }
}
