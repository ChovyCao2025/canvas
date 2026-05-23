package org.chovy.cache.strategy;

public enum PenetrationProtectionStrategy {
    NONE,
    CACHE_NULL_SHORT_TTL,
    CACHE_EMPTY_SHORT_TTL,
    BLOOM_FILTER,
    KEY_VALIDATOR,
    CACHE_NULL_AND_BLOOM,
    FULL
}
