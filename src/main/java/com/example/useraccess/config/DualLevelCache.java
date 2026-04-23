package com.example.useraccess.config;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

/**
 * Wrapper cache that attempts to use Redis first, then falls back to 
 * local in-memory cache on failure. This ensures seamless caching even 
 * when Redis becomes unavailable at runtime.
 */
public class DualLevelCache implements Cache {

	private static final Logger logger = LoggerFactory.getLogger(DualLevelCache.class);

	private final Cache redisCache;
	private final Cache localCache;

	public DualLevelCache(Cache redisCache, Cache localCache) {
		this.redisCache = redisCache;
		this.localCache = localCache;
	}

	@Override
	public String getName() {
		return redisCache.getName();
	}

	@Override
	public Object getNativeCache() {
		return redisCache.getNativeCache();
	}

	@Override
	public ValueWrapper get(Object key) {
		try {
			// Try Redis first
			ValueWrapper value = redisCache.get(key);
			if (value != null) {
				logger.debug("Cache HIT from Redis: {}", key);
				// Also update local cache to ensure it's in sync
				localCache.put(key, value.get());
				return value;
			}

			// Try local cache if Redis miss
			logger.debug("Cache MISS from Redis, checking local cache: {}", key);
			ValueWrapper localValue = localCache.get(key);
			if (localValue != null) {
				logger.debug("Cache HIT from local cache: {}", key);
				// Try to update Redis asynchronously for future hits
				try {
					redisCache.put(key, localValue.get());
				} catch (Exception e) {
					logger.debug("Failed to update Redis cache, continuing with local: {}", key, e);
				}
			}
			return localValue;

		} catch (Exception e) {
			logger.warn("Redis cache error for key: {}. Falling back to local cache.", key, e);
			return localCache.get(key);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		try {
			// Try Redis first
			T value = redisCache.get(key, type);
			if (value != null) {
				logger.debug("Cache HIT from Redis: {}", key);
				// Also update local cache
				localCache.put(key, value);
				return value;
			}

			// Try local cache if Redis miss
			logger.debug("Cache MISS from Redis, checking local cache: {}", key);
			T localValue = localCache.get(key, type);
			if (localValue != null) {
				logger.debug("Cache HIT from local cache: {}", key);
				// Try to update Redis for future hits
				try {
					redisCache.put(key, localValue);
				} catch (Exception e) {
					logger.debug("Failed to update Redis cache, continuing with local: {}", key, e);
				}
			}
			return localValue;

		} catch (Exception e) {
			logger.warn("Redis cache error for key: {}. Falling back to local cache.", key, e);
			return localCache.get(key, type);
		}
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		try {
			// Try Redis first
			ValueWrapper value = redisCache.get(key);
			if (value != null) {
				logger.debug("Cache HIT from Redis: {}", key);
				localCache.put(key, value.get());
				return (T) value.get();
			}

			// Try local cache
			logger.debug("Cache MISS from Redis, checking local cache: {}", key);
			ValueWrapper localValue = localCache.get(key);
			if (localValue != null) {
				logger.debug("Cache HIT from local cache: {}", key);
				try {
					redisCache.put(key, localValue.get());
				} catch (Exception e) {
					logger.debug("Failed to update Redis cache: {}", key, e);
				}
				return (T) localValue.get();
			}

			// Load and cache
			logger.debug("Cache MISS, loading value for key: {}", key);
			T loadedValue = valueLoader.call();
			if (loadedValue != null) {
				put(key, loadedValue);
			}
			return loadedValue;

		} catch (Exception e) {
			logger.warn("Redis cache error for key: {}. Falling back to local cache.", key, e);
			try {
				ValueWrapper localValue = localCache.get(key);
				if (localValue != null) {
					return (T) localValue.get();
				}
				// Load and cache in local only
				T loadedValue = valueLoader.call();
				if (loadedValue != null) {
					localCache.put(key, loadedValue);
				}
				return loadedValue;
			} catch (Exception local_e) {
				logger.error("Failed to load value from both caches for key: {}", key, local_e);
				throw new RuntimeException("Cache error", local_e);
			}
		}
	}

	@Override
	public void put(Object key, Object value) {
		try {
			// Always try to update both caches
			redisCache.put(key, value);
			localCache.put(key, value);
			logger.debug("Cache PUT successful for key: {}", key);
		} catch (Exception e) {
			logger.warn("Redis cache PUT failed for key: {}. Storing in local cache only.", key, e);
			// Fall back to local cache only
			localCache.put(key, value);
		}
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		try {
			// Try Redis first
			ValueWrapper existing = redisCache.putIfAbsent(key, value);
			if (existing == null) {
				localCache.putIfAbsent(key, value);
			} else {
				localCache.put(key, existing.get());
			}
			return existing;
		} catch (Exception e) {
			logger.warn("Redis cache putIfAbsent failed for key: {}. Using local cache only.", key, e);
			return localCache.putIfAbsent(key, value);
		}
	}

	@Override
	public void evict(Object key) {
		try {
			redisCache.evict(key);
		} catch (Exception e) {
			logger.warn("Failed to evict from Redis cache for key: {}", key, e);
		}
		try {
			localCache.evict(key);
		} catch (Exception e) {
			logger.warn("Failed to evict from local cache for key: {}", key, e);
		}
	}

	@Override
	public void clear() {
		try {
			redisCache.clear();
		} catch (Exception e) {
			logger.warn("Failed to clear Redis cache", e);
		}
		try {
			localCache.clear();
		} catch (Exception e) {
			logger.warn("Failed to clear local cache", e);
		}
	}
}