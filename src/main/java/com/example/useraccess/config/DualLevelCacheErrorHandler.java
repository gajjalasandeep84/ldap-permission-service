package com.example.useraccess.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Error handler that gracefully handles cache errors by falling back
 * to the local in-memory cache when Redis operations fail at runtime.
 */
public class DualLevelCacheErrorHandler implements CacheErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(DualLevelCacheErrorHandler.class);

	private final ConcurrentMapCacheManager localCacheManager;

	public DualLevelCacheErrorHandler(ConcurrentMapCacheManager localCacheManager) {
		this.localCacheManager = localCacheManager;
	}

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logger.warn("Cache GET failed for cache={}, key={}. Redis may be down. Returning DB result which will be cached locally.",
				cache != null ? cache.getName() : "unknown", key, exception);
		// The calling code will fetch from DB, and the local cache will be updated via @Cacheable
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
		// Attempt to store in local cache as fallback
		if (cache != null && value != null) {
			try {
				Cache localCache = localCacheManager.getCache(cache.getName());
				if (localCache != null) {
					localCache.put(key, value);
					logger.warn("Cache PUT failed for Redis cache={}. Stored in local cache instead.",
							cache.getName());
					return;
				}
			} catch (Exception e) {
				logger.error("Failed to store in local cache as fallback", e);
			}
		}
		logger.warn("Cache PUT failed for cache={}, key={}. Could not fallback to local cache.",
				cache != null ? cache.getName() : "unknown", key, exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logger.warn("Cache EVICT failed for cache={}, key={}, Redis may be down.",
				cache != null ? cache.getName() : "unknown", key, exception);
		// Attempt to evict from local cache
		if (cache != null) {
			try {
				Cache localCache = localCacheManager.getCache(cache.getName());
				if (localCache != null) {
					localCache.evict(key);
					logger.debug("Evicted from local cache: {}", key);
				}
			} catch (Exception e) {
				logger.debug("Failed to evict from local cache", e);
			}
		}
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logger.warn("Cache CLEAR failed for cache={}, Redis may be down.",
				cache != null ? cache.getName() : "unknown", exception);
		// Attempt to clear local cache
		if (cache != null) {
			try {
				Cache localCache = localCacheManager.getCache(cache.getName());
				if (localCache != null) {
					localCache.clear();
					logger.debug("Cleared local cache: {}", cache.getName());
				}
			} catch (Exception e) {
				logger.debug("Failed to clear local cache", e);
			}
		}
	}
}