package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.domain.audience.AudienceDataSourceMapper;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudienceDataSourceService {

    private static final String JDBC_DATA_SOURCE_TYPE = "JDBC";
    private static final String COLUMN_DATA_SOURCE_ID = "data_source_id";
    private static final String COLUMN_DATA_SOURCE_TYPE = "data_source_type";

    private final AudienceDataSourceMapper dataSourceMapper;
    private final AudienceDefinitionMapper definitionMapper;

    public List<AudienceDataSource> list() {
        List<AudienceDataSource> dataSources = dataSourceMapper.selectList(null);
        Map<Long, Long> referenceCounts = countReferences(dataSources);
        dataSources.forEach(item -> item.setReferenceCount(referenceCounts.getOrDefault(item.getId(), 0L)));
        return dataSources;
    }

    public AudienceDataSource get(Long id) {
        return dataSourceMapper.selectById(id);
    }

    public AudienceDataSource create(AudienceDataSource dataSource) {
        dataSourceMapper.insert(dataSource);
        return dataSource;
    }

    public AudienceDataSource update(AudienceDataSource dataSource) {
        dataSourceMapper.updateById(dataSource);
        return dataSource;
    }

    public void delete(Long id) {
        Long count = countReferences(id);
        if (count != null && count > 0) {
            throw new IllegalStateException("Audience data source is still referenced");
        }
        dataSourceMapper.deleteById(id);
    }

    private Map<Long, Long> countReferences(List<AudienceDataSource> dataSources) {
        List<Long> dataSourceIds = dataSources.stream()
                .map(AudienceDataSource::getId)
                .filter(Objects::nonNull)
                .toList();
        if (dataSourceIds.isEmpty()) {
            return Map.of();
        }
        return definitionMapper.selectList(new QueryWrapper<AudienceDefinition>()
                        .select(COLUMN_DATA_SOURCE_ID)
                        .in(COLUMN_DATA_SOURCE_ID, dataSourceIds)
                        .eq(COLUMN_DATA_SOURCE_TYPE, JDBC_DATA_SOURCE_TYPE))
                .stream()
                .map(AudienceDefinition::getDataSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private Long countReferences(Long id) {
        return definitionMapper.selectCount(new QueryWrapper<AudienceDefinition>()
                .eq(COLUMN_DATA_SOURCE_ID, id)
                .eq(COLUMN_DATA_SOURCE_TYPE, JDBC_DATA_SOURCE_TYPE));
    }
}
