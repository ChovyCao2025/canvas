package org.chovy.canvas.canvas.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.VersionStatus;
import org.junit.jupiter.api.Test;

/**
 * 封装CanvasVersionPersistenceMappingTest相关的业务逻辑。
 */
class CanvasVersionPersistenceMappingTest {

    /**
     * 处理mapsVersionDomainToPersistenceRowAndBack。
     */
    @Test
    void mapsVersionDomainToPersistenceRowAndBack() {
        CanvasVersion version = CanvasVersion.published(11L, 7L, 9L, 3, "{\"nodes\":[]}", "publisher");

        CanvasVersionDO row = CanvasVersionPersistenceMapper.toRow(version);
        CanvasVersion restored = CanvasVersionPersistenceMapper.toDomain(row);

        assertThat(row.getStatus()).isEqualTo(1);
        assertThat(row.getGraphJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(restored.status()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(restored.version()).isEqualTo(3);
    }

    /**
     * 处理versionRepositoryRejectsStaleUpdates。
     */
    @Test
    void versionRepositoryRejectsStaleUpdates() {
        CanvasVersionMapper mapper = mock(CanvasVersionMapper.class);
        when(mapper.updateById(any(CanvasVersionDO.class))).thenReturn(0);
        MybatisCanvasVersionRepository repository = new MybatisCanvasVersionRepository(mapper);

        assertThatThrownBy(() -> repository.save(CanvasVersion.draft(
                11L, 7L, 9L, 3, "{\"nodes\":[]}", "editor")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Canvas version update affected 0 rows");
    }
}
