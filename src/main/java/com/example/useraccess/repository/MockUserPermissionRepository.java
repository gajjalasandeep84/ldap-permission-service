package com.example.useraccess.repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mock")
public class MockUserPermissionRepository implements UserPermissionRepository {

	@Override
	public List<String> getDirectPermissions(String userId) {
		Set<String> permissions = new LinkedHashSet<>();

		String normalizedUserId = normalizeUserId(userId);

		if (!normalizedUserId.isBlank()) {
			permissions.add("PROFILE_READ");
		}

		if (normalizedUserId.contains("AUTO")) {
			permissions.add("BATCH_MONITOR_READ");
		}

		if (normalizedUserId.endsWith("2")) {
			permissions.add("EXCEPTION_UPDATE");
		}

		return permissions.stream().sorted().toList();
	}

	@Override
	public int getDirectPermissionCount(String userId) {
		return getDirectPermissions(userId).size();
	}

	private String normalizeUserId(String userId) {
		return userId == null ? "" : userId.trim().toUpperCase();
	}
}