package com.example.useraccess.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.config.CacheNames;
import com.example.useraccess.repository.UserPermissionRepository;

@Service
public class UserPermissionServiceImpl implements UserPermissionService {

	private final UserPermissionRepository userPermissionRepository;

	public UserPermissionServiceImpl(UserPermissionRepository userPermissionRepository) {
		this.userPermissionRepository = userPermissionRepository;
	}

	@Override
	@Cacheable(
		    value = CacheNames.USER_DIRECT_PERMISSIONS,
		    key = "#userId"
		)
	public List<String> getDirectPermissions(String userId) {
		if (userId == null || userId.trim().isBlank()) {
			return List.of();
		}

		return userPermissionRepository.getDirectPermissions(userId.trim());
	}

	@Override
	@Cacheable(value = "userDirectPermissionCount", key = "#userId")
	public int getDirectPermissionCount(String userId) {
		if (userId == null || userId.trim().isBlank()) {
			return 0;
		}

		return userPermissionRepository.getDirectPermissionCount(userId.trim());
	}
}