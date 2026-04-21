# Architecture & Design

## System Components

### 1. Controller Layer (`UserAccessController`)

**Responsibility:** Handle HTTP requests and route to services

```
HTTP Request
    ↓
UserAccessController
    ├─ POST /api/users/summary
    └─ POST /api/users/permissions
    ↓
PermissionService
```

Endpoints:
- `POST /api/users/summary` - Returns permission count
- `POST /api/users/permissions` - Returns permission list

All endpoints require JSON request body with `env`, `userId`, and `roles[]`.

---

### 2. Service Layer

#### PermissionService (Orchestrator)

Merges role-based and user-direct permissions.

```java
@Cacheable(value = "permissions", key = "...")
public PermissionResponse getPermissions(String env, String userId, List<String> roles) {
    // 1. Get role permissions
    List<String> rolePerms = rolePermissionService.getPermissionsByRoles(roles);
    
    // 2. Get user direct permissions
    List<String> userPerms = userPermissionService.getDirectPermissions(userId);
    
    // 3. Merge & deduplicate
    List<String> effective = merge(rolePerms, userPerms);
    
    // 4. Return
    return new PermissionResponse(...);
}
```

**Cache Key Logic:**
```java
key = "{env}:{userId}:{buildRolesCacheKey(roles)}"

Example: "prod:user123:admin|developer|viewer"
```

#### RolePermissionService

Retrieves permissions assigned to a set of roles.

```java
@Cacheable(value = "role_permissions", key = "buildRolesCacheKey(#roles)")
public List<String> getPermissionsByRoles(List<String> roles) {
    // Normalize roles (trim, deduplicate, sort)
    List<String> normalized = normalizeRoles(roles);
    
    // Query repository
    return repository.getPermissionsByRoles(normalized);
}
```

**Role Normalization:**
- Trim whitespace
- Remove duplicates (LinkedHashSet)
- Sort alphabetically
- Result: Consistent cache keys regardless of input order

#### UserPermissionService

Retrieves permissions assigned directly to a user.

```java
@Cacheable(value = "user_direct_permissions", key = "#userId")
public List<String> getDirectPermissions(String userId) {
    return repository.getDirectPermissions(userId.trim());
}
```

---

### 3. Repository Layer

#### RolePermissionRepository

**Interface:**
```java
public interface RolePermissionRepository {
    List<String> getPermissionsByRoles(List<String> roles);
    int getPermissionCountByRoles(List<String> roles);
}
```

**Implementation Options:**
- `RolePermissionRepositoryImpl` - Database implementation
- `MockRolePermissionRepository` - Test data

#### UserPermissionRepository

**Interface:**
```java
public interface UserPermissionRepository {
    List<String> getDirectPermissions(String userId);
    int getDirectPermissionCount(String userId);
}
```

---

### 4. Configuration Layer

#### CacheConfig

Initializes Redis-based caching.

```java
@Bean
@EnableCaching
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
    return cacheManager;
}
```

**Cache Configurations:**
| Cache Name | TTL | Purpose |
|-----------|-----|---------|
| permission_summary | 10m | Quick access to permission counts |
| permissions | 30m | Full permission lists |
| role_permissions | 30m | Role-to-permission mappings |
| user_direct_permissions | 20m | Direct user assignments |

#### RedisConfig

Configures serialization for Redis values.

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```

**Serializers:**
- **Keys:** StringRedisSerializer (human-readable)
- **Values:** GenericJackson2JsonRedisSerializer (JSON format)

#### RedisCacheErrorHandler

Gracefully handles Redis failures using Spring's `CacheErrorHandler` interface.

```java
@Override
public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
    logger.warn("Cache GET error for [{}] key [{}]. Proceeding without cache.", 
        cache.getName(), key);
}
```

**Behavior:**
- Logs warnings (not errors) for visibility
- Returns null for get operations (Spring retries with database)
- Returns void for put/evict (operation skipped safely)
- Application continues normally

---

## Data Flow Diagrams

### Permission Query Flow

```
Client Request
     ↓
UserAccessController.getPermissions()
     ↓
PermissionService.getPermissions()  [@Cacheable]
     ├─→ CACHE HIT → Return cached result
     │
     └─→ CACHE MISS or REDIS DOWN
          ↓
          RolePermissionService.getPermissionsByRoles()  [@Cacheable]
          ├─→ CACHE HIT → List<String>
          └─→ CACHE MISS
               ↓
               RolePermissionRepository.getPermissionsByRoles()
               ↓
               Database Query
               ↓
          ← Return rolePerms
          
          UserPermissionService.getDirectPermissions()  [@Cacheable]
          ├─→ CACHE HIT → List<String>
          └─→ CACHE MISS
               ↓
               UserPermissionRepository.getDirectPermissions()
               ↓
               Database Query
               ↓
          ← Return userPerms
          
          Merge & Deduplicate
          ↓
          Cache Result (if Redis available)
          ↓
Response ← Return PermissionResponse
```

---

## Redis Failure Handling Architecture

### Exception Handling Chain

```
┌─────────────────────────────────────────┐
│  @Cacheable method executes             │
│  Spring Cache tries to get/put value    │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Redis Connection Error occurs          │
│  (RedisConnectionException, etc.)       │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  CacheManager catches exception         │
│  Calls RedisCacheErrorHandler           │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Error logged with WARNING level        │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Original method executed directly      │
│  Data fetched from database             │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Result returned to caller              │
│  No exception propagates to client      │
└─────────────────────────────────────────┘
```

### Error Handler Implementation

```java
public class RedisCacheErrorHandler implements CacheErrorHandler {
    
    @Override
    public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
        // Log and continue - Spring will call underlying method
        logger.warn("Cache GET error for cache [{}] and key [{}]. Proceeding without cache.", 
            cache.getName(), key, e);
    }
    
    @Override
    public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
        // Log and continue - value won't be cached but application works
        logger.warn("Cache PUT error for cache [{}] and key [{}]. Value will not be cached.", 
            cache.getName(), key, e);
    }
}
```

---

## Cache Key Strategy

### Key Format

```
PermissionService.getSummary/getPermissions:
  {env}:{userId}:{rolesHash}
  
Example:
  prod:user123:admin|developer|viewer
  
RolePermissionService.getPermissionsByRoles:
  {rolesHash}
  
Example:
  admin|developer|viewer
  
UserPermissionService.getDirectPermissions:
  {userId}
  
Example:
  user123
```

### Role Hashing Algorithm

```java
public String buildRolesCacheKey(List<String> roles) {
    if (roles == null || roles.isEmpty()) {
        return "NO_ROLES";
    }
    
    // 1. Normalize: trim, deduplicate, sort
    Set<String> normalized = deduplicate(roles);
    List<String> sorted = sort(normalized);
    
    // 2. Join with pipe separator
    return String.join("|", sorted);
}

Example:
Input:  ["developer", "admin", "admin", " viewer "]
Step 1: {" viewer ", "developer", "admin"} → {"viewer", "developer", "admin"}
Step 2: ["admin", "developer", "viewer"]
Output: "admin|developer|viewer"
```

**Benefits:**
- Same permissions always produce same key
- Input order doesn't matter
- Whitespace normalized
- Duplicates eliminated

---

## Concurrency & Thread Safety

### Spring Cache Thread Safety

Spring's `@Cacheable` is thread-safe:
- Multiple threads requesting same key → One database call (due to locking)
- Results cached and returned to all threads
- Prevents thundering herd problem

```
Thread A: [cache miss] → DB query
Thread B: [waiting on lock] 
Thread C: [waiting on lock]
    ↓
Thread A: [cache updated]
    ↓
Thread B: [cache hit] ← Returns cached result
Thread C: [cache hit] ← Returns cached result
```

---

## Performance Optimization

### Cache Hit Optimization

```
Scenario: 100 requests for same permission set
Without cache: 100 DB queries (~50-100ms each)
With cache:    1 DB query + 99 cache hits (~1-2ms each)

Improvement:   ~99% reduction in database load
Response time: 50x faster for cache hits
```

### Database Query Optimization

1. **Connection Pooling:** Reuses connections (configured in properties)
2. **Query Optimization:** Single query per role/user set
3. **Result Caching:** Eliminates repeated queries for same data
4. **Load Distribution:** Cache writes are asynchronous

---

## Security Considerations

### Authorization

- Service assumes user/role data is validated upstream (Spring Security)
- No authorization checks in permission service (trusted data)
- Caller responsible for user authentication

### Cache Security

- Cache keys are deterministic (no sensitive data)
- Cached values are serialized as JSON
- Redis should be on private network with password auth
- Consider encryption at rest for sensitive deployments

### DTOs

Request/Response objects include only necessary fields:
- `env` - Environment identifier
- `userId` - User identifier  
- `roles` - List of role names
- `permissions` - List of permission names (no sensitive data)

---

## Scalability Design

### Horizontal Scaling

```
Load Balancer
    ↓
┌───┴────┬─────┬─────┐
│        │     │     │
v        v     v     v
Instance Instance Instance
  1        2      3
    ↓        ↓      ↓
    └────┬───┴──────┘
         │
        Redis (Shared)
         │
      Database
```

**Architecture Benefits:**
- Stateless instances (can scale horizontally)
- Shared Redis cache across instances
- Single database eliminates data inconsistency

### Cache Invalidation (Future)

Need to consider:
1. Permission updates (role assignment changes)
2. Scheduled cache refresh
3. Manual invalidation API
4. Event-driven cache updates

---

## Monitoring Points

### Key Metrics to Track

1. **Cache Metrics**
   - Hit rate (should be >80%)
   - Miss rate
   - Eviction rate
   - Error count

2. **Database Metrics**
   - Query count per minute
   - Query latency
   - Connection pool utilization

3. **Application Metrics**
   - Request latency (p50, p95, p99)
   - Error rate
   - Thread pool size
   - Memory usage

### Logging Points

- Every cache operation (hit/miss/error)
- Every database query
- Every permission resolution
- All exceptions with stack traces

---

## Technology Rationale

| Technology | Reason |
|-----------|--------|
| Spring Boot | Standard, productive framework |
| Spring Cache | Abstraction layer for pluggable caching |
| Redis | Fast, distributed cache with TTL support |
| Spring Data JPA | ORM for database abstraction |
| Jackson | JSON serialization for Redis |
| OpenAPI/Swagger | API documentation and testing |

---

## Future Enhancement Opportunities

1. **Cache Warming**
   - Load frequently accessed permissions on startup
   - Reduce cold starts

2. **Redis Cluster**
   - High availability setup
   - Data replication
   - Automatic failover

3. **Event-Driven Cache Invalidation**
   - Listen to permission change events
   - Invalidate affected cache entries
   - Real-time consistency

4. **Multi-Level Caching**
   - Local in-memory cache (fast)
   - Redis distributed cache (shared)
   - Database (source of truth)

5. **Permission Analytics**
   - Track most requested permissions
   - Identify hot data sets
   - Optimize cache configuration

6. **Audit Trail**
   - Log all permission queries
   - Track access patterns
   - Security compliance
