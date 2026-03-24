package com.example.useraccess.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.useraccess.dto.PermissionRequest;
import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.SummaryResponse;
import com.example.useraccess.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Access", description = "User Summary and Permissions APIs")
public class UserAccessController {

	private final PermissionService permissionService;

	public UserAccessController(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	@PostMapping("/summary")
	@Operation(summary = "Get user summary", description = "Returns permission count for user based on roles and user mappings")
	public ResponseEntity<SummaryResponse> getSummary(@RequestBody PermissionRequest request) {
		return ResponseEntity
				.ok(permissionService.getSummary(request.getEnv(), request.getUserId(), request.getRoles()));
	}

	@PostMapping("/permissions")
	@Operation(summary = "Get user permissions", description = "Returns full permission list derived from roles and user-specific mappings")
	public ResponseEntity<PermissionResponse> getPermissions(@RequestBody PermissionRequest request) {
		return ResponseEntity
				.ok(permissionService.getPermissions(request.getEnv(), request.getUserId(), request.getRoles()));
	}
}