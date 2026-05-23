package org.chovy.cache.strategy;

public enum LoaderFailureStrategy {
    THROW,
    RETURN_STALE,
    RETURN_EMPTY
}
