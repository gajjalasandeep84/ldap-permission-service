package com.example.useraccess.service;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
 
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
 
import com.example.useraccess.dto.PermissionItem;
import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.SummaryResponse;
 
@Service
public class PermissionService {
 
    private final RolePermissionService rolePermissionService;
    private final UserPermissionService userPermissionService ;
 
    public PermissionService(RolePermissionService rolePermissionService,
                             UserPermissionService userPermissionService) {
        this.rolePermissionService = rolePermissionService;
        this.userPermissionService = userPermissionService;
    }
 
    /**
     * Summary stays lightweight in Option 2.
     * Do not load full permission count here.
     */
    public SummaryResponse getSummary(String env, String userId, List<String> roles) {
        return new SummaryResponse(userId, env, 0);
    }
 
    /**
     * Permission count is loaded separately from summary.
     * Count is computed from merged distinct permissions.
     */
    @Cacheable(
            value = "permissionCount",
            key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)"
    )
    public int getPermissionCount(String env, String userId, List<String> roles) {
        List<String> normalizedRoles = normalizeRoles(roles);
        return getEffectivePermissionNames(env, userId, normalizedRoles).size();
    }
 
    /**
     * Full effective permissions across all roles + user-specific add-on permissions.
     */
    @Cacheable(
            value = "permissions",
            key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)"
    )
    public PermissionResponse getPermissions(String env, String userId, List<String> roles) {
        List<String> normalizedRoles = normalizeRoles(roles);
        List<String> permissionNames = getEffectivePermissionNames(env, userId, normalizedRoles);
 
        List<PermissionItem> items = permissionNames.stream()
                .map(this::toEffectivePermissionItem)
                .toList();
 
        return new PermissionResponse(userId, env, items.size(), items);
    }
 
    /**
     * Single role permissions for "View perms" button in AD Groups tab.
     * Delegates to cached role-level service.
     */
    public PermissionResponse getPermissionsForSingleRole(String env, String roleName) {
        String normalizedRole = normalizeRole(roleName);
        List<String> permissionNames = rolePermissionService.getPermissionsForRole(env, normalizedRole);
 
        List<PermissionItem> items = permissionNames.stream()
                .sorted()
                .map(this::toRolePermissionItem)
                .toList();
 
        return new PermissionResponse(normalizedRole, env, items.size(), items);
    }
 
    /**
     * Builds final effective permission names by:
     * 1. loading cached permissions for each role
     * 2. loading cached user-specific permissions
     * 3. merging and deduplicating
     */
    private List<String> getEffectivePermissionNames(String env, String userId, List<String> roles) {
        Set<String> merged = new LinkedHashSet<>();
 
        for (String role : roles) {
            List<String> rolePermissions = rolePermissionService.getPermissionsForRole(env, role);
            merged.addAll(rolePermissions);
        }
 
        List<String> userPermissions = userPermissionService.getUserPermissions(env, userId);
        merged.addAll(userPermissions);
 
        return merged.stream()
                .sorted()
                .toList();
    }
 
    /**
     * Used in cache key generation so same role list in different order
     * maps to the same cache entry.
     */
    public String buildRolesCacheKey(List<String> roles) {
        return normalizeRoles(roles).stream()
                .collect(Collectors.joining("|"));
    }
 
    public String normalizeRole(String roleName) {
        return roleName == null ? "" : roleName.trim();
    }
 
    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
 
        return new ArrayList<>(roles).stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
 
    private PermissionItem toEffectivePermissionItem(String permissionName) {
        PermissionItem item = new PermissionItem();
        item.setName(permissionName);
        item.setSource("EFFECTIVE_ACCESS");
        item.setRiskLevel(determineRiskLevel(permissionName));
        return item;
    }
 
    private PermissionItem toRolePermissionItem(String permissionName) {
        PermissionItem item = new PermissionItem();
        item.setName(permissionName);
        item.setSource("ROLE");
        item.setRiskLevel(determineRiskLevel(permissionName));
        return item;
    }
 
    private String determineRiskLevel(String permissionName) {
        String upper = permissionName == null ? "" : permissionName.toUpperCase();
 
        if (upper.contains("DELETE") || upper.contains("UPDATE")) {
            return "HIGH";
        }
 
        if (upper.contains("EDIT") || upper.contains("APPROVE") || upper.contains("REJECT")) {
            return "MEDIUM";
        }
 
        return "LOW";
    }
}