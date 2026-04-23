package com.example.useraccess.component;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ibm.db2.jcc.a.a.e;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Implement Redis health check logic here
            // For example, you can try to connect to Redis and return Health.up() if
            // successful
            // or Health.down() if there is an issue.
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equals(pong)) {
                return Health.up().withDetail("redis", "reachable").build();
            } else {
                return Health.down().withDetail("redis", "unexpected response").build();
            }
        } catch (Exception e) {
            return Health.down(e).withDetail("redis", "unreachable").build();
        }

    }
}
