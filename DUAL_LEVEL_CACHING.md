# Dual-Level Caching Implementation

## Overview
This document describes the dual-level caching system implemented to ensure seamless cache fallback when Redis becomes unavailable at runtime. The system maintains both Redis and local in-memory caches, automatically falling back to the local cache if Redis fails.

---

## Problem Statement
Previously, the application would hit the database on every request if Redis went down **after startup**, even for repeated requests with the same parameters. This caused:
- Increased database load
- Performance degradation
- Poor user experience during Redis outages

## Solution
Implemented a dual-level caching strategy that:
1. ✅ Always tries Redis first (for distributability)
2. ✅ Falls back to local in-memory cache if Redis fails
3. ✅ Automatically syncs between both cache layers
4. ✅ Ensures no database hits on repeated requests when Redis is down

---

## Architecture

### Components Added

#### 1. **DualLevelCacheManager.java** (NEW)
**Purpose**: Orchestrates cache retrieval across Redis and local cache layers

**Key Features**:
- Implements Spring's `CacheManager` interface
- Wraps both Redis and local cache managers
- Returns `DualLevelCache` instances that handle fallback logic
- Gracefully handles Redis connection failures

**Location**: `src/main/java/com/example/useraccess/config/DualLevelCacheManager.java`

#### 2. **DualLevelCache.java** (NEW)
**Purpose**: Wraps individual cache instances with fallback logic

**Key Features**:
- Implements Spring's `Cache` interface
- `get()` operations: Tries Redis → Falls back to local cache
- `put()` operations: Writes to both Redis and local cache simultaneously
- `evict()`/`clear()` operations: Cleans both layers gracefully
- Includes comprehensive error handling with debug logging

**Location**: `src/main/java/com/example/useraccess/config/DualLevelCache.java`

#### 3. **DualLevelCacheErrorHandler.java** (NEW)
**Purpose**: Handles cache operation failures at runtime

**Key Features**:
- Implements Spring's `CacheErrorHandler` interface
- Routes failed Redis operations to local cache
- Logs all failures for monitoring
- Gracefully degrades to local-only caching

**Location**: `src/main/java/com/example/useraccess/config/DualLevelCacheErrorHandler.java`

#### 4. **CacheConfig.java** (MODIFIED)
**Changes**:
- Added `localCacheManager()` bean: Creates `ConcurrentMapCacheManager` for local fallback
- Updated `cacheManager()` method: Now returns `DualLevelCacheManager` wrapping both Redis and local cache
- Added `@Primary` annotation to `cacheManager()`: Resolves Spring bean ambiguity
- Updated `cacheErrorHandler()`: Uses `DualLevelCacheErrorHandler` for runtime error recovery
- Fallback logic: If Redis is unavailable at startup, uses local cache only

**Location**: `src/main/java/com/example/useraccess/config/CacheConfig.java`

#### 5. **RedisHealthIndicator.java** (NEW - Added in commit)
**Purpose**: Monitors Redis health for operational visibility

**Location**: `src/main/java/com/example/useraccess/component/RedisHealthIndicator.java`

---

## Behavior Matrix

| Scenario | First Request | Subsequent Request (same params) |
|----------|---|---|
| Redis available | Stores in Redis + Local | Cache HIT from Redis (syncs to local) |
| Redis down at startup | Stores in Local only | Cache HIT from Local ✅ |
| **Redis down mid-session** | **DB fetch → Local cache** | **Cache HIT from Local ✅** |
| Redis recovers | Stores in both layers again | Normal operation resumed |

---

## Cache Configuration

### Cache Names and TTLs
```
PERMISSION_SUMMARY    : 10 minutes (Redis) → Local cache
PERMISSIONS           : 30 minutes (Redis) → Local cache  
ROLE_PERMISSIONS      : 30 minutes (Redis) → Local cache
USER_DIRECT_PERMISSIONS : 20 minutes (Redis) → Local cache
```

### Local Cache Features
- Always available (no external dependency)
- Same TTL as Redis for consistency
- Automatic cleanup via Spring's cache eviction
- No distributed cache considerations (process-local only)

---

## Modified Files

### 1. **pom.xml**
- No new dependencies added
- Compatible with existing Spring Boot and Redis starter versions

### 2. **application.properties**
- Externalized Redis configuration properties
- Allows environment-specific settings without code changes

### 3. **CacheConfig.java**
```java
@Bean
public ConcurrentMapCacheManager localCacheManager() {
    // Local in-memory cache as fallback
}

@Bean
@Primary  // Resolves bean ambiguity
public CacheManager cacheManager(
    RedisConnectionFactory connectionFactory, 
    ConcurrentMapCacheManager localCacheManager) {
    // Dual-level cache logic
}
```

---

## Usage (No Changes Required)

Existing caching annotations work unchanged:

```java
@Cacheable(value = CacheNames.PERMISSION_SUMMARY, key = "#env + ':' + #userId + ':' + #roles")
public SummaryResponse getSummary(String env, String userId, List<String> roles) {
    // Automatically cached at dual levels
}
```

---

## Benefits

| Benefit | Impact |
|---------|--------|
| Zero Database Hits | Repeated requests served from local cache when Redis is down |
| High Availability | Service continues during Redis outages |
| Transparent | No code changes required in service layers |
| Fault Tolerant | Automatic fallback without manual intervention |
| Observable | Comprehensive logging for troubleshooting |
| Scalable | Can be extended to multiple cache layers |

---

## Testing

### Scenario 1: Redis Available
```
Request 1: DB hit → Store in Redis + Local → Response
Request 2: Hit Redis cache → Response (no DB)
```

### Scenario 2: Redis Down (Down at startup)
```
Request 1: DB hit → Store in Local → Response
Request 2: Hit Local cache → Response (no DB) ✅
```

### Scenario 3: Redis Down (Down mid-session)
```
Request 1: Hit Local cache → Response
Request 2: DB hit (Redis error handled) → Store in Local → Response
Request 3: Hit Local cache → Response (no DB) ✅
```

---

## Monitoring & Logging

### Key Log Messages
```
INFO  : Redis connection successful, using dual-level cache (Redis + Local fallback)
WARN  : Redis is unavailable. Using local in-memory cache only.
DEBUG : Cache HIT from Redis
DEBUG : Cache MISS from Redis, checking local cache
DEBUG : Cache HIT from local cache
WARN  : Redis cache GET failed. Falling back to local cache.
WARN  : Redis cache PUT failed. Stored in local cache instead.
```

### Health Indicator
The `RedisHealthIndicator` component provides `/actuator/health` endpoint visibility:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": { }
    }
  }
}
```

---

## Configuration Options

### Environment Properties
```properties
# Redis Connection
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000

# Cache TTLs (optional - overrides defaults)
cache.ttl.permission-summary=10
cache.ttl.permissions=30
```

---

## Future Enhancements

1. **Metrics**: Add Micrometer metrics for cache hit/miss rates
2. **Distributed Cache Event**: Sync local caches across instances if needed
3. **Manual Cache Invalidation**: HTTP endpoints for cache management
4. **Cache Statistics Dashboard**: Visualize cache performance
5. **Configurable TTLs**: Externalize all TTL values to properties

---

## Commit Information

**Commit Hash**: `f51cd73`  
**Author**: Sandeep Gajjala  
**Date**: April 23, 2026  
**Branch**: `main`

### Files Changed
- ✅ `pom.xml` (modified)
- ✅ `src/main/resources/application.properties` (modified)
- ✨ `src/main/java/com/example/useraccess/config/DualLevelCache.java` (new)
- ✨ `src/main/java/com/example/useraccess/config/DualLevelCacheErrorHandler.java` (new)
- ✨ `src/main/java/com/example/useraccess/config/DualLevelCacheManager.java` (new)
- ✨ `src/main/java/com/example/useraccess/component/RedisHealthIndicator.java` (new)
- 🔧 `src/main/java/com/example/useraccess/config/CacheConfig.java` (modified)

---

## How to Sync Your Local Repository

If you're experiencing sync issues with your local repository:

```bash
# Pull the latest changes
git pull origin main

# If merge conflicts occur on CacheConfig.java:
git checkout --ours src/main/java/com/example/useraccess/config/CacheConfig.java
git add src/main/java/com/example/useraccess/config/CacheConfig.java
git rebase --continue

# Verify you have all new files
git log --name-status --oneline -1

# Build to verify everything compiles
mvn clean compile -DskipTests
```

---

## Summary

The dual-level caching implementation provides **automatic Redis fallback** with **zero application changes**. The system ensures:

✅ **High Availability**: Service continues during Redis outages  
✅ **Zero DB Hits**: Repeated requests use local cache  
✅ **Transparent Integration**: Works with existing `@Cacheable` annotations  
✅ **Graceful Degradation**: Automatic failover without manual intervention  
✅ **Full Observability**: Comprehensive logging and monitoring  

This solution addresses the issue of database overload during Redis failures while maintaining the performance benefits of distributed caching when Redis is available.
