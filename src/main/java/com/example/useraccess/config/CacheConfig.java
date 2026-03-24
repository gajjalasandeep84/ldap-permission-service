package com.example.useraccess.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
				.disableCachingNullValues().entryTtl(Duration.ofMinutes(10));

		Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

		cacheConfigurations.put(CacheNames.PERMISSION_SUMMARY, defaultConfig.entryTtl(Duration.ofMinutes(10)));

		cacheConfigurations.put(CacheNames.PERMISSIONS, defaultConfig.entryTtl(Duration.ofMinutes(30)));

		cacheConfigurations.put(CacheNames.ROLE_PERMISSIONS, defaultConfig.entryTtl(Duration.ofMinutes(30)));

		cacheConfigurations.put(CacheNames.USER_DIRECT_PERMISSIONS, defaultConfig.entryTtl(Duration.ofMinutes(20)));

		cacheConfigurations.put("rolePermissionCount", defaultConfig.entryTtl(Duration.ofMinutes(30)));
		cacheConfigurations.put("userDirectPermissionCount", defaultConfig.entryTtl(Duration.ofMinutes(20)));

		return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig)
				.withInitialCacheConfigurations(cacheConfigurations).transactionAware().build();
	}
}