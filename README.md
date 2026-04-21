# LDAP Permission Service

A high-performance microservice for managing user permissions and access control using role-based and user-direct permission mapping with Redis caching and graceful database fallback.

## Overview

The LDAP Permission Service provides REST APIs to query and retrieve user permissions based on:
- **Role-based permissions** - Permissions inherited from user roles
- **Direct permissions** - Permissions assigned directly to a user
- **Merged permissions** - Combination of both role and direct permissions

The service uses Redis for high-performance caching with automatic fallback to database queries when Redis is unavailable.

## Features

✅ **Role-based Access Control (RBAC)** - Manage permissions through role assignments  
✅ **Redis Caching** - Fast response times with configurable TTLs  
✅ **Graceful Degradation** - Automatically queries database when Redis is down  
✅ **OpenAPI/Swagger** - Interactive API documentation  
✅ **Comprehensive Logging** - Track all cache operations and failures  
✅ **Configurable TTLs** - Different cache durations for different permission types  

## Project Structure

```
ldap-permission-service/
├── src/main/java/com/example/useraccess/
│   ├── PermissionServiceApplication.java       # Spring Boot entry point
│   ├── config/
│   │   ├── CacheConfig.java                    # Redis cache configuration
│   │   ├── CacheNames.java                     # Cache name constants
│   │   ├── OpenApiConfig.java                  # Swagger/OpenAPI config
│   │   ├── RedisConfig.java                    # Redis template config
│   │   └── RedisCacheErrorHandler.java         # Cache error handling
│   ├── controller/
│   │   └── UserAccessController.java           # REST API endpoints
│   ├── dto/
│   │   ├── PermissionRequest.java              # API request DTO
│   │   ├── PermissionResponse.java             # API response DTO
│   │   ├── SummaryResponse.java                # Summary response DTO
│   │   └── PermissionItem.java                 # Permission details DTO
│   ├── service/
│   │   ├── PermissionService.java              # Permission service interface
│   │   ├── PermissionServiceImpl.java           # Main permission service
│   │   ├── RolePermissionService.java          # Role permission interface
│   │   ├── RolePermissionServiceImpl.java       # Role permission service
│   │   ├── UserPermissionService.java          # User permission interface
│   │   └── UserPermissionServiceImpl.java       # User permission service
│   └── repository/
│       ├── RolePermissionRepository.java       # Role permission repository interface
│       ├── RolePermissionRepositoryImpl.java    # Role permission repository impl
│       ├── UserPermissionRepository.java       # User permission repository interface
│       ├── UserPermissionRepositoryImpl.java    # User permission repository impl
│       ├── MockRolePermissionRepository.java   # Mock data for testing
│       └── MockUserPermissionRepository.java   # Mock data for testing
├── pom.xml                                      # Maven configuration
└── README.md                                    # This file
```

## Technology Stack

- **Java 17** - Runtime language
- **Spring Boot 3.5** - Framework
- **Spring Data JPA** - Database access
- **Spring Cache** - Caching abstraction
- **Redis** - Primary cache store
- **OpenAPI 3** - API documentation
- **Maven** - Build tool

## REST API Endpoints

### 1. Get User Permission Summary

**Endpoint:** `POST /api/users/summary`

Returns the total count of permissions for a user based on roles and direct permissions.

**Request:**
```json
{
  "env": "production",
  "userId": "user123",
  "roles": ["admin", "developer"]
}
```

**Response:**
```json
{
  "userId": "user123",
  "env": "production",
  "totalPermissions": 25
}
```

**Cache:** TTL 10 minutes

---

### 2. Get User Permissions

**Endpoint:** `POST /api/users/permissions`

Returns the complete list of permissions for a user (merged from roles and direct permissions).

**Request:**
```json
{
  "env": "production",
  "userId": "user123",
  "roles": ["admin", "developer"]
}
```

**Response:**
```json
{
  "userId": "user123",
  "env": "production",
  "count": 25,
  "permissions": [
    {
      "name": "CREATE_USER",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "LOW"
    },
    {
      "name": "DELETE_USER",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "LOW"
    }
  ]
}
```

**Cache:** TTL 30 minutes

---

## Caching Strategy

| Cache Name | Content | TTL | Priority |
|-----------|---------|-----|----------|
| `permission_summary` | Total permission count | 10 min | High |
| `permissions` | Complete permission list | 30 min | High |
| `role_permissions` | Permissions by role | 30 min | Medium |
| `user_direct_permissions` | Direct user permissions | 20 min | Medium |

## Cache Key Format

Cache keys are built using:
- **User Summary/Permissions:** `{env}:{userId}:{roles_hash}`
- **Role Permissions:** `{roles_hash}` (normalized and sorted)
- **User Direct Permissions:** `{userId}`

Roles are normalized (trimmed, deduplicated, sorted) before hashing to ensure consistency.

## How It Works

### Permission Resolution Flow

```
┌─────────────────────────────────────────┐
│  User requests permissions              │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Check Redis Cache                      │
└────────────┬────────┬───────────────────┘
             │        │
       CACHE │        │ NO CACHE
       HIT   │        │
             ▼        ▼
        ┌────────┐  ┌──────────────────────┐
        │ Return │  │ Query Database       │
        │ Cached │  ├──────────────────────┤
        │ Result │  │ 1. Get role perms    │
        └────────┘  │ 2. Get user perms    │
                    │ 3. Merge results     │
                    │ 4. Cache result      │
                    └──────────┬───────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │ Return Result   │
                        └─────────────────┘
```

### Redis Failure Handling

When Redis becomes unavailable:

```
┌─────────────────────────────────────┐
│  @Cacheable method called           │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Spring attempts cache lookup       │
└────────────┬────────────────────────┘
             │
      REDIS  │
        DOWN │
             ▼
┌─────────────────────────────────────┐
│  RedisCacheErrorHandler catches     │
│  exception and logs warning         │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Spring skips cache, executes       │
│  underlying method directly         │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Database is queried for fresh data │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Result returned to client          │
└─────────────────────────────────────┘
```

**Key Points:**
- No exceptions thrown to clients
- Database is automatically queried
- All cache errors are logged as warnings
- Application remains fully functional

## Building & Running

### Prerequisites
- Java 17+
- Maven 3.6+
- Redis (optional - service uses database if Redis unavailable)

### Build

```bash
mvn clean compile
```

### Run

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`

### Access API Documentation

Open your browser to: `http://localhost:8080/swagger-ui.html`

## Configuration

### Application Properties

Create `src/main/resources/application.properties`:

```properties
spring.application.name=ldap-permission-service

# Server
server.port=8080

# Redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000
spring.redis.jedis.pool.max-active=8
spring.redis.jedis.pool.max-idle=8

# Logging
logging.level.root=INFO
logging.level.com.example.useraccess=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
```

## Testing

### Using cURL

**Get Permission Summary:**
```bash
curl -X POST http://localhost:8080/api/users/summary \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "user123",
    "roles": ["admin", "developer"]
  }'
```

**Get Full Permissions:**
```bash
curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "user123",
    "roles": ["admin", "developer"]
  }'
```

## Monitoring & Logging

The service logs important events:

```
2026-04-21 10:30:45 - RedisCacheErrorHandler - Cache GET error for cache [permissions] and key [production:user123:admin|developer]. Proceeding without cache.
2026-04-21 10:30:46 - PermissionServiceImpl - Getting permissions for user: user123
2026-04-21 10:30:46 - RolePermissionServiceImpl - Retrieving permissions for roles: [admin, developer]
```

### Cache Operation Flow
- **Cache Hit:** Returns immediately from Redis
- **Cache Miss:** Queries database, caches result
- **Redis Error:** Logs warning, queries database directly
- **All operations:** Logged with timestamps and context

## Performance Considerations

- **Cache Hit Rate:** With proper TTL configuration, expect 80-95% hit rates
- **Response Time:** ~10ms for cache hits, ~100-500ms for database queries
- **Memory:** Redis memory usage depends on dataset size and TTL settings
- **Database Load:** Reduced by ~90% with caching enabled

## Troubleshooting

### Redis Connection Issues
```
Problem: Unable to connect to Redis
Solution: Check Redis is running and accessible at configured host:port
```

### High Cache Miss Rate
```
Problem: Too many database queries despite caching
Solution: Check cache key format, increase TTL values, verify data consistency
```

### Slow API Responses
```
Problem: Responses taking >1 second
Solution: Check Redis connectivity, monitor database query performance, verify network latency
```

## Future Enhancements

- [ ] Database connection pooling optimization
- [ ] Cache warming on startup
- [ ] Redis cluster support
- [ ] Manual cache invalidation API
- [ ] Permission change event notifications
- [ ] Audit logging for all permission queries
- [ ] Rate limiting

## License

Proprietary - Internal use only
