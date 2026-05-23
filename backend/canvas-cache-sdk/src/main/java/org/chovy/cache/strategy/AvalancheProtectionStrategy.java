package org.chovy.cache.strategy;

public enum AvalancheProtectionStrategy {
    NONE,
    TTL_JITTER,
    REFRESH_AHEAD,
    STALE_ON_ERROR,
    FULL
}
