package com.example.useraccess.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.config.CacheNames;
import com.example.useraccess.dto.PermissionItem;
import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.SummaryResponse;

@Service
public class PermissionServiceImpl implements PermissionService {

	private static final String CACHE_EMPTY_ROLES = "NO_ROLES";

	private final RolePermissionService rolePermissionService;
	private final UserPermissionService userPermissionService;

	public PermissionServiceImpl(RolePermissionService rolePermissionService,
			UserPermissionService userPermissionService) {
		this.rolePermissionService = rolePermissionService;
		this.userPermissionService = userPermissionService;
	}

	@Override
	@Cacheable(value = CacheNames.PERMISSION_SUMMARY, key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)")
	public SummaryResponse getSummary(String env, String userId, List<String> roles) {
		List<String> rolePermissions = rolePermissionService.getPermissionsByRoles(roles);
		List<String> userPermissions = userPermissionService.getDirectPermissions(userId);

		int total = mergePermissions(rolePermissions, userPermissions).size();
		return new SummaryResponse(userId, env, total);
	}

	@Override
	@Cacheable(value = CacheNames.PERMISSIONS, key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)")
	public PermissionResponse getPermissions(String env, String userId, List<String> roles) {
		List<String> rolePermissions = rolePermissionService.getPermissionsByRoles(roles);
		List<String> userPermissions = userPermissionService.getDirectPermissions(userId);

		List<String> effectivePermissions = mergePermissions(rolePermissions, userPermissions);

		List<PermissionItem> items = effectivePermissions.stream().map(this::toPermissionItem).toList();

		return new PermissionResponse(userId, env, items.size(), items);
	}

	public String buildRolesCacheKey(List<String> roles) {
		List<String> normalizedRoles = normalizeRoles(roles);
		if (normalizedRoles.isEmpty()) {
			return CACHE_EMPTY_ROLES;
		}
		return String.join("|", normalizedRoles);
	}

	private List<String> normalizeRoles(List<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return List.of();
		}

		return roles.stream().filter(Objects::nonNull).map(String::trim).filter(role -> !role.isBlank()).distinct()
				.sorted().toList();
	}

	private List<String> mergePermissions(List<String> rolePermissions, List<String> userPermissions) {
		return Stream.concat(rolePermissions.stream(), userPermissions.stream()).distinct().sorted().toList();
	}

	private PermissionItem toPermissionItem(String permissionName) {
		PermissionItem item = new PermissionItem();
		item.setName(permissionName);
		item.setSource("EFFECTIVE_ACCESS");
		item.setRiskLevel("LOW");
		return item;
	}
}