package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceBitmapStoreSetOpsTest {

    @Test
    void overlapMergeAndExcludeReturnNewBitmapsWithoutMutatingStoredData() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AudienceBitmapStore store = new AudienceBitmapStore(redis);

        store.save(10L, RoaringBitmap.bitmapOf(1, 2, 3));
        store.save(11L, RoaringBitmap.bitmapOf(3, 4));

        ArgumentCaptor<String> leftStoredValue = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> rightStoredValue = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("audience:bitmap:10"), leftStoredValue.capture());
        verify(values).set(eq("audience:bitmap:11"), rightStoredValue.capture());
        when(values.get("audience:bitmap:10")).thenReturn(leftStoredValue.getValue());
        when(values.get("audience:bitmap:11")).thenReturn(rightStoredValue.getValue());

        assertThat(store.overlap(10L, 11L).getCardinality()).isEqualTo(1);
        assertThat(store.merge(10L, 11L).getCardinality()).isEqualTo(4);
        assertThat(store.exclude(10L, 11L).getCardinality()).isEqualTo(2);
        assertThat(store.load(10L).getCardinality()).isEqualTo(3);
    }
}
