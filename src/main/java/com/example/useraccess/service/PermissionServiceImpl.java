package com.example.useraccess.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.config.CacheNames;
import com.example.useraccess.dto.PermissionItem;
import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.SummaryResponse;

@Service
public class PermissionServiceImpl implements PermissionService {

	private final RolePermissionServiceImpl rolePermissionService;
	private final UserPermissionServiceImpl userPermissionService;

	public PermissionServiceImpl(RolePermissionServiceImpl rolePermissionService,
			UserPermissionServiceImpl userPermissionService) {
		this.rolePermissionService = rolePermissionService;
		this.userPermissionService = userPermissionService;
	}

	@Override
	@Cacheable(
		    value = CacheNames.PERMISSION_SUMMARY,
		    key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)"
		)
	public SummaryResponse getSummary(String env, String userId, List<String> roles) {
		List<String> rolePermissions = rolePermissionService.getPermissionsByRoles(roles);
		List<String> userPermissions = userPermissionService.getDirectPermissions(userId);

		int total = mergePermissions(rolePermissions, userPermissions).size();
		return new SummaryResponse(userId, env, total);
	}

	@Override
	@Cacheable(
		    value = CacheNames.PERMISSIONS,
		    key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)"
		)
	public PermissionResponse getPermissions(String env, String userId, List<String> roles) {
		List<String> rolePermissions = rolePermissionService.getPermissionsByRoles(roles);
		List<String> userPermissions = userPermissionService.getDirectPermissions(userId);

		List<String> effectivePermissions = mergePermissions(rolePermissions, userPermissions);

		List<PermissionItem> items = effectivePermissions.stream().map(this::toPermissionItem).toList();

		return new PermissionResponse(userId, env, items.size(), items);
	}

	private List<String> mergePermissions(List<String> rolePermissions, List<String> userPermissions) {
		return java.util.stream.Stream.concat(rolePermissions.stream(), userPermissions.stream()).distinct().sorted()
				.toList();
	}

	private PermissionItem toPermissionItem(String permissionName) {
		PermissionItem item = new PermissionItem();
		item.setName(permissionName);
		item.setSource("EFFECTIVE_ACCESS");
		item.setRiskLevel("LOW");
		return item;
	}
}