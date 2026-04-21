package com.example.useraccess.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Custom cache error handler to gracefully handle Redis/cache failures.
 * When Redis is down, allows the application to continue by executing the cached
 * method directly instead of failing completely.
 */
public class RedisCacheErrorHandler implements CacheErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(RedisCacheErrorHandler.class);

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		logger.warn("Cache GET error for cache [{}] and key [{}]. Proceeding without cache.", 
			cache.getName(), key, exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
		logger.warn("Cache PUT error for cache [{}] and key [{}]. Value will not be cached.", 
			cache.getName(), key, exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logger.warn("Cache EVICT error for cache [{}] and key [{}].", 
			cache.getName(), key, exception);
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logger.warn("Cache CLEAR error for cache [{}].", 
			cache.getName(), exception);
	}
}
