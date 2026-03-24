package com.example.useraccess.repository;

import java.util.List;

public interface RolePermissionRepository {

	List<String> getPermissionsByRoles(List<String> roles);

	int getPermissionCountByRoles(List<String> roles);
}