package com.example.useraccess.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.repository.PermissionRepositoryCustom;

@Service
public class UserPermissionService {

	private final PermissionRepositoryCustom permissionRepository;

	public UserPermissionService(PermissionRepositoryCustom permissionRepository) {
		this.permissionRepository = permissionRepository;
	}

	@Cacheable(value = "userPermissions", key = "#env + ':' + #userId")
	public List<String> getUserPermissions(String env, String userId) {
		return permissionRepository.getUserPermissions(userId);
	}
}