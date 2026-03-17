package com.example.useraccess.repository;

import java.util.ArrayList;
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

		List<String> safeRoles = roles == null ? List.of() : roles;

		for (String role : safeRoles) {
			if (role == null) {
				continue;
			}

			String normalizedRole = role.trim().toUpperCase();

			if (normalizedRole.contains("CSR")) {
				permissions.add("CASE_READ");
				permissions.add("MEMBER_READ");
				permissions.add("DOCUMENT_READ");
			}

			if (normalizedRole.contains("PLANADMIN")) {
				permissions.add("PLAN_READ");
				permissions.add("PLAN_UPDATE");
				permissions.add("MEMBER_UPDATE");
			}

			if (normalizedRole.contains("SHOP")) {
				permissions.add("SHOP_READ");
			}

			if (normalizedRole.contains("VENDOR")) {
				permissions.add("VENDOR_READ");
				permissions.add("VENDOR_UPDATE");
			}

			if (normalizedRole.contains("SPLUNK")) {
				permissions.add("AUDIT_READ");
			}

			if (normalizedRole.contains("ADMIN")) {
				permissions.add("USER_READ");
				permissions.add("USER_UPDATE");
				permissions.add("DOCUMENT_DELETE");
			}
		}

		String safeUserId = userId == null ? "" : userId.trim().toUpperCase();

		if (!safeUserId.isBlank()) {
			permissions.add("PROFILE_READ");
		}

		if (safeUserId.contains("AUTO")) {
			permissions.add("BATCH_MONITOR_READ");
		}

		if (safeUserId.endsWith("2")) {
			permissions.add("EXCEPTION_UPDATE");
		}

		return new ArrayList<>(permissions);
	}
}