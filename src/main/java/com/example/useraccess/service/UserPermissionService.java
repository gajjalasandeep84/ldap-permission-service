package com.example.useraccess.service;

import java.util.List;

public interface UserPermissionService {

	List<String> getDirectPermissions(String userId);

	int getDirectPermissionCount(String userId);
}