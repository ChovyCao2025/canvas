package org.chovy.cache;

public interface CacheBloomFilter<K> {
    boolean mightContain(K key);

    void put(K key);
}
