package com.example.useraccess.service;

import java.util.List;

import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.SummaryResponse;

public interface PermissionService {
	SummaryResponse getSummary(String env, String userId, List<String> roles);

	PermissionResponse getPermissions(String env, String userId, List<String> roles);
}