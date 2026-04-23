package com.example.useraccess.config;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

// DEPRECATED: cacheErrorHandler bean moved to CacheConfig.java
// This class is kept for reference but disabled
public class CacheErrorConfig {
 
    // Bean definition moved to CacheConfig - this implementation is no longer used
    public CacheErrorHandler cacheErrorHandlerOld() {
        return new CacheErrorHandler() {

             private final Logger log = LoggerFactory.getLogger(CacheErrorHandler.class);
             @Override

            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed for cache={} key={}. Continuing without cache.",
                        cache != null ? cache.getName() : "unknown",
                        key,
                        exception);
            }
 
            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for cache={} key={}. Continuing without cache.",
                        cache != null ? cache.getName() : "unknown",
                        key,
                        exception);
            }
 
            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed for cache={} key={}. Continuing.",
                        cache != null ? cache.getName() : "unknown",
                        key,
                        exception);
            }
 
            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed for cache={}. Continuing.",
                        cache != null ? cache.getName() : "unknown",
                        exception);
            }
        };
    }
}