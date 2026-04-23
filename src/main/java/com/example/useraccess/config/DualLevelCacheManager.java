package com.example.useraccess.config;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Dual-level cache manager that tries Redis first, then falls back to 
 * in-memory cache if Redis is unavailable. Ensures that even when Redis 
 * goes down at runtime, the application continues to cache data locally.
 */
public class DualLevelCacheManager implements CacheManager {

	private static final Logger logger = LoggerFactory.getLogger(DualLevelCacheManager.class);

	private final CacheManager redisCacheManager;
	private final ConcurrentMapCacheManager localCacheManager;

	public DualLevelCacheManager(CacheManager redisCacheManager, ConcurrentMapCacheManager localCacheManager) {
		this.redisCacheManager = redisCacheManager;
		this.localCacheManager = localCacheManager;
	}

	@Override
	public Cache getCache(String name) {
		try {
			Cache redisCache = redisCacheManager.getCache(name);
			if (redisCache != null) {
				// Wrap Redis cache with fallback to local cache
				return new DualLevelCache(redisCache, localCacheManager.getCache(name));
			}
		} catch (Exception e) {
			logger.warn("Failed to get Redis cache '{}'. Using local cache only.", name, e);
		}

		// Fall back to local cache
		return localCacheManager.getCache(name);
	}

	@Override
	public Collection<String> getCacheNames() {
		// Return all cache names from local cache manager (superset of both)
		return localCacheManager.getCacheNames();
	}
}