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
	public RedisCacheErrorHandler redisCacheErrorHandler() {
		return new RedisCacheErrorHandler();
	}

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
						.serializeKeysWith(RedisSerializationContext.SerializationPair
								.fromSerializer(new StringRedisSerializer()))
						.serializeValuesWith(RedisSerializationContext.SerializationPair
								.fromSerializer(new GenericJackson2JsonRedisSerializer()))
						.disableCachingNullValues().entryTtl(Duration.ofMinutes(10)))
				.withInitialCacheConfigurations(Map.ofEntries(
						Map.entry(CacheNames.PERMISSION_SUMMARY, RedisCacheConfiguration.defaultCacheConfig()
								.entryTtl(Duration.ofMinutes(10))),
						Map.entry(CacheNames.PERMISSIONS, RedisCacheConfiguration.defaultCacheConfig()
								.entryTtl(Duration.ofMinutes(30))),
						Map.entry(CacheNames.ROLE_PERMISSIONS, RedisCacheConfiguration.defaultCacheConfig()
								.entryTtl(Duration.ofMinutes(30))),
						Map.entry(CacheNames.USER_DIRECT_PERMISSIONS, RedisCacheConfiguration.defaultCacheConfig()
								.entryTtl(Duration.ofMinutes(20)))))
				.transactionAware().build();
		
		return cacheManager;
	}
}