package com.example.useraccess.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mock")
public class MockPermissionRepository implements PermissionRepositoryCustom {

	@Override
	public int getEffectivePermissionCount(String userId, List<String> roles) {
		return getEffectivePermissions(userId, roles).size();
	}

	@Override
	public List<String> getEffectivePermissions(String userId, List<String> roles) {
		Set<String> permissions = new LinkedHashSet<>();

		List<String> normalizedRoles = normalizeRoles(roles);
		String normalizedUserId = normalizeUserId(userId);

		for (String role : normalizedRoles) {
			applyRolePermissions(role, permissions);
		}

		applyUserPermissions(normalizedUserId, permissions);

		return permissions.stream().sorted(Comparator.naturalOrder()).toList();
	}

	private List<String> normalizeRoles(List<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return List.of();
		}

		List<String> normalized = new ArrayList<>();

		for (String role : roles) {
			if (role == null) {
				continue;
			}

			String trimmed = role.trim();
			if (!trimmed.isBlank()) {
				normalized.add(trimmed.toUpperCase());
			}
		}

		return normalized;
	}

	private String normalizeUserId(String userId) {
		return userId == null ? "" : userId.trim().toUpperCase();
	}

	private void applyRolePermissions(String role, Set<String> permissions) {
		if (role.contains("CSR")) {
			permissions.add("CASE_READ");
			permissions.add("MEMBER_READ");
			permissions.add("DOCUMENT_READ");
		}

		if (role.contains("PLANADMIN")) {
			permissions.add("PLAN_READ");
			permissions.add("PLAN_UPDATE");
			permissions.add("MEMBER_UPDATE");
		}

		if (role.contains("SHOP")) {
			permissions.add("SHOP_READ");
		}

		if (role.contains("VENDOR")) {
			permissions.add("VENDOR_READ");
			permissions.add("VENDOR_UPDATE");
		}

		if (role.contains("SPLUNK")) {
			permissions.add("AUDIT_READ");
		}

		if (role.contains("ADMIN")) {
			permissions.add("USER_READ");
			permissions.add("USER_UPDATE");
			permissions.add("DOCUMENT_DELETE");
		}
	}

	private void applyUserPermissions(String userId, Set<String> permissions) {
		if (!userId.isBlank()) {
			permissions.add("PROFILE_READ");
		}

		if (userId.contains("AUTO")) {
			permissions.add("BATCH_MONITOR_READ");
		}

		if (userId.endsWith("2")) {
			permissions.add("EXCEPTION_UPDATE");
		}
	}
}