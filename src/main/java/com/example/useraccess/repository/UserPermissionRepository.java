package com.example.useraccess.repository;

import java.util.List;

public interface UserPermissionRepository {

	List<String> getDirectPermissions(String userId);

	int getDirectPermissionCount(String userId);
}