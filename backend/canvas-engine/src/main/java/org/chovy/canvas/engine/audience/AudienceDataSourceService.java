package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.domain.audience.AudienceDataSourceMapper;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AudienceDataSourceService {

    private static final String JDBC_DATA_SOURCE_TYPE = "JDBC";

    private final AudienceDataSourceMapper dataSourceMapper;
    private final AudienceDefinitionMapper definitionMapper;

    public List<AudienceDataSource> list() {
        return dataSourceMapper.selectList(null).stream()
                .peek(item -> item.setReferenceCount(countReferences(item.getId())))
                .toList();
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

    private Long countReferences(Long id) {
        return definitionMapper.selectCount(new LambdaQueryWrapper<AudienceDefinition>()
                .eq(AudienceDefinition::getDataSourceId, id)
                .eq(AudienceDefinition::getDataSourceType, JDBC_DATA_SOURCE_TYPE));
    }
}
