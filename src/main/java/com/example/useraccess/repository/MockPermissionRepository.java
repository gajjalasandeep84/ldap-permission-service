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

	public List<String> getPermissionsForRole(String roleName) {

		Set<String> permissions = new LinkedHashSet<>();

		String role = roleName == null ? "" : roleName.trim().toUpperCase();

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

		if (role.contains("DOCS")) {

			permissions.add("DOCS_READ");

			permissions.add("DOCS_UPDATE");

		}

		if (role.contains("APPEALS")) {

			permissions.add("APPEALS_READ");

			permissions.add("APPEALS_UPDATE");

		}

		return new ArrayList<>(permissions);

	}

	@Override

	public List<String> getUserPermissions(String userId) {

		Set<String> permissions = new LinkedHashSet<>();

		String user = userId == null ? "" : userId.trim().toUpperCase();

		if (!user.isBlank()) {

			permissions.add("PROFILE_READ");

		}

		if (user.contains("AUTO")) {

			permissions.add("BATCH_MONITOR_READ");

		}

		if (user.endsWith("2")) {

			permissions.add("EXCEPTION_UPDATE");

		}

		if (user.contains("ADMIN")) {

			permissions.add("USER_OVERRIDE_UPDATE");

		}

		return new ArrayList<>(permissions);

	}

}
