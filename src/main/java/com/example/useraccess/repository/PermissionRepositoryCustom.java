package com.example.useraccess.repository;

import java.util.List;

public interface PermissionRepositoryCustom {

	List<String> getPermissionsForRole(String roleName);

	List<String> getUserPermissions(String userId);
}