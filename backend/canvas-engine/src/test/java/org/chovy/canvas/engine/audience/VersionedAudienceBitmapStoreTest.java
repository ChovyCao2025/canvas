package org.chovy.canvas.engine.audience;

import org.chovy.canvas.dal.dataobject.AudienceBitmapVersionDO;
import org.chovy.canvas.dal.mapper.AudienceBitmapVersionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VersionedAudienceBitmapStoreTest {

    @Test
    void saveVersionWritesVersionedRedisKeyAndMetadata() {
        AudienceBitmapVersionMapper mapper = mock(AudienceBitmapVersionMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        VersionedAudienceBitmapStore store = new VersionedAudienceBitmapStore(redis, mapper);
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.add(12);

        String key = store.saveVersion(7L, 10L, 3L, bitmap, "OLAP");

        assertThat(key).isEqualTo("audience:bitmap:10:v:3");
        verify(values).set("audience:bitmap:10:v:3", Base64.getEncoder().encodeToString(store.serialize(bitmap)));
        verify(mapper).insert(org.mockito.ArgumentMatchers.<AudienceBitmapVersionDO>argThat(row ->
                row.getTenantId().equals(7L)
                        && row.getAudienceId().equals(10L)
                        && row.getVersion().equals(3L)
                        && "WRITING".equals(row.getStatus())));
    }

    @Test
    void markReadyUpdatesMetadataAndLatestPointer() {
        AudienceBitmapVersionMapper mapper = mock(AudienceBitmapVersionMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        VersionedAudienceBitmapStore store = new VersionedAudienceBitmapStore(redis, mapper);

        store.markReady(7L, 10L, 3L);

        verify(mapper).markReady(7L, 10L, 3L);
        verify(values).set("audience:bitmap:10:latest", "3");
    }

    @Test
    void containsLatestUsesLatestReadyVersionOnly() throws Exception {
        AudienceBitmapVersionMapper mapper = mock(AudienceBitmapVersionMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AudienceBitmapVersionDO latest = new AudienceBitmapVersionDO();
        latest.setBitmapKey("audience:bitmap:10:v:4");
        latest.setStatus("READY");
        when(mapper.selectLatestReady(7L, 10L)).thenReturn(latest);
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.add(99);
        when(values.get("audience:bitmap:10:v:4")).thenReturn(encoded(bitmap));
        VersionedAudienceBitmapStore store = new VersionedAudienceBitmapStore(redis, mapper);

        assertThat(store.containsLatest(7L, 10L, 99L)).isTrue();
        assertThat(store.containsLatest(7L, 10L, 100L)).isFalse();
    }

    @Test
    void containsLatestReturnsFalseWithoutReadyVersion() {
        AudienceBitmapVersionMapper mapper = mock(AudienceBitmapVersionMapper.class);
        VersionedAudienceBitmapStore store = new VersionedAudienceBitmapStore(mock(StringRedisTemplate.class), mapper);
        when(mapper.selectLatestReady(7L, 10L)).thenReturn(null);

        assertThat(store.containsLatest(7L, 10L, 99L)).isFalse();
    }

    @Test
    void rollbackToVersionMarksNewerReadyVersionsAndUpdatesLatestPointer() {
        AudienceBitmapVersionMapper mapper = mock(AudienceBitmapVersionMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AudienceBitmapVersionDO target = new AudienceBitmapVersionDO();
        target.setTenantId(7L);
        target.setAudienceId(10L);
        target.setVersion(2L);
        target.setStatus("READY");
        target.setBitmapKey("audience:bitmap:10:v:2");
        when(mapper.selectReadyVersion(7L, 10L, 2L)).thenReturn(target);
        when(mapper.markReadyVersionsNewerThanRolledBack(7L, 10L, 2L)).thenReturn(2);
        VersionedAudienceBitmapStore store = new VersionedAudienceBitmapStore(redis, mapper);

        VersionedAudienceBitmapStore.RollbackResult result = store.rollbackToVersion(7L, 10L, 2L);

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.rolledBackVersions()).isEqualTo(2);
        assertThat(result.targetBitmapKey()).isEqualTo("audience:bitmap:10:v:2");
        verify(mapper).markReadyVersionsNewerThanRolledBack(7L, 10L, 2L);
        verify(values).set("audience:bitmap:10:latest", "2");
    }

    @Test
    void rollbackRejectsTargetThatIsNotReady() {
        AudienceBitmapVersionMapper mapper = mock(AudienceBitmapVersionMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(mapper.selectReadyVersion(7L, 10L, 2L)).thenReturn(null);
        VersionedAudienceBitmapStore store = new VersionedAudienceBitmapStore(redis, mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> store.rollbackToVersion(7L, 10L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target bitmap version is not READY");
        verify(mapper, never()).markReadyVersionsNewerThanRolledBack(7L, 10L, 2L);
    }

    private String encoded(RoaringBitmap bitmap) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            bitmap.serialize(dos);
            dos.flush();
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
}
