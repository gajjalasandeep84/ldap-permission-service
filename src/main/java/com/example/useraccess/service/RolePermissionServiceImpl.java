package com.example.useraccess.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.config.CacheNames;
import com.example.useraccess.repository.RolePermissionRepository;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

	private static final String CACHE_EMPTY_ROLES = "NO_ROLES";

	private final RolePermissionRepository rolePermissionRepository;

	public RolePermissionServiceImpl(RolePermissionRepository rolePermissionRepository) {
		this.rolePermissionRepository = rolePermissionRepository;
	}

	@Cacheable(value = CacheNames.ROLE_PERMISSIONS, key = "#root.target.buildRolesCacheKey(#roles)")
	public List<String> getPermissionsByRoles(List<String> roles) {
		List<String> normalizedRoles = normalizeRoles(roles);

		if (normalizedRoles.isEmpty()) {
			return List.of();
		}

		return rolePermissionRepository.getPermissionsByRoles(normalizedRoles);
	}

	@Override
	@Cacheable(value = "rolePermissionCount", key = "#root.target.buildRolesCacheKey(#roles)")
	public int getPermissionCountByRoles(List<String> roles) {
		List<String> normalizedRoles = normalizeRoles(roles);

		if (normalizedRoles.isEmpty()) {
			return 0;
		}

		return rolePermissionRepository.getPermissionCountByRoles(normalizedRoles);
	}

	@Override
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

		Set<String> deduped = new LinkedHashSet<>();

		for (String role : roles) {
			if (role != null) {
				String trimmed = role.trim();
				if (!trimmed.isBlank()) {
					deduped.add(trimmed);
				}
			}
		}

		return new ArrayList<>(deduped).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
	}
}