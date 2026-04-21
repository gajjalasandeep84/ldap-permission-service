# API Usage Guide

## Base URL

```
http://localhost:8080/api/users
```

## Authentication

Currently, no authentication is required. In production, configure Spring Security with appropriate auth mechanisms.

## Request Format

All requests use JSON format with `Content-Type: application/json`.

### Common Headers

```
Content-Type: application/json
Accept: application/json
```

## Endpoints

---

## 1. Get Permission Summary

Returns the total count of permissions for a user.

### Request

**Method:** `POST`

**Path:** `/summary`

**URL:** `http://localhost:8080/api/users/summary`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "env": "production",
  "userId": "john_doe_123",
  "roles": ["admin", "developer", "reviewer"]
}
```

### Response

**HTTP Status:** `200 OK`

**Body:**
```json
{
  "userId": "john_doe_123",
  "env": "production",
  "totalPermissions": 47
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | User identifier from request |
| `env` | String | Environment from request |
| `totalPermissions` | Integer | Total unique permissions merged from roles and direct assignments |

### Example cURL

```bash
curl -X POST http://localhost:8080/api/users/summary \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "john_doe_123",
    "roles": ["admin", "developer", "reviewer"]
  }'
```

### Response Time

- **Cache Hit:** ~5-10ms
- **Cache Miss (Redis available):** ~100-200ms (DB + cache write)
- **Redis Down:** ~100-500ms (direct DB query)

---

## 2. Get User Permissions

Returns the complete list of permissions for a user.

### Request

**Method:** `POST`

**Path:** `/permissions`

**URL:** `http://localhost:8080/api/users/permissions`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "env": "staging",
  "userId": "jane_smith_456",
  "roles": ["developer", "tester"]
}
```

### Response

**HTTP Status:** `200 OK`

**Body:**
```json
{
  "userId": "jane_smith_456",
  "env": "staging",
  "count": 28,
  "permissions": [
    {
      "name": "CREATE_USER",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "LOW"
    },
    {
      "name": "EDIT_USER",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "LOW"
    },
    {
      "name": "DELETE_USER",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "MEDIUM"
    },
    {
      "name": "VIEW_LOGS",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "LOW"
    },
    {
      "name": "MODIFY_CONFIG",
      "source": "EFFECTIVE_ACCESS",
      "riskLevel": "HIGH"
    }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | User identifier from request |
| `env` | String | Environment from request |
| `count` | Integer | Total number of permissions in list |
| `permissions` | Array | List of permission objects |
| `permissions[].name` | String | Permission identifier/name |
| `permissions[].source` | String | Source of permission (always "EFFECTIVE_ACCESS") |
| `permissions[].riskLevel` | String | Risk classification: LOW, MEDIUM, HIGH |

### Example cURL

```bash
curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "env": "staging",
    "userId": "jane_smith_456",
    "roles": ["developer", "tester"]
  }'
```

### Sorting

Permissions are returned in alphabetical order by name.

---

## Request Parameters

### Common Parameters

#### env (Environment)

**Type:** String (required)

**Description:** Environment identifier

**Examples:**
- `production`
- `staging`
- `development`
- `qa`

**Usage:** Can be used to segment permissions by environment

#### userId (User ID)

**Type:** String (required)

**Description:** Unique user identifier

**Examples:**
- `john_doe_123`
- `user@company.com`
- `u_12345`

**Validation:**
- Cannot be null or blank
- Trimmed of whitespace
- Case-sensitive

**Rules:**
- Empty/blank user IDs return empty permission lists
- Searched with exact match (after trimming)

#### roles (Role List)

**Type:** Array of Strings (required, can be empty)

**Description:** List of roles assigned to user

**Examples:**
```json
{
  "roles": ["admin"]
}
```

```json
{
  "roles": ["developer", "reviewer", "tester"]
}
```

```json
{
  "roles": []
}
```

**Validation:**
- Each role string is trimmed
- Null/blank roles are filtered out
- Duplicates are automatically removed
- Order doesn't matter (internally sorted)

**Examples:**
```
Input:  ["admin", "viewer", "admin", " reviewer ", null]
Output: ["admin", "reviewer", "viewer"]
```

---

## Error Handling

### Successful Response

```
HTTP 200 OK
{
  "userId": "...",
  "env": "...",
  ...
}
```

### Error Response

**HTTP 400 Bad Request** - Invalid request format

```json
{
  "timestamp": "2026-04-21T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON parse error: ...",
  "path": "/api/users/permissions"
}
```

**Causes:**
- Invalid JSON format
- Missing required fields
- Wrong data types

**HTTP 500 Internal Server Error** - Application error

```json
{
  "timestamp": "2026-04-21T10:30:45.123Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Unexpected error occurred",
  "path": "/api/users/permissions"
}
```

**Note:** With Redis failure handling, this should rarely occur. Database queries are executed as fallback.

---

## Real-World Examples

### Example 1: Admin User

**Request:**
```bash
curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "admin_001",
    "roles": ["admin", "super_admin"]
  }'
```

**Response:**
```json
{
  "userId": "admin_001",
  "env": "production",
  "count": 156,
  "permissions": [
    {"name": "ADMIN_VIEW_LOGS", "source": "EFFECTIVE_ACCESS", "riskLevel": "HIGH"},
    {"name": "CREATE_BACKUP", "source": "EFFECTIVE_ACCESS", "riskLevel": "HIGH"},
    {"name": "CREATE_USER", "source": "EFFECTIVE_ACCESS", "riskLevel": "MEDIUM"},
    ...
  ]
}
```

### Example 2: Developer User

**Request:**
```bash
curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "env": "development",
    "userId": "dev_john",
    "roles": ["developer", "devops"]
  }'
```

**Response:**
```json
{
  "userId": "dev_john",
  "env": "development",
  "count": 34,
  "permissions": [
    {"name": "BUILD_PROJECT", "source": "EFFECTIVE_ACCESS", "riskLevel": "LOW"},
    {"name": "DEPLOY_DEV", "source": "EFFECTIVE_ACCESS", "riskLevel": "LOW"},
    {"name": "READ_LOGS", "source": "EFFECTIVE_ACCESS", "riskLevel": "LOW"},
    ...
  ]
}
```

### Example 3: User with No Roles

**Request:**
```bash
curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "user_guest",
    "roles": []
  }'
```

**Response:**
```json
{
  "userId": "user_guest",
  "env": "production",
  "count": 5,
  "permissions": [
    {"name": "VIEW_PROFILE", "source": "EFFECTIVE_ACCESS", "riskLevel": "LOW"},
    {"name": "VIEW_PUBLIC_DATA", "source": "EFFECTIVE_ACCESS", "riskLevel": "LOW"},
    ...
  ]
}
```

### Example 4: Quick Summary Request

**Request:**
```bash
curl -X POST http://localhost:8080/api/users/summary \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "app_service",
    "roles": ["service_account", "automation"]
  }'
```

**Response:**
```json
{
  "userId": "app_service",
  "env": "production",
  "totalPermissions": 18
}
```

---

## Performance Considerations

### Request Size

- Typical request: ~200 bytes
- Typical response (summary): ~100 bytes
- Typical response (full permissions): ~5-50 KB

### Response Times

| Scenario | Time | Notes |
|----------|------|-------|
| Cache hit | 5-10ms | Redis working, data cached |
| Cache miss (fresh) | 100-300ms | Query DB, write to cache |
| Redis timeout | 2000+ms | Cache operation times out |
| Redis down | 100-500ms | Direct database query |
| High concurrency | Variable | Lock contention on cache |

### Rate Limiting

Currently no rate limiting implemented. Consider adding in production:

```properties
# Future enhancement: rate limiting
management.endpoints.web.exposure.include=metrics,health
spring.cloud.gateway.routes[0].predicates[0].args.capacity=100
```

---

## Testing & Debugging

### Using Swagger UI

1. Open `http://localhost:8080/swagger-ui.html`
2. Expand "User Access" section
3. Click "Try it out" button
4. Fill in request JSON
5. Click "Execute"
6. View response

### Using Postman

1. Create new POST request
2. URL: `http://localhost:8080/api/users/permissions`
3. Headers tab:
   - Key: `Content-Type`
   - Value: `application/json`
4. Body tab (raw, JSON):
   ```json
   {
     "env": "production",
     "userId": "test_user",
     "roles": ["admin"]
   }
   ```
5. Click "Send"

### Using Visual Studio Code REST Client

Create `.vscode/settings.json`:

```json
{
  "rest-client.environment": "local"
}
```

Create file `requests/permissions.http`:

```http
### Get user permissions
POST http://localhost:8080/api/users/permissions
Content-Type: application/json

{
  "env": "production",
  "userId": "john_doe",
  "roles": ["admin", "developer"]
}

### Get permission summary
POST http://localhost:8080/api/users/summary
Content-Type: application/json

{
  "env": "production",
  "userId": "john_doe",
  "roles": ["admin", "developer"]
}
```

Then click "Send Request" link above each request.

---

## Common Use Cases

### Use Case 1: Check User Permissions

Get all permissions for a user to show what actions they can perform:

```bash
curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "env": "production",
    "userId": "'$USER_ID'",
    "roles": "'$ROLES_JSON'"
  }'
```

### Use Case 2: Audit Permission Count

Get quick count for logging/metrics:

```bash
curl -X POST http://localhost:8080/api/users/summary \
  -H "Content-Type: application/json" \
  -d '{"env":"prod","userId":"'$USER_ID'","roles":['$ROLES']}'
```

### Use Case 3: Monitor Cache Performance

Check response times:

```bash
# Measure response time
time curl -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d '{"env":"prod","userId":"user123","roles":["admin"]}'
```

Expected:
- First call: ~200ms (cache miss)
- Second call: ~10ms (cache hit)
- 20x faster! ⚡

---

## Troubleshooting

### Issue: Empty Permissions Returned

**Cause:** User might not have any direct permissions

**Solution:** Check if user ID and roles are correct

### Issue: Connection Refused

**Cause:** Service not running

**Solution:** `mvn spring-boot:run` or check port 8080

### Issue: Slow Response Times (>500ms)

**Cause:** Redis down or database slow

**Solution:** Check logs for "Cache GET error", verify Redis/DB connectivity

### Issue: Inconsistent Permission Counts

**Cause:** Role or direct permission data changed while querying

**Solution:** Data is eventually consistent due to cache TTLs

---

## Batch Operations

To get permissions for multiple users efficiently:

```bash
# Get permissions for 3 users (sequential)
for USER_ID in "user1" "user2" "user3"; do
  curl -X POST http://localhost:8080/api/users/permissions \
    -H "Content-Type: application/json" \
    -d '{"env":"prod","userId":"'$USER_ID'","roles":["admin"]}'
done
```

Better approach using parallel requests:

```bash
# Parallel requests with GNU parallel
parallel 'curl -s -X POST http://localhost:8080/api/users/permissions \
  -H "Content-Type: application/json" \
  -d "\"env\":\"prod\",\"userId\":\"{}\",\"roles\":[\"admin\"]"' \
  ::: user1 user2 user3
```

---

## API Versioning

Current API version: **v1** (implicit)

Plan for future:
- **v2:** Add response pagination, filtering
- **v3:** Support batch operations
- **v4:** Add event streaming

---

## Support

For issues or questions:
1. Check logs: `tail -f application.log | grep ERROR`
2. Review CACHE_FAILOVER.md for Redis issues
3. Check ARCHITECTURE.md for system design
4. Review this guide for API details
