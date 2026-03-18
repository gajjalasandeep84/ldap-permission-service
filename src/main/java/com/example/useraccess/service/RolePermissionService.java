package com.example.useraccess.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.useraccess.repository.PermissionRepositoryCustom;

@Service
public class RolePermissionService {

	private final PermissionRepositoryCustom permissionRepository;

	public RolePermissionService(PermissionRepositoryCustom permissionRepository) {
		this.permissionRepository = permissionRepository;
	}

	@Cacheable(value = "rolePermissions", key = "#env + ':' + #roleName")
	public List<String> getPermissionsForRole(String env, String roleName) {
		return permissionRepository.getPermissionsForRole(roleName);
	}
}