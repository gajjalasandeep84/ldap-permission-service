package com.example.useraccess.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.dto.PermissionItem;
import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.SummaryResponse;
import com.example.useraccess.repository.PermissionRepositoryCustom;

@Service
public class PermissionService {

	private final PermissionRepositoryCustom permissionRepository;

	public PermissionService(PermissionRepositoryCustom permissionRepository) {
		this.permissionRepository = permissionRepository;
	}

	@Cacheable(value = "permissionSummary", key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)")
	public SummaryResponse getSummary(String env, String userId, List<String> roles) {
		List<String> normalizedRoles = normalizeRoles(roles);
		int permissionCount = permissionRepository.getEffectivePermissionCount(userId, normalizedRoles);
		return new SummaryResponse(userId, env, permissionCount);
	}

	@Cacheable(value = "permissions", key = "#env + ':' + #userId + ':' + #root.target.buildRolesCacheKey(#roles)")
	public PermissionResponse getPermissions(String env, String userId, List<String> roles) {
		List<String> normalizedRoles = normalizeRoles(roles);
		List<String> permissionNames = permissionRepository.getEffectivePermissions(userId, normalizedRoles);

		List<PermissionItem> items = permissionNames.stream().map(this::toPermissionItem).toList();

		return new PermissionResponse(userId, env, items.size(), items);
	}

	public String buildRolesCacheKey(List<String> roles) {
		return normalizeRoles(roles).stream().collect(Collectors.joining("|"));
	}

	private List<String> normalizeRoles(List<String> roles) {
		if (roles == null) {
			return List.of();
		}

		return new ArrayList<>(roles).stream().filter(role -> role != null && !role.isBlank()).map(String::trim)
				.sorted(Comparator.naturalOrder()).toList();
	}

	private PermissionItem toPermissionItem(String permissionName) {
		PermissionItem item = new PermissionItem();
		item.setName(permissionName);
		item.setSource("EFFECTIVE_ACCESS");
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